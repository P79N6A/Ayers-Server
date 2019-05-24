package cn.leancloud.platform.utils;

import cn.leancloud.platform.codec.SymmetricEncryptor;
import cn.leancloud.platform.common.Configure;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

import java.net.URLEncoder;

public class TemplateRenderUtils {
  public static final String VAR_USERNAME = "username";
  public static final String VAR_LINK = "link";
  public static final String VAR_EMAIL = "email";
  public static final String VAR_APPNAME = "appname";

  public static String generateVerifyUrl(String path, JsonObject data) {
    if (null == data) {
      return "";
    }
    if (null == path) {
      path = "";
    }
    String serverBaseHost = Configure.getInstance().getBaseHost();
    String secretKey = Configure.getInstance().secretKey();
    try {
      String part = SymmetricEncryptor.encodeWithDES(data.toString(), secretKey);
      String urlEncodePart = URLEncoder.encode(part, "utf-8");
      return serverBaseHost + path + urlEncodePart;
    } catch (Exception ex) {
      return "";
    }
  }
  public static void render(Vertx vertx, JsonObject data, String templateFile, Handler<AsyncResult<String>> handler) {
    HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);
    engine.render(data, templateFile, response -> {
      handler.handle(response.map(buffer -> buffer.toString()));
    });
  }
}
