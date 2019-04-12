package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;

public class Transformer {
  public static JsonObject wrapQueryResult(JsonObject o) {
    if (null == o) {
      return o;
    }
    // replace _id with objectId.
    if (o.containsKey(Configure.CLASS_ATTR_MONGO_ID)) {
      o.put(Configure.CLASS_ATTR_OBJECT_ID, o.getString(Configure.CLASS_ATTR_MONGO_ID)).remove(Configure.CLASS_ATTR_MONGO_ID);
    }
    return o;
  }
}
