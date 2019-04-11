package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.StringUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import static cn.leancloud.platform.common.JsonFactory.toJsonArray;

public class MongoDBVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBVerticle.class);
  private static final String COUNT_FLAG = "1";

  private String mongoQueue;
  private JsonObject mongoConfig;
  private static final String MONGO_POOL_NAME = "MongoDBPool";

  private void prepareDatabase() {
    mongoConfig = new JsonObject()
            .put("host", "127.0.0.1")
            .put("port", 27027)
            .put("db_name", "uluru-test")
            .put("maxPoolSize", 50)
            .put("minPoolSize", 3)
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
    if (!message.headers().contains(Configure.INTERNAL_MSG_HEADER_OP)) {
      message.fail(DatabaseVerticle.ErrorCodes.NO_OPERATION_SPECIFIED.ordinal(), "no operation specified.");
      return;
    }
    logger.debug("received message: " + message.body().toString());

    JsonObject body = message.body();
    String clazz = body.getString(Configure.INTERNAL_MSG_ATTR_CLASS, "");
    String objectId = body.getString(Configure.INTERNAL_MSG_ATTR_OBJECT_ID);
    JsonObject param = body.getJsonObject(Configure.INTERNAL_MSG_ATTR_PARAM, new JsonObject());

    String operation = message.headers().get(Configure.INTERNAL_MSG_HEADER_OP);

    final MongoClient client = getSharedClient();
    Instant now = Instant.now();
    // replace
    if (null != param && param.containsKey(Configure.CLASS_ATTR_OBJECT_ID)) {
      param.put(Configure.CLASS_ATTR_MONGO_ID, param.getString(Configure.CLASS_ATTR_OBJECT_ID)).remove(Configure.CLASS_ATTR_OBJECT_ID);
    }

    switch (operation) {
      case Configure.OP_OBJECT_UPSERT:
        if (!StringUtils.isEmpty(objectId)) {
          param.put(Configure.CLASS_ATTR_MONGO_ID, objectId);
        } else {
          param.put(Configure.CLASS_ATTR_CREATED_TS, now);
        }
        param.put(Configure.CLASS_ATTR_UPDATED_TS, now);

        client.insert(clazz, body, res -> {
          client.close();
          if (res.failed()) {
            reportOoperationError(message, res.cause());
          } else {
            logger.debug("insert doc result:" + res.result());
            message.reply(res.result());
          }
        });
        break;
      case Configure.OP_OBJECT_DELETE:
        if (!StringUtils.isEmpty(objectId)) {
          param.put(Configure.CLASS_ATTR_MONGO_ID, objectId);
        }
        client.removeDocument(clazz, param, res -> {
          client.close();
          if (res.failed()) {
            reportOoperationError(message, res.cause());
          } else {
            message.reply(res.result().toJson());
          }
        });
        break;
      case Configure.OP_OBJECT_QUERY:
        String where = param.getString(Configure.QUERY_KEY_WHERE, "{}");
        String order = param.getString(Configure.QUERY_KEY_ORDER);
        String limit = param.getString(Configure.QUERY_KEY_LIMIT, "100");
        String skip = param.getString(Configure.QUERY_KEY_SKIP, "0");
        String count = param.getString(Configure.QUERY_KEY_COUNT);
        String include = param.getString(Configure.QUERY_KEY_INCLUDE);
        String keys = param.getString(Configure.QUERY_KEY_KEYS);
        JsonObject condition = new JsonObject(where);
        if (!StringUtils.isEmpty(objectId)) {
          condition.put(Configure.CLASS_ATTR_MONGO_ID, objectId);
        }
        FindOptions options = new FindOptions();
        options.setLimit(Integer.valueOf(limit));
        options.setSkip(Integer.valueOf(skip));

        if (COUNT_FLAG.equalsIgnoreCase(count)) {
          client.count(clazz, condition, res -> {
            client.close();
            if (res.failed()) {
              reportOoperationError(message, res.cause());
            } else {
              message.reply(res.result());
            }
          });
        } else {
          client.findWithOptions(clazz, condition, options, res->{
            client.close();
            if (res.failed()) {
              reportOoperationError(message, res.cause());
            } else {
              message.reply(res.result().stream().parallel().map(o -> {
                // replace _id with objectId.
                if (o.containsKey(Configure.CLASS_ATTR_MONGO_ID)) {
                  o.put(Configure.CLASS_ATTR_OBJECT_ID, o.getString(Configure.CLASS_ATTR_MONGO_ID)).remove(Configure.CLASS_ATTR_MONGO_ID);
                }
                return o;
              }).collect(toJsonArray()));
            }
          });
        }
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
    stopFuture.complete();
  }
}

