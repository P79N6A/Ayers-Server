package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.CommonVerticle;
import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.cache.UnifiedCache;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.modules.ClassMetaData;
import cn.leancloud.platform.modules.ClassPermission;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.Objects;

import static cn.leancloud.platform.ayers.handler.ObjectQueryHandler.QUERY_KEY_KEYS;

public class CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(CommonHandler.class);

  protected Vertx vertx;
  protected RoutingContext routingContext;
  protected InMemoryLRUCache classMetaCache;

  public CommonHandler(Vertx vertx, RoutingContext context) {
    this.vertx = vertx;
    this.routingContext = context;
    this.classMetaCache = Configure.getInstance().getClassMetaDataCache();
  }

  protected static <T> AsyncResult<T> wrapActualResult(T value) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return value;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }

  protected static <T> AsyncResult wrapErrorResult(Throwable throwable) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return throwable;
      }

      @Override
      public boolean succeeded() {
        return false;
      }

      @Override
      public boolean failed() {
        return true;
      }
    };
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

  protected void classPermissionCheck(String clazz, JsonObject body, RequestParse.RequestHeaders request, ClassPermission.OP op,
                                      Handler<AsyncResult<Boolean>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(request);
    Objects.requireNonNull(handler);
    if (request.isUseMasterKey()) {
      handler.handle(wrapActualResult(true));
      return;
    }
    JsonObject classMeta = Configure.getInstance().getClassMetaDataCache().get(clazz);
    if (null == classMeta) {
      // always allow create class.
      handler.handle(wrapActualResult(true));
      return;
    }
    ClassPermission classPermission = ClassPermission.fromJson(ClassMetaData.fromJson(classMeta).getClassPermissions());
    // check public permission at first.
    if (classPermission.checkOperation(op, null, null)) {
      handler.handle(wrapActualResult(true));
      return;
    }
    String sessionToken = request.getSessionToken();
    if (StringUtils.isEmpty(sessionToken)) {
      // unauth user.
      handler.handle(wrapActualResult(false));
      return;
    }
    // FIXME: maybe we need to fetch user's roles.
    JsonObject user = UnifiedCache.getGlobalInstance().get(sessionToken);
    if (null != user) {
      boolean allowed = classPermission.checkOperation(op, user.getString(LeanObject.ATTR_NAME_OBJECTID), null);
      handler.handle(wrapActualResult(allowed));
      return;
    }
    UserHandler userHandler = new UserHandler(this.vertx, this.routingContext);
    userHandler.validateSessionToken(sessionToken, response -> {
      if (response.failed() || null == response.result()) {
        handler.handle(wrapActualResult(false));
      } else {
        JsonObject newUser = response.result();
        boolean allowed = classPermission.checkOperation(op, newUser.getString(LeanObject.ATTR_NAME_OBJECTID), null);
        handler.handle(wrapActualResult(allowed));
        return;
      }
    });
  }

  protected void objectACLCheck(String clazz, String objectId, RequestParse.RequestHeaders request, ClassPermission.OP op,
                                Handler<AsyncResult<Boolean>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(objectId);
    Objects.requireNonNull(request);
    Objects.requireNonNull(handler);
    if (request.isUseMasterKey()) {
      // for masterkey, any operation is allowed.
      handler.handle(wrapActualResult(true));
      return;
    }
    ObjectQueryHandler queryHandler = new ObjectQueryHandler(this.vertx, this.routingContext);
    queryHandler.query(clazz, objectId, new JsonObject().put(QUERY_KEY_KEYS, LeanObject.BUILTIN_ATTR_ACL), response -> {
      if (response.failed()) {
        // database error.
        handler.handle(wrapErrorResult(response.cause()));
        return;
      } else if (null == response.result() || response.result().size() < 1) {
        // not found.
        handler.handle(wrapActualResult(false));
        return;
      } else {
        LeanObject targetObject = LeanObject.fromJson(clazz, response.result());
        boolean isWriteOp = op.equals(ClassPermission.OP.UPDATE) || op.equals(ClassPermission.OP.DELETE);
        String sessionToken = request.getSessionToken();
        if (StringUtils.isEmpty(sessionToken)) {
          boolean allowed = targetObject.checkOperationByACL(isWriteOp, null, null);
          handler.handle(wrapActualResult(allowed));
          return;
        } else {
          // FIXME: maybe we need to fetch user's roles.
          JsonObject user = UnifiedCache.getGlobalInstance().get(sessionToken);
          if (null != user) {
            boolean allowed = targetObject.checkOperationByACL(isWriteOp, user.getString(LeanObject.ATTR_NAME_OBJECTID), null);
            handler.handle(wrapActualResult(allowed));
            return;
          }
          UserHandler userHandler = new UserHandler(this.vertx, this.routingContext);
          userHandler.validateSessionToken(sessionToken, response2 -> {
            if (response2.failed()) {
              handler.handle(wrapErrorResult(response2.cause()));
              return;
            } else {
              JsonObject newUser = response2.result();
              String userObjectId = null == newUser ? null : newUser.getString(LeanObject.ATTR_NAME_OBJECTID);
              boolean allowed = targetObject.checkOperationByACL(isWriteOp, userObjectId, null);
              handler.handle(wrapActualResult(allowed));
              return;
            }
          });
        }
      }
    });
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

  protected boolean shouldChangeSchema(String clazz, String operation) {
    return HttpMethod.POST.toString().equals(operation) || HttpMethod.PUT.toString().equals(operation)
            || RequestParse.OP_USER_SIGNUP.equals(operation) || RequestParse.OP_USER_SIGNIN.equals(operation);
  }

  protected void sendDataOperation(String clazz, String objectId, String operation,
                                   JsonObject query, JsonObject update, final Handler<AsyncResult<JsonObject>> handler) {
    sendDataOperationWithOption(clazz, objectId, operation, query, update, false, handler);
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
    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(context);
    JsonObject headerJson = headers.toHeaders()
            .put(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .put("Accept", RequestParse.CONTENT_TYPE_JSON);
    return headerJson;
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
      request.put(CommonVerticle.INTERNAL_MSG_ATTR_UPDATE_PARAM, update);
    }
    String upperOperation = operation.toUpperCase();
    DeliveryOptions options = new DeliveryOptions().addHeader(CommonVerticle.INTERNAL_MSG_HEADER_OP, upperOperation);
    if (shouldChangeSchema(clazz, upperOperation)) {
      logger.debug("send to damocles for scheme checking...");
      vertx.eventBus().send(Configure.MAIL_ADDRESS_DAMOCLES_QUEUE, request, options, response -> {
        if (response.failed()) {
          logger.warn("failed to check schema by damocles.");
          handler.handle(response.map(v -> (JsonObject)v.body()));
        } else {
          logger.debug("pass schema check, send to storage verticle.");
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
