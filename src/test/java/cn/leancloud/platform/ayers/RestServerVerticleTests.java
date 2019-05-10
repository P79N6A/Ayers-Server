package cn.leancloud.platform.ayers;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.persistence.DataStoreFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class RestServerVerticleTests {
  private Vertx vertx;
  private Integer port;

  @Before
  public void setUp(TestContext context) throws Exception {
    vertx = Vertx.vertx();

    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    Future<JsonObject> configFuture = Future.future();
    Configure.getInstance().initialize(vertx, configFuture);
    configFuture.compose(response -> {
      JsonObject mongoConfig = new JsonObject()
              .put("host", "localhost")
              .put("port", 27027)
              .put("db_name", "uluru-test")
              .put("maxPoolSize", 10)
              .put("minPoolSize", 1)
              .put("maxIdleTimeMS", 10000)
              .put("maxLifeTimeMS", 30000)
              .put("waitQueueMultiple", 10)
              .put("waitQueueTimeoutMS", 5000)
              .put("serverSelectionTimeoutMS", 5000)
              .put("keepAlive", true);
      DataStoreFactory dataStoreFactory = new DataStoreFactory.Builder().setType(DataStoreFactory.SUPPORT_DATA_SOURCE_MONGO)
              .setOptions(mongoConfig)
              .setSourceName("MongoPool")
              .build(vertx);
      Configure.getInstance().setListenPort(port);
      Configure.getInstance().setDataStoreFactory(dataStoreFactory);
      Configure.getInstance().setClassMetaDataCache(new InMemoryLRUCache<>(1000));
      Future<String> httpVerticleDeployment = Future.future();
      vertx.deployVerticle(new RestServerVerticle(), httpVerticleDeployment);
      return httpVerticleDeployment;
    }).compose(id1 -> {
      Future<String> damoclesVerticleDeployment = Future.future();
      vertx.deployVerticle(DamoclesVerticle.class,
              new DeploymentOptions().setInstances(1),
              damoclesVerticleDeployment);
      return damoclesVerticleDeployment;
    }).compose(id2 -> {
      Future<String> datastoreVerticleDeployment = Future.future();
      vertx.deployVerticle(StorageVerticle.class,
              new DeploymentOptions().setInstances(1),
              datastoreVerticleDeployment);
      return datastoreVerticleDeployment;
    }).setHandler(arr -> {
      System.out.println("setUp finished.");
      if (arr.succeeded()) {
        context.asyncAssertSuccess();
      } else {
        context.asyncAssertFailure();
      }
    });
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(res -> {
      System.out.println("tearDown and invoke vertx.close()");
      context.asyncAssertSuccess();
    });
  }

  @Test
  public void testPing(TestContext context) {
    // This test is asynchronous, so get an async handler to inform the test when we are done.
    final Async async = context.async();

    // We create a HTTP client and query our application. When we get the response we check it contains the 'Hello'
    // message. Then, we call the `complete` method on the async handler to declare this async (and here the test) done.
    // Notice that the assertions are made on the 'context' object and are not Junit assert. This ways it manage the
    // async aspect of the test the right way.
    vertx.createHttpClient().getNow(port, "localhost", "/ping", response -> {
      response.handler(body -> {
        context.assertTrue(body.toString().contains("status"));
        async.complete();
      });
    });
  }

  @Test
  public void testCurrentDate(TestContext context) {
    Async async = context.async();
    final String json = Json.encodePrettily(new JsonObject().put("Jameson", "Ireland"));
    vertx.createHttpClient().get(port, "localhost", "/1.1/date")
            .putHeader("X-LC-Id", "application/json")
            .putHeader("X-LC-Key", "application/json")
            .putHeader("X-LC-SessionToken", "application/json")
            .putHeader("content-type", "application/json")
            .putHeader("content-length", Integer.toString(json.length()))
            .handler(response -> {
              context.assertEquals(response.statusCode(), 200);
              context.assertTrue(response.headers().get("content-type").contains("application/json"));
              response.bodyHandler(body -> {
                System.out.println(body.toString());
                final JsonObject whisky = new JsonObject(body.toString());
                context.assertNotNull(whisky.getString("iso"));
                async.complete();
              });
            })
            .write(json)
            .end();
  }
}