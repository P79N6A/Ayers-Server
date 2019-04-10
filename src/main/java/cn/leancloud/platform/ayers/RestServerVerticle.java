package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.CommonResult;
import cn.leancloud.platform.cache.SimpleRedisClient;
import cn.leancloud.platform.common.StringUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Hello world!
 */
public class RestServerVerticle extends AbstractVerticle {
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

  private void queryInstallation(RoutingContext context) {
    String jobId = context.pathParam("jobId");
    logger.debug("fetch job with jobId: " + jobId);
    this.redisClient.get(jobId, res -> {
      logger.debug("result from redis cache: " + res.toString());
      if (res.succeeded() && null != res.result()) {
        CommonResult result = new CommonResult();
        result.setStatus("ok");
        result.setDetails(res.result().toString());
        context.response().putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(result));
      } else {
        // retrieve job status from database;
        JsonObject request = new JsonObject().put("jobId", jobId);
        logger.debug("send message: " + request.toString());
        DeliveryOptions options = new DeliveryOptions().addHeader("operation", "fetch");
        vertx.eventBus().send(this.dbQueue, request, options, reply -> {
          logger.debug("received response from db queue.");
          if (reply.failed()) {
            logger.warn("db operationn failed. cause: ", reply.cause());
            context.fail(reply.cause());
          } else {
            CommonResult result = new CommonResult();
            result.setStatus("ok");
            result.setDetails(reply.result().toString());
            context.response().putHeader("Content-Type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(result));
          }
        });

      }
    });
  }

  private void upsertInstallation(RoutingContext context) {
    JsonObject param = new JsonObject(context.getBodyAsString());
    String sql = param.getJsonObject("jobConfig").getString("sql");
    logger.debug("create query job with request: " + param.toString() + " sql=" + sql);

    String appId = "appId";//RequestParse.getAppId(context);
    long curHourTime = new Date().getTime() / 3600000;
    String jobId = StringUtils.computeMD5(String.format("%s-%d-%s", appId, curHourTime, sql));
    param.put("id", jobId);
    param.put("appId", appId);
    logger.debug("send message: " + param.toString());

    DeliveryOptions options = new DeliveryOptions().addHeader("operation", "create");
    vertx.eventBus().send(this.dbQueue, param, options, reply -> {
      logger.debug("received response from db queue.");
      if (reply.failed()) {
        logger.warn("db operationn failed. cause: ", reply.cause());
        context.fail(reply.cause());
      } else {
        CommonResult result = new CommonResult();
        result.setStatus("ok");
        result.setDetails(reply.result().toString());
        context.response().putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(result));
      }
    });
  }

  private void deleteInstallation(RoutingContext context) {
    JsonObject request = context.getBodyAsJson();
    DeliveryOptions options = new DeliveryOptions().addHeader("operation", "create");
    vertx.eventBus().send(this.dbQueue, request, options, reply -> {
      if (reply.failed()) {
        context.fail(reply.cause());
      } else {
        CommonResult result = new CommonResult();
        result.setStatus("ok");
        result.setDetails(reply.result().toString());
        context.response().putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(result));
      }
    });
  }

  private void serverDate(RoutingContext context) {
    CommonResult result = new CommonResult();
    result.setStatus("ok");
    result.setDetails(new Date().toString());
    context.response().putHeader("Content-Type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(result));
  }

  private void healthcheck(RoutingContext context) {
    logger.debug("response for healthcheck.");
    CommonResult result = new CommonResult();
    result.setStatus("ok");
    result.setDetails("healthcheck");
    context.response().putHeader("Content-Type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(result));
  }

  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    httpServer = vertx.createHttpServer(new HttpServerOptions()
            .setTcpFastOpen(true)
            .setTcpCork(true)
            .setTcpQuickAck(true)
            .setReusePort(true));

    ApplicationValidator validator = new ApplicationValidator("testkey");
    HTTPRequestValidationHandler validationHandler = HTTPRequestValidationHandler.create().addCustomValidatorFunction(validator);

    Router router = Router.router(vertx);

    router.get("/ping").handler(this::healthcheck);

    BodyHandler bodyHandler = BodyHandler.create();
    router.route("/*").handler(bodyHandler);

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
    router.post("/1.1/installations").handler(validationHandler).handler(this::upsertInstallation);
    router.put("/1.1/installations/:objectId").handler(validationHandler).handler(this::upsertInstallation);
    router.get("/1.1/installations").handler(validationHandler).handler(this::queryInstallation);
    router.get("/1.1/installations/:objectId").handler(validationHandler).handler(this::queryInstallation);
    router.delete("/1.1/installations/:objectId").handler(validationHandler).handler(this::deleteInstallation);

    router.get("/1.1/date").handler(validationHandler).handler(this::serverDate);


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
    int redisPort = config().getInteger(CONFIG_REDIS_PORT, 16379);
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
