package cn.leancloud.platform.common;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class CommonWebClient {
  private CircuitBreaker circuitBreaker = null;
  private WebClient webClient = null;
  public CommonWebClient(Vertx vertx, String name) {
    this(vertx, name, new WebClientOptions().setConnectTimeout(5000).setUserAgent("Ayers Server"),
            new CircuitBreakerOptions().setMaxFailures(10).setMaxRetries(100).setTimeout(2000).setResetTimeout(60000));
  }

  public CommonWebClient(Vertx vertx, String name, WebClientOptions options) {
    this(vertx, name, options, new CircuitBreakerOptions().setMaxFailures(10).setMaxRetries(100).setTimeout(2000).setResetTimeout(60000));
  }

  public CommonWebClient(Vertx vertx, String name, WebClientOptions options, CircuitBreakerOptions circuitBreakerOptions) {
    webClient = WebClient.create(vertx, options);
    circuitBreaker = CircuitBreaker.create("circuit-breaker-" + name, vertx, circuitBreakerOptions);
  }
}
