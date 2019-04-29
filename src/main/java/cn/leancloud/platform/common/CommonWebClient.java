package cn.leancloud.platform.common;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonWebClient {
  private static final Logger logger = LoggerFactory.getLogger(CommonWebClient.class);
  private CircuitBreaker circuitBreaker = null;
  private WebClient webClient = null;
  public CommonWebClient(Vertx vertx, String name) {
    this(vertx, name, new WebClientOptions().setFollowRedirects(true).setSsl(true).setKeepAlive(true).setConnectTimeout(6000).setUserAgent("Ayers Server"));
  }

  public CommonWebClient(Vertx vertx, String name, WebClientOptions options) {
    this(vertx, name, options, new CircuitBreakerOptions().setMaxFailures(100).setMaxRetries(0).setTimeout(1000).setResetTimeout(60000));
  }

  public CommonWebClient(Vertx vertx, String name, WebClientOptions options, CircuitBreakerOptions circuitBreakerOptions) {
    webClient = WebClient.create(vertx, options);
    circuitBreaker = CircuitBreaker.create("circuit-breaker-" + name, vertx, circuitBreakerOptions);
  }

  public void get(String host, int port, String path, MultiMap headers, String parameter, Handler<AsyncResult<JsonObject>> handler) {
    if (null != headers) {
      headers.entries().stream().forEach(entry -> webClient.head(entry.getKey(), entry.getValue()));
    }
    circuitBreaker.<JsonObject>execute(future -> {
      webClient.get(port, host, path).send(ar -> {
        if (ar.failed()) {
          future.fail(ar.cause());
        } else {
          future.complete(ar.result().bodyAsJsonObject());
        }
      });
    }).setHandler(handler);
  }

  public void post(String host, int port, String path, JsonObject headers, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
    if (null != headers) {
      headers.stream().forEach(entry -> {
        logger.debug("add http header. key:" + entry.getKey() + ", value: " + entry.getValue());
        webClient.head(entry.getKey(), (String) entry.getValue());
      });
    }
    webClient.post(host, path).ssl(true).followRedirects(true).sendJsonObject(body, response -> {
      if (response.failed()) {
        logger.warn("response is failed. cause: " + response.cause());
      } else {
        if (HttpStatus.SC_OK == response.result().statusCode()) {
          JsonObject result = response.result().bodyAsJsonObject();
          logger.debug("response ok. result: " + result);
        } else {
          System.out.println(response.result().bodyAsString("UTF-8"));
        }
      }
      handler.handle(response.map(v -> HttpStatus.SC_OK == v.statusCode()?v.bodyAsJsonObject() : new JsonObject()));
    });

//    circuitBreaker.<JsonObject>execute(future -> {
//      logger.debug("send post request to " + host + path + ", para: " + body);
//      webClient.post(host, path).sendJsonObject(body, response -> {
//        if (response.failed()) {
//          logger.warn("response is failed. cause: " + response.cause());
//          future.fail(response.cause());
//        } else {
//          if (HttpStatus.SC_OK == response.result().statusCode()) {
//            JsonObject result = response.result().bodyAsJsonObject();
//            logger.debug("response ok. result: " + result);
//            future.complete(result);
//          } else {
//            System.out.println(response.result().bodyAsString("UTF-8"));
//            future.fail(response.result().bodyAsString());
//          }
//        }
//      });
//    }).setHandler(handler);
  }
}
