package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import io.vertx.core.json.JsonObject;

public class User extends LeanObject {
  public User(JsonObject value) {
    super(Constraints.USER_CLASS, value);
  }
}
