package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.SMSServiceClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmsCodeHandler extends CommonHandler {
  private static final Logger logger = LoggerFactory.getLogger(SmsCodeHandler.class);

  private static final String LC_REQUEST_SMSCODE_PATH = "/1.1/requestSmsCode";
  private static final String LC_REQUEST_VERIFYSMSCODE_FORMAT = "/1.1/verifySmsCode/%s";

  private String apiHost;
  private int apiPort;

  public SmsCodeHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
    apiHost = Configure.getInstance().getUluruAPIHost();
    apiPort = Configure.getInstance().getUluruAPIPort();
  }

  public void requestSmsCode(Handler<AsyncResult<JsonObject>> handler) {
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    JsonObject body = routingContext.getBodyAsJson();
    JsonObject headerJson = copyRequestHeaders(routingContext);

    logger.debug("process requestSmsCode request. body:" + body);
    client.post(apiHost, apiPort, LC_REQUEST_SMSCODE_PATH, headerJson, body, handler);
  }

  public void verifySmsCode(JsonObject body, String smsCode, Handler<AsyncResult<JsonObject>> handler) {
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    JsonObject headerJson = copyRequestHeaders(routingContext);
    String requestPath = String.format(LC_REQUEST_VERIFYSMSCODE_FORMAT, smsCode);
    client.post(apiHost, apiPort, requestPath, headerJson, body, handler);
  }

  public void verifySmsCode(Handler<AsyncResult<JsonObject>> handler) {
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    JsonObject body = routingContext.getBodyAsJson();
    JsonObject headerJson = copyRequestHeaders(routingContext);

    logger.debug("process verifySmsCode request. body:" + body);
    client.post(apiHost, apiPort, routingContext.request().path(), headerJson, body, handler);
  }
}
