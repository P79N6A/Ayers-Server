package cn.leancloud.platform.common;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.persistence.DataStoreFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configure {
  private static final Logger logger = LoggerFactory.getLogger(Configure.class);
  public static final String MAIL_ADDRESS_DATASTORE_QUEUE = "datastore.queue";
  public static final String MAIL_ADDRESS_DAMOCLES_QUEUE = "damocles.queue";

  private static Configure instance = null;
  private JsonObject settings = new JsonObject();
  private DataStoreFactory dataStoreFactory = null;
  private InMemoryLRUCache<String, JsonObject> classMetaDataCache;

  public static Configure getInstance() {
    if (null == instance) {
      instance = new Configure();
    }
    return instance;
  }

  public InMemoryLRUCache<String, JsonObject> getClassMetaDataCache() {
    return classMetaDataCache;
  }

  public void setClassMetaDataCache(InMemoryLRUCache<String, JsonObject> classMetaDataCache) {
    this.classMetaDataCache = classMetaDataCache;
  }

  public DataStoreFactory getDataStoreFactory() {
    return dataStoreFactory;
  }

  public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
    this.dataStoreFactory = dataStoreFactory;
  }

  public int listenPort() {
    return settings.getInteger("server.listenPort");
  }

  /**
   * add for test.
   * @param port
   */
  public void setListenPort(int port) {
    settings.put("server.listenPort", port);
  }

  public String getBaseHost() {
    return settings.getString("server.baseHost", "http://127.0.0.1");
  }

  public String dataStoreType() {
    return settings.getString("server.datastoreType", "mongo");
  }

  public String secretKey() {
    return settings.getString("server.secretKey", "leancloudsec");
  }

  public int datastoreVerticleCount() {
    return settings.getInteger("server.datastoreVerticles", 1);
  }

  public String getUluruAPIHost() {
    return settings.getString("server.uluru_api.host", "api.leancloud.cn");
  }

  public int getUluruAPIPort() {
    return settings.getInteger("server.uluru_api.port", 443);
  }

  public String getUluruEngineHost() {
    return settings.getString("server.uluru_engine.host", "api.leancloud.cn");
  }

  public int getUluruEnginePort() {
    return settings.getInteger("server.uluru_engine.port", 80);
  }

  public String mysqlHosts() {
    return settings.getString("mysql.hosts");
  }

  public String mysqlDatabase() {
    return settings.getString("mysql.database");
  }

  public String mysqlUsername() {
    return settings.getString("mysql.username");
  }

  public String mysqlPassword() {
    return settings.getString("mysql.password");
  }

  public int mysqlQueryTimeoutMS() {
    return settings.getInteger("mysql.queryTimeoutMS", 5000);
  }

  public int mysqlMaxPoolSize() {
    return settings.getInteger("mysql.maxPoolSize", 50);
  }

  public int mysqlConnectTimeout() {
    return settings.getInteger("mysql.connectTimeout", 30000);
  }

  public int mysqlConnRetryDelay() {
    return settings.getInteger("mysql.connectionRetryDelay", 5000);
  }

  public String mongoHosts() {
    return settings.getString("mongo.hosts");
  }

  public String mongoDatabase() {
    return settings.getString("mongo.database");
  }

  public String mongoUsername() {
    return settings.getString("mongo.username");
  }

  public String mongoPassword() {
    return settings.getString("mongo.password");
  }

  public int mongoMinPoolSize() {
    return settings.getInteger("mongo.minPoolSize", 5);
  }

  public int mongoMaxPoolSize() {
    return settings.getInteger("mongo.maxPoolSize", 100);
  }

  public int mongoServerSelectionTimeoutMS() {
    return settings.getInteger("mongo.serverSelectionTimeoutMS", 15000);
  }

  public int mongoMaxIdleTimeMS() {
    return settings.getInteger("mongo.maxIdleTimeMS", 300000);
  }
  public int mongoMaxLifeTimeMS() {
    return settings.getInteger("mongo.maxLifeTimeMS", 3600000);
  }
  public int mongoWaitQueueMultiple() {
    return settings.getInteger("mongo.waitQueueMultiple", 20);
  }
  public int mongoWaitQueueTimeoutMS() {
    return settings.getInteger("mongo.waitQueueTimeoutMS", 10000);
  }

  public String redisHosts() {
    return settings.getString("redis.hosts");
  }

  public String redisHAType() {
    return settings.getString("redis.type");
  }

  public String redisMasterName() {
    return settings.getString("redis.masterName");
  }

  public String redisRole() {
    return settings.getString("redis.role");
  }

  public String fileProvideName() {
    return settings.getString("file.provider.name");
  }

  public String fileDefaultHost() {
    return settings.getString("file.defaultHost");
  }

  public String fileUploadHost() {
    return settings.getString("file.uploadHost");
  }

  public String fileBucket() {
    return settings.getString("file.bucket");
  }

  public String fileProviderAccessKey() {
    return settings.getString("file.provider.accessKey");
  }

  public String fileProviderSecretKey() {
    return settings.getString("file.provider.secretKey");
  }

  private JsonObject getSettings() {
    return settings;
  }

  public void initialize(Vertx vertx, Handler<AsyncResult<JsonObject>> handler) {
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
            .setType("file").setFormat("properties")
            .setConfig(new JsonObject().put("path", "conf/application.properties"));
    ConfigStoreOptions sysPropsStore = new ConfigStoreOptions().setType("sys").setOptional(true);
    ConfigStoreOptions envStore = new ConfigStoreOptions().setType("env").setOptional(true);
    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
            .addStore(fileStore).addStore(sysPropsStore).addStore(envStore);

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    retriever.getConfig(ar -> {
      if (ar.failed()) {
        // use default setting.
        logger.warn("failed to load configure. cause: ", ar.cause());
      } else {
        this.settings = ar.result();
      }
      handler.handle(ar);
    });
  }
}
