package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.cache.UnifiedCache;
import cn.leancloud.platform.common.BatchRequest;
import cn.leancloud.platform.engine.EngineHookProxy;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.engine.HookType;
import cn.leancloud.platform.modules.*;
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
import java.util.Objects;
import java.util.stream.Collectors;

public class ObjectModifyHandler extends CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(ObjectModifyHandler.class);

  private EngineHookProxy hookProxy;
  private boolean enableCreateClassByClient = true;

  public ObjectModifyHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
    this.hookProxy = EngineHookProxy.getInstance(vertx);
  }

  private void appendACLIfNeed(String clazz, JsonObject body, ClassMetaData classMetaData, String currentUserObjectId) {
    if (body.containsKey(LeanObject.BUILTIN_ATTR_ACL)) {
      return;
    }
    if (null == classMetaData || null == classMetaData.getACLTemplate()) {
      return;
    }
    JsonObject aclTemplate = classMetaData.getACLTemplate();
    ObjectACLTemplate template = ObjectACLTemplate.Builder.build(aclTemplate);
    ACL acl = template.genACL4User(currentUserObjectId, null);
    body.put(LeanObject.BUILTIN_ATTR_ACL, acl.toJson());
  }

  private void sendCreateOperation(String clazz, JsonObject body, boolean returnNewDoc, Handler<AsyncResult<JsonObject>> handler) {
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

  private void executeCreateOperation(ClassMetaData classMetaData, String clazz, JsonObject body, boolean returnNewDoc,
                                      Handler<AsyncResult<JsonObject>> handler) {
    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(this.routingContext);
    classPermissionCheck(clazz, body, headers, ClassPermission.OP.CREATE, response -> {
      if (response.failed()) {
        logger.warn("failed to check class permission. cause: " + response.cause().getMessage());
        handler.handle(wrapErrorResult(new UnsupportedOperationException("Class permission check failed.")));
      } else if (!response.result()) {
        logger.debug("not allowed operation bcz of class permission.");
        handler.handle(wrapErrorResult(new UnsupportedOperationException("Class permission check failed.")));
      } else {
        String sessionToken = headers.getSessionToken();
        String currentUserId = null;
        if (StringUtils.notEmpty(sessionToken)) {
          JsonObject currentUser = UnifiedCache.getGlobalInstance().get(sessionToken);
          if (null != currentUser) {
            currentUserId = currentUser.getString(LeanObject.ATTR_NAME_OBJECTID);
          }
          appendACLIfNeed(clazz, body, classMetaData, currentUserId);
        }
        sendCreateOperation(clazz, body, returnNewDoc, handler);
      }
    });

  }

  public void createSingleObject(String clazz, JsonObject body, boolean returnNewDoc, Handler<AsyncResult<JsonObject>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(body);
    Objects.requireNonNull(handler);

    //make sure class existed.
    ClassMetaData classMetaData = (ClassMetaData) this.classMetaCache.get(clazz);
    if (null == classMetaData) {
      if (!enableCreateClassByClient) {
        logger.debug("create class from REST isn't allowed.");
        handler.handle(wrapErrorResult(new UnsupportedOperationException("It's forbidden to create class from REST API")));
      } else {
        MetaDataHandler handler1 = new MetaDataHandler(vertx, routingContext);
        handler1.createClass(clazz, "normal", null, response -> {
          if (response.failed()) {
            logger.warn("failed to create class. cause:" + response.cause().getMessage());
            handler.handle(response);
          } else {
            executeCreateOperation(classMetaData, clazz, body, returnNewDoc, handler);
          }
        });
      }
    } else {
      executeCreateOperation(classMetaData, clazz, body, returnNewDoc, handler);
    }
  }

  private void sendUpdateOperation(String clazz, String objectId, JsonObject query, JsonObject update, boolean returnNewDoc,
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
  public void updateSingleObject(String clazz, String objectId, JsonObject query, JsonObject update, boolean returnNewDoc,
                                 Handler<AsyncResult<JsonObject>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(objectId);
    Objects.requireNonNull(update);
    Objects.requireNonNull(handler);

    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(this.routingContext);
    classPermissionCheck(clazz, update, headers, ClassPermission.OP.UPDATE, response -> {
      if (response.failed()) {
        logger.warn("failed to check class permission. cause: " + response.cause().getMessage());
        handler.handle(wrapErrorResult(new UnsupportedOperationException("Class permission check failed.")));
      } else if (!response.result()) {
        logger.debug("not allowed operation bcz of class permission.");
        handler.handle(wrapErrorResult(new UnsupportedOperationException("Class permission check failed.")));
      } else {
        objectACLCheck(clazz, objectId, headers, ClassPermission.OP.UPDATE, res2 -> {
          if (res2.failed()) {
            logger.warn("failed to check object ACL. cause: " + res2.cause().getMessage());
            handler.handle(wrapErrorResult(new UnsupportedOperationException("Object ACL permission check failed.")));
          } else if (!res2.result()) {
            logger.debug("not allowed operation bcz of object ACL.");
            handler.handle(wrapErrorResult(new UnsupportedOperationException("Object ACL permission check failed.")));
          } else {
            sendUpdateOperation(clazz, objectId, query, update, returnNewDoc, handler);
          }
        });
      }
    });
  }

  private void sendDeleteOperation(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
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
  public void deleteSingleObject(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(objectId);
    Objects.requireNonNull(handler);

    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(this.routingContext);
    classPermissionCheck(clazz, null, headers, ClassPermission.OP.DELETE, response -> {
      if (response.failed()) {
        logger.warn("failed to check class permission. cause: " + response.cause().getMessage());
        handler.handle(wrapErrorResult(new UnsupportedOperationException("Class permission check failed.")));
      } else if (!response.result()) {
        handler.handle(wrapErrorResult(new UnsupportedOperationException("Class permission check failed.")));
      } else {
        objectACLCheck(clazz, objectId, headers, ClassPermission.OP.DELETE, res2 -> {
          if (res2.failed()) {
            logger.warn("failed to check object ACL. cause: " + res2.cause().getMessage());
            handler.handle(wrapErrorResult(new UnsupportedOperationException("Object ACL permission check failed.")));
          } else if (!res2.result()) {
            logger.debug("not allowed operation bcz of object ACL.");
            handler.handle(wrapErrorResult(new UnsupportedOperationException("Object ACL permission check failed.")));
          } else {
            sendDeleteOperation(clazz, objectId, query, handler);
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
          createSingleObject(clazz, body, false, response -> processBatchResult(response, tmp));
        } else if (RequestParse.OP_OBJECT_PUT.equalsIgnoreCase(method)) {
          updateSingleObject(clazz, objectId, null, body, false, response -> processBatchResult(response, tmp));
        } else if (RequestParse.OP_OBJECT_DELETE.equalsIgnoreCase(method)) {
          deleteSingleObject(clazz, objectId, body, response -> processBatchResult(response, tmp));
        }
      }
      return tmp;
    }).collect(Collectors.toList());
    CompositeFuture.all(results).setHandler(handler);
  }
}
