package cn.leancloud.platform.ayers.handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ObjectQueryHandler extends CommonHandler {
  public ObjectQueryHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public void query(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperation(clazz, objectId, HttpMethod.GET.toString(), null, query, handler);
  }
}
