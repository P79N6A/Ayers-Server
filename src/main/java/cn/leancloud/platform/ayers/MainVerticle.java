package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.Configure;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class MainVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    final Configure configure = Configure.getInstance();
    Future<JsonObject> configFuture = Future.future();
    configure.initialize(vertx, configFuture);

    configFuture.compose(response -> {
      // start rest verticle.
      Future<String> httpVerticleDeployment = Future.future();
      vertx.deployVerticle(new RestServerVerticle(), httpVerticleDeployment);
      return httpVerticleDeployment;
    }).compose(id1 -> {
      // start database verticle.
      logger.info("try to deploy "+ configure.mysqlVerticleCount() + " mysql verticles.");
      Future<String> dbVerticleDeployment = Future.future();
      vertx.deployVerticle(DatabaseVerticle.class,
              new DeploymentOptions().setInstances(configure.mysqlVerticleCount()),
              dbVerticleDeployment);
      return dbVerticleDeployment;
    }).compose(id2 -> {
      // start mongo verticle.
      logger.info("try to deploy "+ configure.mysqlVerticleCount() + " mongodb verticles.");
      Future<String> mongoVerticleDeployment = Future.future();
      vertx.deployVerticle(MongoDBVerticle.class,
              new DeploymentOptions().setInstances(configure.mongoVerticleCount()),
              mongoVerticleDeployment);
      return mongoVerticleDeployment;
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
//    vertx.close(res -> {
//      logger.info("main verticle stop!");
//      stopFuture.complete();
//    });
//    stopFuture.complete();
  }
}
