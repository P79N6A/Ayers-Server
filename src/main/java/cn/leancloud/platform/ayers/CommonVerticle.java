package cn.leancloud.platform.ayers;

import io.vertx.core.AbstractVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class CommonVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(CommonVerticle.class);

  protected Supplier<JsonObject> dummyJsonGenerator = new Supplier<JsonObject>() {
    @Override
    public JsonObject get() {
      return new JsonObject();
    }
  };
}
