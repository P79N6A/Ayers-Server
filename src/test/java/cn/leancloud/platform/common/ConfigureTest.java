package cn.leancloud.platform.common;

import io.vertx.core.Vertx;
import junit.framework.TestCase;

public class ConfigureTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testConfigureInit() {
    Vertx vertx = Vertx.vertx();
    Configure configure = Configure.getInstance();
    configure.initialize(vertx, res -> {});
    assertTrue(null != configure);
    assertTrue(configure.listenPort() == 8080);
  }
}
