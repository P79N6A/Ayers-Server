package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.common.BatchRequest;
import cn.leancloud.platform.engine.EngineHookProxy;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.engine.HookType;
import cn.leancloud.platform.modules.ObjectSpecifics;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.impl.NoStackTraceThrowable;
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
    JsonObject headers = copyRequestHeaders(routingContext);
    this.hookProxy.call(clazz, HookType.BeforeSave, body, headers, routingContext, res -> {
      if (res.failed()) {
        logger.warn("lean engine hook failed. cause: " + res.cause());
        handler.handle(res);
      } else {
        JsonObject hookedBody = res.result();
        if (null == hookedBody) {
          handler.handle(wrapErrorResult(new IllegalAccessException("operation failed by BeforeSave Hook.")));
        } else {
          logger.debug("get lean enginne hook result: " + hookedBody);
          sendDataOperationWithOption(clazz, null, RequestParse.OP_OBJECT_POST, null, hookedBody,
                  returnNewDoc, response -> {
                    if (response.failed()) {
                      handler.handle(response);
                    } else {
                      // maybe we need calculate request-sign again.
                      this.hookProxy.call(clazz, HookType.AfterSave, new JsonObject().put("object", response.result()), headers, routingContext, any -> {
                        // ignore after hook result.
                        handler.handle(response);
                      });
                    }
                  });
        }
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
        if (null == hookedBody) {
          handler.handle(wrapErrorResult(new IllegalAccessException("operation failed by BeforeUpdate Hook.")));
        } else {
          logger.debug("get lean enginne hook result: " + hookedBody);
          sendDataOperationWithOption(clazz, objectId, RequestParse.OP_OBJECT_PUT, query, hookedBody, returnNewDoc, responnse -> {
            if (responnse.failed()) {
              handler.handle(responnse);
            } else {
              this.hookProxy.call(clazz, HookType.AfterUpdate, new JsonObject().put("object", responnse.result()), headers, routingContext, any -> {
                // ignore after hook result.
                handler.handle(responnse);
              });
            }
          });
        }
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
        sendDataOperation(clazz, objectId, RequestParse.OP_OBJECT_DELETE, query, null, response ->{
          handler.handle(response);
          if (response.succeeded()) {
            this.hookProxy.call(clazz, HookType.AfterDetele, query, headers, routingContext, any -> {
              // ignore it.
            });
          }
        });
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
            || !ObjectSpecifics.validateRequestPath(path)) {
      logger.warn("invalid request. method:" + method + ", path:" + path);
      return null;
    }
    if (StringUtils.isEmpty(clazz) && !ObjectSpecifics.validateClassName(clazz)) {
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

  private void processBatchResult(AsyncResult<JsonObject> response, Future<JsonObject> tmp) {
    if (response.failed()) {
      if (response.cause() instanceof ReplyException) {
        int failureCode = ((ReplyException) response.cause()).failureCode();
        JsonObject responseJson = new JsonObject().put("code", failureCode).put("error", response.cause().getMessage());
        tmp.complete(new JsonObject().put("error", responseJson));
      } else if (response.cause() instanceof NoStackTraceThrowable) {
        // failed by leanengine hook func.
        tmp.complete(new JsonObject().put("error", new JsonObject(response.cause().getMessage())));
      } else {
        tmp.complete(new JsonObject().put("error",
                new JsonObject().put("code", ErrorCodes.INTERNAL_ERROR.getCode())
                        .put("error", response.cause().getMessage())));
      }
    } else {
      tmp.complete(new JsonObject().put("success", response.result()));
    }
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
        String clazz = batchRequest.getClazz();
        String objectId = batchRequest.getObjectId();
        JsonObject body = batchRequest.getBody();
        if (RequestParse.OP_OBJECT_POST.equalsIgnoreCase(method)) {
          create(clazz, body, false, response -> processBatchResult(response, tmp));
        } else if (RequestParse.OP_OBJECT_PUT.equalsIgnoreCase(method)) {
          update(clazz, objectId, null, body, false, response -> processBatchResult(response, tmp));
        } else if (RequestParse.OP_OBJECT_DELETE.equalsIgnoreCase(method)) {
          delete(clazz, objectId, body, response -> processBatchResult(response, tmp));
        }
      }
      return tmp;
    }).collect(Collectors.toList());
    CompositeFuture.all(results).setHandler(handler);
  }
}
