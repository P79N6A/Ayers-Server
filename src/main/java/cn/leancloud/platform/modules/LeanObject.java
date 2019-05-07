package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.BsonTransformer;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.AbstractMap;

public class LeanObject extends JsonObject{
  protected static final Logger logger = LoggerFactory.getLogger(LeanObject.class);

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

  public static final String BUILTIN_ATTR_USERNAME = "username";
  public static final String BUILTIN_ATTR_EMAIL = "email";
  public static final String BUILTIN_ATTR_MOBILEPHONE = "mobilePhoneNumber";

  public static final String BUILTIN_ATTR_FILE_URL = "url";
  public static final String BUILTIN_ATTR_FILE_MIMETYPE = "mime_type";
  public static final String BUILTIN_ATTR_FILE_PROVIDER = "provider";
  public static final String BUILTIN_ATTR_FILE_BUCKET = "bucket";

  private String className = "";

  public static JsonObject getCurrentDate() {
    return new JsonObject().put(ATTR_NAME_TYPE, Schema.DATA_TYPE_DATE).put(ATTR_NAME_ISO, Instant.now().toString());
  }

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
      return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Number.class.getSimpleName());
    } else if (value instanceof JsonObject) {
      JsonObject newValue = (JsonObject) value;
      if (newValue.containsKey(ATTR_NAME_TYPE)) {
        String type = newValue.getString(ATTR_NAME_TYPE);
        if (Schema.DATA_TYPE_DATE.equalsIgnoreCase(type)) {
          if (StringUtils.notEmpty(newValue.getString(ATTR_NAME_ISO))) {
            return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_DATE);
          }
        } else if (Schema.DATA_TYPE_POINTER.equalsIgnoreCase(type)) {
          String className = newValue.getString(ATTR_NAME_CLASSNAME);
          if (StringUtils.notEmpty(className)) {
            return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_POINTER).put(Schema.SCHEMA_KEY_REF, className);
          }
        } else if (Schema.DATA_TYPE_GEOPOINT.equalsIgnoreCase(type)) {
          return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_GEOPOINT);
        } else if (Schema.DATA_TYPE_FILE.equalsIgnoreCase(type)) {
          return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_FILE).put("v", 2);
        }
      } else if (newValue.containsKey(ATTR_NAME_OP)) {
        String operation = newValue.getString(ATTR_NAME_OP);
        switch (operation.toLowerCase()) {
          case BsonTransformer.REST_OP_ADD:
          case BsonTransformer.REST_OP_ADD_RELATION:
          case BsonTransformer.REST_OP_ADD_UNIQUE:
          case BsonTransformer.REST_OP_REMOVE_RELATION:
          case BsonTransformer.REST_OP_REMOVE:
            return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_ARRAY);
          case BsonTransformer.REST_OP_BITAND:
          case BsonTransformer.REST_OP_BITOR:
          case BsonTransformer.REST_OP_BITXOR:
          case BsonTransformer.REST_OP_INCREMENT:
          case BsonTransformer.REST_OP_DECREMENT:
            return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_NUMBER);
          case BsonTransformer.REST_OP_DELETE:
            return null;
          default:
            break;
        }
      }
      JsonObject recurSchema = newValue.stream()
              .map(entry -> new AbstractMap.SimpleEntry(entry.getKey(), guessValueType(entry.getValue())))
              .filter(simpleEntry -> simpleEntry.getValue() != null)
              .collect(JsonFactory.toJsonObject());
      return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, Schema.DATA_TYPE_OBJECT).put(Schema.SCHEMA_KEY_SCHEMA, recurSchema);
    }
    return new JsonObject().put(Schema.SCHEMA_KEY_TYPE, value.getClass().getSimpleName());
  }

  public Schema guessSchema() {
    JsonObject result = stream().filter(entry -> null != entry.getValue()).map(entry -> {
      Object value = entry.getValue();
      JsonObject valueType = guessValueType(value);
      return new AbstractMap.SimpleEntry(entry.getKey(), valueType);
    }).filter(simpleEntry -> simpleEntry.getValue() != null).collect(JsonFactory.toJsonObject());
    return new Schema(result);
  }
}
