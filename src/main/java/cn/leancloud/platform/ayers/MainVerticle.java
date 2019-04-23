package cn.leancloud.platform.ayers;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.persistence.DataStoreFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  private static final String DATASTORE_MONGO = "mongo";

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    final Configure configure = Configure.getInstance();
    Future<JsonObject> configFuture = Future.future();
    configure.initialize(vertx, configFuture);
    configFuture.compose(response -> {
      boolean useMongoDB = DATASTORE_MONGO.equalsIgnoreCase(configure.dataStoreType());

      // initialize mongo data store factory.
      DataStoreFactory dataStoreFactory = null;
      if (useMongoDB) {
        String hosts = configure.mongoHosts();
        String[] hostParts = hosts.split(":");
        int port = 27017;
        if (hostParts.length > 1) {
          port = Integer.valueOf(hostParts[1]);
        }
        JsonObject mongoConfig = new JsonObject()
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
        dataStoreFactory = new DataStoreFactory.Builder().setType(DataStoreFactory.SUPPORT_DATA_SOURCE_MONGO)
                .setOptions(mongoConfig)
                .setSourceName("MongoPool")
                .build(vertx);
      } else {
        String hosts = configure.mysqlHosts();
        String[] hostParts = hosts.split(":");
        int port = 3306;
        if (hostParts.length > 1) {
          port = Integer.valueOf(hostParts[1]);
        }
        JsonObject mySQLClientConfig = new JsonObject()
                .put("host", hostParts[0])
                .put("port", port)
                .put("username", configure.mysqlUsername())
                .put("password", configure.mysqlPassword())
                .put("database", configure.mysqlDatabase())
                .put("charset", "utf-8")
                .put("connectTimeout", configure.mysqlConnectTimeout())
                .put("queryTimeout", configure.mysqlQueryTimeoutMS())
                .put("connectionRetryDelay", configure.mysqlConnRetryDelay())
                .put("maxPoolSize", configure.mysqlMaxPoolSize());
        logger.info("initialize mysql with config: " + mySQLClientConfig);
        dataStoreFactory = new DataStoreFactory.Builder().setType(DataStoreFactory.SUPPORT_DATA_SOURCE_MYSQL)
                .setOptions(mySQLClientConfig)
                .setSourceName("MysqlPool")
                .build(vertx);
      }
      configure.setDataStoreFactory(dataStoreFactory);
      configure.setSchemaCache(new InMemoryLRUCache<>(1000));

      Future<String> httpVerticleDeployment = Future.future();
      vertx.deployVerticle(new RestServerVerticle(), httpVerticleDeployment);
      return httpVerticleDeployment;
    }).compose(id1 -> {
      logger.info("try to deploy "+ configure.datastoreVerticleCount() + " democles verticles.");
      Future<String> democlesVerticleDeployment = Future.future();
      vertx.deployVerticle(DemoclesVerticle.class,
              new DeploymentOptions().setInstances(configure.datastoreVerticleCount()),
              democlesVerticleDeployment);
      return democlesVerticleDeployment;
    }).compose(id2 -> {
      logger.info("try to deploy "+ configure.datastoreVerticleCount() + " dataStore verticles.");
      Future<String> datastoreVerticleDeployment = Future.future();
      vertx.deployVerticle(StorageVerticle.class,
              new DeploymentOptions().setInstances(configure.datastoreVerticleCount()),
              datastoreVerticleDeployment);
      return datastoreVerticleDeployment;
    }).setHandler(arr -> {
      if (arr.succeeded()) {
        logger.info("main verticle start!");
        startFuture.complete();
      } else {
        logger.error("main verticle failed! cause: " + arr.cause().getMessage());
        startFuture.fail(arr.cause());
      }
    });

  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    vertx.close(res -> {
      logger.info("main verticle stop!");
      stopFuture.complete();
    });
  }
}
