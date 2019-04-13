package cn.leancloud.platform.ayers;

import io.vertx.core.AbstractVerticle;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class CommonVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(CommonVerticle.class);
  protected static final String HEADER_CONTENT_TYPE = "Content-Type";
  protected static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

  protected Supplier<JsonObject> dummyJsonGenerator = new Supplier<JsonObject>() {
    @Override
    public JsonObject get() {
      return new JsonObject();
    }
  };

  protected void response(RoutingContext context, int status, JsonObject header, JsonObject result) {
    HttpServerResponse response = context.response();
    response.setStatusCode(status).putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
    if (null != header) {
      header.stream().forEach(entry -> response.putHeader(entry.getKey(), (String) entry.getValue()));
    }
    response.end(Json.encodePrettily(result));
  }

  protected void ok(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_OK, null, result);
  }

  protected void created(RoutingContext context, JsonObject header, JsonObject result) {
    response(context, HttpStatus.SC_CREATED, header, result);
  }
  protected void forbidden(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_FORBIDDEN, null, result);
  }
  protected void badRequest(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_BAD_REQUEST, null, result);
  }
  protected void notFound(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_NOT_FOUND, null, result);
  }
  protected void internalServerError(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_INTERNAL_SERVER_ERROR, null, result);
  }
  protected void tooManyRequests(RoutingContext context, JsonObject result) {
    response(context, 429, null, result);
  }
  protected void paymentRequired(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_PAYMENT_REQUIRED, null, result);
  }
  protected void unauthorized(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_UNAUTHORIZED, null, result);
  }
  protected void status(RoutingContext context, int code, JsonObject result) {
    response(context, code, null, result);
  }
}
