package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.common.StringUtils;
import cn.leancloud.platform.common.Transformer;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.BulkOperation;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDBDataStore implements DataStore {
  private static final Logger logger = LoggerFactory.getLogger(MongoDBDataStore.class);
  private MongoClient mongoClient;
  private static class InvalidParameterResult<T> implements AsyncResult<T> {
    private String errorMessage;
    public InvalidParameterResult(String message) {
      this.errorMessage = message;
    }
    @Override
    public T result() {
      return null;
    }

    @Override
    public Throwable cause() {
      return new InvalidParameterException(this.errorMessage);
    }

    @Override
    public boolean succeeded() {
      return false;
    }

    @Override
    public boolean failed() {
      return true;
    }
  }

  public MongoDBDataStore(MongoClient client) {
    this.mongoClient = client;
  }

  public DataStore insertWithOptions(String clazz, JsonObject obj, JsonObject options,
                                     Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == obj || obj.size() < 1) {
      resultHandler.handle(new InvalidParameterResult("class or object is required."));
    } else {
      JsonObject encodedObject = Transformer.encode2BsonRequest(obj, Transformer.REQUEST_OP.CREATE);
      final boolean fetchWhenSave = (null != options)? options.getBoolean(Constraints.INTERNAL_MSG_ATTR_FETCHWHENSAVE, false) : false;

      this.mongoClient.insertWithOptions(clazz, encodedObject, WriteOption.ACKNOWLEDGED, event -> {
        if (null != resultHandler) {
          resultHandler.handle(event.map(objectId -> {
            if (fetchWhenSave) {
              return Transformer.decodeBsonObject(new JsonObject().mergeIn(encodedObject));
            } else {
              return new JsonObject().put(Constraints.CLASS_ATTR_OBJECT_ID, objectId);
            }
          }));
        }
      });
    }
    return this;
  }

  private static Handler<AsyncResult<JsonObject>> wrapResultHandler(Handler<AsyncResult<JsonObject>> rawHandler) {
    if (null == rawHandler) {
      return null;
    }
    return event -> rawHandler.handle(event.map(obj -> Transformer.decodeBsonObject(obj)));
  }

  private static Handler<AsyncResult<List<JsonObject>>> wrapResultListHandler(Handler<AsyncResult<List<JsonObject>>> rawHandler) {
    if (null == rawHandler) {
      return null;
    }
    return event -> rawHandler.handle(event.map(obj -> obj.stream().map(Transformer::decodeBsonObject).collect(Collectors.toList())));
  }

  public DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);
      this.mongoClient.findOne(clazz, condition, fields, wrapResultHandler(resultHandler));
    }
    return this;
  }

  public DataStore updateWithOptions(String clazz, JsonObject query, JsonObject object, UpdateOption options,
                                     Handler<AsyncResult<Long>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query || null == object) {
      resultHandler.handle(new InvalidParameterResult("class or query or object is required."));
    } else {
      JsonObject encodedObject = Transformer.encode2BsonRequest(object, Transformer.REQUEST_OP.UPDATE);
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);

      UpdateOptions mongoOptions = null;
      if (null != options) {
        mongoOptions = new UpdateOptions();
        mongoOptions.setUpsert(options.isUpsert());
        mongoOptions.setReturningNewDocument(options.isReturnNewDocument());
      }

      this.mongoClient.updateCollectionWithOptions(clazz, condition, encodedObject, mongoOptions, event ->{
        if (null != resultHandler) {
          resultHandler.handle(event.map(MongoClientUpdateResult::getDocModified));
        }
      });
    }
    return this;
  }

  public DataStore find(String clazz, JsonObject query, Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);
      this.mongoClient.find(clazz, condition, wrapResultListHandler(resultHandler));
    }
    return this;
  }

  public DataStore findWithOptions(String clazz, JsonObject query, QueryOption findOptions,
                                   Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);

      FindOptions options = null;
      if (null != findOptions) {
        options = new FindOptions();
        options.setSkip(findOptions.getSkip());
        options.setLimit(findOptions.getLimit());
        if (null != findOptions.getSort()) {
          options.setSort(findOptions.getSort());
        }
        if (null != findOptions.getFields()) {
          options.setFields(findOptions.getFields());
        }
      }

      this.mongoClient.findWithOptions(clazz, condition, options, wrapResultListHandler(resultHandler));
    }
    return this;
  }

  public DataStore findOneAndUpdate(String clazz, JsonObject query, JsonObject update,
                                    Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query || null == update) {
      resultHandler.handle(new InvalidParameterResult("class or query or update is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);
      JsonObject updateObject = Transformer.encode2BsonRequest(update, Transformer.REQUEST_OP.UPDATE);

      this.mongoClient.findOneAndUpdate(clazz, condition, updateObject, wrapResultHandler(resultHandler));
    }
    return this;
  }

  public DataStore findOneAndUpdateWithOptions(String clazz, JsonObject query, JsonObject update, QueryOption queryOption,
                                               UpdateOption updateOption, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query || null == update) {
      resultHandler.handle(new InvalidParameterResult("class or query or update is required."));
    } else {

      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);
      JsonObject updateObject = Transformer.encode2BsonRequest(update, Transformer.REQUEST_OP.UPDATE);

      FindOptions findOptions = null;
      if (null != queryOption) {
        findOptions = new FindOptions();
        findOptions.setSkip(queryOption.getSkip());
        findOptions.setLimit(queryOption.getLimit());
        if (null != queryOption.getSort()) {
          findOptions.setSort(queryOption.getSort());
        }
        if (null != queryOption.getFields()) {
          findOptions.setFields(queryOption.getFields());
        }
      }

      UpdateOptions updateOptions = null;
      if (null != updateOption) {
        updateOptions = new UpdateOptions();
        updateOptions.setUpsert(updateOption.isUpsert());
        updateOptions.setMulti(updateOption.isMulti());
        updateOptions.setReturningNewDocument(updateOption.isReturnNewDocument());
      }

      this.mongoClient.findOneAndUpdateWithOptions(clazz, condition, updateObject, findOptions, updateOptions,
              wrapResultHandler(resultHandler));
    }
    return this;
  }

  public DataStore remove(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);
      this.mongoClient.removeDocuments(clazz, condition, event -> {
        if (null != resultHandler) {
          resultHandler.handle(event.map(MongoClientDeleteResult::getRemovedCount));
        }
      });
    }
    return this;
  }

  public DataStore removeWithOptions(String clazz, JsonObject query, JsonObject options, Handler<AsyncResult<Long>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);

      this.mongoClient.removeDocumentsWithOptions(clazz, condition, null, res -> {
        if (null != resultHandler) {
          resultHandler.handle(res.map(MongoClientDeleteResult::getRemovedCount));
        }
      });
    }
    return this;
  }
  public DataStore count(String clazz, JsonObject query, Handler<AsyncResult<Long>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = Transformer.encode2BsonRequest(query, Transformer.REQUEST_OP.QUERY);

      this.mongoClient.count(clazz, condition, resultHandler);
    }
    return this;
  }

  public DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonArray>> resultHandler) {
    return this;
  }

  public DataStore dropClass(String clazz, Handler<AsyncResult<Void>> resultHandler) {
    if (StringUtils.isEmpty(clazz)) {
      resultHandler.handle(new InvalidParameterResult<>("class is required."));
    } else {
      this.mongoClient.dropCollection(clazz, resultHandler);
    }
    return this;
  }
  public DataStore createClass(String clazz, Handler<AsyncResult<Void>> resultHandler) {
    if (StringUtils.isEmpty(clazz)) {
      resultHandler.handle(new InvalidParameterResult<>("class is required."));
    } else {
      this.mongoClient.createCollection(clazz, resultHandler);
    }
    return this;
  }

  public DataStore dropIndex(String clazz, String indexName, Handler<AsyncResult<Void>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || StringUtils.isEmpty(indexName)) {
      resultHandler.handle(new InvalidParameterResult<>("class or indexName is required."));
    } else {
      this.mongoClient.dropIndex(clazz, indexName, resultHandler);
    }
    return this;
  }

  public DataStore createIndexWithOptions(String clazz, JsonObject keys, IndexOption options,
                                          Handler<AsyncResult<Void>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == keys || keys.size() < 1) {
      resultHandler.handle(new InvalidParameterResult<>("class or keys is required."));
    } else {
      IndexOptions mongoOptions = null;
      if (null != options) {
        mongoOptions = new IndexOptions(options.toJson());
      }
      this.mongoClient.createIndexWithOptions(clazz, keys, mongoOptions, resultHandler);
    }
    return this;
  }

  public DataStore listIndexes(String clazz, Handler<AsyncResult<JsonArray>> resultHandler) {
    if (StringUtils.isEmpty(clazz)) {
      resultHandler.handle(new InvalidParameterResult<>("class is required."));
    } else {
      this.mongoClient.listIndexes(clazz, resultHandler);
    }
    return this;
  }

  public DataStore findSchema(String clazz, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }

  public DataStore upsertSchema(String clazz, Schema schema, Handler<AsyncResult<JsonObject>> resultHandler) {
    return this;
  }

  public DataStore listSchemas(Handler<AsyncResult<JsonArray>> resultHandler) {
    return this;
  }

  public void close() {
    this.mongoClient.close();
  }
}
