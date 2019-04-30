package cn.leancloud.platform.ayers;

import cn.leancloud.platform.auth.ApplicationAuthenticator;
import cn.leancloud.platform.auth.SessionValidator;
import cn.leancloud.platform.ayers.handler.*;
import cn.leancloud.platform.cache.SimpleRedisClient;
import cn.leancloud.platform.common.*;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.ObjectSpecifics;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import io.vertx.core.Handler;
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
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

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
      logger.warn("invalid class name: " + clazz);
      badRequest(context, ErrorCodes.INVALID_CLASSNAME.toJson());
      return;
    }
    crudCommonData(clazz, context);
  }

  private void crudUser(RoutingContext context) {
    crudCommonData(Constraints.USER_CLASS, context);
  }

  private void validateUser(RoutingContext context) {
    String sessionToken = RequestParse.getSessionToken(context);
    if (StringUtils.isEmpty(sessionToken)) {
      JsonObject request = parseRequestBody(context);
      if (null != request) {
        sessionToken = request.getString(UserHandler.PARAM_SESSION_TOKEN);
      }
    }
    if (StringUtils.isEmpty(sessionToken)) {
      badRequest(context, new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("error", "session token is required."));
    } else {
      UserHandler handler = new UserHandler(vertx, context);
      handler.validateSessionToken(null, sessionToken, res -> {
        if (res.failed()) {
          internalServerError(context, ErrorCodes.DATABASE_ERROR.toJson());
        } else if (null == res.result() || res.result().size() < 1) {
          notFound(context, new JsonObject());
        } else {
          ok(context, res.result());
        }
      });
    }
  }

  private void refreshUserToken(RoutingContext context) {
    String sessionToken = RequestParse.getSessionToken(context);
    String objectId = parseRequestObjectId(context);
    if (StringUtils.isEmpty(sessionToken)) {
      badRequest(context, new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("error", "session_token is required."));
    } else {
      String newSessionToken = StringUtils.getRandomString(Constraints.SESSION_TOKEN_LENGTH);
      UserHandler handler = new UserHandler(vertx, context);
      handler.updateSessionToken(objectId, sessionToken, newSessionToken, res -> {
        if (res.failed()) {
          internalServerError(context, ErrorCodes.DATABASE_ERROR.toJson());
        } else if (null == res.result()) {
          notFound(context, ErrorCodes.USER_NOT_FOUND.toJson());
        } else {
          JsonObject resultJson = res.result();
          ok(context, resultJson);
        }
      });
    }
  }

  private void updateUserPassword(RoutingContext context) {
    // TODO: fix me.
    badRequest(context, ErrorCodes.OPERATION_NOT_SUPPORT.toJson());
  }

  private void requestEmailVerify(RoutingContext context) {
    // TODO: fix me.
    badRequest(context, ErrorCodes.OPERATION_NOT_SUPPORT.toJson());
  }

  private void requestPasswordReset(RoutingContext context) {
    // TODO: fix me.
    badRequest(context, ErrorCodes.OPERATION_NOT_SUPPORT.toJson());
  }

  private void createFileToken(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    String fileKey = body.getString("key");
    String name = body.getString("name");
    if (StringUtils.isEmpty(fileKey) || StringUtils.isEmpty(name)) {
      badRequest(context, ErrorCodes.INVALID_PARAMETER.toJson());
      return;
    }

    FileHandler fileHandler = new FileHandler(vertx, context);
    fileHandler.createFileToken(context.request().method(), name, fileKey, body, res -> {
      if (res.failed()) {
        internalServerError(context, ErrorCodes.INTERNAL_ERROR.toJson());
      } else {
        ok(context, res.result());
      }
    });
  }

  //{"result":true,
  // "token":"w6ZYeC-arS2yNzZ9"}'
  private void fileUploadCallback(RoutingContext context) {
    JsonObject body = parseRequestBody(context);

    FileHandler fileHandler = new FileHandler(vertx, context);
    fileHandler.uploadCallback(body, res -> {
      ok(context, new JsonObject());
    });
  }

  private void crudSingleFile(RoutingContext context) {
    crudCommonData(Constraints.FILE_CLASS, context);
  }

  private void crudRole(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    HttpMethod httpMethod = context.request().method();
    if (HttpMethod.POST.equals(httpMethod)) {
      String roleName = body.getString("name");
      JsonObject acl = body.getJsonObject(LeanObject.BUILTIN_ATTR_ACL);
      if (!ObjectSpecifics.validRoleName(roleName)) {
        badRequest(context, ErrorCodes.INVALID_ROLENAME.toJson());
        return;
      }
      if (null == acl || acl.size() < 1) {
        badRequest(context, new JsonObject().put("message", "Role ACL is required."));
        return;
      }
    }

    crudCommonData(Constraints.ROLE_CLASS, context);
  }

  private void userSignup(RoutingContext context) {
    JsonObject requestBody = parseRequestBody(context);
    CommonResult commonResult = UserHandler.parseSignupParam(requestBody);
    if (commonResult.isFailed()) {
      badRequest(context, new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("message", commonResult.getMessage()));
    } else {
      JsonObject body = commonResult.getObject();
      String mobilePhone = body.getString(UserHandler.PARAM_MOBILEPHONE);
      String smsCode = body.getString(UserHandler.PARAM_SMSCODE);
      if (StringUtils.notEmpty(mobilePhone) && StringUtils.notEmpty(smsCode)) {
        SmsCodeHandler smsHandler = new SmsCodeHandler(vertx, context);
        smsHandler.verifySmsCode(new JsonObject().put(UserHandler.PARAM_MOBILEPHONE, mobilePhone), smsCode, res -> {
          if (res.failed()) {
            badRequest(context, ErrorCodes.INVALID_SMS_CODE.toJson());
          } else {
            body.remove(UserHandler.PARAM_SMSCODE);
            UserHandler handler = new UserHandler(vertx, context);
            handler.signup(body, res2 -> {
              if (res2.failed()) {
                internalServerError(context, ErrorCodes.INTERNAL_ERROR.toJson());
              } else {
                // TODO: add sessionToken - userObject to cache.
                ok(context, res2.result());
              }
            });
          }
        });
      } else {
        UserHandler handler = new UserHandler(vertx, context);
        handler.signup(body, res -> {
          if (res.failed()) {
            internalServerError(context, ErrorCodes.INTERNAL_ERROR.toJson());
          } else {
            // TODO: add sessionToken - userObject to cache.
            ok(context, res.result());
          }
        });

      }
    }
  }

  private void userSignin(RoutingContext context) {
    CommonResult commonResult = UserHandler.parseSigninParam(parseRequestBody(context));
    if (commonResult.isFailed()) {
      badRequest(context, new JsonObject().put("code", ErrorCodes.INVALID_PARAMETER.getCode()).put("message", commonResult.getMessage()));
    } else {
      JsonObject body = commonResult.getObject();
      String mobilePhone = body.getString(UserHandler.PARAM_MOBILEPHONE);
      String smsCode = body.getString(UserHandler.PARAM_SMSCODE);
      if (StringUtils.notEmpty(mobilePhone) && StringUtils.notEmpty(smsCode)) {
        SmsCodeHandler smsHandler = new SmsCodeHandler(vertx, context);
        smsHandler.verifySmsCode(new JsonObject().put(UserHandler.PARAM_MOBILEPHONE, mobilePhone), smsCode, response -> {
          if (response.failed()) {
            badRequest(context, ErrorCodes.INVALID_SMS_CODE.toJson());
          } else {
            body.remove(UserHandler.PARAM_SMSCODE);
            UserHandler handler = new UserHandler(vertx, context);
            handler.signin(body, res -> {
              if (res.failed()) {
                notFound(context, new JsonObject().put("error", res.cause().getMessage()));
              } else if (null == res.result()) {
                notFound(context, ErrorCodes.PASSWORD_WRONG.toJson());
              } else {
                // TODO: add sessionToken - userObject to cache.
                ok(context, res.result());
              }
            });
          }
        });
      } else {
        UserHandler handler = new UserHandler(vertx, context);
        handler.signin(body, res -> {
          if (res.failed()) {
            notFound(context, new JsonObject().put("error", res.cause().getMessage()));
          } else if (null == res.result()) {
            notFound(context, ErrorCodes.PASSWORD_WRONG.toJson());
          } else {
            // TODO: add sessionToken - userObject to cache.
            ok(context, res.result());
          }
        });
      }
    }
  }

  private void crudCommonDataEx(String clazz, RoutingContext context, Handler<JsonObject> handler) {
    String objectId = parseRequestObjectId(context);
    JsonObject body = parseRequestBody(context);
    HttpMethod httpMethod = context.request().method();
    String fetchWhenSave = context.request().getParam("fetchWhenSave");
    boolean returnNewObject = Boolean.valueOf(fetchWhenSave);

    logger.debug("curl object. clazz=" + clazz + ", objectId=" + objectId + ", method=" + httpMethod
            + ", param=" + body + ", fetchWhenSave=" + fetchWhenSave);

    Handler<AsyncResult<JsonObject>> replayHandler = reply -> {
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
          internalServerError(context, ErrorCodes.DATABASE_ERROR.toJson());
        }
      } else {
        JsonObject result = reply.result();
        if (null != handler) {
          handler.handle(result);
        }
        logger.debug("rest server response: " + result);
        if (HttpMethod.POST == httpMethod && StringUtils.isEmpty(objectId)) {
          //Status: 201 Created
          //Location: https://heqfq0sw.api.lncld.net/1.1/classes/Post/<objectId>
          JsonObject location = new JsonObject().put("Location", Configure.getInstance().getBaseHost() + "/"
                  + context.request().path() + result.getString(LeanObject.ATTR_NAME_OBJECTID));
          created(context, location, result);
        } else {
          ok(context, result);
        }
      }
    };
    if (HttpMethod.GET.equals(httpMethod)) {
      ObjectQueryHandler queryHandler = new ObjectQueryHandler(vertx, context);
      queryHandler.query(clazz, objectId, body, replayHandler);
    } else {
      ObjectModifyHandler modifyHandler = new ObjectModifyHandler(vertx, context);
      if (HttpMethod.POST.equals(httpMethod)) {
        modifyHandler.create(clazz, body, returnNewObject, replayHandler);
      } else if (HttpMethod.PUT.equals(httpMethod)) {
        modifyHandler.update(clazz, objectId, null, body, returnNewObject, replayHandler);
      } else if (HttpMethod.DELETE.equals(httpMethod)){
        modifyHandler.delete(clazz, objectId, null, replayHandler);
      } else {
        logger.warn("not support http method: " + httpMethod);
        forbidden(context, new JsonObject().put("error", "not support HTTP Method - " + httpMethod));
      }
    }
  }

  private void batchWrite(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    JsonArray requests = body.getJsonArray("requests");
    if (null == requests || requests.size() < 1) {
      logger.debug("invalid requests. requests is empty");
      badRequest(context, ErrorCodes.INVALID_PARAMETER.toJson());
      return;
    }

    ObjectModifyHandler handler = new ObjectModifyHandler(vertx, context);
    handler.batchSave(requests, res -> {
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

  private void healthcheck(RoutingContext context) {
    logger.debug("response for healthcheck.");
    JsonObject result = new JsonObject().put("status", "green")
            .put("availableProcessors", Runtime.getRuntime().availableProcessors())
            .put("freeMemory", Runtime.getRuntime().freeMemory())
            .put("totalMemory", Runtime.getRuntime().totalMemory())
            .put("maxMemory", Runtime.getRuntime().maxMemory()).put("activeThreads", Thread.activeCount());
    ok(context, result);
  }

  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    httpServer = vertx.createHttpServer(new HttpServerOptions()
            .setTcpFastOpen(true)
            .setTcpCork(true)
            .setTcpQuickAck(true)
            .setReusePort(true));

    ApplicationAuthenticator appKeyValidator = new ApplicationAuthenticator();
    HTTPRequestValidationHandler appKeyValidationHandler = HTTPRequestValidationHandler.create().addCustomValidatorFunction(appKeyValidator);
    HTTPRequestValidationHandler sessionValidationHandler = HTTPRequestValidationHandler.create().addCustomValidatorFunction(new SessionValidator());

    Router router = Router.router(vertx);

    router.get("/").handler(rc -> ok(rc,
            new JsonObject()
                    .put("Description", "Ayers server supported by LeanCloud(https://leancloud.cn).")
                    .put("License", "Apache License Version 2.0")
                    .put("Contacts", "support@leancloud.rocks")));
    router.get("/ping").handler(this::healthcheck);

    router.route("/1.1/*").handler(appKeyValidationHandler).handler(BodyHandler.create())
            .handler(CorsHandler.create("*")
                    .allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.PUT)
                    .allowedMethod(HttpMethod.DELETE).allowedMethod(HttpMethod.OPTIONS).allowedMethod(HttpMethod.HEAD)
                    .allowedHeader("Access-Control-Request-Method").allowedHeader("Access-Control-Allow-Credentials")
                    .allowedHeader("Access-Control-Allow-Origin").allowedHeader("Access-Control-Allow-Headers")
                    .allowedHeader("Content-Type").allowedHeader("Origin").allowedHeader("Accept")
                            .allowedHeaders(RequestParse.ALLOWED_HEADERS_SET)
            );

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

    router.getWithRegex("\\/1\\.1\\/users\\/(?<objectId>[^\\/]{16,})").handler(this::crudUser);
    router.get("/1.1/users").handler(this::crudUser);
    router.delete("/1.1/users/:objectId").handler(this::crudUser);
    router.put("/1.1/users/:objectId").handler(this::crudUser);

    router.get("/1.1/users/me").handler(sessionValidationHandler).handler(this::validateUser);
    router.put("/1.1/users/:objectId/refreshSessionToken").handler(sessionValidationHandler).handler(this::refreshUserToken);
    router.put("/1.1/users/:objectId/updatePassword").handler(sessionValidationHandler).handler(this::updateUserPassword);
    router.post("/1.1/requestEmailVerify").handler(this::requestEmailVerify);
    router.post("/1.1/requestPasswordReset").handler(this::requestPasswordReset);

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
    router.post("/1.1/batch/save").handler(this::batchWrite);

    router.post("/1.1/schema/:clazz/_test").handler(this::testSchema);
    router.post("/1.1/schema/:clazz").handler(this::addSchemaIfAbsent);
    router.get("/1.1/schema/:clazz").handler(this::listSchema);

    router.post("/1.1/indices/:clazz").handler(this::createIndex);
    router.get("/1.1/indices/:clazz").handler(this::listIndex);
    router.delete("/1.1/indices/:clazz/:indexName").handler(this::deleteIndex);

    router.post("/1.1/requestSmsCode").handler(this::requestSmsCode);
    router.post("/1.1/verifySmsCode/:smscode").handler(this::verifySmsCode);

    router.errorHandler(400, routingContext -> {
      if (routingContext.failure() instanceof ValidationException) {
        // Something went wrong during validation!
        String validationErrorMessage = routingContext.failure().getMessage();
        logger.warn("invalid request. cause: " + validationErrorMessage);
      }
    });
//    router.errorHandler(HttpStatus.SC_INTERNAL_SERVER_ERROR, routingContext -> {
//      if (routingContext.failed()) {
//        logger.warn("internal error. cause: " + routingContext.failure().getMessage());
//      }
//      routingContext.next();
//    });

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

  private void testSchema(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    JsonObject body = parseRequestBody(context);
    if (null == body || body.size() < 1) {
      badRequest(context, new JsonObject().put("error", "request body is required."));
    } else {
      SchemaHandler handler = new SchemaHandler(vertx, context);
      handler.test(clazz, body, res -> {
        if (res.failed()) {
          internalServerError(context, new JsonObject().put("error", res.cause().getMessage()));
        } else {
          ok(context, res.result());
        }
      });
    }
  }

  private void addSchemaIfAbsent(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    JsonObject body = parseRequestBody(context);
    if (null == body || body.size() < 1) {
      badRequest(context, new JsonObject().put("error", "request body is required."));
    } else {
      SchemaHandler handler = new SchemaHandler(vertx, context);
      handler.addIfAbsent(clazz, body, res -> {
        if (res.failed()) {
          internalServerError(context, new JsonObject().put("error", res.cause().getMessage()));
        } else {
          ok(context, res.result());
        }
      });
    }
  }

  private void listSchema(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    SchemaHandler handler = new SchemaHandler(vertx, context);
    handler.find(clazz, res -> {
      if (res.failed()) {
        internalServerError(context, new JsonObject().put("error", res.cause().getMessage()));
      } else {
        ok(context, res.result());
      }
    });
  }

  private void createIndex(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    JsonObject body = parseRequestBody(context);
    if (null == body || body.size() < 1) {
      badRequest(context, new JsonObject().put("error", "request body is required."));
    } else {
      JsonObject keys = body.getJsonObject(RequestParse.REQUEST_INDEX_KEYS);
      boolean unique = body.getBoolean(RequestParse.REQUEST_INDEX_OPTION_UNIQUE, false);
      boolean sparse = body.getBoolean(RequestParse.REQUEST_INDEX_OPTION_SPARSE, true);
      String name = body.getString(RequestParse.REQUEST_INDEX_OPTION_NAME);
      if (null == keys || keys.size() < 1) {
        badRequest(context, new JsonObject().put("error", "keys is required."));
      } else {
        JsonObject indexOptions = new JsonObject().put(RequestParse.REQUEST_INDEX_OPTION_UNIQUE, unique)
                .put(RequestParse.REQUEST_INDEX_OPTION_SPARSE, sparse).put(RequestParse.REQUEST_INDEX_OPTION_NAME, name);
        IndexHandler handler = new IndexHandler(vertx, context);
        handler.create(clazz, keys, indexOptions, res -> {
          if (res.failed()) {
            internalServerError(context, new JsonObject().put("error", res.cause().getMessage()));
          } else {
            ok(context, res.result());
          }
        });

      }
    }
  }

  private void listIndex(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    IndexHandler handler = new IndexHandler(vertx, context);
    handler.list(clazz, res -> {
      if (res.failed()) {
        internalServerError(context, new JsonObject().put("error", res.cause().getMessage()));
      } else {
        ok(context, res.result());
      }
    });
  }

  private void deleteIndex(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    String indexName = parseRequestIndexName(context);
    IndexHandler handler = new IndexHandler(vertx, context);
    handler.delete(clazz, indexName, res -> {
      if (res.failed()) {
        internalServerError(context, new JsonObject().put("error", res.cause().getMessage()));
      } else {
        ok(context, res.result());
      }
    });
  }

  private void requestSmsCode(RoutingContext context) {
    SmsCodeHandler handler = new SmsCodeHandler(vertx, context);
    handler.requestSmsCode(res -> {
      if (res.failed()) {
        logger.warn("requestSmsCode failed. cause: " + res.cause().getMessage());
        badRequest(context, new JsonObject().put("error", res.cause().getMessage()));
      } else {
        ok(context, res.result());
      }
    });
  }

  private void verifySmsCode(RoutingContext context) {
    SmsCodeHandler handler = new SmsCodeHandler(vertx, context);
    handler.verifySmsCode(res -> {
      if (res.failed()) {
        logger.warn("verifySmsCode failed. cause: " + res.cause().getMessage());
        badRequest(context, new JsonObject().put("error", res.cause().getMessage()));
      } else {
        ok(context, res.result());
      }
    });
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
