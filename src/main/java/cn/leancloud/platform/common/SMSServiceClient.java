package cn.leancloud.platform.common;

import io.vertx.core.Vertx;

public class SMSServiceClient extends CommonWebClient {
  private static SMSServiceClient instance = null;
  private SMSServiceClient(Vertx vertx) {
    super(vertx, "SMSService");
  }

  public static SMSServiceClient getClient(Vertx vertx) {
    if (null == instance) {
      instance = new SMSServiceClient(vertx);
    }
    return instance;
  }
}
