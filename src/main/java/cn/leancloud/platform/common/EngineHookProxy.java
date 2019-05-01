package cn.leancloud.platform.common;

import cn.leancloud.platform.engine.EngineMetaStore;
import cn.leancloud.platform.engine.HookType;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineHookProxy extends CommonWebClient {
  private static final Logger logger = LoggerFactory.getLogger(EngineHookProxy.class);
  private static EngineHookProxy instance = null;

  private EngineHookProxy(Vertx vertx) {
    super(vertx, "HookFunction");
  }

  public static EngineHookProxy getInstance(Vertx vertx) {
    if (null == instance) {
      instance = new EngineHookProxy(vertx);
    }
    return instance;
  }

  private AsyncResult<JsonObject> wrapAsyncResult(JsonObject param) {
    return new AsyncResult<JsonObject>() {
      @Override
      public JsonObject result() {
        return param;
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
    };
  }

  public void call(String clazz, HookType type, JsonObject param, JsonObject headers, RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
    logger.debug("EngineHookProxy#call");
    EngineMetaStore engineMetaStore = EngineMetaStore.getInstance();
    String funcPath = engineMetaStore.getHookFunctionPath(clazz, type);
    if (StringUtils.isEmpty(funcPath)) {
      logger.debug("not found hook function for class:" + clazz + ", type:" + type.getName());
      handler.handle(wrapAsyncResult(param));
    } else {
      logger.debug("try to call hook function for class:" + clazz + ", type:" + type.getName() + "，param:" + param);
      String leanengineHost = engineMetaStore.getEngineHost();
      int leanenginePort = engineMetaStore.getEnginePort();
      postWithFallback(leanengineHost, leanenginePort, funcPath, headers, param,
              response -> handler.handle(response),
              throwable -> {
                logger.warn("failed to call lean engine. cause: " + throwable);
                return param;
      });
    }
  }
}
