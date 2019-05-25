package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Relation;
import cn.leancloud.platform.modules.Role;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.utils.HandlerUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static cn.leancloud.platform.modules.Relation.BUILTIN_ATTR_RELATIONN_RELATED_ID;

/**
 * TODO(feature in future):
 *   maybe we need to cache user owing roles in memory for a short term, depends on requests sampling.
 */
public class UserRoleQueryHandler {
  private static final Logger logger = LoggerFactory.getLogger(UserRoleQueryHandler.class);

  public void queryUserRoles(DataStore dataStore, String userObjectId, Handler<AsyncResult<List<String>>> handler) {
    Objects.requireNonNull(dataStore);
    Objects.requireNonNull(userObjectId);
    Objects.requireNonNull(handler);

    String userRelationTable = Role.getUserRelationTable();
    JsonObject query = Role.getRelatedByUserQuery(userObjectId);
    dataStore.find(userRelationTable, query, response -> {
      if (response.failed()) {
        logger.warn("failed to query relation. cause:" + response.cause().getMessage());
        handler.handle(response.map(jsonObjects -> null));
        return;
      } else {
        if (null == response.result()) {
          logger.debug("response is null.");
          handler.handle(HandlerUtils.wrapActualResult(new ArrayList<>()));
          return;
        }
        List<String> roleIds = response.result().stream().map(object -> object.getJsonObject(Relation.BUILTIN_ATTR_RELATION_OWNING_ID).getString(LeanObject.ATTR_NAME_OBJECTID))
                .collect(Collectors.toList());
        if (roleIds.size() < 1) {
          logger.debug("response is empty.");
          handler.handle(HandlerUtils.wrapActualResult(new ArrayList<>()));
          return;
        }
        JsonObject roleQuery = new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID, new JsonObject().put("$in", roleIds));
        dataStore.find(Constraints.ROLE_CLASS, roleQuery, secondQueryRes -> {
          if (secondQueryRes.failed()) {
            logger.warn("failed to query _Role. cause: " + secondQueryRes.cause().getMessage());
            handler.handle(secondQueryRes.map(jsonObjects -> null));
          } else {
            if (null == secondQueryRes.result()) {
              handler.handle(HandlerUtils.wrapActualResult(new ArrayList<>()));
              return;
            }
            List<String> directRoles = secondQueryRes.result().stream()
                    .map(object -> object.getString(Role.BUILTIN_ATTR_NAME)).collect(Collectors.toList());
            if (directRoles.size() < 1) {
              handler.handle(HandlerUtils.wrapActualResult(new ArrayList<>()));
              return;
            }
            List<String> directRoleObjectIds = secondQueryRes.result().stream()
                    .map(object -> object.getString(LeanObject.ATTR_NAME_OBJECTID)).collect(Collectors.toList());
            JsonObject relatedByRoleQuery = Role.getRelatedRoleQuery(directRoleObjectIds);
            String roleRelationTable = Role.getRoleRelationTable();
            dataStore.find(roleRelationTable, relatedByRoleQuery, recurResponse -> {
              if (recurResponse.failed()) {
                logger.warn("failed to query parent role from relation table. cause: " + recurResponse.cause().getMessage());
                handler.handle(HandlerUtils.wrapActualResult(directRoles));
                return;
              } else {
                if (null == recurResponse.result()) {
                  handler.handle(HandlerUtils.wrapActualResult(directRoles));
                  return;
                }
                List<String> parentRoleObjectIds = recurResponse.result().stream().map(object ->
                        object.getJsonObject(BUILTIN_ATTR_RELATIONN_RELATED_ID).getString(LeanObject.ATTR_NAME_OBJECTID))
                        .collect(Collectors.toList());
                if (parentRoleObjectIds.size() < 1) {
                  handler.handle(HandlerUtils.wrapActualResult(directRoles));
                  return;
                }
                JsonObject parentRoleQuery = new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID,
                        new JsonObject().put("$in", parentRoleObjectIds));
                dataStore.find(Constraints.ROLE_CLASS, parentRoleQuery, parentResponse -> {
                  if (parentResponse.failed()) {
                    logger.warn("failed to query parent role from role table. cause: " + parentResponse.cause().getMessage());
                    handler.handle(HandlerUtils.wrapActualResult(directRoles));
                    return;
                  }
                  parentResponse.result().stream().forEach(object -> directRoles.add(object.getString(Role.BUILTIN_ATTR_NAME)));
                  handler.handle(HandlerUtils.wrapActualResult(directRoles));
                  return;
                });
              }
            });
          }
        });
      }
    });
  }
}
