package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.JsonUtils;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.leancloud.platform.utils.HandlerUtils.wrapActualResult;
import static cn.leancloud.platform.utils.HandlerUtils.wrapErrorResult;

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
  public static final String OP_DONT_SELECT = "$dontSelect";
  public static final String OP_IN_QUERY = "$inQuery";
  public static final String OP_NOTIN_QUERY = "$notInQuery";

  private static final String[] ALWAYS_PROJECT_KEYS = {"_id", "createdAt", "updatedAt", "ACL"};

  public ObjectQueryHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public static JsonObject parseSortParam(String order) {
    if (StringUtils.isEmpty(order)) {
      return null;
    }
    JsonObject sortJson = new JsonObject();
    Arrays.stream(order.split(",")).filter(StringUtils::notEmpty).forEach(a -> {
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

  public static JsonObject parseProjectParam(String keys, List<String> includeArray) {
    if (StringUtils.isEmpty(keys)) {
      return null;
    }
    JsonObject fieldJson = new JsonObject();
    List<String> appendKeys = new ArrayList<>();//Arrays.asList(ALWAYS_PROJECT_KEYS);
    appendKeys.addAll(Arrays.asList(ALWAYS_PROJECT_KEYS));
    if (null != includeArray && includeArray.size() > 0) {
      appendKeys.addAll(includeArray);
    }
    Set<String> attrSet = Stream.concat(Arrays.stream(keys.split(",")), appendKeys.stream())
            .filter(StringUtils::notEmpty)
            .collect(Collectors.toSet());
    for (String k : attrSet) {
      fieldJson.put(k, 1);
    }
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

  public void query(String clazz, String objectId, JsonObject queryParam, Handler<AsyncResult<JsonObject>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(queryParam);
    Objects.requireNonNull(handler);

    queryWithCondition(clazz, objectId, queryParam, handler);
  }

  private void queryWithCondition(String clazz, String objectId, JsonObject queryParam, Handler<AsyncResult<JsonObject>> handler) {
    String where = queryParam.getString(QUERY_KEY_WHERE, "{}");
    String order = queryParam.getString(QUERY_KEY_ORDER);
    int limit = Integer.valueOf(queryParam.getString(QUERY_KEY_LIMIT, DEFAULT_QUERY_LIMIT));
    int skip = Integer.valueOf(queryParam.getString(QUERY_KEY_SKIP, DEFAULT_QUERY_SKIP));
    int count = Integer.valueOf(queryParam.getString(QUERY_KEY_COUNT, DEFAULT_QUERY_COUNT));
    String include = queryParam.getString(QUERY_KEY_INCLUDE);
    String keys = queryParam.getString(QUERY_KEY_KEYS);

    final List<String> includeArray;
    if (StringUtils.notEmpty(include)) {
      includeArray = Arrays.asList(include.split(",")).stream().filter(StringUtils::notEmpty)
              .collect(Collectors.toList());
    } else {
      includeArray = new ArrayList<>();
    }

    JsonObject condition = new JsonObject(where);
    JsonObject orderJson = parseSortParam(order);
    JsonObject fieldJson = parseProjectParam(keys, includeArray);

    if (StringUtils.notEmpty(objectId)) {
      condition.put(LeanObject.ATTR_NAME_OBJECTID, objectId);
    }

    logger.debug("where-" + condition + ", order-" + orderJson + ", field-" + fieldJson + ", include-" + include);

    try {
      final JsonObject transformedCondition = transformSubQuery(condition);
      Future<Boolean> future = resolveSubQuery(transformedCondition);
      future.setHandler(res -> {
        if (res.failed()) {
          logger.warn("failed to execute subQuery. cause:" + res.cause().getMessage());
          handler.handle(wrapErrorResult(res.cause()));
        } else {
          QueryOptions queryOptions = new QueryOptions();
          queryOptions.setWhere(transformedCondition).setOrder(orderJson).setKeys(fieldJson).setLimit(limit).setSkip(skip).setCount(count);

          logger.debug("begin to execute actual queryParam option: " + queryOptions.toJson());

          execute(clazz, objectId, queryOptions.toJson(), includeArray, response -> {
            if (response.failed()) {
              logger.warn("query failed: " + response.cause().getMessage());
              handler.handle(response);
              return;
            }
            if (includeArray.size() < 1) {
              logger.debug("include list is empty, return directly.");
              handler.handle(response);
              return;
            }
            JsonArray results = response.result().getJsonArray("results");
            int resultCount = (null == results) ? 0 : results.size();
            if (null == results || resultCount < 1) {
              logger.debug("result list is empty, return directly.");
              handler.handle(response);
              return;
            }
//            logger.debug("first results:" + results);
            List<Future> futures = includeArray.stream()
                    .map(attr -> {
                      Future<Void> future1Attr = Future.future();
                      List<Object> includedAttrResults = results.stream()
                              .filter(obj -> (obj instanceof JsonObject) && null != JsonUtils.getJsonObject((JsonObject) obj, attr))
                              .collect(Collectors.toList());
                      if (null == includedAttrResults || includedAttrResults.size() < 1) {
                        logger.debug("there is nothing for target attr: " + attr);
                        future1Attr.complete();
                        return future1Attr;
                      }
                      JsonObject firstResult = (JsonObject) includedAttrResults.get(0);
                      JsonObject pointerJson = JsonUtils.getJsonObject(firstResult, attr);
                      String className = pointerJson.getString(LeanObject.ATTR_NAME_CLASSNAME);
                      if (StringUtils.isEmpty(className)) {
                        logger.warn("there is invalid Pointer(no className) for target attr: " + attr);
                        future1Attr.complete();
                        return future1Attr;
                      }

                      logger.debug("try to execute include query. attr=" + attr + ", targetClazz=" + className);

                      List<String> targetObjectIds = includedAttrResults.stream()
                              .map(obj -> JsonUtils.getJsonObject((JsonObject) obj, attr).getString(LeanObject.ATTR_NAME_OBJECTID))
                              .filter(StringUtils::notEmpty)
                              .collect(Collectors.toList());

                      JsonObject subWhere = new JsonObject().put("objectId", new JsonObject().put("$in", targetObjectIds));
                      QueryOptions subQueryOptions = new QueryOptions();
                      subQueryOptions.setWhere(subWhere).setLimit(resultCount);
//                      logger.debug("include query options: " + subQueryOptions.toJson());

                      execute(className, null, subQueryOptions.toJson(), null, any -> {
                        if (any.failed()) {
                          logger.warn("failed to execute include query. cause: " + any.cause().getMessage());
                          future1Attr.fail(any.cause());
                        } else {
                          JsonArray oneAttrResults = any.result().getJsonArray("results");
//                          logger.debug("include query results: " + oneAttrResults);

                          if (null != oneAttrResults && oneAttrResults.size() > 0) {
                            // parse and mapping result.
                            Map<String, JsonObject> attrMaps = new HashMap<>(oneAttrResults.size());

                            oneAttrResults.stream()
                                    .forEach(obj -> attrMaps.put(((JsonObject) obj).getString(LeanObject.ATTR_NAME_OBJECTID), (JsonObject) obj));

                            results.stream().forEach(obj2 -> {
                              JsonObject tmp = (JsonObject) obj2;
                              JsonObject attrTargetJson = JsonUtils.getJsonObject(tmp, attr);
                              if (null != attrTargetJson) {
                                String tmpObjectId = attrTargetJson.getString(LeanObject.ATTR_NAME_OBJECTID);
                                JsonObject completedObject = attrMaps.get(tmpObjectId);
                                if (null != completedObject) {
                                  logger.debug("replace attr:" + attr + ", objectId:" + tmpObjectId + ", jsonObject:" + completedObject);
                                  JsonUtils.replaceJsonValue(tmp, attr, completedObject);
                                } else {
                                  logger.warn("not found result for attr:" + attr + ", objectId:" + tmpObjectId);
                                }
                              }
                            });
                          }
                          future1Attr.complete();
                        }
                      });
                      return future1Attr;
                    }).collect(Collectors.toList());
            CompositeFuture.all(futures).setHandler(any -> {
              if (any.failed()) {
                logger.warn("failed to execute all include query. cause: " + any.cause().getMessage());
                handler.handle(wrapErrorResult(any.cause()));
              } else {
//                logger.debug("AllInOne finished. results=" + results);
                handler.handle(wrapActualResult(new JsonObject().put("results", results)));
              }
            });
          });
        }
      });
    } catch (IllegalArgumentException ex) {
      logger.warn("failed to validate subQuery clause. cause:" + ex.getMessage());
      handler.handle(wrapErrorResult(ex));
    }
  }

  /**
   * execute query actually.
   *
   * @param clazz        className
   * @param options      query options.
   * @param includeArray reserved for call aggregate api in future.
   * @param handler      callback handler.
   */
  private void execute(String clazz, String objectId, JsonObject options, List<String> includeArray,
                       Handler<AsyncResult<JsonObject>> handler) {
    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(routingContext);
    sendDataOperation(clazz, objectId, HttpMethod.GET.toString(), options, null, headers, handler);
  }

  private Future<Boolean> resolveSubQuery(JsonObject condition) {
    Future<Boolean> future = Future.succeededFuture(true);
    for (Map.Entry<String, Object> entry : condition.getMap().entrySet()) {
      Object value = entry.getValue();
      String key = entry.getKey();
      if (null != value && value instanceof JsonObject) {
        JsonObject jsonValue = (JsonObject) value;
        if (jsonValue.containsKey(OP_SELECT)) {
          future = future.compose(res -> {
            JsonObject options = jsonValue.getJsonObject(OP_SELECT);
            String clazz = options.getString(QUERY_KEY_CLASSNAME);
            String attrName = options.getString(QUERY_KEY_SUBQUERY_PROJECT);
            Future<Boolean> subQueryFuture = Future.future();
            execute(clazz, null, options, null, any -> {
              if (any.failed()) {
                logger.warn("failed to execute subQuery:" + options + ". cause: " + any.cause().getMessage());
                condition.put(key, new JsonObject().put("$in", new JsonArray()));
                subQueryFuture.fail(any.cause());
              } else {
                JsonArray actualValues = any.result().getJsonArray("results").stream()
                        .map(obj -> ((JsonObject) obj).getValue(attrName))
                        .collect(JsonFactory.toJsonArray());
                condition.put(key, new JsonObject().put("$in", actualValues));
                subQueryFuture.complete(true);
              }
            });
            return subQueryFuture;
          });
        } else if (jsonValue.containsKey(OP_DONT_SELECT)) {
          future = future.compose(res -> {
            JsonObject options = jsonValue.getJsonObject(OP_DONT_SELECT);
            String clazz = options.getString(QUERY_KEY_CLASSNAME);
            String attrName = options.getString(QUERY_KEY_SUBQUERY_PROJECT);
            Future<Boolean> subQueryFuture = Future.future();
            execute(clazz, null, options, null, any -> {
              if (any.failed()) {
                logger.warn("failed to execute subQuery:" + options + ". cause: " + any.cause().getMessage());
                condition.put(key, new JsonObject().put("$nin", new JsonArray()));
                subQueryFuture.fail(any.cause());
              } else {
                JsonArray actualValues = any.result().getJsonArray("results").stream()
                        .map(obj -> ((JsonObject) obj).getValue(attrName))
                        .collect(JsonFactory.toJsonArray());
                condition.put(key, new JsonObject().put("$nin", actualValues));
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
          JsonArray arrayValue = (JsonArray) value;
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
          }
          ;
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
  private QueryOptions validateSubQuery(JsonObject selectClause) {
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
    JsonObject result = condition.stream().map(entry -> {
      Object value = entry.getValue();
      if (null != value && value instanceof JsonObject) {
        JsonObject tmpValue = (JsonObject) value;
        if (tmpValue.containsKey(OP_SELECT)) {
          JsonObject select = tmpValue.getJsonObject(OP_SELECT);

          QueryOptions options = validateSubQuery(select);
          if (null == options) {
            logger.warn("invalid subQuery clause for key:" + entry.getKey());
            throw new IllegalArgumentException("invalid subQuery clause for key:" + entry.getKey());
          }
          return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), new JsonObject().put(OP_SELECT, options.toJson()));
        } else if (tmpValue.containsKey(OP_DONT_SELECT)) {
          JsonObject select = tmpValue.getJsonObject(OP_DONT_SELECT);
          QueryOptions options = validateSubQuery(select);
          if (null == options) {
            logger.warn("invalid subQuery clause for key:" + entry.getKey());
            throw new IllegalArgumentException("invalid subQuery clause for key:" + entry.getKey());
          }
          return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), new JsonObject().put(OP_DONT_SELECT, options.toJson()));
        } else {
          return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), transformSubQuery(tmpValue));
        }
      }
      if (null != value && value instanceof JsonArray) {
        JsonArray newValue = ((JsonArray) value).stream().map(v -> {
          if (v instanceof JsonObject) {
            return transformSubQuery((JsonObject) v);
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
