package cn.leancloud.platform.integrate;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import junit.framework.TestCase;

import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;

public class WebClientTests extends TestCase {
  protected CountDownLatch latch = null;
  protected Boolean testSuccessed = false;
  protected WebClient webClient = null;
  protected static final String HOST = "localhost";
  protected static final int PORT = 8080;


  public WebClientTests() {
    webClient = WebClient.create(Vertx.vertx());
  }

  @Override
  protected void setUp() throws Exception {
    latch = new CountDownLatch(1);
    testSuccessed = false;
  }

  @Override
  protected void tearDown() throws Exception {
  }

  private void fillHeaders(HttpRequest httpRequest) {
    httpRequest.putHeader(RequestParse.HEADER_LC_APPID, "testAppId");
    httpRequest.putHeader(RequestParse.HEADER_LC_APPKEY, "testAppKey");
    httpRequest.putHeader(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON);
    httpRequest.putHeader("Accept", RequestParse.CONTENT_TYPE_JSON);
  }
  protected void get(String path, String queryParam, Handler<AsyncResult<JsonObject>> handler) {
    String fullPath = path;
    if (StringUtils.notEmpty(queryParam)) {
      fullPath += "?" + URLEncoder.encode(queryParam);
    }
    HttpRequest<Buffer> request = this.webClient.get(PORT, HOST, fullPath);
    fillHeaders(request);
    request.send(response -> handler.handle(response.map(response.result().bodyAsJsonObject())));
  }

  protected void post(String path, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    HttpRequest<Buffer> request = this.webClient.post(PORT, HOST, path);
    fillHeaders(request);
    request.sendJsonObject(data, response -> handler.handle(response.map(response.result().bodyAsJsonObject())));
  }

  protected void postWithResultArray(String path, JsonObject data, Handler<AsyncResult<JsonArray>> handler) {
    HttpRequest<Buffer> request = this.webClient.post(PORT, HOST, path);
    fillHeaders(request);
    request.sendJsonObject(data, response -> handler.handle(response.map(response.result().bodyAsJsonArray())));
  }

  protected void put(String path, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    HttpRequest<Buffer> request = this.webClient.put(PORT, HOST, path);
    fillHeaders(request);
    request.sendJsonObject(data, response -> handler.handle(response.map(response.result().bodyAsJsonObject())));
  }

  protected void delete(String path, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    HttpRequest<Buffer> request = this.webClient.delete(PORT, HOST, path);
    fillHeaders(request);
    request.sendJsonObject(data, response -> handler.handle(response.map(response.result().bodyAsJsonObject())));
  }
}
