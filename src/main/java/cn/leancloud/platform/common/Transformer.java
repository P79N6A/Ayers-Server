package cn.leancloud.platform.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.types.ObjectId;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Transformer {
  private static final String TYPE_DATE = "Date";
  private static final String TYPE_POINTER = "Pointer";
  private static final String TYPE_GEOPOINTER = "GeoPoint";
  private static final String DATE_FORMAT_REG = "\\d-\\d-\\d-\\dT\\d:\\d:\\d.\\dZ";
  private static final String[] ALWAYS_PROJECT_KEYS = {"_id", "createdAt", "updatedAt", "ACL"};

  private static final String REST_OP_REMOVE_RELATION = "removerelation";
  private static final String REST_OP_ADD_RELATION = "addrelation";
  private static final String REST_OP_INCREMENT = "increment";
  private static final String REST_OP_DECREMENT = "decrement";
  private static final String REST_OP_BITAND = "bitand";
  private static final String REST_OP_BITOR = "bitor";
  private static final String REST_OP_BITXOR = "bitxor";
  private static final String REST_OP_ADD = "add";
  private static final String REST_OP_DELETE = "delete";
  private static final String REST_OP_ADD_UNIQUE = "addunique";
  private static final String REST_OP_REMOVE = "remove";
  private static final String REST_OP_SETONINSERT = "setoninsert";

  private static final Map<String, String> bsonModifierMap = new HashMap<>();
  static {
    bsonModifierMap.put(REST_OP_BITAND, "and");
    bsonModifierMap.put(REST_OP_BITOR, "or");
    bsonModifierMap.put(REST_OP_BITXOR, "xor");
    bsonModifierMap.put(REST_OP_ADD_RELATION, "$each");
    bsonModifierMap.put(REST_OP_ADD, "$each");
    bsonModifierMap.put(REST_OP_ADD_UNIQUE, "$each");
  }

  public enum REQUEST_OP {
    CREATE, UPDATE, QUERY
  }

  // "location":{"__type":"GeoPoint","latitude":45.9,"longitude":76.3}
  //    to {"type":"Point","coordinates":[124.6682391,-17.8978304]}
  private static JsonObject convertBuiltinTypeUnit(JsonObject o) {
    if (null == o) {
      return o;
    }
    JsonObject result = null;
    if (o.containsKey("__type")) {
      String type = o.getString("__type");
      if (TYPE_DATE.equalsIgnoreCase(type)) {
        String isoString = o.getString("iso");
        result = new JsonObject().put("$date", isoString);
      } else if (TYPE_POINTER.equalsIgnoreCase(type)) {
        String className = o.getString("className");
        String objectId = o.getString("objectId");
        result = new JsonObject();
        result.put("$ref", className);
        result.put("$id", new ObjectId(objectId).toString());
      } else if (TYPE_GEOPOINTER.equalsIgnoreCase(type)) {
        double latitude = o.getDouble("latitude");
        double longitude = o.getDouble("longitude");
        result = new JsonObject().put("type", "Point");
        result.put("coordinates", new JsonArray(Arrays.asList(longitude, latitude)));
      }
    }
    return result;
  }

  private static JsonObject encode2BsonObject(JsonObject o, final boolean isCreateOp) throws ClassCastException {
    if (null == o) {
      return o;
    }
    JsonObject tmp = convertBuiltinTypeUnit(o);
    if (null != tmp) {
      return tmp;
    }
    JsonObject resultObject = o.stream().map(entry -> {
              Map.Entry<String, Object> result = entry;
              String key = entry.getKey();
              Object value = entry.getValue();
              Object newValue = value;
              if ("objectId".equals(key)) {
                result = new AbstractMap.SimpleEntry<>("_id", newValue);
              } else if (null == value) {
                // do nothing
              } else if (value instanceof JsonObject) {
                newValue = encode2BsonObject((JsonObject) value, isCreateOp);
                result = new AbstractMap.SimpleEntry<>(key, newValue);
              } else if (value instanceof JsonArray) {
                newValue = ((JsonArray) value).stream()
                        .map(v ->{
                          if (v instanceof JsonObject)
                            return encode2BsonObject((JsonObject) v, isCreateOp);
                          else
                            return v;
                        }).collect(JsonFactory.toJsonArray());
                result = new AbstractMap.SimpleEntry<>(key, newValue);
              }
              return result;
            }).collect(JsonFactory.toJsonObject());
    return resultObject;
  }

  private static JsonObject addOperatorEntry(JsonObject result, String operator, String key, Object value, boolean isCreateOp) {
    if (isCreateOp) {
      result.put(key, value);
    } else {
      if (result.containsKey(operator)) {
        result.getJsonObject(operator).put(key, value);
      } else {
        result.put(operator, new JsonObject().put(key, value));
      }
    }
    return result;
  }

  private static String getBsonModifierFromOperation(String op) {
    return bsonModifierMap.get(op);
  }

  public static JsonObject mergeComplexOps(JsonObject o, boolean isCreateOp) {
    if (null == o) {
      return o;
    }
    JsonObject complexOps = new JsonObject();
    JsonObject directSetEntries = new JsonObject();
    o.stream().forEach(entry -> {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (null == value || !(value instanceof JsonObject)) {
        addOperatorEntry(directSetEntries, "$set", key, value, isCreateOp);
      } else {
        JsonObject newValue = (JsonObject)value;
        if (newValue.containsKey("__op") && (newValue.getValue("__op") instanceof String)) {
          String op = newValue.getString("__op").toLowerCase();
          switch (op) {
            case REST_OP_BITAND:
            case REST_OP_BITOR:
            case REST_OP_BITXOR:
              int intValue = newValue.getInteger("value");
              if (isCreateOp) {
                addOperatorEntry(directSetEntries, "$set", key, intValue, isCreateOp);
              } else {
                addOperatorEntry(complexOps, "$bit", key,
                        new JsonObject().put(getBsonModifierFromOperation(op), intValue), isCreateOp);
              }
              break;

            case REST_OP_INCREMENT:
            case REST_OP_DECREMENT:
              int interval = newValue.getInteger("amount");
              if (REST_OP_DECREMENT.equals(op)) {
                interval = 0 - interval;
              }
              if (isCreateOp) {
                addOperatorEntry(directSetEntries, "$set", key, interval, isCreateOp);
              } else {
                addOperatorEntry(complexOps, "$inc", key, interval, isCreateOp);
              }
              break;
            case REST_OP_ADD_RELATION:
            case REST_OP_REMOVE_RELATION:
            case REST_OP_ADD:
            case REST_OP_REMOVE:
              JsonArray objects = newValue.getJsonArray("objects");
              if (isCreateOp) {
                if (op.equals(REST_OP_REMOVE) || op.equals(REST_OP_REMOVE_RELATION)) {
                  // ignore
                } else {
                  addOperatorEntry(directSetEntries, "$set", key, objects, isCreateOp);
                }
              } else {
                if (op.equals(REST_OP_REMOVE) || op.equals(REST_OP_REMOVE_RELATION)) {
                  addOperatorEntry(complexOps, "$pullAll", key, objects, isCreateOp);
                } else {
                  addOperatorEntry(complexOps, "$push", key,
                          new JsonObject().put(getBsonModifierFromOperation(op), objects), isCreateOp);
                }
              }
              break;
            case REST_OP_ADD_UNIQUE:
              JsonArray uniqueObjects = newValue.getJsonArray("objects").stream().distinct().collect(JsonFactory.toJsonArray());
              if (isCreateOp) {
                addOperatorEntry(directSetEntries, "$set", key, uniqueObjects, isCreateOp);
              } else {
                addOperatorEntry(complexOps, "$addToSet", key,
                        new JsonObject().put(getBsonModifierFromOperation(op), uniqueObjects), isCreateOp);
              }
              break;
            case REST_OP_SETONINSERT:
              addOperatorEntry(directSetEntries, "$setoninsert", key, newValue.remove("__op"), isCreateOp);
              break;
            case REST_OP_DELETE:
              if (isCreateOp) {
                // ignore
              } else {
                addOperatorEntry(directSetEntries, "$unset", key, "", isCreateOp);
              }
            default:
              break;
          }
        } else {
          addOperatorEntry(directSetEntries, "$set", key, value, isCreateOp);
        }
      }
    }); // end forEach
    return directSetEntries.mergeIn(complexOps);
  }

  public static JsonObject encode2BsonRequest(JsonObject o, REQUEST_OP op) throws ClassCastException {
    JsonObject result = encode2BsonObject(o, op == REQUEST_OP.CREATE);
    if (op == REQUEST_OP.QUERY) {
      return result;
    } else {
      return mergeComplexOps(result, op == REQUEST_OP.CREATE);
    }
  }

  private static JsonObject decodeBsonUnit(JsonObject o) {
    JsonObject newValue = null;
    if (o.containsKey("$ref") && o.containsKey("$id")) {
      String className = o.getString("$ref");
      String objectId = o.getString("$id");
      newValue = new JsonObject().put("__type", "Pointer");
      newValue.put("objectId", objectId);
      newValue.put("className", className);
    } else if (o.containsKey("$date")) {
      newValue = new JsonObject().put("__type", "Date");
      newValue.put("iso", o.getString("$date"));
    } else if ("Point".equalsIgnoreCase(o.getString("type")) && null != o.getJsonArray("coordinates")) {
      JsonArray coordinates = o.getJsonArray("coordinates");
      if (coordinates.size() == 2) {
        double longitude = coordinates.getDouble(0);
        double latitude = coordinates.getDouble(1);
        newValue = new JsonObject().put("__type", TYPE_GEOPOINTER);
        newValue.put("longitude", longitude);
        newValue.put("latitude", latitude);
      } else {
        // log warning.
      }
    }
    return newValue;
  }

  public static JsonObject decodeBsonObject(JsonObject o) {
    if (null == o) {
      return o;
    }
    JsonObject tmp = decodeBsonUnit(o);
    if (null != tmp) {
      return tmp;
    }

    JsonObject jsonResult = o.stream().map(entry  -> {
      String key = entry.getKey();
      Object value = entry.getValue();
      Map.Entry<String, Object> result = entry;
      if (Constraints.CLASS_ATTR_MONGO_ID.equalsIgnoreCase(key)) {
        // replace _id with objectId.
        if (value instanceof JsonObject && ((JsonObject) value).containsKey("$oid")) {
          result = new AbstractMap.SimpleEntry<String, Object>(Constraints.CLASS_ATTR_OBJECT_ID, ((JsonObject) value).getString("$oid"));
        } else {
          result = new AbstractMap.SimpleEntry<String, Object>(Constraints.CLASS_ATTR_OBJECT_ID, value);
        }
      } else if (value instanceof JsonObject) {
        JsonObject newValue = decodeBsonUnit((JsonObject) value);
        if (null != newValue) {
          result = new AbstractMap.SimpleEntry<>(key, newValue);
        }
      } else if (value instanceof JsonArray) {
        JsonArray newValue = ((JsonArray)value).stream().map(v -> {
          if (v instanceof JsonObject)
            return decodeBsonObject((JsonObject) v);
          else
            return v;
        }).collect(JsonFactory.toJsonArray());
        result = new AbstractMap.SimpleEntry<>(key, newValue);
      }
      return result;
    }).collect(JsonFactory.toJsonObject());

    return jsonResult;
  }

  public static JsonObject parseSortParam(String order) {
    if (StringUtils.isEmpty(order)) {
      return null;
    }
    JsonObject sortJson = new JsonObject();
    Arrays.stream(order.split(",")).filter(StringUtils::notEmpty).forEach( a -> {
      if (a.startsWith("+")) {
        sortJson.put(a.substring(1), 1);
      } else if (a.startsWith("-")) {
        sortJson.put(a.substring(1), -1);
      } else {
        sortJson.put(a, 1);
      }
    });
    return sortJson;
  }

  public static JsonObject parseProjectParam(String keys) {
    if (StringUtils.isEmpty(keys)) {
      return null;
    }
    JsonObject fieldJson = new JsonObject();
    Stream.concat(Arrays.stream(keys.split(",")), Arrays.stream(ALWAYS_PROJECT_KEYS))
            .filter(StringUtils::notEmpty)
            .collect(Collectors.toSet())
            .forEach(k -> fieldJson.put(k, 1));
    return fieldJson;
  }
}
