package cn.leancloud.platform.ayers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDBVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBVerticle.class);

  private String mongoQueue;
  private JsonObject mongoConfig;
  private static final String MONGO_POOL_NAME = "MongoDBPool";

  private void prepareDatabase() {
    mongoConfig = new JsonObject()
            .put("host", "127.0.0.1")
            .put("port", 27017)
            .put("db_name", "uluru")
            .put("maxPoolSize", 50)
            .put("minPoolSize", 10)
            .put("useObjectId", true)
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

  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("operation")) {
      message.fail(DatabaseVerticle.ErrorCodes.NO_OPERATION_SPECIFIED.ordinal(), "no operation specified.");
      return;
    }

    JsonObject body = message.body();
    String operation = message.headers().get("operation");
    MongoClient client = getSharedClient();

    switch (operation) {
      case "upsert":
        break;
      case "delete":
        break;
      case "query":
        break;
      default:
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

