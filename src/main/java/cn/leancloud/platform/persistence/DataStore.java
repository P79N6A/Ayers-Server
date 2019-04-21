package cn.leancloud.platform.persistence;

import cn.leancloud.platform.modules.Schema;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface DataStore {
  class QueryOption {
    private int skip = 0;
    private int limit = -1;
    private JsonObject sort = null;
    private JsonObject fields = null;
    private List<String> includes = null;

    public int getSkip() {
      return skip;
    }

    public void setSkip(int skip) {
      this.skip = skip;
    }

    public int getLimit() {
      return limit;
    }

    public void setLimit(int limit) {
      this.limit = limit;
    }

    public JsonObject getSort() {
      return sort;
    }

    public void setSort(JsonObject sort) {
      this.sort = sort;
    }

    public JsonObject getFields() {
      return fields;
    }

    public void setFields(JsonObject fields) {
      this.fields = fields;
    }

    public List<String> getIncludes() {
      return includes;
    }

    public void setIncludes(List<String> includes) {
      this.includes = includes;
    }

    public String toString() {
      return Json.encode(this);
    }
  }

  class UpdateOption {
    private boolean upsert = false;
    private boolean multi = false;
    private boolean returnNewDocument = false; // uniquely valid on findOneAnd* methods

    public boolean isUpsert() {
      return upsert;
    }

    public void setUpsert(boolean upsert) {
      this.upsert = upsert;
    }

    public boolean isMulti() {
      return multi;
    }

    public void setMulti(boolean multi) {
      this.multi = multi;
    }

    public boolean isReturnNewDocument() {
      return returnNewDocument;
    }

    public void setReturnNewDocument(boolean returnNewDocument) {
      this.returnNewDocument = returnNewDocument;
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

    public void setBackground(boolean background) {
      this.background = background;
    }

    public boolean isUnique() {
      return unique;
    }

    public void setUnique(boolean unique) {
      this.unique = unique;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isSparse() {
      return sparse;
    }

    public void setSparse(boolean sparse) {
      this.sparse = sparse;
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
  DataStore insertWithOptions(String clazz, JsonObject obj, JsonObject options, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore updateWithOptions(String clazz, JsonObject query, JsonObject object, UpdateOption options,
                              Handler<AsyncResult<Long>> resultHandler);
  DataStore findWithOptions(String clazz, JsonObject query, QueryOption findOptions, Handler<AsyncResult<List<JsonObject>>> resultHandler);
  DataStore findOneAndUpdate(String clazz, JsonObject query, JsonObject update, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore findOneAndUpdateWithOptions(String clazz, JsonObject query, JsonObject update, QueryOption queryOption,
                                        UpdateOption updateOption, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore removeWithOptions(String clazz, JsonObject query, JsonObject options, Handler<AsyncResult<Long>> resultHandler);
  DataStore count(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler);

  DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonArray>> resultHandler);

  DataStore dropClass(String clazz, Handler<AsyncResult<Void>> resultHandler);
  DataStore createClass(String clazz, Handler<AsyncResult<Void>> resultHandler);

  DataStore dropIndex(String clazz, String indexName, Handler<AsyncResult<Void>> resultHandler);
  DataStore createIndexWithOptions(String clazz, JsonObject keys, IndexOption options,
                                   Handler<AsyncResult<Void>> resultHandler);
  DataStore listIndexes(String clazz, Handler<AsyncResult<JsonArray>> resultHandler);

  DataStore findSchema(String clazz, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore upsertSchema(String clazz, Schema schema, Handler<AsyncResult<JsonObject>> resultHandler);
  DataStore listSchemas(Handler<AsyncResult<JsonArray>> resultHandler);
  void close();
}
