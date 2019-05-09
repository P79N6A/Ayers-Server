package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

public class IndexHandler extends CommonHandler {
  public IndexHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public void create(String clazz, JsonObject keys, JsonObject options, Handler<AsyncResult<JsonObject>> handler) {
    String indexName = null;
    if (null == options || StringUtils.isEmpty(options.getString(RequestParse.REQUEST_INDEX_OPTION_NAME))) {
      List<String> attrs = keys.stream().map(entry -> entry.getKey()).sorted().collect(Collectors.toList());
      indexName = String.join("-", attrs);
    } else {
      indexName = options.getString(RequestParse.REQUEST_INDEX_OPTION_NAME);
      options.remove(RequestParse.REQUEST_INDEX_OPTION_NAME);
    }
    sendDataOperation(clazz, indexName, RequestParse.OP_CREATE_INDEX, options, keys,
            RequestParse.extractRequestHeaders(routingContext), handler);
  }

  public void list(String clazz, Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperation(clazz, null, RequestParse.OP_LIST_INDEX, null, null,
            RequestParse.extractRequestHeaders(routingContext), handler);
  }

  public void delete(String clazz, String indexName, Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperation(clazz, indexName, RequestParse.OP_DELETE_INDEX, null, null,
            RequestParse.extractRequestHeaders(routingContext), handler);
  }
}
