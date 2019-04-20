package cn.leancloud.platform.persistence;

import cn.leancloud.platform.modules.Schema;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface DataStore {
  DataStore insertWithOptions(String clazz, JsonObject obj, JsonObject options, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore updateWithOptions(String clazz, JsonObject query, JsonObject fields, JsonObject options,
                              Handler<AsyncResult<Long>> resultHandler);
  DataStore findWithOptions(String collection, JsonObject query, JsonObject findOptions, Handler<AsyncResult<List<JsonObject>>> resultHandler);
  DataStore findOneAndUpdate(String clazz, JsonObject query, JsonObject update, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore removeWithOptions(String clazz, JsonObject query, JsonObject options, Handler<AsyncResult<Long>> resultHandler);
  DataStore count(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler);

  DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonArray>> resultHandler);

  DataStore dropClass(String clazz, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore createClass(String clazz, Handler<AsyncResult<JsonObject>> resultHandler);

  DataStore dropIndex(String clazz, String indexName, Handler<AsyncResult<Void>> resultHandler);
  DataStore createIndexWithOptions(String clazz, String indexName, JsonObject index, JsonObject options,
                                   Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore listIndexes(String clazz, Handler<AsyncResult<JsonArray>> resultHandler);

  DataStore findSchema(String clazz, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore upsertSchema(String clazz, Schema schema, Handler<AsyncResult<JsonObject>> resultHandler);
  void close();
}
