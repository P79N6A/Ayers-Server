package cn.leancloud.platform.persistence;

import cn.leancloud.platform.modules.Schema;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import javafx.util.converter.DateStringConverter;

import java.util.List;

public interface DataStore {
  class QueryOption {
    private int skip = 0;
    private int limit = -1;
    private JsonObject sort = null;
    private JsonObject fields = null;

    public int getSkip() {
      return skip;
    }

    public QueryOption setSkip(int skip) {
      this.skip = skip;
      return this;
    }

    public int getLimit() {
      return limit;
    }

    public QueryOption setLimit(int limit) {
      this.limit = limit;
      return this;
    }

    public JsonObject getSort() {
      return sort;
    }

    public QueryOption setSort(JsonObject sort) {
      this.sort = sort;
      return this;
    }

    public JsonObject getFields() {
      return fields;
    }

    public QueryOption setFields(JsonObject fields) {
      this.fields = fields;
      return this;
    }

    public String toString() {
      return Json.encode(this);
    }
  }

  class InsertOption {
    private boolean returnNewDocument = false;
    public boolean isReturnNewDocument() {
      return returnNewDocument;
    }

    public InsertOption setReturnNewDocument(boolean returnNewDocument) {
      this.returnNewDocument = returnNewDocument;
      return this;
    }

  }
  class UpdateOption {
    private boolean upsert = false;
    private boolean multi = false;
    private boolean returnNewDocument = false; // uniquely valid on findOneAnd* methods

    public boolean isUpsert() {
      return upsert;
    }

    public UpdateOption setUpsert(boolean upsert) {
      this.upsert = upsert;
      return this;
    }

    public boolean isMulti() {
      return multi;
    }

    public UpdateOption setMulti(boolean multi) {
      this.multi = multi;
      return this;
    }

    public boolean isReturnNewDocument() {
      return returnNewDocument;
    }

    public UpdateOption setReturnNewDocument(boolean returnNewDocument) {
      this.returnNewDocument = returnNewDocument;
      return this;
    }
  }

  class IndexOption {
    private boolean background = false;
    private boolean unique = false;
    private String name = null;
    private boolean sparse = false;

    public boolean isBackground() {
      return background;
    }

    public IndexOption setBackground(boolean background) {
      this.background = background;
      return this;
    }

    public boolean isUnique() {
      return unique;
    }

    public IndexOption setUnique(boolean unique) {
      this.unique = unique;
      return this;
    }

    public String getName() {
      return name;
    }

    public IndexOption setName(String name) {
      this.name = name;
      return this;
    }

    public boolean isSparse() {
      return sparse;
    }

    public IndexOption setSparse(boolean sparse) {
      this.sparse = sparse;
      return this;
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("background", background);
      json.put("unique", unique);
      json.put("sparse", sparse);
      if (name != null) {
        json.put("name", name);
      }
      return json;
    }
  }

  DataStore insertWithOptions(String clazz, JsonObject obj, InsertOption options, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore updateWithOptions(String clazz, JsonObject query, JsonObject object, UpdateOption options,
                              Handler<AsyncResult<Long>> resultHandler);
  DataStore find(String clazz, JsonObject query, Handler<AsyncResult<List<JsonObject>>> resultHandler);
  DataStore findWithOptions(String clazz, JsonObject query, QueryOption findOptions, Handler<AsyncResult<List<JsonObject>>> resultHandler);
  DataStore findOneAndUpdate(String clazz, JsonObject query, JsonObject update, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore findOneAndUpdateWithOptions(String clazz, JsonObject query, JsonObject update, QueryOption queryOption,
                                        UpdateOption updateOption, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore remove(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler);
  DataStore removeWithOptions(String clazz, JsonObject query, JsonObject options, Handler<AsyncResult<Long>> resultHandler);
  DataStore count(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler);

  DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonArray>> resultHandler);

  DataStore dropClass(String clazz, Handler<AsyncResult<Void>> resultHandler);
  DataStore createClass(String clazz, Handler<AsyncResult<Void>> resultHandler);

  DataStore dropIndex(String clazz, String indexName, Handler<AsyncResult<Void>> resultHandler);
  DataStore createIndexWithOptions(String clazz, JsonObject keys, IndexOption options,
                                   Handler<AsyncResult<Void>> resultHandler);
  DataStore listIndices(String clazz, Handler<AsyncResult<JsonArray>> resultHandler);

  DataStore findMetaInfo(String clazz, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore upsertMetaInfo(String clazz, JsonObject metaInfo, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore listClassMetaInfo(Handler<AsyncResult<List<JsonObject>>> resultHandler);
  DataStore removeMetaInfo(String clazz, Handler<AsyncResult<Long>> resultHandler);

  void close();
}
