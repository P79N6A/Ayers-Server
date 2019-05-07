package cn.leancloud.platform.modules;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * for example:
 *    user collection: { "create" : { "*" : true },
 *                        "find" : { "*" : false },
 *                        "get" : { "*" : true },
 *                        "update" : { "*" : true },
 *                        "delete" : { "*" : false },
 *                        "add_fields" : { "*" : true } }
 *    one collection:  { "create" : { "*" : true, "users" : [ ], "roles" : [ ] },
 *                       "find" : { "*" : true, "users" : [ ], "roles" : [ ] },
 *                       "get" : { "*" : true },
 *                       "update" : { "*" : true },
 *                       "delete" : { "users" : [ "5cd102ced3761600696fce99" ], "roles" : [ ] },
 *                       "add_fields" : { "onlySignInUsers" : true, "users" : [ ], "roles" : [ ] } }
 *    one collection:  { "create" : { "*" : true, "users" : [ ], "roles" : [ ] },
 *                      "find" : { "*" : true, "users" : [ ], "roles" : [ ] },
 *                      "get" : { "*" : true },
 *                      "update" : { "users" : [ ], "roles" : [ "Administrator" ] },
 *                      "delete" : { "users" : "5cd102ced3761600696fce99", "roles" : "" },
 *                      "add_fields" : { "onlySignInUsers" : true, "users" : [ ], "roles" : [ ] } }
 */
public class ClassPermission {
  private static final String KEY_PUBLIC = "*";
  private static final String KEY_USERS = "users";
  private static final String KEY_ROLES = "roles";
  private static final String KEY_SIGNIN_USERS = "onlySignInUsers";

  public enum OP {
    CREATE("create"),
    FIND("find"),
    GET("get"),
    UPDATE("update"),
    DELETE("delete"),
    ADD_FIELDS("add_fields");

    String name;
    OP(String name) {
      this.name = name;
    }
    String getName() {
      return this.name;
    }
  }

  private JsonObject permissions = null;

  public ClassPermission(JsonObject json) {
    this.permissions = json;
  }

  public boolean checkOperation(OP op, String currentUser, List<String> userRoles) {
    if (null == this.permissions) {
      return false;
    }
    JsonObject opPermission = this.permissions.getJsonObject(op.getName());
    if (null == opPermission) {
      // invalid op
      return false;
    }
    boolean publicEnable = opPermission.getBoolean(KEY_PUBLIC, false);
    if (publicEnable) {
      // public enable
      return true;
    }
    if (StringUtils.isEmpty(currentUser)) {
      // user not signin
      return false;
    }
    boolean signinUserEnable = opPermission.getBoolean(KEY_SIGNIN_USERS, false);
    if (signinUserEnable) {
      // enable for all signin users.
      return true;
    }
    Object users = opPermission.getValue(KEY_USERS);
    if (null != users && users instanceof String) {
      // users should be json array, just compatible with existed data.
      if (currentUser.equals(users)) {
        return true;
      }
    } else if (null != users && users instanceof JsonArray) {
      JsonArray userArray = (JsonArray) users;
      if (userArray.stream().collect(Collectors.toSet()).contains(currentUser)) {
        return true;
      }
    }

    if (null != userRoles && userRoles.size() > 0) {
      Object roles = opPermission.getValue(KEY_ROLES);
      if (null != roles && roles instanceof String) {
        // roles should be json array, just compatible with existed data.
        return userRoles.stream().anyMatch(v -> v.equals(roles));
      } else if (null != roles && roles instanceof JsonArray) {
        JsonArray roleArray = (JsonArray) roles;
        Set<Object> roleSet = null == roleArray? new HashSet<>() : roleArray.stream().collect(Collectors.toSet());
        return userRoles.stream().anyMatch(v -> roleSet.contains(v));
      }
    }
    return false;
  }
}
