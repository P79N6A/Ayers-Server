package cn.leancloud.platform.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

public class JsonUtils {

  public static JsonObject getJsonObject(JsonObject source, String fieldPath) {
    if (null == source || StringUtils.isEmpty(fieldPath)) {
      return null;
    }
    int dotIndex = fieldPath.indexOf(".");
    String first;
    String rest;
    if (dotIndex <= 0) {
      first = fieldPath;
      Object firstJson = source.getValue(first);
      if (null == firstJson || !(firstJson instanceof JsonObject)) {
        return null;
      } else {
        return (JsonObject) firstJson;
      }
    } else {
      first = fieldPath.substring(0, dotIndex);
      rest = fieldPath.substring(dotIndex + 1);
      Object firstJson = source.getValue(first);
      if (null == firstJson || !(firstJson instanceof JsonObject)) {
        return null;
      } else {
        return getJsonObject((JsonObject) firstJson, rest);
      }
    }
  }

  public static boolean replaceJsonValue(JsonObject source, String fieldPath, JsonObject value) {
    if (null == source || StringUtils.isEmpty(fieldPath)) {
      return false;
    }
    int dotIndex = fieldPath.indexOf(".");
    String first;
    String rest;
    if (dotIndex <= 0) {
      first = fieldPath;
      source.put(first, value);
      return true;
    } else {
      first = fieldPath.substring(0, dotIndex);
      rest = fieldPath.substring(dotIndex + 1);
      Object firstJson = source.getValue(first);
      if (null == firstJson || !(firstJson instanceof JsonObject)) {
        return false;
      } else {
        return replaceJsonValue((JsonObject) firstJson, rest, value);
      }
    }
  }

  public static JsonObject deepMergeIn(JsonObject to, JsonObject from) {
    Objects.requireNonNull(to);
    Objects.requireNonNull(from);
    from.stream().forEach(entry -> {
      String k = entry.getKey();
      Object v = entry.getValue();
      if (null == v) {
        return;
      } else if (!to.containsKey(k) || null == to.getValue(k)) {
        to.put(k, v);
      } else {
        Object leftV = to.getValue(k);
        if (!leftV.getClass().equals(v.getClass())) {
          return;
        } else if (leftV instanceof JsonArray) {
          ((JsonArray)leftV).addAll((JsonArray) v);
        } else if (leftV instanceof JsonObject) {
          leftV = deepMergeIn((JsonObject) leftV, (JsonObject) v);
        }
      }
    });
    return to;
  }
}
