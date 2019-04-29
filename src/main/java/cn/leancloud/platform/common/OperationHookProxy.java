package cn.leancloud.platform.common;

import cn.leancloud.platform.ayers.RequestParse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class OperationHookProxy extends CommonWebClient {
  private static OperationHookProxy instance = null;

  private OperationHookProxy(Vertx vertx) {
    super(vertx, "HookFunction");
  }

  public static OperationHookProxy getInstance(Vertx vertx) {
    if (null == instance) {
      instance = new OperationHookProxy(vertx);
    }
    return instance;
  }

  public void beforeCheckThen(String clazz, RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
    // do nothing.
    handler.handle(new AsyncResult<JsonObject>() {
      @Override
      public JsonObject result() {
        return context.getBodyAsJson();
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    });
  }

  public void afterCheckThen(String clazz, JsonObject result, Handler<AsyncResult<JsonObject>> handler) {
    // do nothing.
    handler.handle(new AsyncResult<JsonObject>() {
      @Override
      public JsonObject result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    });
  }
}
