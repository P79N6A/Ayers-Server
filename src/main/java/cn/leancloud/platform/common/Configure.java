package cn.leancloud.platform.common;

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
  public static final String MAILADDRESS_DB_QUEUE = "mysql.queue";
  public static final String MAILADDRESS_MONGO_QUEUE = "mongo.queue";

  private static Configure instance = null;
  private JsonObject settings = new JsonObject();

  public static Configure getInstance() {
    if (null == instance) {
      instance = new Configure();
    }
    return instance;
  }

  public int listenPort() {
    return settings.getInteger("server.listenPort");
  }

  public int mysqlVerticleCount() {
    return settings.getInteger("server.mysqlVerticles", 1);
  }

  public int mongodbVerticleCount() {
    return settings.getInteger("server.mongodbVerticles", 1);
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

  public JsonObject getSettings() {
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
