package cn.leancloud.platform.integrate;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

public class SmsCodeTests extends WebClientTests {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testRequestSMSCode() throws Exception {
    JsonObject param = new JsonObject().put("mobilePhoneNumber", "18600345198");
    post("/1.1/requestSmsCode", param, response -> {
      if (response.failed()) {
        System.out.println("failed to call requestSmsCode. cause:" + response.cause().getMessage());
      } else {
        System.out.println("succeed to call requestSmsCode. result:" + response.result());
        testSuccessed = true;
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }
}
