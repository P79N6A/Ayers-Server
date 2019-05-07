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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class WebClientTests extends TestCase {
  protected CountDownLatch latch = null;
  protected Boolean testSuccessed = false;
  protected WebClient webClient = null;
  protected static final String HOST = "localhost";
  protected static final int PORT = 8080;

  private Map<String, String> otherHeaders = new HashMap<>();

  public WebClientTests() {
    webClient = WebClient.create(Vertx.vertx());
  }

  @Override
  protected void setUp() throws Exception {
    latch = new CountDownLatch(1);
    testSuccessed = false;
    otherHeaders.clear();
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testDummy() throws Exception {}

  protected void addHttpHeader(String name, String value) {
    otherHeaders.put(name, value);
  }

  private void fillHeaders(HttpRequest httpRequest) {
    httpRequest.putHeader(RequestParse.HEADER_LC_APPID, "testAppId");
    httpRequest.putHeader(RequestParse.HEADER_LC_APPKEY, "testAppKey");
    httpRequest.putHeader(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON);
    httpRequest.putHeader("Accept", RequestParse.CONTENT_TYPE_JSON);
    for (Map.Entry<String, String> entry : otherHeaders.entrySet()) {
      httpRequest.putHeader(entry.getKey(), entry.getValue());
    }
  }
  protected void get(String path, JsonObject queryParam, Handler<AsyncResult<JsonObject>> handler) {
    String fullPath = path;
    HttpRequest<Buffer> request = this.webClient.get(PORT, HOST, fullPath);
    fillHeaders(request);
    if (null != queryParam) {
      queryParam.stream().forEach(kv -> request.addQueryParam(kv.getKey(), (String) kv.getValue()));
    }
    request.send(response -> {
      if (response.failed()) {
        handler.handle(new AsyncResult<JsonObject>() {
          @Override
          public JsonObject result() {
            return null;
          }

          @Override
          public Throwable cause() {
            return response.cause();
          }

          @Override
          public boolean succeeded() {
            return false;
          }

          @Override
          public boolean failed() {
            return true;
          }
        });
      } else {
        HttpResponse<Buffer> res = response.result();
        int statusCode = res.statusCode();
        String resString = res.bodyAsString();
        System.out.println("statusCode:" + statusCode + ", response: " + resString);
        handler.handle(new AsyncResult<JsonObject>() {
          @Override
          public JsonObject result() {
            try {
              return new JsonObject(resString);
            } catch (Exception ex) {
              return new JsonObject();
            }
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
    });
  }

  protected void post(String path, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    HttpRequest<Buffer> request = this.webClient.post(PORT, HOST, path);
    fillHeaders(request);
    request.sendJson(data, response -> handler.handle(response.map(response.result().bodyAsJsonObject())));
  }

  protected void postWithResultArray(String path, JsonObject data, Handler<AsyncResult<JsonArray>> handler) {
    HttpRequest<Buffer> request = this.webClient.post(PORT, HOST, path);
    fillHeaders(request);
    request.sendJson(data, response -> handler.handle(response.map(response.result().bodyAsJsonArray())));
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
