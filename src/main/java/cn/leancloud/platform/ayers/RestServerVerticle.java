package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.UserHandler;
import cn.leancloud.platform.cache.SimpleRedisClient;
import cn.leancloud.platform.common.*;
import com.qiniu.util.Auth;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.immutable.Stream;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hello world!
 */
public class RestServerVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(RestServerVerticle.class);

  private SimpleRedisClient redisClient = new SimpleRedisClient();
  private HttpServer httpServer;

  private void serverDate(RoutingContext context) {
    JsonObject result = new JsonObject().put("__type", "Date").put("iso", Instant.now());
    ok(context, result);
  }

  private void crudInstallation(RoutingContext context) {
    crudCommonData(Constraints.INSTALLATION_CLASS, context);
  }

  private void crudObject(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    if (!ObjectSpecifics.validClassName(clazz)) {
      badRequest(context, ErrorCodes.INVALID_CLASSNAME.toJson());
      return;
    }
    crudCommonData(clazz, context);
  }

  private void crudUser(RoutingContext context) {
    // TODO: fix me.
    badRequest(context, ErrorCodes.OPERATION_NOT_SUPPORT.toJson());
  }

  private void createFileToken(RoutingContext context) {
    Configure configure = Configure.getInstance();
    JsonObject body = parseRequestBody(context);
    String bucketName = configure.fileBucket();
    String fileKey = body.getString("key");
    String name = body.getString("name");
    if (StringUtils.isEmpty(fileKey) || StringUtils.isEmpty(name)) {
      badRequest(context, ErrorCodes.INVALID_PARAMETER.toJson());
      return;
    }

    String mimeType = Constraints.DEFAULT_MIME_TYPE;
    String[] nameParts = name.split("\\.");
    if (nameParts.length > 1) {
      mimeType = MimeUtils.guessMimeTypeFromExtension(nameParts[nameParts.length - 1]);
    }

    Auth auth = Auth.create(configure.fileProviderAccessKey(), configure.fileProviderSecretKey());
    final String token = auth.uploadToken(bucketName, fileKey);
    body.remove("__type");
    body.put(Constraints.BUILTIN_ATTR_FILE_URL, configure.fileDefaultHost() + fileKey)
            .put(Constraints.BUILTIN_ATTR_FILE_MIMETYPE, mimeType)
            .put(Constraints.BUILTIN_ATTR_FILE_PROVIDER,configure.fileProvideName())
            .put(Constraints.BUILTIN_ATTR_FILE_BUCKET, bucketName);

    HttpMethod httpMethod = context.request().method();
    String operation = "";
    if (HttpMethod.GET.equals(httpMethod)) {
      operation = Constraints.OP_OBJECT_QUERY;
    } else if (HttpMethod.DELETE.equals(httpMethod)) {
      operation = Constraints.OP_OBJECT_DELETE;
    } else if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod)) {
      operation = Constraints.OP_OBJECT_UPSERT;
    }

    sendMongoOperationEx(context, Constraints.FILE_CLASS, null, operation, body, file -> {
      file.mergeIn(body);
      file.put(Constraints.PARAM_FILE_TOKEN, token);
      file.put(Constraints.PARAM_FILE_UPLOAD_URL, configure.fileUploadHost());
      // TODO: add appId/objectId/token to cache.
    });
  }

  //{"result":true,
  // "token":"w6ZYeC-arS2yNzZ9"}'
  private void fileUploadCallback(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    boolean result = body.getBoolean("result");
    if (result) {
      // TODO: remove appId/objectId/token from cache.
      ok(context, new JsonObject());
    } else {
      // TODO: remove appId/objectId/token from cache, delete File document.
      ok(context, new JsonObject());
    }
  }

  private void crudSingleFile(RoutingContext context) {
    crudCommonData(Constraints.FILE_CLASS, context);
  }

  private void crudRole(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    HttpMethod httpMethod = context.request().method();
    if (HttpMethod.POST.equals(httpMethod)) {
      String roleName = body.getString("name");
      JsonObject acl = body.getJsonObject("ACL");
      if (!ObjectSpecifics.validRoleName(roleName)) {
        badRequest(context, ErrorCodes.INVALID_ROLENAME.toJson());
        return;
      }
      if (null == acl) {
        badRequest(context, new JsonObject().put("message", "Role ACL is required."));
        return;
      }
    }
    crudCommonData(Constraints.ROLE_CLASS, context);
  }

  private void userSignup(RoutingContext context) {
    CommonResult commonResult = UserHandler.fillSignupParam(parseRequestBody(context));
    if (commonResult.isFailed()) {
      badRequest(context, new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("message", commonResult.getMessage()));
    } else {
      JsonObject body = commonResult.getObject();
      sendMongoOperationEx(context, Constraints.USER_CLASS, null, Constraints.OP_OBJECT_UPSERT, body,
              jsonObject -> jsonObject.put(Constraints.BUILTIN_ATTR_SESSION_TOKEN, body.getString(Constraints.BUILTIN_ATTR_SESSION_TOKEN)));
    }
  }

  private void userSignin(RoutingContext context) {
    CommonResult commonResult = UserHandler.fillSigninParam(parseRequestBody(context));
    if (commonResult.isFailed()) {
      badRequest(context, new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("message", commonResult.getMessage()));
    } else {
      JsonObject body = commonResult.getObject();
      sendMongoOperation(context, Constraints.USER_CLASS, null, Constraints.OP_USER_SIGNIN, body);
    }
  }

  private void crudCommonDataEx(String clazz, RoutingContext context, Handler<JsonObject> handler) {
    String objectId = parseRequestObjectId(context);
    JsonObject body = parseRequestBody(context);
    HttpMethod httpMethod = context.request().method();
    String operation = "";
    if (HttpMethod.GET.equals(httpMethod)) {
      operation = Constraints.OP_OBJECT_QUERY;
    } else if (HttpMethod.DELETE.equals(httpMethod)) {
      operation = Constraints.OP_OBJECT_DELETE;
    } else if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod)) {
      operation = Constraints.OP_OBJECT_UPSERT;
    }

    logger.debug("curl object. clazz=" + clazz + ", objectId=" + objectId + ", method=" + httpMethod + ", param=" + body);

    sendMongoOperationEx(context, clazz, objectId, operation, body, handler);
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

  private void batchWrite(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    JsonArray requests = body.getJsonArray("requests");
    if (null == requests || requests.size() < 1) {
      logger.debug("invalid requests. requests is empty");
      badRequest(context, ErrorCodes.INVALID_PARAMETER.toJson());
      return;
    }

    List<Future> results = requests.stream().map( req -> {
      final Future<JsonObject> tmp = Future.future();
      BatchRequest batchRequest = parseBatchRequest(req);
      if (null == batchRequest) {
        tmp.complete(new JsonObject().put("error", ErrorCodes.INVALID_PARAMETER.toJson()));
      } else {
        String method = batchRequest.getMethod();
        JsonObject request = new JsonObject();
        if (!StringUtils.isEmpty(batchRequest.getClazz())) {
          request.put(Constraints.INTERNAL_MSG_ATTR_CLASS, batchRequest.getClazz());
        }
        if (!StringUtils.isEmpty(batchRequest.getObjectId())) {
          request.put(Constraints.INTERNAL_MSG_ATTR_OBJECT_ID, batchRequest.getObjectId());
        }
        if (null != batchRequest.getBody()) {
          request.put(Constraints.INTERNAL_MSG_ATTR_PARAM, batchRequest.getBody());
        }
        String operation = Constraints.OP_OBJECT_QUERY;
        if ("delete".equalsIgnoreCase(method)) {
          operation = Constraints.OP_OBJECT_DELETE;
        } else if ("post".equalsIgnoreCase(method) || "put".equalsIgnoreCase(method)) {
          operation = Constraints.OP_OBJECT_UPSERT;
        }
        DeliveryOptions options = new DeliveryOptions().addHeader(Constraints.INTERNAL_MSG_HEADER_OP, operation);
        vertx.eventBus().send(Configure.MAILADDRESS_MONGO_QUEUE, request, options, res -> {
          if (res.failed()) {
            tmp.complete(new JsonObject().put("error", new JsonObject().put("code", ErrorCodes.DATABASE_ERROR.getCode()).put("error", res.cause().getMessage())));
          } else {
            tmp.complete(new JsonObject().put("success", (JsonObject) res.result().body()));
          }
        });
      }
      return tmp;
    }).collect(Collectors.toList());

    CompositeFuture.all(results).setHandler( res -> {
      logger.debug("batchWrite all requests finished.");
      int futuresCount = res.result().size();
      JsonArray resultObjects = new JsonArray();
      for (int i = 0; i < futuresCount; i++) {
        resultObjects.add((JsonObject) res.result().resultAt(i));
      }
      ok(context, resultObjects.encode());
    });
  }

  private void crudCommonData(String clazz, RoutingContext context) {
    crudCommonDataEx(clazz, context, null);
  }

  private void sendMongoOperationEx(RoutingContext context, String clazz, String objectId, String operation,
                                    JsonObject param, final Handler<JsonObject> handler) {
    JsonObject request = new JsonObject();
    if (!StringUtils.isEmpty(clazz)) {
      request.put(Constraints.INTERNAL_MSG_ATTR_CLASS, clazz);
    }
    if (!StringUtils.isEmpty(objectId)) {
      request.put(Constraints.INTERNAL_MSG_ATTR_OBJECT_ID, objectId);
    }
    if (null != param) {
      request.put(Constraints.INTERNAL_MSG_ATTR_PARAM, param);
    }
    DeliveryOptions options = new DeliveryOptions().addHeader(Constraints.INTERNAL_MSG_HEADER_OP, operation);
    vertx.eventBus().send(Configure.MAILADDRESS_MONGO_QUEUE, request, options, reply -> {
      if (reply.failed()) {
        if (reply.cause() instanceof ReplyException) {
          int failureCode = ((ReplyException) reply.cause()).failureCode();
          JsonObject responseJson = new JsonObject().put("code", failureCode).put("error", reply.cause().getMessage());
          if (failureCode == ErrorCodes.DATABASE_ERROR.getCode() || failureCode == ErrorCodes.INTERNAL_ERROR.getCode()) {
            internalServerError(context, responseJson);
          } else {
            badRequest(context, responseJson);
          }
        } else {
          context.fail(reply.cause());
        }
      } else {
        JsonObject result = (JsonObject) reply.result().body();
        if (null != handler) {
          handler.handle(result);
        }
        if (Constraints.OP_OBJECT_UPSERT.equalsIgnoreCase(operation) && StringUtils.isEmpty(objectId)) {
          //Status: 201 Created
          //Location: https://heqfq0sw.api.lncld.net/1.1/classes/Post/<objectId>
          JsonObject location = new JsonObject().put("Location", context.request().absoluteURI() + result.getString(Constraints.CLASS_ATTR_OBJECT_ID));
          created(context, location, result);
        } else {
          ok(context, result);
        }
      }
    });
  }
  private void sendMongoOperation(RoutingContext context, String clazz, String objectId, String operation, JsonObject param) {
    sendMongoOperationEx(context, clazz, objectId, operation, param, null);
  }

  private void healthcheck(RoutingContext context) {
    logger.debug("response for healthcheck.");
    JsonObject result = new JsonObject().put("status", "green")
            .put("availableProcessors", Runtime.getRuntime().availableProcessors())
            .put("freeMemory", Runtime.getRuntime().freeMemory())
            .put("totalMemory", Runtime.getRuntime().totalMemory())
            .put("maxMemory", Runtime.getRuntime().maxMemory()).put("activeThreads", Thread.activeCount())
            .put("config", Configure.getInstance().getSettings());
    ok(context, result);
  }

  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    httpServer = vertx.createHttpServer(new HttpServerOptions()
            .setTcpFastOpen(true)
            .setTcpCork(true)
            .setTcpQuickAck(true)
            .setReusePort(true));

    ApplicationAuthenticator appKeyValidator = new ApplicationAuthenticator("testkey");
    HTTPRequestValidationHandler appKeyValidationHandler = HTTPRequestValidationHandler.create().addCustomValidatorFunction(appKeyValidator);
    HTTPRequestValidationHandler sessionValidationHandler = HTTPRequestValidationHandler.create().addCustomValidatorFunction(new SessionValidator());

    Router router = Router.router(vertx);

    router.get("/").handler(rc -> ok(rc,
            new JsonObject()
                    .put("Description", "Ayers server supported by LeanCloud(https://leancloud.cn).")
                    .put("License", "Apache License Version 2.0")
                    .put("Contacts", "support@leancloud.rocks")));
    router.get("/ping").handler(this::healthcheck);

    SessionStore sessionStore = LocalSessionStore.create(vertx, "ayers.sessionmap");
    router.route("/1.1/*").handler(BodyHandler.create())
            .handler(CookieHandler.create())
            .handler(SessionHandler.create(sessionStore))
            .handler(appKeyValidationHandler);

    /**
     * Storage Service endpoints.
     */
    router.get("/1.1/date").handler(this::serverDate);

    router.post("/1.1/classes/:clazz").handler(this::crudObject);
    router.get("/1.1/classes/:clazz").handler(this::crudObject);
    router.get("/1.1/classes/:clazz/:objectId").handler(this::crudObject);
    router.put("/1.1/classes/:clazz/:objectId").handler(this::crudObject);
    router.delete("/1.1/classes/:clazz/:objectId").handler(this::crudObject);

    router.post("/1.1/installations").handler(this::crudInstallation);
    router.put("/1.1/installations/:objectId").handler(this::crudInstallation);
    router.get("/1.1/installations").handler(this::crudInstallation);
    router.get("/1.1/installations/:objectId").handler(this::crudInstallation);
    router.delete("/1.1/installations/:objectId").handler(this::crudInstallation);

    router.post("/1.1/users").handler(this::userSignup);
    router.post("/1.1/login").handler(this::userSignin);

    router.get("/1.1/users/:objectId").handler(this::crudUser);
    router.get("/1.1/users").handler(this::crudUser);
    router.delete("/1.1/users/:objectId").handler(this::crudUser);
    router.put("/1.1/users/:objectId").handler(this::crudUser);

    router.get("/1.1/users/me").handler(sessionValidationHandler).handler(this::crudUser);
    router.put("/1.1/users/:objectId/refreshSessionToken").handler(sessionValidationHandler).handler(this::crudUser);
    router.put("/1.1/users/:objectId/updatePassword").handler(sessionValidationHandler).handler(this::crudUser);
    router.post("/1.1/requestEmailVerify").handler(this::crudUser);
    router.post("/1.1/requestPasswordReset").handler(this::crudUser);

    router.post("/1.1/fileTokens").handler(this::createFileToken);
    router.post("/1.1/fileCallback").handler(this::fileUploadCallback);
    router.get("/1.1/files/:objectId").handler(this::crudSingleFile);
    router.delete("/1.1/files/:objectId").handler(this::crudSingleFile);
    router.post("/1.1/files").handler(this::crudSingleFile);

    router.post("/1.1/roles").handler(this::crudRole);
    router.get("/1.1/roles").handler(this::crudRole);
    router.get("/1.1/roles/:objectId").handler(this::crudRole);
    router.put("/1.1/roles/:objectId").handler(this::crudRole);
    router.delete("/1.1/roles/:objectId").handler(this::crudRole);

    router.post("/1.1/batch").handler(this::batchWrite);

    router.errorHandler(400, routingContext -> {
      if (routingContext.failure() instanceof ValidationException) {
        // Something went wrong during validation!
        String validationErrorMessage = routingContext.failure().getMessage();
        logger.warn("invalid request. cause: " + validationErrorMessage);
      } else {
        // Unknown 400 failure happened
        routingContext.response().setStatusCode(400).end();
      }
    });
    router.errorHandler(HttpStatus.SC_INTERNAL_SERVER_ERROR, routingContext -> {
      if (routingContext.failed()) {
        logger.warn("internal error. cause: " + routingContext.failure().getMessage());
      }
    });

    int portNumber = Configure.getInstance().listenPort();
    httpServer.requestHandler(router).listen(portNumber, ar -> {
      if (ar.failed()) {
        logger.error("could not start a http server, cause: ", ar.cause());
        future.fail(ar.cause());
      } else {
        logger.info("http server running on port 8080");
        future.complete();
      }
    });
    return future;
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Configure configure = Configure.getInstance();
    String hosts = configure.redisHosts();
    String type = configure.redisHAType(); // ignore
    String[] hostParts = hosts.split(":");
    final int redisPort = (hostParts.length > 1)? Integer.valueOf(hostParts[1]) : 6379;
    String redisHost = hostParts[0];

    startHttpServer().compose(ar -> {
      Future<Void> future = Future.future();
      redisClient.connect(vertx, redisHost, redisPort, future);
      return future;
    }).setHandler(ar -> {
      logger.info("start RestServerVerticle...");
      startFuture.complete();
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    httpServer.close();
    redisClient.close();
    logger.info("stop RestServerVerticle...");
    stopFuture.complete();
  }
}
