package cn.leancloud.platform.ayers;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    // start rest verticle.
    Future<String> httpVerticleDeployment = Future.future();
    vertx.deployVerticle(new RestServerVerticle(), httpVerticleDeployment);

    httpVerticleDeployment.compose(id -> {
      // start database verticle.
      Future<String> dbVerticleDeployment = Future.future();
      vertx.deployVerticle(DatabaseVerticle.class,
              new DeploymentOptions().setInstances(1),
              dbVerticleDeployment);
      return dbVerticleDeployment;
    }).compose(id -> {
      // start mongo verticle.
      Future<String> mongoVerticleDeployment = Future.future();
      vertx.deployVerticle(MongoDBVerticle.class,
              new DeploymentOptions().setInstances(1),
              mongoVerticleDeployment);
      return mongoVerticleDeployment;
    }).setHandler(ar -> {
      if (ar.succeeded()) {
        logger.info("main verticle start!");
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    ;
  }
}
