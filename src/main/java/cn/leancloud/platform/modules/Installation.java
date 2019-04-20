package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import io.vertx.core.json.JsonObject;

public class Installation extends LeanObject {
  public Installation(JsonObject value) {
    super(Constraints.INSTALLATION_CLASS, value);
  }
}
