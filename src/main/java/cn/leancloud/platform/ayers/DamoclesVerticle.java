package cn.leancloud.platform.ayers;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.ClassMetaData;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.modules.ConsistencyViolationException;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static cn.leancloud.platform.common.ErrorCodes.INVALID_PARAMETER;

/**
 * Data consistency defender.
 */
public class DamoclesVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DamoclesVerticle.class);
  private InMemoryLRUCache<String, JsonObject> classMetaCache;
  private DataStoreFactory dataStoreFactory = null;

  private void saveSchema(String clazz, Schema schema, JsonArray indices) {
    this.classMetaCache.put(clazz, new ClassMetaData(clazz, schema, indices));
    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.upsertSchema(clazz, schema, event -> {
      if (event.failed()) {
        logger.warn("failed to upsert class schema. cause: " + event.cause());
      } else {
        logger.info("succeed to save schema. clazz=" + clazz + ", schema=" + schema);
      }
    });

    final JsonArray newIndices = (null == indices)?new JsonArray():indices;

    Future<Boolean> future = Future.succeededFuture(true);
    future.compose(res -> {
      // make sure 2dsphere
      String  geoPointAttrPath = schema.findGeoPointAttr();
      Future<Boolean> sphereFuture = Future.future();
      if (StringUtils.notEmpty(geoPointAttrPath)) {
        JsonObject indexJson = new JsonObject().put(geoPointAttrPath, "2dsphere");
        DataStore.IndexOption indexOption = new DataStore.IndexOption().setSparse(true).setName(geoPointAttrPath);
        makeSureIndexCreated(clazz, indexJson, indexOption, indices, dataStore, response -> {
          if (response.failed()) {
            logger.warn("failed to create index. attr=" + geoPointAttrPath + ". cause: " + response.cause());
            future.fail(response.cause());
          } else {
            if (response.result()) {
              logger.info("success to create index. attr=" + geoPointAttrPath);
              newIndices.add(indexOption.toJson());
            }
            future.complete(response.result());
          }
        });
      } else {
        sphereFuture.complete();
      }
      return sphereFuture;
    })
    .compose(res -> makeSureDefaultIndex(clazz, LeanObject.ATTR_NAME_UPDATED_TS, false, indices, newIndices, dataStore))
    .compose(res -> makeSureDefaultIndex(clazz, LeanObject.ATTR_NAME_CREATED_TS, false, indices, newIndices, dataStore))
    .compose(res -> {
      if (Constraints.USER_CLASS.equals(clazz)) {
        return makeSureDefaultIndex(clazz, LeanObject.BUILTIN_ATTR_EMAIL, true, indices, newIndices, dataStore);
      } else {
        return Future.succeededFuture(true);
      }
    }).compose(res -> {
        if (Constraints.USER_CLASS.equals(clazz)) {
          return makeSureDefaultIndex(clazz, LeanObject.BUILTIN_ATTR_USERNAME, true, indices, newIndices, dataStore);
        } else {
          return Future.succeededFuture(true);
        }
    }).compose(res -> {
      if (Constraints.USER_CLASS.equals(clazz)) {
        return makeSureDefaultIndex(clazz, LeanObject.BUILTIN_ATTR_MOBILEPHONE, true, indices, newIndices, dataStore);
      } else {
        return Future.succeededFuture(true);
      }
    }).compose(res -> {
      if (Constraints.USER_CLASS.equals(clazz)) {
        List<String> authIndexPaths = schema.findAuthDataIndex();
        Future<Boolean> composedFuture = Future.succeededFuture(true);
        for (String attr : authIndexPaths) {
          composedFuture = composedFuture.compose(r -> makeSureDefaultIndex(clazz, attr, true, indices, newIndices, dataStore));
        }
        return composedFuture;
      } else {
        return Future.succeededFuture(true);
      }
    }).setHandler(response -> {
      dataStore.close();
      this.classMetaCache.put(clazz, new ClassMetaData(clazz, schema, newIndices));
    });
  }

  private Future<Boolean> makeSureDefaultIndex(String clazz, String attr, boolean isUnique, JsonArray existedIndex, JsonArray newIndices,
                                               DataStore dataStore) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(attr);
    Objects.requireNonNull(newIndices);
    Future<Boolean> defaultFuture = Future.future();
    JsonObject indexJson = new JsonObject().put(attr, 1);
    DataStore.IndexOption indexOption = new DataStore.IndexOption().setSparse(true).setUnique(isUnique).setName(attr);
    makeSureIndexCreated(clazz, indexJson, indexOption, existedIndex, dataStore, response -> {
      if (response.failed()) {
        logger.warn("failed to create index. attr=" + attr + ". cause: " + response.cause());
        defaultFuture.fail(response.cause());
      } else {
        if (response.result()) {
          logger.info("success to create index. attr=" + attr);
          newIndices.add(indexOption.toJson());
        }
        defaultFuture.complete(response.result());
      }
    });

    return defaultFuture;
  }

  private void makeSureIndexCreated(String clazz, JsonObject indexJson, DataStore.IndexOption indexOption,
                                    JsonArray existedIndex, DataStore dataStore, Handler<AsyncResult<Boolean>> handler) {
    Objects.requireNonNull(indexJson);
    Objects.requireNonNull(indexOption);
    Objects.requireNonNull(dataStore);
    Objects.requireNonNull(handler);
    if (indexJson.size() < 1) {
      throw new IllegalArgumentException("indexJson is empty.");
    }

    String attrPath = indexJson.stream().findFirst().get().getKey();
    boolean existed = (null == existedIndex) ? false :
            existedIndex.stream().filter(json -> attrPath.equals(((JsonObject)json).getString("name"))).count() > 0;
    if (existed) {
      handler.handle(new AsyncResult<Boolean>() {
        @Override
        public Boolean result() {
          return false;
        }

        @Override
        public Throwable cause() {
          return null;
        }

        @Override
        public boolean succeeded() {
          return true;
        }

        @Override
        public boolean failed() {
          return false;
        }
      });
    } else {
      dataStore.createIndexWithOptions(clazz, indexJson, indexOption, res -> handler.handle(res.map(res.succeeded())));
    }
  }

  public void onMessage(Message<JsonObject> message) {
    JsonObject body = message.body();
    String clazz = body.getString(INTERNAL_MSG_ATTR_CLASS, "");
    JsonObject param = body.getJsonObject(INTERNAL_MSG_ATTR_UPDATE_PARAM);

    String operation = message.headers().get(INTERNAL_MSG_HEADER_OP).toUpperCase();

    ClassMetaData cachedData;
    if (StringUtils.isEmpty(clazz)) {
      logger.warn("clazz name is empty, ignore schema check...");
      message.fail(ErrorCodes.INVALID_PARAMETER.getCode(), "className is required.");
    } else {
      LeanObject object = null != param? new LeanObject(param) : null;
      Schema inputSchema;

      if (RequestParse.OP_FIND_SCHEMA.equals(operation)) {
        cachedData = (ClassMetaData) this.classMetaCache.get(clazz);
        if (null == cachedData) {
          message.reply(new JsonObject());
        } else {
          message.reply(cachedData.getSchema());
        }
      } else if (null == object) {
        message.fail(ErrorCodes.INVALID_PARAMETER.getCode(), "param json is required.");
      } else {
        switch (operation) {
          case RequestParse.OP_ADD_SCHEMA:
            inputSchema = object.guessSchema();
            cachedData = (ClassMetaData)this.classMetaCache.get(clazz);
            if (null == cachedData || null == cachedData.getSchema()) {
              saveSchema(clazz, inputSchema, null == cachedData? null : cachedData.getIndices());
              message.reply(new JsonObject().put("result", true));
            } else {
              message.reply(new JsonObject().put("result", false));
            }
            break;
          case RequestParse.OP_TEST_SCHEMA:
            try {
              inputSchema = object.guessSchema();
              cachedData = (ClassMetaData)this.classMetaCache.get(clazz);
              if (null != cachedData) {
                Schema cachedSchema = new Schema(cachedData.getSchema());
                Schema.CompatResult compatResult = inputSchema.compatibleWith(cachedSchema);
                if (compatResult == Schema.CompatResult.NOT_MATCHED) {
                  message.reply(new JsonObject().put("result", false));
                } else {
                  message.reply(new JsonObject().put("result", true));
                }
              } else {
                // anything is matched if first occurring.
                message.reply(new JsonObject().put("result", true));
              }
            } catch (ConsistencyViolationException ex) {
              message.reply(new JsonObject().put("result", false));
            }
            break;
          default:
            // object CRUD.
            try {
              inputSchema = object.guessSchema();
              cachedData = (ClassMetaData)this.classMetaCache.get(clazz);
              if (null != cachedData) {
                Schema cachedSchema = new Schema(cachedData.getSchema());
                Schema.CompatResult compatResult = inputSchema.compatibleWith(cachedSchema);
                logger.debug("compatibility test. input=" + inputSchema + ", rule=" + cachedSchema + ", result=" + compatResult);
                if (compatResult == Schema.CompatResult.NOT_MATCHED) {
                  message.fail(INVALID_PARAMETER.getCode(), "data consistency violated.");
                } else {
                  if (compatResult == Schema.CompatResult.OVER_MATCHED) {
                    saveSchema(clazz, inputSchema, cachedData.getIndices());
                  }
                  message.reply(new JsonObject().put("result", true));
                }
              } else {
                // first encounter, pass.
                logger.info("no cached schema, maybe it's new clazz.");
                saveSchema(clazz, inputSchema, new JsonArray());
                message.reply(new JsonObject().put("result", true));
              }
            } catch (ConsistencyViolationException ex) {
              logger.warn("failed to parse object schema. cause: " + ex.getMessage());
              message.fail(INVALID_PARAMETER.getCode(), ex.getMessage());
            }
            break;
        }
      }
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    logger.info("start DamoclesVerticle...");

    dataStoreFactory = Configure.getInstance().getDataStoreFactory();
    classMetaCache = Configure.getInstance().getSchemaCache();
    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.listSchemas(event -> {
      if (event.failed()) {
        logger.warn("failed to fetch schemas, cause: " + event.cause());
        dataStore.close();
      } else {
        event.result().forEach(object  -> {
          String clazz = object.getString("class");
          JsonObject schema = object.getJsonObject("schema");
          if (StringUtils.notEmpty(clazz) && null != schema) {
            logger.info("found class scheme. clazz=" + clazz + ", schema=" + schema);
            dataStore.listIndices(clazz, res-> {
              JsonArray indices = null;
              if (res.failed()) {
                logger.warn("failed to load indices. cause: " + res.cause());
              } else {
                indices = res.result().stream().filter(json -> !((JsonObject)json).getString("name").equals("_id_"))
                        .collect(JsonFactory.toJsonArray());
                logger.info("found indices. clazz=" + clazz + ", indices=" + indices.toString());
              }
              if (null == indices) {
                indices = new JsonArray();
              }
              classMetaCache.putIfAbsent(clazz, new ClassMetaData(clazz, schema, indices));
            });
          }
        });
      }
      vertx.eventBus().consumer(Configure.MAIL_ADDRESS_DAMOCLES_QUEUE, this::onMessage);
      logger.info("begin to consume address: " + Configure.MAIL_ADDRESS_DAMOCLES_QUEUE);
      startFuture.complete();
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop DamoclesVerticle...");
    stopFuture.complete();
  }
}
