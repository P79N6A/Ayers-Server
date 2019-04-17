package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;

public class BatchRequest {
  private String method;
  private String path;
  private JsonObject body;
  private String clazz;
  private String objectId;
  public BatchRequest(String method, String path, String clazz, String objectId, JsonObject body) {
    this.method = method;
    this.path = path;
    this.clazz = clazz;
    this.objectId = objectId;
    this.body = body;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public JsonObject getBody() {
    return body;
  }

  public String getClazz() {
    return clazz;
  }

  public String getObjectId() {
    return objectId;
  }
}
