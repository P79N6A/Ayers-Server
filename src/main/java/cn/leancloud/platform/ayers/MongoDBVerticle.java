package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.UserHandler;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.StringUtils;
import cn.leancloud.platform.common.Transformer;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.leancloud.platform.common.JsonFactory.toJsonArray;

public class MongoDBVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBVerticle.class);
  private static final int COUNT_FLAG = 1;

  private String mongoQueue;
  private JsonObject mongoConfig;
  private static final String MONGO_POOL_NAME = "MongoDBPool";
  private static final String[] ALWAYS_PROJECT_KEYS = {"_id", "createdAt", "updatedAt", "ACL"};

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

  private void reportUserError(Message<JsonObject> msg, int code, String message) {
    msg.fail(code, message);
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
      case Configure.OP_USER_SIGNIN:
        String password = param.getString("password");
        param.remove("password");
        client.findOne(clazz, param, null, user -> {
          if (user.failed()) {
            reportOoperationError(message, user.cause());
          } else if (null == user.result()) {
            reportUserError(message, DatabaseVerticle.ErrorCodes.NOT_FOUND_USER.ordinal(), "username is not existed.");
          } else {
            JsonObject mongoUser = user.result();
            String salt = mongoUser.getString("salt");
            String mongoPassword = mongoUser.getString("password");
            String hashPassword = UserHandler.hashPassword(password, salt);
            mongoUser.remove("salt");
            mongoUser.remove("password");
            if (hashPassword.equals(mongoPassword)) {
              message.reply(mongoUser);
            } else {
              reportUserError(message, DatabaseVerticle.ErrorCodes.PASSWORD_ERROR.ordinal(), "password is wrong.");
            }
          }
        });
        break;
      case Configure.OP_OBJECT_UPSERT:
        if (!StringUtils.isEmpty(objectId)) {
          param.put(Configure.CLASS_ATTR_MONGO_ID, objectId);
        } else {
          param.put(Configure.CLASS_ATTR_CREATED_TS, now);
        }
        param.put(Configure.CLASS_ATTR_UPDATED_TS, now);

        param = Transformer.encode2BsonObject(param);

        client.save(clazz, param, res -> {
          client.close();
          if (res.failed()) {
            reportOoperationError(message, res.cause());
          } else {
            String docId = StringUtils.isEmpty(objectId) ? res.result() : objectId;
            logger.debug("insert doc result:" + docId);
            JsonObject result = new JsonObject().put("objectId", docId);
            if (!StringUtils.isEmpty(objectId)) {
              result.put("updatedAt", now.toString());
            } else {
              result.put("createdAt", now.toString());
            }
            message.reply(result);
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
        int limit = Integer.valueOf(param.getString(Configure.QUERY_KEY_LIMIT, "100"));
        int skip = Integer.valueOf(param.getString(Configure.QUERY_KEY_SKIP, "0"));
        int count = Integer.valueOf(param.getString(Configure.QUERY_KEY_COUNT, "0"));
        String include = param.getString(Configure.QUERY_KEY_INCLUDE);
        String keys = param.getString(Configure.QUERY_KEY_KEYS);

        JsonObject condition = new JsonObject(where);
        if (!StringUtils.isEmpty(objectId)) {
          condition.put(Configure.CLASS_ATTR_MONGO_ID, objectId);
        }
        FindOptions options = new FindOptions();
        options.setLimit(limit);
        options.setSkip(skip);
        if (!StringUtils.isEmpty(order)) {
          JsonObject sortJson = new JsonObject();
          Arrays.stream(order.split(",")).filter(StringUtils::notEmpty).forEach( a -> {
            if (a.startsWith("+")) {
              sortJson.put(a.substring(1), 1);
            } else if (a.startsWith("-")) {
              sortJson.put(a.substring(1), -1);
            } else {
              sortJson.put(a, 1);
            }
          });
          options.setSort(sortJson);
        }
        if (!StringUtils.isEmpty(keys)) {
          JsonObject fieldJson = new JsonObject();
          Stream.concat(Arrays.stream(keys.split(",")), Arrays.stream(ALWAYS_PROJECT_KEYS))
                  .filter(StringUtils::notEmpty)
                  .collect(Collectors.toSet())
                  .forEach(k -> fieldJson.put(k, 1));
          options.setFields(fieldJson);
        }

        if (COUNT_FLAG == count) {
          logger.debug("count clazz=" + clazz + ", condition=" + condition.toString());
          client.count(clazz, condition, res -> {
            client.close();
            if (res.failed()) {
              reportOoperationError(message, res.cause());
            } else {
              message.reply(new JsonObject().put("count", res.result()));
            }
          });
        } else {
          logger.debug("find clazz=" + clazz + ", condition=" + condition.toString() + ", options=" + options.toJson());
          client.findWithOptions(clazz, condition, options, res->{
            client.close();
            if (res.failed()) {
              reportOoperationError(message, res.cause());
            } else {
              Stream<JsonObject> resultStream = res.result().stream().map(Transformer::decodeBsonObject);
              if (StringUtils.isEmpty(objectId)) {
                JsonArray results = resultStream.collect(toJsonArray());
                message.reply(new JsonObject().put("results", results));
              } else {
                message.reply(resultStream.findFirst().orElseGet(dummyJsonGenerator));
              }
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

