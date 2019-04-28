package cn.leancloud.platform.common;

import io.vertx.core.Vertx;

public class SMSServiceClient extends CommonWebClient {
  public SMSServiceClient(Vertx vertx) {
    super(vertx, "SMSService");
  }
}
