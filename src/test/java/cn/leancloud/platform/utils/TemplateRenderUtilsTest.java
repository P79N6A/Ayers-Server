package cn.leancloud.platform.utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;

public class TemplateRenderUtilsTest extends TestCase {
  private boolean testSucceed = false;
  @Override
  protected void setUp() throws Exception {
    testSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testTemplateRendering() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    JsonObject data = new JsonObject().put("appname", "LeanCloud").put("username", "sama")
            .put("email", "app.leanapp.cn").put("link", "https://weibo.com/r/23132");
    TemplateRenderUtils.render(Vertx.vertx(), data, "templates/verify_email.hbs", response -> {
      if (response.failed()) {
        System.out.println("failed to render template. cause:" + response.cause().getMessage());
      } else {
        testSucceed = response.result().length() > 0;
        System.out.println(response.result());
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSucceed);
  }
}
