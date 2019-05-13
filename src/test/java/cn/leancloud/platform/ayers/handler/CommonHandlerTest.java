package cn.leancloud.platform.ayers.handler;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.CountDownLatch;

public class CommonHandlerTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testParseBatchPath() throws Exception {
    String[] paths = new String[]{"1.1/classes/Post//", "", "/1.1/user/fhiea", "/1.1/class/Post/hfiea", "/classes/Post/"};
    boolean[] expected = new boolean[]{false, false, false, false, false};
    for (int i = 0; i < paths.length; i++) {
      Pair<String, String> p = CommonHandler.parseClazzAndObjectId(paths[i]);
      System.out.println("testSchema " + paths[i] + "||| pair: " + p.getLeft() + "," + p.getRight());
      assertTrue(expected[i] == p.getLeft().length() > 0);
    }
  }

  public void testRegexMatch() throws Exception {
    String[] paths = new String[]{"/1.1/classes/Post/Abc", "/1.1/classes", "/1.1/users/obj?", "/1.1/users",
            "/1.1/installations/fhei", "/1.1/installations", "/1.1/installations/", "/1.1/files",
            "/1.1/files/feifahie", "/1.1/files/fhaiefhe?afhie", "/1.1/roles", "/1.1/roles/hfiaeaihfhh?",
            "/1.1/class/fehia", "1.1/classes/testSchema", "/1.1/testSchema", "/1.1/classesfe/Post"};
    boolean[] expecteds = new boolean[]{true, false, true, true,
            true, true, true, true,
            true, true, true, true,
            false, false, false, false};
    for (int i = 0; i < paths.length; i++) {
      Pair<String, String> p = CommonHandler.parseClazzAndObjectId(paths[i]);
      System.out.println("testSchema " + paths[i] + "||| pair: " + p.getLeft() + "," + p.getRight());
      assertTrue(expecteds[i] == p.getLeft().length() > 0);
    }
  }

  public void testTemplateRendering() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(Vertx.vertx());
    JsonObject data = new JsonObject().put("appname", "LeanCloud").put("username", "sama")
            .put("email", "app.leanapp.cn").put("link", "https://weibo.com/r/23132");
    engine.render(data, "templates/verify_email.hbs", response -> {
      if (response.failed()) {
        System.out.println("failed to render template. cause:" + response.cause().getMessage());
      } else {
        Buffer buffer = response.result();
        System.out.println(buffer.toString());
      }
      latch.countDown();
    });
    latch.await();
  }
}
