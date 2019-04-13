package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.UserHandler;
import cn.leancloud.platform.cache.SimpleRedisClient;
import cn.leancloud.platform.codec.Base64;
import cn.leancloud.platform.codec.MessageDigest;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.StringUtils;
import io.vertx.core.Future;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Hello world!
 */
public class RestServerVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(RestServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_DB_QUEUE = "db.queue";
  public static final String CONFIG_MONGO_QUEUE = "mongo.queue";
  public static final String CONFIG_REDIS_HOST = "redis.host";
  public static final String CONFIG_REDIS_PORT = "redis.port";

  private String dbQueue = "db.queue";

  private String mongoQueue = "mongo.queue";

  private SimpleRedisClient redisClient = new SimpleRedisClient();
  private HttpServer httpServer;

  private void serverDate(RoutingContext context) {
    JsonObject result = new JsonObject().put("__type", "Date").put("iso", Instant.now());
    ok(context, result);
  }

  private void crudInstallation(RoutingContext context) {
    crudCommonData(Configure.INSTALLATION_CLASS, context);
  }

  private void crudObject(RoutingContext context) {
    String clazz = parseRequestClassname(context);
    crudCommonData(clazz, context);
  }

  private void crudUser(RoutingContext context) {
    ;
  }

  private void userSignup(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    String username = body.getString("username");
    String password = body.getString("password");
    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
      badRequest(context, new JsonObject().put("code", 403).put("message", "username or password is empty."));
    } else {
      String salt = StringUtils.getRandomString(16);
      String hashPassword = UserHandler.hashPassword(password, salt);
      body.put("ACL", getUserDefaultACL());
      body.put("password", hashPassword);
      body.put("salt", salt);
      body.put("sessionToken", StringUtils.getRandomString(16));
      sendMongoOperation(context, Configure.USER_CLASS, null, Configure.OP_OBJECT_UPSERT, body);
    }
  }

  private void userSignin(RoutingContext context) {
    JsonObject body = parseRequestBody(context);
    String username = body.getString("username");
    String password = body.getString("password");
    if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
      badRequest(context, new JsonObject().put("code", 403).put("message", "username or password is empty."));
    } else {
      sendMongoOperation(context, Configure.USER_CLASS, null, Configure.OP_USER_SIGNIN, body);
    }
  }

  private void crudCommonData(String clazz, RoutingContext context) {
    String objectId = parseRequestObjectId(context);
    JsonObject body = parseRequestBody(context);
    HttpMethod httpMethod = context.request().method();
    String operation = "";
    if (HttpMethod.GET.equals(httpMethod)) {
      operation = Configure.OP_OBJECT_QUERY;
    } else if (HttpMethod.DELETE.equals(httpMethod)) {
      operation = Configure.OP_OBJECT_DELETE;
    } else if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod)) {
      operation = Configure.OP_OBJECT_UPSERT;
    }

    logger.debug("curl object. clazz=" + clazz + ", objectId=" + objectId + ", method=" + httpMethod + ", param=" + body);

    sendMongoOperation(context, clazz, objectId, operation, body);
  }

  private void sendMongoOperation(RoutingContext context, String clazz, String objectId, String operation, JsonObject param) {
    JsonObject request = new JsonObject();
    if (!StringUtils.isEmpty(clazz)) {
      request.put(Configure.INTERNAL_MSG_ATTR_CLASS, clazz);
    }
    if (!StringUtils.isEmpty(objectId)) {
      request.put(Configure.INTERNAL_MSG_ATTR_OBJECT_ID, objectId);
    }
    if (null != param) {
      request.put(Configure.INTERNAL_MSG_ATTR_PARAM, param);
    }
    DeliveryOptions options = new DeliveryOptions().addHeader(Configure.INTERNAL_MSG_HEADER_OP, operation);
    vertx.eventBus().send(this.mongoQueue, request, options, reply -> {
      if (reply.failed()) {
        logger.warn("mongo operationn failed. cause: ", reply.cause());
        context.fail(reply.cause());
      } else {
        JsonObject result = (JsonObject) reply.result().body();
        if (Configure.OP_OBJECT_UPSERT.equalsIgnoreCase(operation) && StringUtils.isEmpty(objectId)) {
          //Status: 201 Created
          //Location: https://heqfq0sw.api.lncld.net/1.1/classes/Post/<objectId>
          JsonObject location = new JsonObject().put("Location", context.request().absoluteURI() + result.getString("objectId"));
          created(context, location, result);
        } else {
          ok(context, result);
        }
      }
    });
  }

  private String parseRequestObjectId(RoutingContext context) {
    return context.request().getParam("objectId");
  }

  private String parseRequestClassname(RoutingContext context) {
    return context.request().getParam("clazz");
  }

  private JsonObject parseRequestBody(RoutingContext context) {
    HttpMethod httpMethod = context.request().method();
    JsonObject body = null;
    if (HttpMethod.GET.equals(httpMethod)) {
      Map<String, String> filteredEntries = context.request().params().entries()
              .stream().parallel()
              .filter(entry -> !"clazz".equalsIgnoreCase(entry.getKey()) && !"objectId".equalsIgnoreCase(entry.getKey()))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      body = JsonObject.mapFrom(filteredEntries);
    } else if (HttpMethod.PUT.equals(httpMethod) || HttpMethod.POST.equals(httpMethod)){
      body = context.getBodyAsJson();
    } else {
      String bodyString = context.getBodyAsString();
      if (StringUtils.isEmpty(bodyString)) {
        body = new JsonObject();
      } else {
        body = new JsonObject(bodyString);
      }
    }
    return body;
  }
  private void healthcheck(RoutingContext context) {
    logger.debug("response for healthcheck.");
    JsonObject result = new JsonObject().put("status", "green");
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

    router.get("/ping").handler(this::healthcheck);

    BodyHandler bodyHandler = BodyHandler.create();
    router.route("/1.1/*").handler(bodyHandler).handler(appKeyValidationHandler);

    /**
     * get    /1.1/date
     *
     * post   /1.1/classes/:clazz
     * get    /1.1/classes/:clazz
     * get    /1.1/classes/:clazz/:objectId
     * put    /1.1/classes/:clazz/:objectId
     * delete    /1.1/classes/:clazz/:objectId
     *
     * post /1.1/installations
     * get  /1.1/installations
     * get  /1.1/installations/:objectId
     * put  /1.1/installations/:objectId
     * delete  /1.1/installations/:objectId
     *
     */
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

    router.get("/1.1/date").handler(this::serverDate);

    router.post("/1.1/users")
//            .handler(HTTPRequestValidationHandler.create().addJsonBodySchema(""))
            .handler(this::userSignup);
    router.post("/1.1/login").handler(this::userSignin);

    router.get("/1.1/users/:userId").handler(this::crudUser);
    router.get("/1.1/users").handler(this::crudUser);
    router.delete("/1.1/users/:userId").handler(this::crudUser);
    router.put("/1.1/users/:userId").handler(this::crudUser);

    router.get("/1.1/users/me").handler(sessionValidationHandler).handler(this::crudUser);
    router.put("/1.1/users/:userId/refreshSessionToken").handler(this::crudUser);
    router.put("/1.1/users/:userId/updatePassword").handler(this::crudUser);
    router.post("/1.1/requestEmailVerify").handler(this::crudUser);
    router.post("/1.1/requestPasswordReset").handler(this::crudUser);

    router.get("/").handler(rc -> rc.response().end("hello from LeanCloud"));

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

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
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
    this.dbQueue = config().getString(CONFIG_DB_QUEUE, "db.queue");
    this.mongoQueue = config().getString(CONFIG_MONGO_QUEUE, "mongo.queue");
    String redisHost = config().getString(CONFIG_REDIS_HOST, "localhost");
    int redisPort = config().getInteger(CONFIG_REDIS_PORT, 6379);
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
    logger.info("stop RestServerVerticle...");
    stopFuture.complete();
  }
}
