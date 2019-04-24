package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.CommonVerticle;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(CommonHandler.class);

  protected Vertx vertx;
  protected RoutingContext routingContext;
  public CommonHandler(Vertx vertx, RoutingContext context) {
    this.vertx = vertx;
    this.routingContext = context;
  }

  protected void assertNotNull(String value, Handler<AsyncResult<JsonObject>> handler) {
  }

  protected boolean shouldChangeSchema(String clazz, String operation) {
    return HttpMethod.POST.toString().equals(operation) || HttpMethod.PUT.toString().equals(operation)
            || CommonVerticle.OP_USER_SIGNUP.equals(operation) || CommonVerticle.OP_USER_AUTH_LOGIN.equals(operation);
  }

  protected void sendDataOperation(String clazz, String objectId, String operation,
                                   JsonObject query, JsonObject update, final Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperationWithOption(clazz, objectId, operation, query, update, false, handler);
  }

  protected void sendDataOperationWithOption(String clazz, String objectId, String operation,
                                             JsonObject query, JsonObject update, boolean returnNewDocument,
                                             final Handler<AsyncResult<JsonObject>> handler) {
    JsonObject request = new JsonObject().put(CommonVerticle.INTERNAL_MSG_ATTR_RETURNNEWDOC, returnNewDocument);
    if (!StringUtils.isEmpty(clazz)) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_CLASS, clazz);
    }
    if (!StringUtils.isEmpty(objectId)) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_OBJECT_ID, objectId);
    }
    if (null != query) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_QUERY, query);
    }
    if (null != update) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_PARAM, update);
    }
    String upperOperation = operation.toUpperCase();
    DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, upperOperation);
    if (shouldChangeSchema(clazz, upperOperation)) {
      vertx.eventBus().send(Configure.MAILADDRESS_DEMOCLES_QUEUE, request, options, response -> {
        if (response.failed()) {
          logger.warn("failed to check schema by democles.");
          handler.handle(response.map(v -> (JsonObject)v.body()));
        } else {
          logger.debug("pass schema check, send to storage verticle.");
          vertx.eventBus().send(Configure.MAILADDRESS_DATASTORE_QUEUE, request, options,
                  res2 -> handler.handle(res2.map(v -> (JsonObject)v.body())));
        }
      });
    } else {
      logger.debug("send to storage verticle directly.");
      vertx.eventBus().send(Configure.MAILADDRESS_DATASTORE_QUEUE, request, options,
              res -> handler.handle(res.map(v -> (JsonObject) v.body())));
    }
  }
}
