package cn.leancloud.platform.modules;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

import static cn.leancloud.platform.modules.Schema.CompatResult.*;

public class Schema extends JsonObject {
  public static final String DATA_TYPE_OBJECT = "Object";
  public static final String DATA_TYPE_ARRAY = "Array";
  public static final String DATA_TYPE_POINTER = "Pointer";
  public static final String DATA_TYPE_GEOPOINT = "GeoPoint";
  public static final String DATA_TYPE_FILE = "File";
  public static final String DATA_TYPE_DATE = "Date";
  public static final String DATA_TYPE_ANY = "Any";
  public static final String DATA_TYPE_NUMBER = "Number";
  public static final String DATA_TYPE_STRING = "String";
  public static final String DATA_TYPE_BOOLEAN = "Boolean";

  public static final String SCHEMA_KEY_TYPE = "type";
  public static final String SCHEMA_KEY_SCHEMA = "schema";
  public static final String SCHEMA_KEY_REF = "reference";

  public enum CompatResult {
    OVER_MATCHED_ON_FIELD_LAYER, // overmatched on first layer, for add_field operation.
    OVER_MATCHED, // overmatched for Object field.
    MATCHED,      // all matched.
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
      if (null == entry.getValue() || !(entry.getValue() instanceof JsonObject)) {
        return null;
      }
      JsonObject platformSchema = ((JsonObject) entry.getValue()).getJsonObject("schema");
      if (null == platformSchema) {
        return null;
      }
      Optional<String> idOption = platformSchema.fieldNames().stream().filter(s -> s.endsWith("id") && !s.equals("unionid")).findFirst();
      if (!idOption.isPresent()) {
        return null;
      }
      String idAttr = idOption.get();
      return "authData." + platform + "." + idAttr;
    }).filter(StringUtils::notEmpty).collect(Collectors.toList());
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
      String typeString = typeJson.getString(SCHEMA_KEY_TYPE, "");
      if (DATA_TYPE_GEOPOINT.equals(typeString)) {
        firstAttrName = attr;
        foundFirst = true;
        break;
      } else if (DATA_TYPE_OBJECT.equals(typeString)) {
        JsonObject subJson = typeJson.getJsonObject(SCHEMA_KEY_SCHEMA);
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

  private CompatResult compatibilityTest(Schema other, boolean first) throws ConsistencyViolationException {
    if (null == other) {
      return first ? OVER_MATCHED_ON_FIELD_LAYER : OVER_MATCHED;
    }
    boolean foundNewAttr = false;
    for (String key : this.fieldNames()) {
      if (!other.containsKey(key)) {
        // new field found.
        foundNewAttr = true;
        continue;
      }
      JsonObject value = this.getJsonObject(key);
      JsonObject otherValue = other.getJsonObject(key);
      if (null != value && null != otherValue) {
        String myType = value.getString(SCHEMA_KEY_TYPE);
        String otherType = otherValue.getString(SCHEMA_KEY_TYPE);
        if (DATA_TYPE_ANY.equalsIgnoreCase(otherType)) {
          // pass ANY type
          continue;
        } else if (!myType.equals(otherType)) {
          throw new ConsistencyViolationException("data consistency violated. key:" + key
                  + ", expectedType:" + otherType + ", actualType:" + myType);
        } else if (DATA_TYPE_POINTER.equals(myType)) {
          if (!value.getString(SCHEMA_KEY_REF, "").equals(otherValue.getString(SCHEMA_KEY_REF))) {
            throw new ConsistencyViolationException("data consistency violated. key:" + key
                    + ", expectedPoint2Type:" + otherValue.getString(SCHEMA_KEY_REF)
                    + ", actualPoint2Type:" + value.getString(SCHEMA_KEY_REF));
          }
        } else if (DATA_TYPE_OBJECT.equals(myType)) {
          Schema mySchema = new Schema(value.getJsonObject(SCHEMA_KEY_SCHEMA));
          Schema otherSchema = new Schema(otherValue.getJsonObject(SCHEMA_KEY_SCHEMA));
          CompatResult tmpResult = mySchema.compatibilityTest(otherSchema, false);
          if (tmpResult == OVER_MATCHED) {
            foundNewAttr = true;
          }
        }
      } else {
        // ignore invalid schema.
      }
    }
    if (!foundNewAttr) {
      return MATCHED;
    }
    if (!first) {
      return OVER_MATCHED;
    }
    boolean hasNewFields = other.fieldNames().containsAll(this.fieldNames());
    if (!hasNewFields) {
      return OVER_MATCHED_ON_FIELD_LAYER;
    } else {
      return OVER_MATCHED;
    }
  }

  public CompatResult compatibleWith(Schema other) throws ConsistencyViolationException {
    return compatibilityTest(other, true);
  }
}
