package cn.leancloud.platform.common;

import io.vertx.core.Vertx;

public class MailServiceClient extends CommonWebClient {
  public MailServiceClient(Vertx vertx) {
    super(vertx, "MailService");
  }
}
