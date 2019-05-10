package cn.leancloud.platform.common;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;

public class SMSServiceClientTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
//    System.setProperty("sun.net.spi.nameservice.nameservers", "8.8.8.8");
//    System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");

  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testErrorJsonResponse() throws Exception {
    Vertx vertx = Vertx.vertx();
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    JsonObject headers = new JsonObject().put("X-LC-Id", "30pAYbNNuWoTfcLE85Q7wx7H-gzGzoHsz")
            .put("X-LC-Key","1zoU0YOSqbCuMWPMc0LVe9lR").put("Content-Type", "application/json");
    JsonObject body = new JsonObject().put("mobilePhoneNumber","18600345198");
    CountDownLatch latch = new CountDownLatch(1);
    String defaultHost = "api.leancloud.cn";
    //String defaultHost = "30paybnn.api.lncld.net";
    client.post(defaultHost, 443, "/1.1/requestSmsCode", headers, body, res -> {
      if (res.failed()) {
        System.out.println(res.cause());
      } else {
        System.out.println(res.result());
      }
      latch.countDown();
    });
    latch.await();
  }
}
