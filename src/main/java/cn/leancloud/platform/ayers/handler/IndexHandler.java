package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
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

  public void create(String clazz, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
    List<String> attrs = body.stream().map(entry -> entry.getKey()).collect(Collectors.toList());
    String indexName = String.join("-", attrs);
    sendDataOperation(clazz, indexName, RequestParse.OP_CREATE_INDEX, null, body, handler);
  }

  public void list(String clazz, Handler<AsyncResult<JsonObject>> handler) {
    ;
  }

  public void delete(String clazz, String indexName, Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperation(clazz, indexName, RequestParse.OP_DELETE_INDEX, null, null, handler);
  }
}
