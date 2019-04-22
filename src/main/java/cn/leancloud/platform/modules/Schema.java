package cn.leancloud.platform.modules;

import cn.leancloud.platform.modules.type.LeanPointer;
import io.vertx.core.json.JsonObject;

import static cn.leancloud.platform.modules.Schema.CompatResult.*;

public class Schema extends JsonObject {
  public enum CompatResult {
    OVER_MATCHED,
    FULLY_MATCHED,
    NOT_MATCHED
  }

  public Schema(JsonObject object) {
    super();
    if (null != object) {
      mergeIn(object);
    }
  }

  /**
   *
   * @param other
   * @return 1  -
   *         0  -
   *         -1 -
   */
  public CompatResult compatiableWith(Schema other) {
    if (null == other) {
      return OVER_MATCHED;
    }
    boolean foundNewAttr = false;
    for (String key : this.fieldNames()) {
      if (!other.containsKey(key)) {
        foundNewAttr = true;
        continue;
      }
      JsonObject value = this.getJsonObject(key);
      JsonObject otherValue = other.getJsonObject(key);
      if (null != value && otherValue != null) {
        String myType = value.getString("type");
        String otherType = otherValue.getString("type");
        if (!myType.equals(otherType)) {
          return NOT_MATCHED;
        } else if (LeanPointer.class.getName().equals(myType)) {
          if (!value.getString("reference", "").equals(otherValue.getString("reference"))) {
            return NOT_MATCHED;
          }
        } else if (JsonObject.class.getName().equals(myType)) {
          Schema mySchema = new Schema(value.getJsonObject("schema"));
          Schema otherSchema = new Schema(otherValue.getJsonObject("schema"));
          CompatResult tmpResult = mySchema.compatiableWith(otherSchema);
          if (tmpResult == NOT_MATCHED) {
            return NOT_MATCHED;
          } else if (tmpResult == OVER_MATCHED) {
            foundNewAttr = true;
          }
        }
      }
    }

    return foundNewAttr? OVER_MATCHED : FULLY_MATCHED;
  }
}
