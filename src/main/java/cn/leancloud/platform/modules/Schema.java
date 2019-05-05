package cn.leancloud.platform.modules;

import cn.leancloud.platform.modules.type.LeanGeoPoint;
import cn.leancloud.platform.modules.type.LeanPointer;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  public String findGeoPointAttr() {
    return lookupGeoPoint(null, this);
  }

  public List<String> findAuthDataIndex() {
    JsonObject authDataSchema = getJsonObject("authData");
    JsonObject schema = null == authDataSchema? null : authDataSchema.getJsonObject("schema");
    if (null == authDataSchema || null == schema) {
      return new ArrayList<>();
    }
    List<String> result = schema.stream().map(entry -> {
      String platform = entry.getKey();
      JsonObject platformSchema = ((JsonObject) entry.getValue()).getJsonObject("schema");
      String idAttr = platformSchema.fieldNames().stream().filter(s -> s.endsWith("id") && !s.equals("unionid")).findFirst().get();
      return "authData." + platform + "." + idAttr;
    }).collect(Collectors.toList());
    return result;
  }

  private static String lookupGeoPoint(String base, JsonObject object) {
    if (null == object) {
      return null;
    }
    boolean foundFirst = false;
    String firstAttrName = null;
    for (Map.Entry<String, Object> entry : object.getMap().entrySet()) {
      String attr = entry.getKey();
      JsonObject typeJson = (JsonObject) entry.getValue();
      String typeString = typeJson.getString("type", "");
      if (LeanGeoPoint.class.getName().equals(typeString)) {
        firstAttrName = attr;
        foundFirst = true;
        break;
      } else if (JsonObject.class.getName().equals(typeString)) {
        JsonObject subJson = typeJson.getJsonObject("schema");
        String tmp = lookupGeoPoint(attr, subJson);
        if (StringUtils.notEmpty(tmp)) {
          firstAttrName = tmp;
          foundFirst = true;
          break;
        }
      }
    }
    String result = null;
    if (foundFirst) {
      if (StringUtils.isEmpty(base)) {
        result = firstAttrName;
      } else {
        result = base + "." + firstAttrName;
      }
    }
    return result;
  }

  public CompatResult compatibleWith(Schema other) throws ConsistencyViolationException {
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
        if (Object.class.getName().equalsIgnoreCase(otherType)) {
          // pass all Object type
          continue;
        } else if (!myType.equals(otherType)) {
          throw new ConsistencyViolationException("data consistency violated. key:" + key
                  + ", expectedType:" + otherType + ", actualType:" + myType);
        } else if (LeanPointer.class.getName().equals(myType)) {
          if (!value.getString("reference", "").equals(otherValue.getString("reference"))) {
            throw new ConsistencyViolationException("data consistency violated. key:" + key
                    + ", expectedPoint2Type:" + otherValue.getString("reference")
                    + ", actualPoint2Type:" + value.getString("reference"));
          }
        } else if (JsonObject.class.getName().equals(myType)) {
          Schema mySchema = new Schema(value.getJsonObject("schema"));
          Schema otherSchema = new Schema(otherValue.getJsonObject("schema"));
          CompatResult tmpResult = mySchema.compatibleWith(otherSchema);
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
