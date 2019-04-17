package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.UserHandler;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.StringUtils;
import cn.leancloud.platform.common.Transformer;
import com.mongodb.internal.validator.CollectibleDocumentFieldNameValidator;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static cn.leancloud.platform.common.JsonFactory.toJsonArray;

public class MongoDBVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBVerticle.class);
  private static final int COUNT_FLAG = 1;

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

  private void reportOperationError(Message<JsonObject> message, Throwable cause) {
    logger.error("mongodb operation error", cause);
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
            reportOperationError(message, user.cause());
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
        boolean isCreateOp = false;
        if (StringUtils.isEmpty(objectId)) {
          isCreateOp = true;
          param.put(Configure.CLASS_ATTR_CREATED_TS, now);
        }
        param.put(Configure.CLASS_ATTR_UPDATED_TS, now);

        try {
          param = Transformer.encode2BsonRequest(param, isCreateOp? Transformer.REQUEST_OP.CREATE :Transformer.REQUEST_OP.UPDATE);
        } catch (ClassCastException ex) {
          reportUserError(message, 400, ex.getMessage());
          return;
        }

        if (isCreateOp) {
          logger.debug("doc=== " + param.toString());
          client.insert(clazz, param, res -> {
            client.close();
            if (res.failed()) {
              reportOperationError(message, res.cause());
            } else {
              String docId = StringUtils.isEmpty(objectId) ? res.result() : objectId;
              logger.debug("insert doc result:" + docId);
              JsonObject result = new JsonObject().put("objectId", docId);
              result.put("createdAt", now.toString());
              message.reply(result);
            }
          });
        } else {
          logger.debug("doc=== " + param.toString());
          JsonObject query = new JsonObject().put(Configure.CLASS_ATTR_MONGO_ID, objectId);
          logger.debug("query= " + query.toString());
          UpdateOptions option = new UpdateOptions().setUpsert(false);
          client.updateCollectionWithOptions(clazz, query, param, option, res -> {
            client.close();
            if (res.failed()) {
              reportOperationError(message, res.cause());
            } else {
              if (res.result().getDocModified() > 0) {
                JsonObject result = new JsonObject().put("objectId", objectId);
                result.put("updatedAt", now.toString());
                message.reply(result);
              } else {
                reportUserError(message, 404, "object not found");
              }
            }

          });
        }
        break;
      case Configure.OP_OBJECT_DELETE:
        if (!StringUtils.isEmpty(objectId)) {
          param.put(Configure.CLASS_ATTR_MONGO_ID, objectId);
        }
        client.removeDocument(clazz, param, res -> {
          client.close();
          if (res.failed()) {
            reportOperationError(message, res.cause());
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
        try {
          condition = Transformer.encode2BsonRequest(condition, Transformer.REQUEST_OP.QUERY);
        } catch (ClassCastException ex) {
          reportUserError(message, 400, ex.getMessage());
          return;
        }

        FindOptions options = new FindOptions();
        options.setLimit(limit);
        options.setSkip(skip);
        JsonObject sortJson = Transformer.parseSortParam(order);
        if (null != sortJson) {
          options.setSort(sortJson);
        }
        JsonObject fieldJson = Transformer.parseProjectParam(keys);
        if (null != fieldJson) {
          options.setFields(fieldJson);
        }

        if (COUNT_FLAG == count) {
          logger.debug("count clazz=" + clazz + ", condition=" + condition.toString());
          client.count(clazz, condition, res -> {
            client.close();
            if (res.failed()) {
              reportOperationError(message, res.cause());
            } else {
              message.reply(new JsonObject().put("count", res.result()));
            }
          });
        } else {
          logger.debug("find clazz=" + clazz + ", condition=" + condition.toString() + ", options=" + options.toJson());
          client.findWithOptions(clazz, condition, options, res->{
            client.close();
            if (res.failed()) {
              reportOperationError(message, res.cause());
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

    Field field = CollectibleDocumentFieldNameValidator.class.getDeclaredField("EXCEPTIONS");
    field.setAccessible(true);

    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    List<String> newValue = Arrays.asList("$db","$ref","$id","$gte","_id", "$push","$pushAll", "$pull", "$each", "$set");
    field.set(null, newValue);

    mongoQueue = config().getString(RestServerVerticle.CONFIG_MONGO_QUEUE, "mongo.queue");
    vertx.eventBus().consumer(mongoQueue, this::onMessage);
    startFuture.complete();
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    stopFuture.complete();
  }
}

