package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.UserHandler;
import cn.leancloud.platform.common.*;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.leancloud.platform.utils.JsonFactory.toJsonArray;

public class StorageVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(StorageVerticle.class);
  private static final int COUNT_FLAG = 1;
  private DataStoreFactory dataStoreFactory = null;

  private void prepareDatabase() {
    dataStoreFactory = Configure.getInstance().getDataStoreFactory();
  }

  private void reportDatabaseError(Message<JsonObject> message, Throwable cause) {
    logger.error("datastore operation error", cause);
    message.fail(ErrorCodes.DATABASE_ERROR.getCode(), cause.getMessage());
  }

  private void reportUserError(Message<JsonObject> msg, int code, String message) {
    msg.fail(code, message);
  }

  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains(INTERNAL_MSG_HEADER_OP)) {
      message.fail(ErrorCodes.INTERNAL_ERROR.ordinal(), "no operation specified.");
      return;
    }
    logger.debug("received message: " + message.body().toString());

    JsonObject body = message.body();
    String clazz = body.getString(INTERNAL_MSG_ATTR_CLASS, "");
    String objectId = body.getString(INTERNAL_MSG_ATTR_OBJECT_ID);
    JsonObject param = body.getJsonObject(INTERNAL_MSG_ATTR_PARAM, new JsonObject());
    boolean fetchWhenSave = body.getBoolean(INTERNAL_MSG_ATTR_FETCHWHENSAVE, false);

    String operation = message.headers().get(INTERNAL_MSG_HEADER_OP);

    final DataStore dataStore = dataStoreFactory.getStore();

    Instant now = Instant.now();

    switch (operation) {
      case OP_USER_SIGNIN:
        String password = param.getString(UserHandler.PARAM_PASSWORD);
        param.remove(UserHandler.PARAM_PASSWORD);
        logger.debug("query= " + param.toString());
        dataStore.findOne(clazz, param, null, user -> {
          dataStore.close();
          if (user.failed()) {
            reportDatabaseError(message, user.cause());
          } else if (null == user.result()) {
            reportUserError(message, ErrorCodes.OBJECT_NOT_FOUND.ordinal(), "username is not existed.");
          } else {
            JsonObject mongoUser = user.result();
            String salt = mongoUser.getString(LeanObject.BUILTIN_ATTR_SALT);
            String mongoPassword = mongoUser.getString(LeanObject.BUILTIN_ATTR_PASSWORD);
            mongoUser.remove(LeanObject.BUILTIN_ATTR_SALT);
            mongoUser.remove(LeanObject.BUILTIN_ATTR_PASSWORD);
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
      case OP_OBJECT_UPSERT:
        boolean isCreateOp = false;
        if (StringUtils.isEmpty(objectId)) {
          isCreateOp = true;
          param.put(LeanObject.ATTR_NAME_CREATED_TS, now);
        }
        param.put(LeanObject.ATTR_NAME_UPDATED_TS, now);

        if (isCreateOp) {
          logger.debug("doc=== " + param.toString());
          DataStore.InsertOption insertOption = new DataStore.InsertOption();
          insertOption.setReturnNewDocument(fetchWhenSave);
          dataStore.insertWithOptions(clazz, param, insertOption, res -> {
            dataStore.close();
            if (res.failed()) {
              reportDatabaseError(message, res.cause());
            } else {
              JsonObject result = res.result();
              if (!fetchWhenSave) {
                result.put("createdAt", now.toString());
              }
              logger.debug("storage verticle response: " + result);
              message.reply(result);
            }
          });
        } else {
          logger.debug("doc=== " + param.toString());
          JsonObject query = new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID, objectId);
          logger.debug("query= " + query.toString());
          DataStore.UpdateOption option = new DataStore.UpdateOption();
          option.setUpsert(false);
          option.setReturnNewDocument(fetchWhenSave);
          if (fetchWhenSave) {
            DataStore.QueryOption queryOption = new DataStore.QueryOption();
            dataStore.findOneAndUpdateWithOptions(clazz, query, param, queryOption, option, res -> {
              dataStore.close();
              if (res.failed()) {
                reportDatabaseError(message, res.cause());
              } else {
                if (null != res.result()) {
                  message.reply(res.result());
                } else {
                  reportUserError(message, 404, "object not found");
                }
              }
            });
          } else {
            dataStore.updateWithOptions(clazz, query, param, option, res1 -> {
              dataStore.close();
              if (res1.failed()) {
                reportDatabaseError(message, res1.cause());
              } else {
                if (res1.result() > 0) {
                  JsonObject result = new JsonObject().put("objectId", objectId);
                  result.put("updatedAt", now.toString());
                  message.reply(result);
                } else {
                  reportUserError(message, 404, "object not found");
                }
              }
            });
          }
        }
        break;
      case OP_OBJECT_DELETE:
        if (!StringUtils.isEmpty(objectId)) {
          param.put(LeanObject.ATTR_NAME_OBJECTID, objectId);
        }
        dataStore.removeWithOptions(clazz, param, null, res -> {
          dataStore.close();
          if (res.failed()) {
            reportDatabaseError(message, res.cause());
          } else {
            message.reply(new JsonObject().put("removeCount", res.result()));
          }
        });
        break;
      case OP_OBJECT_QUERY:
        String where = param.getString(QUERY_KEY_WHERE, "{}");
        String order = param.getString(QUERY_KEY_ORDER);
        int limit = Integer.valueOf(param.getString(QUERY_KEY_LIMIT, "100"));
        int skip = Integer.valueOf(param.getString(QUERY_KEY_SKIP, "0"));
        int count = Integer.valueOf(param.getString(QUERY_KEY_COUNT, "0"));
        String include = param.getString(QUERY_KEY_INCLUDE);
        String keys = param.getString(QUERY_KEY_KEYS);

        JsonObject condition = new JsonObject(where);
        if (!StringUtils.isEmpty(objectId)) {
          condition.put(LeanObject.ATTR_NAME_OBJECTID, objectId);
        }

        if (COUNT_FLAG == count) {
          logger.debug("count clazz=" + clazz + ", condition=" + condition.toString());
          dataStore.count(clazz, condition, res -> {
            dataStore.close();
            if (res.failed()) {
              reportDatabaseError(message, res.cause());
            } else {
              message.reply(new JsonObject().put("count", res.result()));
            }
          });
        } else {
          DataStore.QueryOption options = new DataStore.QueryOption();
          options.setLimit(limit);
          options.setSkip(skip);
          JsonObject sortJson = BsonTransformer.parseSortParam(order);
          if (null != sortJson) {
            options.setSort(sortJson);
          }
          JsonObject fieldJson = BsonTransformer.parseProjectParam(keys);
          if (null != fieldJson) {
            options.setFields(fieldJson);
          }
          if (!StringUtils.isEmpty(include)) {
            List<String> includeArray = Arrays.asList(include.split(",")).stream().filter(StringUtils::notEmpty)
                    .collect(Collectors.toList());
            options.setIncludes(includeArray);
          }
          logger.debug("find clazz=" + clazz + ", condition=" + condition.toString() + ", options=" + options);
          dataStore.findWithOptions(clazz, condition, options, res->{
            dataStore.close();
            if (res.failed()) {
              reportDatabaseError(message, res.cause());
            } else {
              Stream<JsonObject> resultStream = res.result().stream().map(BsonTransformer::decodeBsonObject);
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
    logger.info("start StorageVerticle...");
    prepareDatabase();

    vertx.eventBus().consumer(Configure.MAILADDRESS_DATASTORE_QUEUE, this::onMessage);
    logger.info("begin to consume address: " + Configure.MAILADDRESS_DATASTORE_QUEUE);
    startFuture.complete();
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop StorageVerticle...");
    stopFuture.complete();
  }
}

