package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
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
  private static final String LC_API_HOST = "api.leancloud.cn";
  private static final int LC_API_PORT = 443;
//  private static final String LC_REQUEST_SMSCODE_PATH = "/1.1/requestSmsCode";
  private static final String LC_REQUEST_VERIFYSMSCODE_FORMAT = "/1.1/verifySmsCode/%s";
  public SmsCodeHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public void requestSmsCode(Handler<AsyncResult<JsonObject>> handler) {
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(routingContext);
//    String appId = headers.getAppId();  //とりあえず、固定てきなホストを使用
//    String host = String.format(LC_API_HOST_FORMAT, appId.substring(0, 8).toLowerCase());
    JsonObject body = routingContext.getBodyAsJson();
    JsonObject headerJson = headers.toHeaders()
            .put(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .put("Accept", RequestParse.CONTENT_TYPE_JSON);

    logger.debug("process requestSmsCode request. body:" + body);
    client.post(LC_API_HOST, LC_API_PORT, routingContext.request().path(), headerJson, body, handler);
  }

  public void verifySmsCode(JsonObject body, String smsCode, Handler<AsyncResult<JsonObject>> handler) {
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(routingContext);
    JsonObject headerJson = headers.toHeaders()
            .put(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .put("Accept", RequestParse.CONTENT_TYPE_JSON);
    String requestPath = String.format(LC_REQUEST_VERIFYSMSCODE_FORMAT, smsCode);
    client.post(LC_API_HOST, LC_API_PORT, requestPath, headerJson, body, handler);
  }

  public void verifySmsCode(Handler<AsyncResult<JsonObject>> handler) {
    SMSServiceClient client = SMSServiceClient.getClient(vertx);
    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(routingContext);
//    String appId = headers.getAppId();
//    String host = String.format(LC_API_HOST_FORMAT, appId.substring(0, 8).toLowerCase());
    JsonObject body = routingContext.getBodyAsJson();
    JsonObject headerJson = headers.toHeaders()
            .put(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .put("Accept", RequestParse.CONTENT_TYPE_JSON);

    logger.debug("process verifySmsCode request. body:" + body);
    client.post(LC_API_HOST, LC_API_PORT, routingContext.request().path(), headerJson, body, handler);
  }
}
