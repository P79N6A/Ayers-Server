package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectQueryHandler extends CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(ObjectQueryHandler.class);

  private static final String DEFAULT_QUERY_LIMIT = "100";
  private static final String DEFAULT_QUERY_SKIP = "0";
  private static final String DEFAULT_QUERY_COUNT = "0";

  public static final String QUERY_KEY_COUNT = "count";
  public static final String QUERY_KEY_WHERE = "where";
  public static final String QUERY_KEY_LIMIT = "limit";
  public static final String QUERY_KEY_SKIP = "skip";
  public static final String QUERY_KEY_ORDER = "order";
  public static final String QUERY_KEY_INCLUDE = "include";
  public static final String QUERY_KEY_KEYS = "keys";
  public static final String QUERY_KEY_CLASSNAME = "className";
  public static final String QUERY_KEY_SUBQUERY_PROJECT = "key";
  public static final String QUERY_KEY_SUBQUERY_QUERY = "query";

  public static final String OP_SELECT = "$select";
  public static final String OP_INQUERY = "$inQuery"; // not use yet.

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

  private static class QueryOptions {
    private String className;
    private JsonObject where;
    private JsonObject order;
    private JsonObject keys;
    private int limit = 100;
    private int skip = 0;
    private int count = 0;

    public JsonObject getWhere() {
      return where;
    }

    public QueryOptions setWhere(JsonObject where) {
      this.where = where;
      return this;
    }

    public String getClassName() {
      return className;
    }

    public QueryOptions setClassName(String className) {
      this.className = className;
      return this;
    }

    public JsonObject getKeys() {
      return keys;
    }

    public QueryOptions setKeys(JsonObject key) {
      this.keys = key;
      return this;
    }

    public int getLimit() {
      return limit;
    }

    public QueryOptions setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public int getSkip() {
      return skip;
    }

    public QueryOptions setSkip(int skip) {
      this.skip = skip;
      return this;
    }

    public JsonObject getOrder() {
      return order;
    }

    public QueryOptions setOrder(JsonObject order) {
      this.order = order;
      return this;
    }

    public int getCount() {
      return count;
    }

    public QueryOptions setCount(int count) {
      this.count = count;
      return this;
    }

    public JsonObject toJson() {
      return new JsonObject().put(QUERY_KEY_CLASSNAME, this.className)
              .put(QUERY_KEY_WHERE, this.where).put(QUERY_KEY_KEYS, this.keys).put(QUERY_KEY_ORDER, this.order)
              .put(QUERY_KEY_LIMIT, this.limit).put(QUERY_KEY_SKIP, this.skip).put(QUERY_KEY_COUNT, this.count);
    }
  }

  public void query(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    String where = query.getString(QUERY_KEY_WHERE, "{}");
    String order = query.getString(QUERY_KEY_ORDER);
    int limit = Integer.valueOf(query.getString(QUERY_KEY_LIMIT, DEFAULT_QUERY_LIMIT));
    int skip = Integer.valueOf(query.getString(QUERY_KEY_SKIP, DEFAULT_QUERY_SKIP));
    int count = Integer.valueOf(query.getString(QUERY_KEY_COUNT, DEFAULT_QUERY_COUNT));
    String include = query.getString(QUERY_KEY_INCLUDE);
    String keys = query.getString(QUERY_KEY_KEYS);

    final List<String> includeArray;
    if (StringUtils.notEmpty(include)) {
      includeArray = Arrays.asList(include.split(",")).stream().filter(StringUtils::notEmpty)
              .collect(Collectors.toList());
    } else {
      includeArray = new ArrayList<>();
    }

    JsonObject condition = new JsonObject(where);
    JsonObject orderJson = parseSortParam(order);
    JsonObject fieldJson = parseProjectParam(keys);

    if (StringUtils.notEmpty(objectId)) {
      condition.put(LeanObject.ATTR_NAME_OBJECTID, objectId);
    }

    try {
      final JsonObject transformedCondition = transformSubQuery(condition);
      Future<Boolean> future = resolveSubQuery(transformedCondition);
      future.setHandler(res -> {
        if (res.failed()) {
          logger.warn("failed to execute subQuery. cause:" + res.cause().getMessage());
          if (null != handler) {
            handler.handle(new AsyncResult<JsonObject>() {
              @Override
              public JsonObject result() {
                return null;
              }

              @Override
              public Throwable cause() {
                return res.cause();
              }

              @Override
              public boolean succeeded() {
                return false;
              }

              @Override
              public boolean failed() {
                return true;
              }
            });
          }
        } else {
          QueryOptions queryOptions = new QueryOptions();
          queryOptions.setWhere(transformedCondition).setOrder(orderJson).setKeys(fieldJson).setLimit(limit).setSkip(skip).setCount(count);

          logger.debug("begin to execute actual query option: " + queryOptions.toJson());
          // TODO: we need to process include clause yet.
          execute(clazz, queryOptions.toJson(), includeArray, handler);
        }
      });
    } catch (IllegalArgumentException ex) {
      logger.warn("failed to validate subQuery clause. cause:" + ex.getMessage());
      handler.handle(new AsyncResult<JsonObject>() {
        @Override
        public JsonObject result() {
          return null;
        }

        @Override
        public Throwable cause() {
          return ex;
        }

        @Override
        public boolean succeeded() {
          return false;
        }

        @Override
        public boolean failed() {
          return true;
        }
      });
    }

//    JsonObject integratedQuery = new JsonObject().put(QUERY_KEY_WHERE, condition).put(QUERY_KEY_ORDER, orderJson)
//            .put(QUERY_KEY_KEYS, fieldJson).put(QUERY_KEY_LIMIT, limit).put(QUERY_KEY_SKIP, skip).put(QUERY_KEY_COUNT, count);
//
//    sendDataOperation(clazz, null, HttpMethod.GET.toString(), integratedQuery, null, handler);
  }

  private void execute(String clazz, JsonObject options, List<String> includeArray,
                       Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperation(clazz, null, HttpMethod.GET.toString(), options, null, handler);
  }

  private Future<Boolean> resolveSubQuery(JsonObject condition) {
    logger.debug("try to resolve query: " + condition.toString());
    Future<Boolean> future = Future.succeededFuture(true);
    for (Map.Entry<String, Object> entry : condition.getMap().entrySet()) {
      Object value = entry.getValue();
      String key = entry.getKey();
      if (null != value && value instanceof JsonObject) {
        JsonObject jsonValue = (JsonObject) value;
        if (jsonValue.containsKey(OP_SELECT)) {
          logger.debug("found $select node.");
          future = future.compose(res -> {
            JsonObject options = jsonValue.getJsonObject(OP_SELECT);
            String clazz = options.getString(QUERY_KEY_CLASSNAME);
            String attrName = options.getString(QUERY_KEY_SUBQUERY_PROJECT);
            Future<Boolean> subQueryFuture = Future.future();
            execute(clazz, options, null, any -> {
              if (any.failed()) {
                logger.warn("failed to execute subQuery:" + options + ". cause: " + any.cause().getMessage());
                condition.put(key, new JsonObject().put("$in", new JsonArray()));
                subQueryFuture.fail(any.cause());
              } else {
                JsonArray actualValues = any.result().getJsonArray("results").stream()
                        .map(obj -> ((JsonObject)obj).getValue(attrName))
                        .collect(JsonFactory.toJsonArray());
                logger.debug("succeed to execute subQuery:" + options + ". results: " + actualValues.toString());
                condition.put(key, new JsonObject().put("$in", actualValues));
                subQueryFuture.complete(true);
              }
            });
            return subQueryFuture;
          });
        } else {
          future = future.compose(j -> resolveSubQuery(jsonValue));
        }
      } else if (null != value && value instanceof JsonArray) {
        future = future.compose(val -> {
          JsonArray arrayValue = (JsonArray)value;
          Future<Boolean> arrayFuture = Future.future();
          for (Object v : arrayValue.getList()) {
            if (null != v && v instanceof JsonObject) {
              arrayFuture = arrayFuture.compose(a -> resolveSubQuery((JsonObject) v));
            } else {
              // do nothing.
              arrayFuture = arrayFuture.compose(res -> {
                Future<Boolean> dummyFuture = Future.future();
                dummyFuture.complete(true);
                return dummyFuture;
              });
            }
          };
          return arrayFuture;
        });
      } else {
        // do nothing.
        future = future.compose(res -> {
          Future<Boolean> dummyFuture = Future.future();
          dummyFuture.complete(true);
          return dummyFuture;
        });
      }
    }
    return future;
  }

  // {
  //      "$select": {
  //        "query": {
  //          "className":"_Followee",
  //           "where": {
  //             "user":{
  //               "__type": "Pointer",
  //               "className": "_User",
  //               "objectId": "55a39634e4b0ed48f0c1845c"
  //             }
  //           }
  //        },
  //        "key":"followee"
  //      }
  private QueryOptions validSubQuery(JsonObject selectClause) {
    if (null == selectClause) {
      return null;
    }
    if (!selectClause.containsKey(QUERY_KEY_SUBQUERY_QUERY) || !selectClause.containsKey(QUERY_KEY_SUBQUERY_PROJECT)) {
      return null;
    }
    String projectField = selectClause.getString(QUERY_KEY_SUBQUERY_PROJECT);
    JsonObject queryClause = selectClause.getJsonObject(QUERY_KEY_SUBQUERY_QUERY);
    if (StringUtils.isEmpty(projectField) || null == queryClause || !queryClause.containsKey(QUERY_KEY_CLASSNAME) || !queryClause.containsKey(QUERY_KEY_WHERE)) {
      return null;
    }
    String className = queryClause.getString(QUERY_KEY_CLASSNAME);
    JsonObject whereClause = queryClause.getJsonObject(QUERY_KEY_WHERE);
    if (StringUtils.isEmpty(className) || null == whereClause || whereClause.size() < 1) {
      return null;
    }
    QueryOptions subQuery = new QueryOptions().setClassName(className).setWhere(whereClause).setKeys(new JsonObject().put(projectField, 1));
    if (queryClause.containsKey(QUERY_KEY_SKIP)) {
      subQuery.setSkip(queryClause.getInteger(QUERY_KEY_SKIP));
    }
    if (queryClause.containsKey(QUERY_KEY_LIMIT)) {
      subQuery.setLimit(queryClause.getInteger(QUERY_KEY_LIMIT));
    }
    if (queryClause.containsKey(QUERY_KEY_ORDER)) {
      subQuery.setOrder(parseSortParam(queryClause.getString(QUERY_KEY_ORDER)));
    }
    return subQuery;
  }

  protected JsonObject transformSubQuery(JsonObject condition) {
    if (null == condition || condition.size() == 0) {
      return condition;
    }
    logger.debug("try to transform query: " + condition);
    JsonObject result = condition.stream().map(entry -> {
      Object value = entry.getValue();
      if (null != value && value instanceof JsonObject) {
        JsonObject tmpValue = (JsonObject) value;
        if (tmpValue.containsKey(OP_SELECT)) {
          JsonObject select = tmpValue.getJsonObject(OP_SELECT);
          logger.debug("found $select node: " + select);

          QueryOptions options = validSubQuery(select);
          if (null == options) {
            logger.warn("invalid subQuery clause for key:" + entry.getKey());
            throw new IllegalArgumentException("invalid subQuery clause for key:" + entry.getKey());
          }
          return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), new JsonObject().put(OP_SELECT, options.toJson()));
//        } if (tmpValue.containsKey(OP_INQUERY)) {
//          JsonObject select = tmpValue.getJsonObject(OP_INQUERY);
//          QueryOptions options = validSubQuery(select);
//          if (null == options) {
//            throw new IllegalArgumentException("invalid subQuery clause for key:" + entry.getKey());
//          }
//          return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), new JsonObject().put(OP_INQUERY, options.toJson()));
        } else {
          return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), transformSubQuery(tmpValue));
        }
      } if (null != value && value instanceof JsonArray) {
        JsonArray newValue = ((JsonArray)value).stream().map(v -> {
          if (v instanceof JsonObject) {
            return transformSubQuery((JsonObject)v);
          } else {
            return v;
          }
        }).collect(JsonFactory.toJsonArray());
        return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), newValue);
      }
      return entry;
    }).collect(JsonFactory.toJsonObject());
    return result;
  }
}
