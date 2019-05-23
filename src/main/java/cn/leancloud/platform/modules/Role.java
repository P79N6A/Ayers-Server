package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import io.vertx.core.json.JsonObject;

import static cn.leancloud.platform.modules.Relation.RELATEDTO_QUERY_FORMAT;

public class Role extends LeanObject {
  public static final String BUILTIN_ATTR_NAME = "name";
  public static final String BUILTIN_ATTR_RELATION_USER = "users";
  public static final String BUILTIN_ATTR_RELATION_ROLE = "roles";

  public static JsonObject getUserRelationQuery(String userObjectId) {
    String relationQuery = String.format(RELATEDTO_QUERY_FORMAT, Constraints.USER_CLASS, userObjectId, BUILTIN_ATTR_RELATION_USER);
    return new JsonObject(relationQuery);
  }

  public static JsonObject getRoleRelationQuery(String roleObjectId) {
    String relationQuery = String.format(RELATEDTO_QUERY_FORMAT, Constraints.ROLE_CLASS, roleObjectId, BUILTIN_ATTR_RELATION_ROLE);
    return new JsonObject(relationQuery);
  }
}
