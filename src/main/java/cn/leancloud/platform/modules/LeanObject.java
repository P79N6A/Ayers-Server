package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import io.vertx.core.json.JsonObject;

public class LeanObject extends JsonObject{
  private String className = "";

  public LeanObject(String className, JsonObject value) {
    this(value);
    this.className = className;
  }

  public LeanObject() {
    super();
  }

  public LeanObject(JsonObject value) {
    super(value.getMap());
  }
  public String getObjectId() {
    return getString(Constraints.CLASS_ATTR_OBJECT_ID);
  }

  public String getUpdatedAt() {
    return getString(Constraints.CLASS_ATTR_UPDATED_TS);
  }

  public String getCreatedAt() {
    return getString(Constraints.CLASS_ATTR_CREATED_TS);
  }

  public String getClassName() {
    return this.className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Schema getSchema() {
    return null;
  }
}
