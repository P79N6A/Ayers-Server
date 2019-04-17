package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.StringUtils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ACL {
  private static final String PUBLIC_KEY = "*";
  private static final String ROLE_PREFIX = "role:";
  private static final String KEY_READ_PERMISSION = "read";
  private static final String KEY_WRITE_PERMISSION = "write";

  private static class Permissions extends HashMap<String, Boolean> {

    Permissions(boolean read, boolean write) {
      super();
      if (read) {
        put(KEY_READ_PERMISSION, read);
      }
      if (write) {
        put(KEY_WRITE_PERMISSION, write);
      }
    }

    Permissions(Map<String, Object> map) {
      super();
      if (null == map) {
        return;
      }
      Object readValue = map.get(KEY_READ_PERMISSION);
      Object writeValue = map.get(KEY_WRITE_PERMISSION);
      if (null == readValue || !(readValue instanceof Boolean)) {
        put(KEY_READ_PERMISSION, false);
      } else {
        put(KEY_READ_PERMISSION, (Boolean)readValue);
      }
      if (null == writeValue || !(writeValue instanceof Boolean)) {
        put(KEY_WRITE_PERMISSION, false);
      } else {
        put(KEY_WRITE_PERMISSION, (Boolean)writeValue);
      }
    }

    Permissions(Permissions permissions) {
      super();
      if (null == permissions) {
        return;
      }
      if (permissions.getReadPermission()) {
        put(KEY_READ_PERMISSION, true);
      }
      if (permissions.getWritePermission()) {
        put(KEY_WRITE_PERMISSION, true);
      }
    }

    boolean getReadPermission() {
      return get(KEY_READ_PERMISSION);
    }

    boolean getWritePermission() {
      return get(KEY_WRITE_PERMISSION);
    }
  }

  private final Map<String, Permissions> permissionsById = new HashMap<>();

  public ACL(JsonObject object) {
    if (null != object) {
      object.stream().forEach(entry -> {
        String key = entry.getKey();
        Object v = entry.getValue();
        if (v instanceof HashMap) {
          permissionsById.put(key, new Permissions((HashMap<String, Object>) v));
        } else if (v instanceof JsonObject) {
          permissionsById.put(key, new Permissions(((JsonObject) v).getMap()));
        }
      });
    }
  }

  public ACL() {
  }

  public ACL(String data) {
    this(new JsonObject(data));
  }

  public ACL(ACL other) {
  }

  public static ACL publicRWInstance() {
    ACL ret = new ACL();
    ret.setPublicWriteAccess(true);
    ret.setPublicReadAccess(true);
    return ret;
  }

  public Map<String, Permissions> getPermissionsById() {
    return this.permissionsById;
  }

  public String toJsonString() {
    return Json.encode(this.permissionsById);
  }

  public JsonObject toJson() {
    JsonObject result = new JsonObject();
    result.getMap().putAll(this.permissionsById);
    return result;
  }

  private ACL setPermissionsIfNonEmpty(String userId, boolean readPermission, boolean writePermission) {
    if (!(readPermission || writePermission)) {
      permissionsById.remove(userId);
    } else {
      permissionsById.put(userId, new Permissions(readPermission, writePermission));
    }
    return this;
  }

  /**
   * Set whether the given user id is allowed to read this object.
   */
  public ACL setReadAccess(String userId, boolean allowed) {
    if (StringUtils.isEmpty(userId)) {
      throw new IllegalArgumentException("cannot setRead/WriteAccess for null userId");
    }
    boolean writePermission = getWriteAccess(userId);
    return setPermissionsIfNonEmpty(userId, allowed, writePermission);
  }

  /**
   * Get whether the given user id is *explicitly* allowed to read this object. Even if this returns
   * {@code false}, the user may still be able to access it if getPublicReadAccess returns
   * {@code true} or a role  that the user belongs to has read access.
   */
  public boolean getReadAccess(String userId) {
    if (StringUtils.isEmpty(userId)) {
      return false;
    }
    Permissions permissions = permissionsById.get(userId);
    return permissions != null && permissions.getReadPermission();
  }

  /**
   * Get whether the given user id is *explicitly* allowed to write this object. Even if this
   * returns {@code false}, the user may still be able to write it if getPublicWriteAccess returns
   * {@code true} or a role that the user belongs to has write access.
   */
  public boolean getWriteAccess(String userId) {
    if (StringUtils.isEmpty(userId)) {
      return false;
    }
    Permissions permissions = permissionsById.get(userId);
    return permissions != null && permissions.getWritePermission();
  }

  /**
   * Set whether the given user id is allowed to write this object.
   */
  public ACL setWriteAccess(String userId, boolean allowed) {
    if (StringUtils.isEmpty(userId)) {
      throw new IllegalArgumentException("cannot setRead/WriteAccess for null userId");
    }
    boolean readPermission = getReadAccess(userId);
    return setPermissionsIfNonEmpty(userId, readPermission, allowed);
  }

  /**
   * Set whether the public is allowed to read this object.
   */
  public ACL setPublicReadAccess(boolean allowed) {
    return setReadAccess(PUBLIC_KEY, allowed);
  }

  /**
   * Get whether the public is allowed to read this object.
   */
  public boolean getPublicReadAccess() {
    return getReadAccess(PUBLIC_KEY);
  }

  /**
   * Set whether the public is allowed to write this object.
   */
  public ACL setPublicWriteAccess(boolean allowed) {
    return setWriteAccess(PUBLIC_KEY, allowed);
  }

  /**
   * Set whether the public is allowed to write this object.
   */
  public boolean getPublicWriteAccess() {
    return getWriteAccess(PUBLIC_KEY);
  }


  public ACL setRoleReadAccess(String role, boolean allowed) {
    if (StringUtils.isEmpty(role)) {
      throw new IllegalArgumentException("cannot setRead/WriteAccess to a empty role");
    }
    return setReadAccess(ROLE_PREFIX + role, allowed);
  }

  public boolean getRoleReadAccess(String role) {
    if (StringUtils.isEmpty(role)) {
      return false;
    }
    return getReadAccess(ROLE_PREFIX + role);
  }

  public ACL setRoleWriteAccess(String role, boolean allowed) {
    if (StringUtils.isEmpty(role)) {
      throw new IllegalArgumentException("cannot setRead/WriteAccess to a empty role");
    }
    return setWriteAccess(ROLE_PREFIX + role, allowed);
  }

  public boolean getRoleWriteAccess(String role) {
    if (StringUtils.isEmpty(role)) {
      return false;
    }
    return getWriteAccess(ROLE_PREFIX + role);
  }

}
