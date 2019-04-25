package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectQueryHandler extends CommonHandler {
  public static final String QUERY_KEY_COUNT = "count";
  public static final String QUERY_KEY_WHERE = "where";
  public static final String QUERY_KEY_LIMIT = "limit";
  public static final String QUERY_KEY_SKIP = "skip";
  public static final String QUERY_KEY_ORDER = "order";
  public static final String QUERY_KEY_INCLUDE = "include";
  public static final String QUERY_KEY_KEYS = "keys";

  private static final String[] ALWAYS_PROJECT_KEYS = {"_id", "createdAt", "updatedAt", "ACL"};

  public ObjectQueryHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
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

  public void query(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    String where = query.getString(QUERY_KEY_WHERE, "{}");
    String order = query.getString(QUERY_KEY_ORDER);
    int limit = Integer.valueOf(query.getString(QUERY_KEY_LIMIT, "100"));
    int skip = Integer.valueOf(query.getString(QUERY_KEY_SKIP, "0"));
    int count = Integer.valueOf(query.getString(QUERY_KEY_COUNT, "0"));
    String include = query.getString(QUERY_KEY_INCLUDE);
    String keys = query.getString(QUERY_KEY_KEYS);
    final List<String> includeArray;
    if (!StringUtils.isEmpty(include)) {
      includeArray = Arrays.asList(include.split(",")).stream().filter(StringUtils::notEmpty)
              .collect(Collectors.toList());
    } else {
      includeArray = new ArrayList<>();
    }

    JsonObject condition = new JsonObject(where);
    JsonObject orderJson = parseSortParam(order);
    JsonObject fieldJson = parseProjectParam(keys);
    JsonObject integratedQuery = new JsonObject().put(QUERY_KEY_WHERE, condition).put(QUERY_KEY_ORDER, orderJson)
            .put(QUERY_KEY_KEYS, fieldJson).put(QUERY_KEY_LIMIT, limit).put(QUERY_KEY_SKIP, skip).put(QUERY_KEY_COUNT, count);

    sendDataOperation(clazz, objectId, HttpMethod.GET.toString(), integratedQuery, null, handler);
  }
}
