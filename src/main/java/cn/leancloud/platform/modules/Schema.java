package cn.leancloud.platform.modules;

import io.vertx.core.json.JsonObject;

public class Schema extends JsonObject {
  public int compatiableWith(Schema other) {
    return 0;
  }
}
