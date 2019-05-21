package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.BulkOperation;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;

import java.util.List;

public class MysqlDataStore implements DataStore {
  private AsyncSQLClient sqlClient;
  public MysqlDataStore(AsyncSQLClient sqlClient) {
    this.sqlClient = sqlClient;
  }

  public DataStore insertWithOptions(String clazz, JsonObject obj, InsertOption options, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore updateWithOptions(String clazz, JsonObject query, JsonObject object, UpdateOption options,
                                     Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }
  public DataStore find(String clazz, JsonObject query, Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    return this;
  }
  public DataStore findWithOptions(String collection, JsonObject query, QueryOption findOptions,
                                   Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    return this;
  }
  public DataStore findOneAndUpdate(String clazz, JsonObject query, JsonObject update, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore findOneAndUpdateWithOptions(String clazz, JsonObject query, JsonObject update, QueryOption queryOption,
                                        UpdateOption updateOption, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore remove(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }
  public DataStore removeWithOptions(String clazz, JsonObject query, JsonObject options, Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }
  public DataStore count(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }

  public DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }

  public DataStore dropClass(String clazz, Handler<AsyncResult<Void>> resultHandler) {
    return this;
  }
  public DataStore createClass(String clazz, Handler<AsyncResult<Void>> resultHandler) {
    return this;
  }

  public DataStore dropIndex(String clazz, String indexName, Handler<AsyncResult<Void>> resultHandler) {
    return this;
  }
  public DataStore createIndexWithOptions(String clazz, JsonObject keys, IndexOption options,
                                          Handler<AsyncResult<Void>> resultHandler) {
    return this;
  }
  public DataStore listIndices(String clazz, Handler<AsyncResult<JsonArray>> resultHandler) {
    return this;
  }

  public DataStore findMetaInfo(String clazz, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore upsertMetaInfo(String clazz, JsonObject metaInfo, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }
  public DataStore listClassMetaInfo(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    return this;
  }
  public DataStore removeMetaInfo(String clazz, Handler<AsyncResult<Long>> resultHandler) {
    return this;
  }

  public void close() {
    this.sqlClient.close();
  }
}
