package cn.leancloud.platform.common;

import io.vertx.core.Vertx;

public class OperationHookProxy extends CommonWebClient {
  public OperationHookProxy(Vertx vertx) {
    super(vertx, "HookFunction");
  }
}
