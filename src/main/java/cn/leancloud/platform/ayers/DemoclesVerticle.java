package cn.leancloud.platform.ayers;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.ClassMetaData;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.modules.ConsistencyViolationException;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cn.leancloud.platform.common.ErrorCodes.INVALID_PARAMETER;

/**
 * Data consistency defender.
 */
public class DemoclesVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DemoclesVerticle.class);
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
    String  geoPointAttrPath = schema.findGeoPointAttr();
    if (StringUtils.notEmpty(geoPointAttrPath)) {
      boolean existed = indices.stream().filter(json -> geoPointAttrPath.equals(((JsonObject)json).getString("name")))
              .count() > 0;
      if (!existed) {
        logger.info("auto create 2dsphere index for clazz=" + clazz + ", attr=" + geoPointAttrPath);
        DataStore.IndexOption indexOption = new DataStore.IndexOption().setSparse(true).setName(geoPointAttrPath);
        dataStore.createIndexWithOptions(clazz, new JsonObject().put(geoPointAttrPath, "2dsphere"), indexOption, res -> {
          if (res.failed()) {
            logger.warn("failed to create index. attr=" + geoPointAttrPath + ". cause: " + res.cause());
          } else {
            logger.info("success to create index. attr=" + geoPointAttrPath);
            indices.add(indexOption.toJson());
            this.classMetaCache.put(clazz, new ClassMetaData(clazz, schema, indices));
          }
        });
      }
    }
  }

  public void onMessage(Message<JsonObject> message) {
    JsonObject body = message.body();
    String clazz = body.getString(INTERNAL_MSG_ATTR_CLASS, "");
    JsonObject param = body.getJsonObject(INTERNAL_MSG_ATTR_UPDATE_PARAM);

    String operation = message.headers().get(INTERNAL_MSG_HEADER_OP).toUpperCase();

    ClassMetaData cachedData;
    if (null == param || StringUtils.isEmpty(clazz)) {
      logger.warn("clazz or param json is empty, ignore schema check...");
      message.reply("ok");
    } else {
      LeanObject object = null != param? new LeanObject(param) : null;
      Schema inputSchema;

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
              Schema.CompatResult compatResult = inputSchema.compatiableWith(cachedSchema);
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
        case RequestParse.OP_FIND_SCHEMA:
          cachedData = (ClassMetaData) this.classMetaCache.get(clazz);
          if (null == cachedData) {
            message.reply(new JsonObject());
          } else {
            message.reply(cachedData.getSchema());
          }
          break;
        default:
          // object CRUD.
          try {
            inputSchema = object.guessSchema();
            cachedData = (ClassMetaData)this.classMetaCache.get(clazz);
            if (null != cachedData) {
              Schema cachedSchema = new Schema(cachedData.getSchema());
              Schema.CompatResult compatResult = inputSchema.compatiableWith(cachedSchema);
              logger.debug("compatiable test. input=" + inputSchema + ", rule=" + cachedSchema + ", result=" + compatResult);
              if (compatResult == Schema.CompatResult.NOT_MATCHED) {
                message.fail(INVALID_PARAMETER.getCode(), "data consistency violated.");
              } else {
                if (compatResult == Schema.CompatResult.OVER_MATCHED) {
                  saveSchema(clazz, inputSchema, cachedData.getIndices());
                }
                message.reply("ok");
              }
            } else {
              // first encounter, pass.
              logger.info("no cached schema, maybe it's new clazz.");
              saveSchema(clazz, inputSchema, new JsonArray());
              message.reply("ok");
            }
          } catch (ConsistencyViolationException ex) {
            logger.warn("failed to parse object schema. cause: " + ex.getMessage());
            message.fail(INVALID_PARAMETER.getCode(), ex.getMessage());
          }
          break;
      }
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    logger.info("start DemoclesVerticle...");

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
      vertx.eventBus().consumer(Configure.MAILADDRESS_DEMOCLES_QUEUE, this::onMessage);
      logger.info("begin to consume address: " + Configure.MAILADDRESS_DEMOCLES_QUEUE);
      startFuture.complete();
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop DemoclesVerticle...");
    stopFuture.complete();
  }
}
