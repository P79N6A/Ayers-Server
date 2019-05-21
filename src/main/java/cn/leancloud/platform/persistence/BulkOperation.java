package cn.leancloud.platform.persistence;

import io.vertx.core.json.JsonObject;

public class BulkOperation {
  public enum OpType {
    UPDATE,
    REPLACE,
    INSERT,
    DELETE;
  }

  private OpType type;
  private JsonObject document;

  public BulkOperation(JsonObject doc, OpType type) {
    this.document = doc;
    this.type = type;
  }

  public JsonObject getDocument() {
    return document;
  }

  public OpType getType() {
    return type;
  }
}
