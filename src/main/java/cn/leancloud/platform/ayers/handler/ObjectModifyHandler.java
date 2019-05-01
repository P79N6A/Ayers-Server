package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.CommonVerticle;
import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.common.BatchRequest;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.EngineHookProxy;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.engine.HookType;
import cn.leancloud.platform.modules.ObjectSpecifics;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectModifyHandler extends CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(ObjectModifyHandler.class);

  private EngineHookProxy hookProxy;
  public ObjectModifyHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
    this.hookProxy = EngineHookProxy.getInstance(vertx);
  }

  public void create(String clazz, JsonObject body, boolean returnNewDoc, Handler<AsyncResult<JsonObject>> handler) {
    //sendDataOperationWithOption(clazz, null, HttpMethod.POST.toString(), null, body, returnNewDoc, handler);

    logger.debug("ObjectModifyHandler#create");
    JsonObject headers = copyRequestHeaders(routingContext);
    logger.debug("ObjectModifyHandler#create2");
    this.hookProxy.call(clazz, HookType.BeforeSave, body, headers, routingContext, res -> {
      if (res.failed()) {
        logger.warn("lean engine hook failed. cause: " + res.cause());
        handler.handle(res);
      } else {
        JsonObject hookedBody = res.result();
        logger.debug("get lean enginne hook result: " + hookedBody);
        sendDataOperationWithOption(clazz, null, HttpMethod.POST.toString(), null, hookedBody, returnNewDoc, handler);
      }
    });
  }

  public void update(String clazz, String objectId, JsonObject query, JsonObject update, boolean returnNewDoc,
                     Handler<AsyncResult<JsonObject>> handler) {
    JsonObject headers = copyRequestHeaders(routingContext);
    this.hookProxy.call(clazz, HookType.BeforeUpdate, update, headers, routingContext, res -> {
      if (res.failed()) {
        logger.warn("lean engine hook failed. cause: " + res.cause());
        handler.handle(res);
      } else {
        JsonObject hookedBody = res.result();
        logger.debug("get lean enginne hook result: " + hookedBody);
        sendDataOperationWithOption(clazz, objectId, HttpMethod.PUT.toString(), query, hookedBody, returnNewDoc, handler);
      }
    });
  }

  public void delete(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject headers = copyRequestHeaders(routingContext);
    this.hookProxy.call(clazz, HookType.BeforeDelete, query, headers, routingContext, res -> {
      if (res.failed()) {
        logger.warn("lean engine hook failed. cause: " + res.cause());
        handler.handle(res);
      } else {
        JsonObject hookedBody = res.result();
        logger.debug("get lean enginne hook result: " + hookedBody);
        sendDataOperation(clazz, objectId, HttpMethod.DELETE.toString(), query, null, handler);
      }
    });
  }

  private static BatchRequest parseBatchRequest(Object req) {
    if (null == req || !(req instanceof JsonObject)) {
      return null;
    }
    JsonObject reqJson = (JsonObject)req;
    String method = reqJson.getString("method");
    String path = reqJson.getString("path");
    JsonObject param = reqJson.getJsonObject("body");
    Pair<String, String> pair = parseClazzAndObjectId(path);
    String clazz = pair.getLeft();
    String objectId = pair.getRight();
    if (StringUtils.isEmpty(method) || !isUpdatableOperation(method) || StringUtils.isEmpty(path)
            || !ObjectSpecifics.validRequestPath(path)) {
      logger.warn("invalid request. method:" + method + ", path:" + path);
      return null;
    }
    if (StringUtils.isEmpty(clazz) && !ObjectSpecifics.validClassName(clazz)) {
      logger.warn("invalid clazz:" + clazz);
      return null;
    }
    if (null == param && !method.equalsIgnoreCase(RequestParse.OP_OBJECT_DELETE)) {
      logger.warn("request body is null for POST and PUT");
      return null;
    }
    if ((method.equalsIgnoreCase(RequestParse.OP_OBJECT_PUT) || method.equalsIgnoreCase(RequestParse.OP_OBJECT_DELETE))
            && StringUtils.isEmpty(objectId)) {
      logger.warn("objectId is null for Delete and PUT");
      return null;
    }
    return new BatchRequest(method, path, clazz, objectId, param);
  }

  public void batchSave(JsonArray requests, Handler<AsyncResult<CompositeFuture>> handler) {
    List<Future> results = requests.stream().map(req -> {
      final Future<JsonObject> tmp = Future.future();
      BatchRequest batchRequest = parseBatchRequest(req);
      if (null == batchRequest) {
        logger.warn("failed to parse request:" + req);
        tmp.complete(new JsonObject().put("error", ErrorCodes.INVALID_PARAMETER.toJson()));
      } else {
        String method = batchRequest.getMethod();
        JsonObject request = new JsonObject();
        if (!StringUtils.isEmpty(batchRequest.getClazz())) {
          request.put(CommonVerticle.INTERNAL_MSG_ATTR_CLASS, batchRequest.getClazz());
        }
        if (!StringUtils.isEmpty(batchRequest.getObjectId())) {
          request.put(CommonVerticle.INTERNAL_MSG_ATTR_OBJECT_ID, batchRequest.getObjectId());
        }
        if (null != batchRequest.getBody()) {
          request.put(CommonVerticle.INTERNAL_MSG_ATTR_UPDATE_PARAM, batchRequest.getBody());
        }
        DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, method);
        vertx.eventBus().send(Configure.MAILADDRESS_DEMOCLES_QUEUE, request, options, response -> {
          if (response.failed()) {
            tmp.complete(new JsonObject().put("error",
                    new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode())
                            .put("error", response.cause().getMessage())));
          } else {
            vertx.eventBus().send(Configure.MAILADDRESS_DATASTORE_QUEUE, request, options, res -> {
              if (res.failed()) {
                tmp.complete(new JsonObject().put("error",
                        new JsonObject().put("code", ErrorCodes.DATABASE_ERROR.getCode())
                                .put("error", res.cause().getMessage())));
              } else {
                tmp.complete(new JsonObject().put("success", (JsonObject) res.result().body()));
              }
            });
          }
        });
      }
      return tmp;
    }).collect(Collectors.toList());
    CompositeFuture.all(results).setHandler(handler);
  }
}
