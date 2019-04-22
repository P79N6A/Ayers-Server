package cn.leancloud.platform.modules;

import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import cn.leancloud.platform.modules.type.LeanDate;
import cn.leancloud.platform.modules.type.LeanGeoPoint;
import cn.leancloud.platform.modules.type.LeanPointer;
import io.vertx.core.json.JsonObject;

import java.util.AbstractMap;

public class LeanObject extends JsonObject{
  public static final String DATA_TYPE_DATE = "Date";
  public static final String DATA_TYPE_POINTER = "Pointer";
  public static final String DATA_TYPE_GEOPOINTER = "GeoPoint";

  public static final String ATTR_NAME_TYPE = "__type";
  public static final String ATTR_NAME_OP = "__op";
  public static final String ATTR_NAME_ISO = "iso";
  public static final String ATTR_NAME_CLASSNAME = "className";
  public static final String ATTR_NAME_OBJECTID = "objectId";
  public static final String ATTR_NAME_LATITUDE = "latitude";
  public static final String ATTR_NAME_LONGITUDE = "longitude";
  public static final String ATTR_NAME_OBJECTS = "objects";
  public static final String ATTR_NAME_CREATED_TS = "createdAt";
  public static final String ATTR_NAME_UPDATED_TS = "updatedAt";

  public static final String BUILTIN_ATTR_SESSION_TOKEN = "sessionToken";
  public static final String BUILTIN_ATTR_ACL = "ACL";
  public static final String BUILTIN_ATTR_SALT = "salt";
  public static final String BUILTIN_ATTR_PASSWORD = "password";
  public static final String BUILTIN_ATTR_EMAIL_VERIFIED = "emailVerified";
  public static final String BUILTIN_ATTR_PHONENUM_VERIFIED = "mobilePhoneVerified";

  public static final String BUILTIN_ATTR_FILE_URL = "url";
  public static final String BUILTIN_ATTR_FILE_MIMETYPE = "mime_type";
  public static final String BUILTIN_ATTR_FILE_PROVIDER = "provider";
  public static final String BUILTIN_ATTR_FILE_BUCKET = "bucket";

  private String className = "";

  public LeanObject(String className, JsonObject value) {
    this(value);
    this.className = className;
  }

  public LeanObject(String className) {
    super();
    this.className = className;
  }

  public LeanObject() {
    super();
  }

  public LeanObject(JsonObject value) {
    super(value.getMap());
  }
  public String getObjectId() {
    return getString(ATTR_NAME_OBJECTID);
  }

  public String getUpdatedAt() {
    return getString(ATTR_NAME_UPDATED_TS);
  }

  public String getCreatedAt() {
    return getString(ATTR_NAME_CREATED_TS);
  }

  public String getClassName() {
    return this.className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  private static JsonObject guessValueType(Object value) {
    if (null == value) {
      return null;
    }
    if (value instanceof Integer || value instanceof Float || value instanceof Double) {
      return new JsonObject().put("type", Number.class.getName());
    } else if (value instanceof JsonObject) {
      JsonObject newValue = (JsonObject) value;
      if (newValue.containsKey(ATTR_NAME_TYPE)) {
        String type = newValue.getString(ATTR_NAME_TYPE);
        if (DATA_TYPE_DATE.equalsIgnoreCase(type)) {
          if (StringUtils.notEmpty(newValue.getString(ATTR_NAME_ISO))) {
            return new JsonObject().put("type", LeanDate.class.getName());
          }
        } else if (DATA_TYPE_POINTER.equalsIgnoreCase(type)) {
          String className = newValue.getString(ATTR_NAME_CLASSNAME);
          if (StringUtils.notEmpty(className)) {
            return new JsonObject().put("type", LeanPointer.class.getName()).put("reference", className);
          }
        } else if (DATA_TYPE_GEOPOINTER.equalsIgnoreCase(type)) {
          return new JsonObject().put("type", LeanGeoPoint.class.getName());
        }
      }
      JsonObject recurSchema = newValue.stream()
              .map(entry -> new AbstractMap.SimpleEntry(entry.getKey(), guessValueType(entry.getValue())))
              .collect(JsonFactory.toJsonObject());
      return new JsonObject().put("type", JsonObject.class.getName()).put("schema", recurSchema);
    }
    return new JsonObject().put("type", value.getClass().getName());
  }

  public Schema guessSchema() throws ConsistencyViolationException {
    JsonObject result = stream().filter(entry -> null != entry.getValue()).map(entry -> {
      Object value = entry.getValue();
      JsonObject valueType = guessValueType(value);
      return new AbstractMap.SimpleEntry(entry.getKey(), valueType);
    }).collect(JsonFactory.toJsonObject());
    return new Schema(result);
  }
}
