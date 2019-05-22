package cn.leancloud.platform.persistence;

import io.vertx.core.json.JsonObject;

public class BulkOperation {
  public enum OpType {
    UPDATE,
    INSERT,
    DELETE
  }

  private OpType type;
  private JsonObject document;
  private JsonObject filter;

  public BulkOperation(JsonObject doc, OpType type) {
    this.document = doc;
    this.type = type;
  }

  public JsonObject getFilter() {
    return filter;
  }

  public void setFilter(JsonObject filter) {
    this.filter = filter;
  }

  public JsonObject getDocument() {
    return document;
  }

  public OpType getType() {
    return type;
  }
}
