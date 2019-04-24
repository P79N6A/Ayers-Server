package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.CommonVerticle;
import cn.leancloud.platform.common.BatchRequest;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.modules.ObjectSpecifics;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectModifyHandler extends CommonHandler {
  public ObjectModifyHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public void create(String clazz, JsonObject body, boolean returnNewDoc, Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperationWithOption(clazz, null, HttpMethod.POST.toString(), null, body, returnNewDoc, handler);
  }

  public void update(String clazz, String objectId, JsonObject query, JsonObject update, boolean returnNewDoc,
                     Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperationWithOption(clazz, objectId, HttpMethod.PUT.toString(), query, update, returnNewDoc, handler);
  }

  public void delete(String clazz, String objectId, JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperation(clazz, objectId, HttpMethod.DELETE.toString(), query, null, handler);
  }

  private static BatchRequest parseBatchRequest(Object req) {
    if (null == req || !(req instanceof JsonObject)) {
      return null;
    }
    JsonObject reqJson = (JsonObject)req;
    String method = reqJson.getString("method");
    String path = reqJson.getString("path");
    JsonObject param = reqJson.getJsonObject("body");
    if (StringUtils.isEmpty(method) || StringUtils.isEmpty(path) || !ObjectSpecifics.validRequestPath(path)) {
      return null;
    }
    String[] pathParts = path.split("/");
    String clazz = (pathParts.length >= 4)? pathParts[3] : "";
    String objectId = (pathParts.length >= 5)? pathParts[4] : "";
    if (!StringUtils.isEmpty(clazz) && !ObjectSpecifics.validClassName(clazz)) {
      return null;
    }
    if (null == param && !method.equalsIgnoreCase("delete")) {
      return null;
    }
    if ((method.equalsIgnoreCase("put") || method.equalsIgnoreCase("delete")) && StringUtils.isEmpty(objectId)) {
      return null;
    }
    return new BatchRequest(method, path, clazz, objectId, param);
  }

  public void batchSave(JsonArray requests, Handler<AsyncResult<CompositeFuture>> handler) {
    List<Future> results = requests.stream().map(req -> {
      final Future<JsonObject> tmp = Future.future();
      BatchRequest batchRequest = parseBatchRequest(req);
      if (null == batchRequest) {
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
          request.put(CommonVerticle.INTERNAL_MSG_ATTR_PARAM, batchRequest.getBody());
        }
        DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, method);
        vertx.eventBus().send(Configure.MAILADDRESS_DEMOCLES_QUEUE, request, options, response -> {
          if (response.failed()) {
            tmp.complete(new JsonObject().put("error",
                    new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("error", response.cause().getMessage())));
          } else {
            vertx.eventBus().send(Configure.MAILADDRESS_DATASTORE_QUEUE, request, options, res -> {
              if (res.failed()) {
                tmp.complete(new JsonObject().put("error",
                        new JsonObject().put("code", ErrorCodes.DATABASE_ERROR.getCode()).put("error", res.cause().getMessage())));
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
