package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.modules.ClassMetaData;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;

public class MetaDataHandler extends CommonHandler {
  public MetaDataHandler(Vertx vertx, RoutingContext routingContext) {
    super(vertx, routingContext);
  }

  public void testSchema(String clazz, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    sendSchemaOperation(clazz, RequestParse.OP_TEST_SCHEMA, data, handler);
  }

  public void addSchemaIfAbsent(String clazz, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
    sendSchemaOperation(clazz, RequestParse.OP_ADD_SCHEMA, data, handler);
  }

  public void findSchema(String clazz, Handler<AsyncResult<JsonObject>> handler) {
    sendSchemaOperation(clazz, RequestParse.OP_FIND_SCHEMA, null, handler);
  }

  public void createClass(String clazz, String classType, JsonObject aclTemplate, Handler<AsyncResult<JsonObject>> handler) {
    ClassMetaData metaData = new ClassMetaData(clazz);
    metaData.setACLTemplate(aclTemplate);
    metaData.setClassType(classType);
    createClass(metaData, response -> {
      if (response.succeeded()) {
        Configure.getInstance().getClassMetaDataCache().putIfAbsent(clazz, response.result());
      }
      handler.handle(response);
    });
  }

  public void dropClass(String clazz, Handler<AsyncResult<Void>> handler) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(handler);
    sendDataOperationWithOption(clazz, null, RequestParse.OP_DROP_CLASS, null, null,
            true, null, response -> {
              if (response.succeeded()) {
                classMetaCache.remove(clazz);
              }
              handler.handle(response.map(json -> null));
            });

  }
}
