package cn.leancloud.platform.common;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.function.Function;

public class CommonWebClient {
  private static final Logger logger = LoggerFactory.getLogger(CommonWebClient.class);
  protected CircuitBreaker circuitBreaker = null;
  protected WebClient webClient = null;
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

//  public <T> void setFallback(Function<Throwable, T> fallback) {
//    this.circuitBreaker.fallback(fallback);
//  }

  public void getWithFallback(String host, int port, String path, MultiMap headers, String parameter,
                              Handler<AsyncResult<JsonObject>> handler, Function<Throwable, JsonObject> fallback) {
    boolean useSSL = (443 == port);
    HttpRequest httpRequest = webClient.get(port, host, path).ssl(useSSL).followRedirects(true);
    if (null != headers) {
      headers.entries().stream().forEach(entry -> httpRequest.putHeader(entry.getKey(), entry.getValue()));
    }
    circuitBreaker.executeWithFallback(future -> {
      Buffer buffer = Buffer.buffer();
      if (StringUtils.notEmpty(parameter)) {
        buffer.appendString(parameter);
      }
      httpRequest.sendBuffer(buffer, new Handler<AsyncResult<HttpResponse>>() {
        @Override
        public void handle(AsyncResult<HttpResponse> event) {
          if (event.failed()) {
            logger.warn(path + " request failed. cause: " + event.cause());
            future.fail(event.cause());
          } else {
            if (HttpStatus.SC_OK != event.result().statusCode()) {
              future.fail(event.result().bodyAsString());
            } else {
              logger.debug(path + " response: " + event.result().bodyAsJsonObject());
              future.complete(event.result().bodyAsJsonObject());
            }
          }
        }
      });
    }, fallback).setHandler(handler);
  }

  public void get(String host, int port, String path, MultiMap headers, String parameter, Handler<AsyncResult<JsonObject>> handler) {
    boolean useSSL = (443 == port);
    HttpRequest httpRequest = webClient.get(port, host, path).ssl(useSSL).followRedirects(true);
    if (null != headers) {
      headers.entries().stream().forEach(entry -> httpRequest.putHeader(entry.getKey(), entry.getValue()));
    }
    circuitBreaker.<JsonObject>execute(future -> {
      Buffer buffer = Buffer.buffer();
      if (StringUtils.notEmpty(parameter)) {
        buffer.appendString(parameter);
      }
      httpRequest.sendBuffer(buffer, new Handler<AsyncResult<HttpResponse>>() {
                         @Override
                         public void handle(AsyncResult<HttpResponse> event) {
                           if (event.failed()) {
                             logger.warn(path + " request failed. cause: " + event.cause());
                             future.fail(event.cause());
                           } else {
                             if (HttpStatus.SC_OK != event.result().statusCode()) {
                               future.fail(event.result().bodyAsString());
                             } else {
                               logger.debug(path + " response: " + event.result().bodyAsJsonObject());
                               future.complete(event.result().bodyAsJsonObject());
                             }
                           }
                         }
                       });
    }).setHandler(handler);
  }

  public void postWithFallback(String host, int port, String path, JsonObject headers, JsonObject body,
                               Handler<AsyncResult<JsonObject>> handler, Function<Throwable, JsonObject> fallback) {
    boolean useSSL = (443 == port);
    HttpRequest httpRequest = webClient.post(port, host, path).ssl(useSSL).followRedirects(true);
    if (null != headers) {
      headers.stream().forEach(entry -> {
        logger.debug("add http header. " + entry.getKey() + ", " + entry.getValue());
        httpRequest.putHeader(entry.getKey(), (String) entry.getValue());
      });
    }
    circuitBreaker.executeWithFallback(future -> {
      logger.debug("send post request to " + host + path + ", para: " + body);
      httpRequest.sendJsonObject(body, new Handler<AsyncResult<HttpResponse>>() {
        @Override
        public void handle(AsyncResult<HttpResponse> event) {
          if (event.failed()) {
            if (event.cause() != null && event.cause() instanceof ConnectException) {
              logger.warn(path + " request failed. cause: " + event.cause());
              future.complete(fallback.apply(event.cause()));
            } else {
              logger.debug(path + " request failed. cause: " + event.cause());
              future.fail(event.cause());
            }
          } else {
            if (HttpStatus.SC_OK != event.result().statusCode()) {
              future.fail(event.result().bodyAsString());
            } else {
              logger.debug(path + " response: " + event.result().bodyAsJsonObject());
              future.complete(event.result().bodyAsJsonObject());
            }
          }
        }});
      }, fallback).setHandler(handler);
  }

  public void post(String host, int port, String path, JsonObject headers, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
    boolean useSSL = (443 == port);
    HttpRequest httpRequest = webClient.post(port, host, path).ssl(useSSL).followRedirects(true);
    if (null != headers) {
      headers.stream().forEach(entry -> {
        logger.debug("add http header. " + entry.getKey() + ", " + entry.getValue());
        httpRequest.putHeader(entry.getKey(), (String) entry.getValue());
      });
    }
    circuitBreaker.<JsonObject>execute(future -> {
      logger.debug("send post request to " + host + path + ", para: " + body);
      httpRequest.sendJsonObject(body, new Handler<AsyncResult<HttpResponse>>() {
        @Override
        public void handle(AsyncResult<HttpResponse> event) {
          if (event.failed()) {
            logger.warn(path + " request failed. cause: " + event.cause());
            future.fail(event.cause());
          } else {
            if (HttpStatus.SC_OK != event.result().statusCode()) {
              future.fail(event.result().bodyAsString());
            } else {
              logger.debug(path + " response: " + event.result().bodyAsJsonObject());
              future.complete(event.result().bodyAsJsonObject());
            }
          }
      }});
    }).setHandler(handler);
  }
}
