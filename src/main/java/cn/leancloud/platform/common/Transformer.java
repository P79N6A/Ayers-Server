package cn.leancloud.platform.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.types.ObjectId;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Transformer {
  private static final String TYPE_DATE = "Date";
  private static final String TYPE_POINTER = "Pointer";
  private static final String TYPE_GEOPOINTER = "GeoPoint";
  private static final String DATE_FORMAT_REG = "\\d-\\d-\\d-\\dT\\d:\\d:\\d.\\dZ";

  // "location":{"__type":"GeoPoint","latitude":45.9,"longitude":76.3}
  //    to {"type":"Point","coordinates":[124.6682391,-17.8978304]}
  private static JsonObject encode2BsonUnit(JsonObject o) {
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

  public static JsonObject encode2BsonObject(JsonObject o) {
    if (null == o) {
      return o;
    }
    JsonObject tmp = encode2BsonUnit(o);
    if (null != tmp) {
      return tmp;
    }
    Map<String, Object> all = o.stream().map(entry -> {
      Map.Entry<String, Object> result = entry;
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof JsonObject) {
        JsonObject newValue = encode2BsonUnit((JsonObject) value);
        if (null != newValue) {
          result = new AbstractMap.SimpleEntry<>(key, newValue);;
        }
      } else if (value instanceof JsonArray) {
        JsonArray newValue = ((JsonArray)value).stream().map(v -> encode2BsonObject((JsonObject)v)).collect(JsonFactory.toJsonArray());
        result = new AbstractMap.SimpleEntry<>(key, newValue);
      }
      return result;
    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new JsonObject(all);
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

    Map<String, Object> all = o.stream().map(entry  -> {
      String key = entry.getKey();
      Object value = entry.getValue();
      Map.Entry<String, Object> result = entry;
      if (Configure.CLASS_ATTR_MONGO_ID.equalsIgnoreCase(key)) {
        // replace _id with objectId.
        if (value instanceof JsonObject && ((JsonObject) value).containsKey("$oid")) {
          result = new AbstractMap.SimpleEntry<String, Object>(Configure.CLASS_ATTR_OBJECT_ID, ((JsonObject) value).getString("$oid"));
        } else {
          result = new AbstractMap.SimpleEntry<String, Object>(Configure.CLASS_ATTR_OBJECT_ID, value);
        }
      } else if (value instanceof JsonObject) {
        JsonObject newValue = decodeBsonUnit((JsonObject) value);
        if (null != newValue) {
          result = new AbstractMap.SimpleEntry<>(key, newValue);
        }
      } else if (value instanceof JsonArray) {
        JsonArray newValue = ((JsonArray)value).stream().map(v -> decodeBsonObject((JsonObject) v)).collect(JsonFactory.toJsonArray());
        result = new AbstractMap.SimpleEntry<>(key, newValue);
      }
      return result;
    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new JsonObject(all);
  }
}