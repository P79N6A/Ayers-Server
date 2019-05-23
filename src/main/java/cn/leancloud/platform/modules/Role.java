package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.utils.JsonFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static cn.leancloud.platform.modules.Relation.BUILTIN_ATTR_RELATIONN_RELATED_ID;
import static cn.leancloud.platform.modules.Relation.BUILTIN_ATTR_RELATION_OWNING_ID;
import static cn.leancloud.platform.modules.Relation.RELATEDTO_QUERY_FORMAT;

public class Role extends LeanObject {
  public static final String BUILTIN_ATTR_NAME = "name";
  public static final String BUILTIN_ATTR_RELATION_USER = "users";
  public static final String BUILTIN_ATTR_RELATION_ROLE = "roles";

  public static String getUserRelationTable() {
    return Constraints.getRelationTable(Constraints.ROLE_CLASS, Constraints.USER_CLASS, BUILTIN_ATTR_RELATION_USER);
  }

  public static String getRoleRelationTable() {
    return Constraints.getRelationTable(Constraints.ROLE_CLASS, Constraints.ROLE_CLASS, BUILTIN_ATTR_RELATION_ROLE);
  }

  public static JsonObject getRelatedByUserQuery(String userObjectId) {
    JsonObject userPointer = new JsonObject().put(LeanObject.ATTR_NAME_TYPE, Schema.DATA_TYPE_POINTER)
            .put(LeanObject.ATTR_NAME_CLASSNAME, Constraints.USER_CLASS)
            .put(LeanObject.ATTR_NAME_OBJECTID, userObjectId);
    JsonObject query = new JsonObject().put(BUILTIN_ATTR_RELATIONN_RELATED_ID, userPointer);
    return query;
  }

  public static JsonObject getRelatedRoleQuery(List<String> roleObjectIds) {
    JsonArray rolePointers = roleObjectIds.stream().map(objectId -> {
      return new JsonObject().put(LeanObject.ATTR_NAME_TYPE, Schema.DATA_TYPE_POINTER)
              .put(LeanObject.ATTR_NAME_CLASSNAME, Constraints.ROLE_CLASS)
              .put(LeanObject.ATTR_NAME_OBJECTID, objectId);
    }).collect(JsonFactory.toJsonArray());
    JsonObject query = new JsonObject().put(BUILTIN_ATTR_RELATION_OWNING_ID, new JsonObject().put("$in", rolePointers));
    return query;
  }

  public static JsonObject getUserRelationQuery(String userObjectId) {
    String relationQuery = String.format(RELATEDTO_QUERY_FORMAT, Constraints.USER_CLASS, userObjectId, BUILTIN_ATTR_RELATION_USER);
    return new JsonObject(relationQuery);
  }

  public static JsonObject getRoleRelationQuery(String roleObjectId) {
    String relationQuery = String.format(RELATEDTO_QUERY_FORMAT, Constraints.ROLE_CLASS, roleObjectId, BUILTIN_ATTR_RELATION_ROLE);
    return new JsonObject(relationQuery);
  }
}
