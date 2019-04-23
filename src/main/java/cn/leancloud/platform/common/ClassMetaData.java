package cn.leancloud.platform.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ClassMetaData extends JsonObject{
  private static final String ATTR_NAME = "name";
  private static final String ATTR_SCHEMA = "schema";
  private static final String ATTR_INDICES = "indices";

  public ClassMetaData() {
    ;
  }

  public ClassMetaData(String name, JsonObject schema, JsonArray indices) {
    setName(name);
    setSchema(schema);
    setIndices(indices);
  }

  public String getName() {
    return this.getString(ATTR_NAME);
  }
  public void setName(String name) {
    this.put(ATTR_NAME, name);
  }

  public JsonObject getSchema() {
    return this.getJsonObject(ATTR_SCHEMA);
  }
  public void setSchema(JsonObject schema) {
    this.put(ATTR_SCHEMA, schema);
  }

  public JsonArray getIndices() {
    return this.getJsonArray(ATTR_INDICES);
  }
  public void setIndices(JsonArray array) {
    this.put(ATTR_INDICES, array);
  }
}
