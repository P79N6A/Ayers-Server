package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.CommonVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Future<String> httpVerticleDeployment = Future.future();
    vertx.deployVerticle(new RestServerVerticle(), httpVerticleDeployment);
    httpVerticleDeployment.compose(id -> {
      Future<String> dbVerticleDeployment = Future.future();
      vertx.deployVerticle(DatabaseVerticle.class,
              new DeploymentOptions().setInstances(2),
              dbVerticleDeployment);
      return dbVerticleDeployment;
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
