package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.UserHandler;
import cn.leancloud.platform.common.*;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.stream.Stream;

import static cn.leancloud.platform.common.JsonFactory.toJsonArray;

public class MongoDBVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBVerticle.class);
  private static final int COUNT_FLAG = 1;
  private static final String MONGO_POOL_NAME = "MongoDBPool";

  private JsonObject mongoConfig;

  private void prepareDatabase() {
    Configure configure = Configure.getInstance();
    String hosts = configure.mongoHosts();
    String[] hostParts = hosts.split(":");
    int port = 27017;
    if (hostParts.length > 1) {
      port = Integer.valueOf(hostParts[1]);
    }
    mongoConfig = new JsonObject()
            .put("host", hostParts[0])
            .put("port", port)
            .put("db_name", configure.mongoDatabase())
            .put("maxPoolSize", configure.mongoMaxPoolSize())
            .put("minPoolSize", configure.mongoMinPoolSize())
            .put("maxIdleTimeMS", configure.mongoMaxIdleTimeMS())
            .put("maxLifeTimeMS", configure.mongoMaxLifeTimeMS())
            .put("waitQueueMultiple", configure.mongoWaitQueueMultiple())
            .put("waitQueueTimeoutMS", configure.mongoWaitQueueTimeoutMS())
            .put("serverSelectionTimeoutMS", configure.mongoServerSelectionTimeoutMS())
            .put("keepAlive", true);
    logger.info("initialize mongo with config: " + mongoConfig);
    MongoClient mongoClient = getSharedClient();
    mongoClient.getCollections(re -> {
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

  private void reportDatabaseError(Message<JsonObject> message, Throwable cause) {
    logger.error("mongodb operation error", cause);
    message.fail(ErrorCodes.DATABASE_ERROR.getCode(), cause.getMessage());
  }

  private void reportUserError(Message<JsonObject> msg, int code, String message) {
    msg.fail(code, message);
  }

  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains(Constraints.INTERNAL_MSG_HEADER_OP)) {
      message.fail(ErrorCodes.INTERNAL_ERROR.ordinal(), "no operation specified.");
      return;
    }
    logger.debug("received message: " + message.body().toString());

    JsonObject body = message.body();
    String clazz = body.getString(Constraints.INTERNAL_MSG_ATTR_CLASS, "");
    String objectId = body.getString(Constraints.INTERNAL_MSG_ATTR_OBJECT_ID);
    JsonObject param = body.getJsonObject(Constraints.INTERNAL_MSG_ATTR_PARAM, new JsonObject());

    String operation = message.headers().get(Constraints.INTERNAL_MSG_HEADER_OP);

    final MongoClient client = getSharedClient();
    Instant now = Instant.now();
    // replace
    if (null != param && param.containsKey(Constraints.CLASS_ATTR_OBJECT_ID)) {
      param.put(Constraints.CLASS_ATTR_MONGO_ID, param.getString(Constraints.CLASS_ATTR_OBJECT_ID)).remove(Constraints.CLASS_ATTR_OBJECT_ID);
    }

    switch (operation) {
      case Constraints.OP_USER_SIGNIN:
        String password = param.getString(UserHandler.PARAM_PASSWORD);
        param.remove(UserHandler.PARAM_PASSWORD);
        logger.debug("query= " + param.toString());
        client.findOne(clazz, param, null, user -> {
          if (user.failed()) {
            reportDatabaseError(message, user.cause());
          } else if (null == user.result()) {
            reportUserError(message, ErrorCodes.OBJECT_NOT_FOUND.ordinal(), "username is not existed.");
          } else {
            JsonObject mongoUser = user.result();
            String salt = mongoUser.getString(Constraints.BUILTIN_ATTR_SALT);
            String mongoPassword = mongoUser.getString(Constraints.BUILTIN_ATTR_PASSWORD);
            mongoUser.remove(Constraints.BUILTIN_ATTR_SALT);
            mongoUser.remove(Constraints.BUILTIN_ATTR_PASSWORD);
            if (StringUtils.isEmpty(password)) {
              message.reply(mongoUser);
            } else {
              String hashPassword = UserHandler.hashPassword(password, salt);
              if (hashPassword.equals(mongoPassword)) {
                message.reply(mongoUser);
              } else {
                reportUserError(message, ErrorCodes.PASSWORD_WRONG.getCode(), "password is wrong.");
              }
            }
          }
        });
        break;
      case Constraints.OP_OBJECT_UPSERT:
        boolean isCreateOp = false;
        if (StringUtils.isEmpty(objectId)) {
          isCreateOp = true;
          param.put(Constraints.CLASS_ATTR_CREATED_TS, now);
        }
        param.put(Constraints.CLASS_ATTR_UPDATED_TS, now);

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
              reportDatabaseError(message, res.cause());
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
          JsonObject query = new JsonObject().put(Constraints.CLASS_ATTR_MONGO_ID, objectId);
          logger.debug("query= " + query.toString());
          UpdateOptions option = new UpdateOptions().setUpsert(false);
          client.updateCollectionWithOptions(clazz, query, param, option, res -> {
            client.close();
            if (res.failed()) {
              reportDatabaseError(message, res.cause());
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
      case Constraints.OP_OBJECT_DELETE:
        if (!StringUtils.isEmpty(objectId)) {
          param.put(Constraints.CLASS_ATTR_MONGO_ID, objectId);
        }
        client.removeDocument(clazz, param, res -> {
          client.close();
          if (res.failed()) {
            reportDatabaseError(message, res.cause());
          } else {
            message.reply(res.result().toJson());
          }
        });
        break;
      case Constraints.OP_OBJECT_QUERY:
        String where = param.getString(Constraints.QUERY_KEY_WHERE, "{}");
        String order = param.getString(Constraints.QUERY_KEY_ORDER);
        int limit = Integer.valueOf(param.getString(Constraints.QUERY_KEY_LIMIT, "100"));
        int skip = Integer.valueOf(param.getString(Constraints.QUERY_KEY_SKIP, "0"));
        int count = Integer.valueOf(param.getString(Constraints.QUERY_KEY_COUNT, "0"));
        String include = param.getString(Constraints.QUERY_KEY_INCLUDE);
        String keys = param.getString(Constraints.QUERY_KEY_KEYS);

        JsonObject condition = new JsonObject(where);
        if (!StringUtils.isEmpty(objectId)) {
          condition.put(Constraints.CLASS_ATTR_MONGO_ID, objectId);
        }
        try {
          condition = Transformer.encode2BsonRequest(condition, Transformer.REQUEST_OP.QUERY);
        } catch (ClassCastException ex) {
          reportUserError(message, ErrorCodes.INVALID_PARAMETER.getCode(), ex.getMessage());
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
              reportDatabaseError(message, res.cause());
            } else {
              message.reply(new JsonObject().put("count", res.result()));
            }
          });
        } else {
          logger.debug("find clazz=" + clazz + ", condition=" + condition.toString() + ", options=" + options.toJson());
          client.findWithOptions(clazz, condition, options, res->{
            client.close();
            if (res.failed()) {
              reportDatabaseError(message, res.cause());
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
        message.fail(ErrorCodes.INTERNAL_ERROR.getCode(), "unknown operation.");
        break;
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    logger.info("start MongoDBVerticle...");
    prepareDatabase();

//    Field field = CollectibleDocumentFieldNameValidator.class.getDeclaredField("EXCEPTIONS");
//    field.setAccessible(true);
//
//    Field modifiers = Field.class.getDeclaredField("modifiers");
//    modifiers.setAccessible(true);
//    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
//
//    List<String> newValue = Arrays.asList("$db","$ref","$id","$gte","_id", "$push","$pushAll", "$pull", "$each", "$set");
//    field.set(null, newValue);

    vertx.eventBus().consumer(Configure.MAILADDRESS_MONGO_QUEUE, this::onMessage);
    logger.info("begin to consume address: " + Configure.MAILADDRESS_MONGO_QUEUE);
    startFuture.complete();
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop MongoDBVerticle...");
    stopFuture.complete();
  }
}

