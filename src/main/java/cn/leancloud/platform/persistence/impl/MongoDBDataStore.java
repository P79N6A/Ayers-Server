package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.BulkOperation;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;

public class MongoDBDataStore implements DataStore{
  private MongoClient mongoClient;

  public MongoDBDataStore(MongoClient client) {
    this.mongoClient = client;
  }

  public DataStore insertWithOptions(String clazz, JsonObject obj, JsonObject options, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore updateWithOptions(String clazz, JsonObject query, JsonObject fields, JsonObject options,
                                     Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }
  public DataStore findWithOptions(String collection, JsonObject query, JsonObject findOptions,
                                   Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    return this;
  }
  public DataStore findOneAndUpdate(String clazz, JsonObject query, JsonObject update, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore removeWithOptions(String clazz, JsonObject query, JsonObject options, Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }
  public DataStore count(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }

  public DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonArray>> resultHandler) {
    return this;
  }

  public DataStore dropClass(String clazz, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore createClass(String clazz, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }

  public DataStore dropIndex(String clazz, String indexName, Handler<AsyncResult<Void>> resultHandler) {
    return this;
  }
  public DataStore createIndexWithOptions(String clazz, String indexName, JsonObject index, JsonObject options,
                                   Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore listIndexes(String clazz, Handler<AsyncResult<JsonArray>> resultHandler) {
    return this;
  }

  public DataStore findSchema(String clazz, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore upsertSchema(String clazz, Schema schema, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public void close() {
    this.mongoClient.close();
  }
}
