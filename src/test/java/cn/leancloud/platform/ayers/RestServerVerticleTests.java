package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.Configure;
import io.vertx.core.DeploymentOptions;
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

    Configure.getInstance().initialize(vertx, res -> {});

    // Let's configure the verticle to listen on the 'test' port (randomly picked).
    // We create deployment options and set the _configuration_ json object:
    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    Configure.getInstance().setListenPort(port);

    DeploymentOptions options = new DeploymentOptions()
            .setConfig(new JsonObject().put("http.port", port)
            );

    // We pass the options as the second parameter of the deployVerticle method.
    vertx.deployVerticle(RestServerVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
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
  public void checkThatWeCanAdd(TestContext context) {
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