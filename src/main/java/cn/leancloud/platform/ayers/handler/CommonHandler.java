package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.CommonVerticle;
import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.modules.ClassMetaData;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.Objects;

import static cn.leancloud.platform.utils.HandlerUtils.wrapErrorResult;

public class CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(CommonHandler.class);

  protected Vertx vertx;
  protected RoutingContext routingContext;
  protected InMemoryLRUCache<String, JsonObject> classMetaCache;

  public CommonHandler(Vertx vertx, RoutingContext context) {
    this.vertx = vertx;
    this.routingContext = context;
    this.classMetaCache = Configure.getInstance().getClassMetaDataCache();
  }

  protected boolean assertNotNull(Object param, Handler<AsyncResult<JsonObject>> handler) {
    AsyncResult<JsonObject> failure = wrapErrorResult(new InvalidParameterException(ErrorCodes.INVALID_PARAMETER.getMessage()));
    if (null == param) {
      if (null != handler) {
        handler.handle(failure);
      }
      return false;
    } else if (param instanceof String && StringUtils.isEmpty((String) param)) {
      if (null != handler) {
        handler.handle(failure);
      }
      return false;
    } else if (param instanceof JsonObject && ((JsonObject) param).size() < 1) {
      if (null != handler) {
        handler.handle(failure);
      }
      return false;
    }
    return true;
  }

  // path examples:
  //   /1.1/classes/Post
  //   /1.1/classes/Post/objectId
  //   /1.1/installations
  //   /1.1/installations/objectId
  //   /1.1/files
  //   /1.1/files/objectId
  //   /1.1/users
  //   /1.1/users/objectId
  //   /1.1/roles
  //   /1.1/roles/objectId
  protected static Pair<String, String> parseClazzAndObjectId(String path) {
    String clazz = "";
    String objectId = "";
    if (StringUtils.notEmpty(path) && path.startsWith("/1.1/")) {
      String[] pathArray = path.substring("/1.1/".length()).split("/");
      if (pathArray[0].equalsIgnoreCase("classes")) {
        if (pathArray.length >= 2) {
          clazz = pathArray[1];
        }
        if (pathArray.length >= 3) {
          objectId = pathArray[2];
        }
      } else {
        if (pathArray.length >= 2) {
          objectId = pathArray[1];
        }
        if (pathArray[0].equalsIgnoreCase("installations")) {
          clazz = Constraints.INSTALLATION_CLASS;
        } else if (pathArray[0].equalsIgnoreCase("files")) {
          clazz = Constraints.FILE_CLASS;
        } else if (pathArray[0].equalsIgnoreCase("users")) {
          clazz = Constraints.USER_CLASS;
        } else if (pathArray[0].equalsIgnoreCase("roles")) {
          clazz = Constraints.ROLE_CLASS;
        } else {
          objectId = "";
        }
      }
    }

    return Pair.of(clazz, objectId);
  }

  protected static boolean isUpdatableOperation(String operation) {
    if (RequestParse.OP_OBJECT_DELETE.equalsIgnoreCase(operation) || RequestParse.OP_OBJECT_POST.equalsIgnoreCase(operation)
            || RequestParse.OP_OBJECT_PUT.equalsIgnoreCase(operation)) {
      return true;
    }
    return false;
  }

  protected boolean needCheckPermissionAndSchema(String clazz, String operation) {
    if (Constraints.FILE_CLASS.equals(clazz) || Constraints.METADATA_CLASS.equals(clazz)
      /*Constraints.ROLE_CLASS.equals(clazz) || Constraints.INSTALLATION_CLASS.equals(clazz)*/) {
      return false;
    }
    if (RequestParse.OP_CREATE_INDEX.equals(operation) || RequestParse.OP_CREATE_CLASS.equals(operation)
            || RequestParse.OP_DROP_CLASS.equals(operation) // it's really?
            || RequestParse.OP_DELETE_INDEX.equals(operation)|| RequestParse.OP_LIST_INDEX.equals(operation)) {
      return false;
    }
    return true;
  }

  protected void createClass(ClassMetaData metaData, Handler<AsyncResult<JsonObject>> handler) {
    Objects.requireNonNull(metaData);
    Objects.requireNonNull(handler);
    final String clazz = metaData.getClassName();
    sendDataOperationWithOption(clazz, null, RequestParse.OP_CREATE_CLASS, null, metaData,
            true, null, response -> {
              if (response.succeeded()) {
                classMetaCache.putIfAbsent(clazz, response.result());
              }
              handler.handle(response);
            });
  }

  protected void sendDataOperation(String clazz, String objectId, String operation,
                                   JsonObject query, JsonObject update, RequestParse.RequestHeaders headers,
                                   final Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperationWithOption(clazz, objectId, operation, query, update, false, headers, handler);
  }

  protected void sendSchemaOperation(String clazz, String operation, JsonObject data,
                                     final Handler<AsyncResult<JsonObject>> handler) {
    JsonObject request = new JsonObject();
    if (!StringUtils.isEmpty(clazz)) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_CLASS, clazz);
    }
    if (null != data) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_UPDATE_PARAM, data);
    }

    String upperOperation = operation.toUpperCase();
    DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, upperOperation);

    vertx.eventBus().send(Configure.MAIL_ADDRESS_DAMOCLES_QUEUE, request, options, res -> {
      handler.handle(res.map(v -> (JsonObject) v.body()));
    });
  }

  protected JsonObject copyRequestHeaders(RoutingContext context) {
    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(context);
    JsonObject headerJson = headers.toJson()
            .put(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .put("Accept", RequestParse.CONTENT_TYPE_JSON);
    headerJson.remove("useMasterKey");
    return headerJson;
  }

  protected JsonObject copyRequestHeaders(RequestParse.RequestHeaders headers) {
    JsonObject headerJson = headers.toJson()
            .put(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .put("Accept", RequestParse.CONTENT_TYPE_JSON);
    headerJson.remove("useMasterKey");
    return headerJson;
  }

  protected void sendDataOperationWithoutCheck(String clazz, String objectId, String operation,
                                             JsonObject query, JsonObject update, boolean returnNewDocument,
                                             RequestParse.RequestHeaders headers,
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
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_UPDATE_PARAM, update);
    }
    if (null != headers) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_REQUESTHEADERS, headers.toJson());
    }

    String upperOperation = operation.toUpperCase();
    DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, upperOperation);
    logger.debug("send to storage verticle directly...");
    vertx.eventBus().send(Configure.MAIL_ADDRESS_DATASTORE_QUEUE, request, options,
            res -> handler.handle(res.map(v -> (JsonObject) v.body())));
  }

  protected void sendDataOperationWithOption(String clazz, String objectId, String operation,
                                             JsonObject query, JsonObject update, boolean returnNewDocument,
                                             RequestParse.RequestHeaders headers,
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
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_UPDATE_PARAM, update);
    }
    if (null != headers) {
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_REQUESTHEADERS, headers.toJson());
    }

    String upperOperation = operation.toUpperCase();
    DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, upperOperation);
    if (needCheckPermissionAndSchema(clazz, upperOperation)) {
      logger.debug("send to damocles verticle for permission / scheme checking...");

      vertx.eventBus().send(Configure.MAIL_ADDRESS_DAMOCLES_QUEUE, request, options, response -> {
        if (response.failed()) {
          logger.warn("failed to check permission / schema by damocles.");
          handler.handle(response.map(v -> (JsonObject)v.body()));
        } else {
          logger.debug("pass permission / schema check, send to storage verticle then.");
          vertx.eventBus().send(Configure.MAIL_ADDRESS_DATASTORE_QUEUE, request, options,
                  res2 -> handler.handle(res2.map(v -> (JsonObject)v.body())));
        }
      });
    } else {
      logger.debug("send to storage verticle directly...");
      vertx.eventBus().send(Configure.MAIL_ADDRESS_DATASTORE_QUEUE, request, options,
              res -> handler.handle(res.map(v -> (JsonObject) v.body())));
    }
  }
}
