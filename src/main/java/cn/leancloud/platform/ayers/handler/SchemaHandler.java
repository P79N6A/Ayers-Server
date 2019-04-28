package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class SchemaHandler extends CommonHandler {
  public SchemaHandler(Vertx vertx, RoutingContext routingContext) {
    super(vertx, routingContext);
  }

  public void test(String clazz, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    sendSchemaOperation(clazz, RequestParse.OP_TEST_SCHEMA, data, handler);
  }

  public void addIfAbsent(String clazz, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    sendSchemaOperation(clazz, RequestParse.OP_ADD_SCHEMA, data, handler);
  }

  public void find(String clazz, Handler<AsyncResult<JsonObject>> handler) {
    sendSchemaOperation(clazz, RequestParse.OP_FIND_SCHEMA, null, handler);
  }
}
