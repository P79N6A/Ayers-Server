package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.StringUtils;
import cn.leancloud.platform.modules.ACL;
import io.vertx.core.AbstractVerticle;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

  protected String parseRequestObjectId(RoutingContext context) {
    return context.request().getParam("objectId");
  }

  protected String parseRequestClassname(RoutingContext context) {
    return context.request().getParam("clazz");
  }

  protected JsonObject parseRequestBody(RoutingContext context) {
    HttpMethod httpMethod = context.request().method();
    JsonObject body = null;
    if (HttpMethod.GET.equals(httpMethod)) {
      Map<String, String> filteredEntries = context.request().params().entries()
              .stream().parallel()
              .filter(entry -> !"clazz".equalsIgnoreCase(entry.getKey()) && !"objectId".equalsIgnoreCase(entry.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      body = JsonObject.mapFrom(filteredEntries);
    } else if (HttpMethod.PUT.equals(httpMethod) || HttpMethod.POST.equals(httpMethod)){
      body = context.getBodyAsJson();
    } else {
      String bodyString = context.getBodyAsString();
      if (StringUtils.isEmpty(bodyString)) {
        body = new JsonObject();
      } else {
        body = new JsonObject(bodyString);
      }
    }
    return body;
  }

  protected JsonObject getUserDefaultACL() {
    return ACL.publicRWInstance().toJson();
  }

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
