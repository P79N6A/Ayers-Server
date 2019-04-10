package cn.leancloud.platform.ayers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import static cn.leancloud.platform.common.JsonFactory.toJsonArray;

public class MongoDBVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBVerticle.class);

  private String mongoQueue;
  private JsonObject mongoConfig;
  private static final String MONGO_POOL_NAME = "MongoDBPool";

  private void prepareDatabase() {
    mongoConfig = new JsonObject()
            .put("host", "127.0.0.1")
            .put("port", 27027)
            .put("db_name", "uluru-test")
            .put("maxPoolSize", 50)
            .put("minPoolSize", 1)
            .put("keepAlive", true);
    MongoClient testClient = getSharedClient();
    testClient.getCollections(re -> {
      if (re.failed()) {
        logger.error("failed to initialize mongo. cause: ", re.cause());
      } else {
        re.result().forEach( x -> System.out.println(x));
      }
    });
  }

  private MongoClient getSharedClient() {
    return MongoClient.createShared(vertx, this.mongoConfig, MONGO_POOL_NAME);
  }

  private void reportOoperationError(Message<JsonObject> message, Throwable cause) {
    logger.error("mongo operation error", cause);
    message.fail(DatabaseVerticle.ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }

  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("operation")) {
      message.fail(DatabaseVerticle.ErrorCodes.NO_OPERATION_SPECIFIED.ordinal(), "no operation specified.");
      return;
    }
    logger.debug("received message: " + message.body().toString());

    JsonObject body = message.body();
    String operation = message.headers().get("operation");
    MongoClient client = getSharedClient();
    if (body.containsKey("objectId")) {
      body.put("_id", body.getString("objectId")).remove("objectId");
    }

    switch (operation) {
      case "upsert":
        if (!body.containsKey("_id")) {
          body.put("createdAt", Instant.now());
        }
        body.put("updatedAt", Instant.now());
        client.insert("_Installation", body, res -> {
          if (res.failed()) {
            reportOoperationError(message, res.cause());
          } else {
            logger.debug("insert doc result:" + res.result());
            message.reply(res.result());
          }
        });
        break;
      case "delete":
        client.removeDocument("_Installation", body, res -> {
          if (res.failed()) {
            reportOoperationError(message, res.cause());
          } else {
            message.reply(res.result());
          }
        });
        break;
      case "query":
        client.find("_Installation", body, res->{
          if (res.failed()) {
            reportOoperationError(message, res.cause());
          } else {
            message.reply(res.result().stream()
                    .parallel()
                    .collect(toJsonArray()));
          }
        });
        break;
      default:
        message.fail(DatabaseVerticle.ErrorCodes.BAD_OPERATION.ordinal(), "unknown operation.");
        break;
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    prepareDatabase();

    mongoQueue = config().getString(RestServerVerticle.CONFIG_MONGO_QUEUE, "mongo.queue");
    vertx.eventBus().consumer(mongoQueue, this::onMessage);
    startFuture.complete();
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    getSharedClient().close();
    stopFuture.complete();
  }
}

