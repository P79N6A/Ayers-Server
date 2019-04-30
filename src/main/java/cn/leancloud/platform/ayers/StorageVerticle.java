package cn.leancloud.platform.ayers;

import cn.leancloud.platform.ayers.handler.ObjectQueryHandler;
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
import java.util.stream.Stream;

import static cn.leancloud.platform.utils.JsonFactory.obj;
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
    logger.warn("encounter common erro. code:" + code + ", message:" + message);
    msg.fail(code, message);
  }

  private void processAuthLogin(Message<JsonObject> message, JsonObject query, JsonObject updateParam) {

  }
  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains(INTERNAL_MSG_HEADER_OP)) {
      message.fail(ErrorCodes.INTERNAL_ERROR.ordinal(), "no operation specified.");
      return;
    }
    String operation = message.headers().get(INTERNAL_MSG_HEADER_OP).toUpperCase();
    logger.debug("received message: " + message.body().toString() + ", operation: " + operation);

    JsonObject body = message.body();
    String clazz = body.getString(INTERNAL_MSG_ATTR_CLASS, "");
    String objectId = body.getString(INTERNAL_MSG_ATTR_OBJECT_ID);
    JsonObject updateParam = body.getJsonObject(INTERNAL_MSG_ATTR_UPDATE_PARAM, new JsonObject());
    final JsonObject query = body.getJsonObject(INTERNAL_MSG_ATTR_QUERY, new JsonObject());
    boolean fetchWhenSave = body.getBoolean(INTERNAL_MSG_ATTR_RETURNNEWDOC, false);

    final DataStore dataStore = dataStoreFactory.getStore();
    Instant now = Instant.now();

    switch (operation) {
      case RequestParse.OP_USER_AUTH_LOGIN:
        logger.debug("thirdparty signup/signin. query=" + query + ", param=" + updateParam);
        if (query.size() < 1) {
          message.fail(ErrorCodes.INVALID_PARAMETER.getCode(), "query parameter is empty.");
        } else {
          updateParam.put(LeanObject.ATTR_NAME_UPDATED_TS, now);

          dataStore.findOneAndUpdateWithOptions(clazz, query, updateParam,
                  new DataStore.QueryOption().setLimit(1), new DataStore.UpdateOption().setUpsert(true).setReturnNewDocument(true),
                  res -> {
                    if (res.failed()) {
                      dataStore.close();
                      message.fail(ErrorCodes.DATABASE_ERROR.getCode(), res.cause().getMessage());
                    } else {
                      // res.result() doesn't include user object, need to fetch again.
                      dataStore.findOne(clazz, query, null, res2 -> {
                        dataStore.close();
                        if (res2.failed()) {
                          message.fail(ErrorCodes.DATABASE_ERROR.getCode(), res2.cause().getMessage());
                        } else {
                          message.reply(res2.result());
                        }
                      });
                    }
                  });
        }
        break;
      case RequestParse.OP_USER_SIGNIN:
        String password = updateParam.getString(UserHandler.PARAM_PASSWORD);
        if (StringUtils.notEmpty(password)) {
          updateParam.remove(UserHandler.PARAM_PASSWORD);
        }
        logger.debug("user signin. query= " + updateParam.toString());
        dataStore.findOne(clazz, updateParam, null, user -> {
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
                reportUserError(message, ErrorCodes.PASSWORD_WRONG.getCode(), ErrorCodes.PASSWORD_WRONG.getMessage());
              }
            }
          }
        });
        break;
      case RequestParse.OP_USER_SIGNUP:
      case RequestParse.OP_OBJECT_POST:
        updateParam.put(LeanObject.ATTR_NAME_CREATED_TS, now);
        updateParam.put(LeanObject.ATTR_NAME_UPDATED_TS, now);
        logger.debug("insert doc=== " + updateParam.toString());
        DataStore.InsertOption insertOption = new DataStore.InsertOption();
        insertOption.setReturnNewDocument(fetchWhenSave);
        dataStore.insertWithOptions(clazz, updateParam, insertOption, res -> {
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
        break;
      case RequestParse.OP_OBJECT_PUT:
        updateParam.put(LeanObject.ATTR_NAME_UPDATED_TS, now);

        logger.debug("doc=== " + updateParam.toString());
        if (StringUtils.notEmpty(objectId)) {
          query.put(LeanObject.ATTR_NAME_OBJECTID, objectId);
        }
        logger.debug("query= " + query);
        if (query.size() < 1) {
          reportUserError(message, ErrorCodes.INVALID_PARAMETER.getCode(), "query is empty, and updating all objects is forbidden.");
        } else {
          DataStore.UpdateOption option = new DataStore.UpdateOption();
          option.setUpsert(false);
          option.setReturnNewDocument(fetchWhenSave);
          if (fetchWhenSave) {
            DataStore.QueryOption queryOption = new DataStore.QueryOption();
            logger.debug("try to call findOneAndUpdateWithOptions.");
            dataStore.findOneAndUpdateWithOptions(clazz, query, updateParam, queryOption, option, res -> {
              dataStore.close();
              if (res.failed()) {
                logger.warn("failed to call findOneAndUpdateWithOptions. cause:" + res.cause());
                reportDatabaseError(message, res.cause());
              } else {
                if (null != res.result()) {
                  logger.debug("succeed to call findOneAndUpdateWithOptions. result=" + res.result());
                  message.reply(res.result());
                } else {
                  logger.debug("object not found.");
                  reportUserError(message, 404, "object not found");
                }
              }
            });
          } else {
            dataStore.updateWithOptions(clazz, query, updateParam, option, res1 -> {
              dataStore.close();
              if (res1.failed()) {
                reportDatabaseError(message, res1.cause());
              } else if (StringUtils.notEmpty(objectId)) {
                if (res1.result() > 0) {
                  JsonObject result = new JsonObject().put("objectId", objectId);
                  result.put("updatedAt", now.toString());
                  message.reply(result);
                } else {
                  reportUserError(message, 404, "object not found");
                }
              } else {
                message.reply(new JsonObject().put("updated_count", res1.result()));
              }
            });
          }
        }
        break;
      case RequestParse.OP_OBJECT_DELETE:
        if (!StringUtils.isEmpty(objectId)) {
          updateParam.put(LeanObject.ATTR_NAME_OBJECTID, objectId);
        }
        dataStore.removeWithOptions(clazz, updateParam, null, res -> {
          dataStore.close();
          if (res.failed()) {
            reportDatabaseError(message, res.cause());
          } else {
            message.reply(new JsonObject().put("removeCount", res.result()));
          }
        });
        break;
      case RequestParse.OP_OBJECT_GET:
        int limit = query.getInteger(ObjectQueryHandler.QUERY_KEY_LIMIT, 100);
        int skip = query.getInteger(ObjectQueryHandler.QUERY_KEY_SKIP, 0);
        int count = query.getInteger(ObjectQueryHandler.QUERY_KEY_COUNT, 0);
        JsonObject condition = query.getJsonObject(ObjectQueryHandler.QUERY_KEY_WHERE);
        JsonObject sortJson = query.getJsonObject(ObjectQueryHandler.QUERY_KEY_ORDER);
        JsonObject fieldJson = query.getJsonObject(ObjectQueryHandler.QUERY_KEY_KEYS);
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
          options.setSort(sortJson);
          options.setFields(fieldJson);
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
      case RequestParse.OP_CREATE_INDEX:
        DataStore.IndexOption indexOption = new DataStore.IndexOption()
                .setName(objectId).setUnique(query.getBoolean(RequestParse.REQUEST_INDEX_OPTION_UNIQUE))
                .setSparse(query.getBoolean(RequestParse.REQUEST_INDEX_OPTION_SPARSE));
        dataStore.createIndexWithOptions(clazz, updateParam, indexOption, ar -> {
          dataStore.close();
          if (ar.failed()) {
            reportDatabaseError(message, ar.cause());
          } else {
            message.reply(new JsonObject());
          }
        });
        break;
      case RequestParse.OP_DELETE_INDEX:
        dataStore.dropIndex(clazz, objectId, res -> {
          dataStore.close();
          if (res.failed()) {
            reportDatabaseError(message, res.cause());
          } else {
            message.reply(new JsonObject());
          }
        });
        break;
      case RequestParse.OP_LIST_INDEX:
        dataStore.listIndices(clazz, res -> {
          dataStore.close();
          if (res.failed()) {
            reportDatabaseError(message, res.cause());
          } else {
            message.reply(new JsonObject().put("results", res.result()));
          }
        });
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

