package cn.leancloud.platform.ayers;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AbstractVerticle;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommonVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(CommonVerticle.class);

  protected static final String REQUEST_PARAM_OBJECTID = "objectId";
  protected static final String REQUEST_PARAM_CLAZZ = "clazz";
  protected static final String REQUEST_PARAM_INDEXNAME = "indexName";

  public static final String INTERNAL_MSG_ATTR_CLASS = "class";
  public static final String INTERNAL_MSG_ATTR_OBJECT_ID = "objectId";
  public static final String INTERNAL_MSG_ATTR_UPDATE_PARAM = "param";
  public static final String INTERNAL_MSG_ATTR_QUERY = "query";
  public static final String INTERNAL_MSG_HEADER_OP = "operation";
  public static final String INTERNAL_MSG_ATTR_RETURNNEWDOC = "returnNewDoc";

  protected Supplier<JsonObject> dummyJsonGenerator = new Supplier<JsonObject>() {
    @Override
    public JsonObject get() {
      return new JsonObject();
    }
  };

  protected String parseRequestObjectId(RoutingContext context) {
    return context.request().getParam(REQUEST_PARAM_OBJECTID);
  }

  protected String parseRequestClassname(RoutingContext context) {
    return context.request().getParam(REQUEST_PARAM_CLAZZ);
  }

  protected String parseRequestIndexName(RoutingContext context) {
    return context.request().getParam(REQUEST_PARAM_INDEXNAME);
  }

  protected JsonObject parseRequestBody(RoutingContext context) {
    HttpMethod httpMethod = context.request().method();
    JsonObject body;
    if (HttpMethod.GET.equals(httpMethod)) {
      Map<String, String> filteredEntries = context.request().params().entries()
              .stream().parallel()
              .filter(entry ->
                      !REQUEST_PARAM_CLAZZ.equalsIgnoreCase(entry.getKey()) && !REQUEST_PARAM_OBJECTID.equalsIgnoreCase(entry.getKey()))
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

  protected void response(RoutingContext context, int status, JsonObject header, JsonObject result) {
    response(context, status, header, Json.encodePrettily(result));
  }

  protected void response(RoutingContext context, int status, JsonObject header, String result) {
    String origin = context.request().getHeader("Origin");
    HttpServerResponse response = context.response().setStatusCode(status)
            .putHeader(RequestParse.HEADER_CONTENT_TYPE, RequestParse.CONTENT_TYPE_JSON)
            .putHeader("Access-Control-Allow-Origin", StringUtils.isEmpty(origin)? "*" : origin);
    if (null != header) {
      header.stream().sequential().forEach(entry -> response.putHeader(entry.getKey(), (String) entry.getValue()));
    }
    response.end(result);
  }

  protected void ok(RoutingContext context, JsonObject result) {
    response(context, HttpStatus.SC_OK, null, result);
  }

  protected void ok(RoutingContext context, String result) {
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
