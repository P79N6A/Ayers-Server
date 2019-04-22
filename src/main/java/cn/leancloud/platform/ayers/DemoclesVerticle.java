package cn.leancloud.platform.ayers;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.modules.ConsistencyViolationException;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cn.leancloud.platform.common.ErrorCodes.INVALID_PARAMETER;

/**
 * Data consistency defender.
 */
public class DemoclesVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DemoclesVerticle.class);
  private InMemoryLRUCache<String, JsonObject> schemaCache;
  private DataStoreFactory dataStoreFactory = null;

  private void saveSchema(String clazz, Schema schema) {
    this.schemaCache.put(clazz, schema);
    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.upsertSchema(clazz, schema, event -> {
      if (event.failed()) {
        logger.warn("failed to upsert class schema. cause: " + event.cause());
      } else {
        logger.warn("succeed to save schema. clazz=" + clazz + ", schema=" + schema);
      }
    });
  }

  public void onMessage(Message<JsonObject> message) {
    JsonObject body = message.body();
    String clazz = body.getString(INTERNAL_MSG_ATTR_CLASS, "");
    JsonObject param = body.getJsonObject(INTERNAL_MSG_ATTR_PARAM, new JsonObject());
    if (null == param || StringUtils.isEmpty(clazz)) {
      logger.warn("clazz or param json is empty, ignore schema check...");
      message.reply("ok");
    } else {
      try {
        LeanObject object = new LeanObject(param);
        Schema inputSchema = object.guessSchema();
        JsonObject cachedData = this.schemaCache.get(clazz);
        if (null != cachedData) {
          Schema cachedSchema = new Schema(cachedData);
          Schema.CompatResult compatResult = inputSchema.compatiableWith(cachedSchema);
          logger.debug("compatiable test. input=" + inputSchema + ", rule=" + cachedSchema + ", result=" + compatResult);
          if (compatResult == Schema.CompatResult.NOT_MATCHED) {
            message.fail(INVALID_PARAMETER.getCode(), "violation data consistency.");
          } else {
            if (compatResult == Schema.CompatResult.OVER_MATCHED) {
              saveSchema(clazz, inputSchema);
            }
            message.reply("ok");
          }
        } else {
          // first encounter, pass.
          logger.info("no cached schema, maybe it's new clazz.");
          saveSchema(clazz, inputSchema);
          message.reply("ok");
        }
      } catch (ConsistencyViolationException ex) {
        message.fail(INVALID_PARAMETER.getCode(), ex.getMessage());
      }
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    logger.info("start DemoclesVerticle...");

    dataStoreFactory = Configure.getInstance().getDataStoreFactory();
    schemaCache = Configure.getInstance().getSchemaCache();
    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.listSchemas(event -> {
      dataStore.close();
      if (event.failed()) {
        logger.warn("failed to fetch schemas, cause: " + event.cause());
      } else {
        event.result().forEach(object  -> {
          String clazz = object.getString("class");
          JsonObject schema = object.getJsonObject("schema");
          if (StringUtils.notEmpty(clazz) && null != schema) {
            logger.info("found class scheme. clazz=" + clazz + ", schema=" + schema);
            schemaCache.putIfAbsent(clazz, schema);
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
