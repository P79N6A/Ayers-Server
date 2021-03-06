package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import cn.leancloud.platform.common.BsonTransformer;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.BulkOperation;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.Json;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.leancloud.platform.common.BsonTransformer.REST_OP_ADD_RELATION;
import static cn.leancloud.platform.common.BsonTransformer.REST_OP_REMOVE_RELATION;
import static cn.leancloud.platform.modules.LeanObject.*;
import static cn.leancloud.platform.modules.Relation.BUILTIN_ATTR_RELATIONN_RELATED_ID;
import static cn.leancloud.platform.modules.Relation.BUILTIN_ATTR_RELATION_OWNING_ID;

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

  private static boolean isRelationOperation(Map.Entry<String, Object> entry) {
    if (null == entry || null == entry.getValue()) {
      return false;
    }
    if (!(entry.getValue() instanceof JsonObject)) {
      return false;
    }
    JsonObject jsonValue = (JsonObject) entry.getValue();
    if (!jsonValue.containsKey(ATTR_NAME_OP)) {
      return false;
    }
    String op = jsonValue.getString(ATTR_NAME_OP);
    return REST_OP_ADD_RELATION.equalsIgnoreCase(op) || REST_OP_REMOVE_RELATION.equalsIgnoreCase(op);
  }

  private Future processRelationOperationAsync(String clazz, String currentObjectId, Map.Entry<String, Object> entry) {
    Future tmpFuture = Future.future();
    String key = entry.getKey();
    JsonObject jsonValue = (JsonObject) entry.getValue();
    if (StringUtils.isEmpty(key) || null == jsonValue) {
      logger.warn("invalid relation operation node. key or value is null");
      tmpFuture.complete();
      return tmpFuture;
    }
    String op = jsonValue.getString(ATTR_NAME_OP);
    if (!REST_OP_ADD_RELATION.equalsIgnoreCase(op) && !REST_OP_REMOVE_RELATION.equalsIgnoreCase(op)) {
      logger.warn("invalid relation operation node. __op is invalid:" + op);
      tmpFuture.complete();
      return tmpFuture;
    }
    BulkOperation.OpType bulkOperationType = REST_OP_ADD_RELATION.equalsIgnoreCase(op)? BulkOperation.OpType.INSERT
            : BulkOperation.OpType.DELETE;
    JsonArray objects = jsonValue.getJsonArray(ATTR_NAME_OBJECTS);
    if (null == objects || objects.size() < 1) {
      logger.warn("invalid relation operation node. objects is empty.");
      tmpFuture.complete();
      return tmpFuture;
    }
    JsonObject sampleObject = objects.getJsonObject(0);
    if (null == sampleObject || !sampleObject.containsKey(ATTR_NAME_CLASSNAME) || !sampleObject.containsKey(ATTR_NAME_OBJECTID)) {
      logger.warn("invalid relation operation node. object element or className/objects under it is empty.");
      tmpFuture.complete();
      return tmpFuture;
    }
    String toClazz = sampleObject.getString(ATTR_NAME_CLASSNAME);
    if (StringUtils.isEmpty(toClazz)) {
      logger.warn("invalid relation operation node. className under object element is null");
      tmpFuture.complete();
      return tmpFuture;
    }
    String relationTableName = Constraints.getRelationTable(clazz, toClazz, key);
    JsonObject owningPointer = new JsonObject().put(LeanObject.ATTR_NAME_TYPE, Schema.DATA_TYPE_POINTER)
            .put(LeanObject.ATTR_NAME_CLASSNAME, clazz).put(LeanObject.ATTR_NAME_OBJECTID, currentObjectId);

    List<BulkOperation> operations = objects.stream().map(obj -> {
      String relatedObjectId = ((JsonObject)obj).getString(ATTR_NAME_OBJECTID);
      JsonObject relatedPointer = new JsonObject().put(LeanObject.ATTR_NAME_TYPE, Schema.DATA_TYPE_POINTER)
              .put(LeanObject.ATTR_NAME_CLASSNAME, toClazz).put(LeanObject.ATTR_NAME_OBJECTID, relatedObjectId);
      JsonObject rawBulkOperation = new JsonObject().put(BUILTIN_ATTR_RELATION_OWNING_ID, owningPointer)
              .put(BUILTIN_ATTR_RELATIONN_RELATED_ID, relatedPointer);
      JsonObject bulkOperation = BsonTransformer.encode2BsonRequest(rawBulkOperation, BsonTransformer.REQUEST_OP.CREATE);
      BulkOperation result = new BulkOperation(bulkOperation, bulkOperationType);
      if (bulkOperationType == BulkOperation.OpType.INSERT) {
        // TODO: add filter avoid duplicated doc.
        // result.setFilter(BsonTransformer.encode2BsonRequest(rawBulkOperation, BsonTransformer.REQUEST_OP.QUERY));
      }
      return result;
    }).collect(Collectors.toList());

    logger.debug("call bulkWrite with operations:" + Json.encode(operations));

    this.bulkWrite(relationTableName, operations, res ->{
      if (res.failed()) {
        logger.warn("failed to bulk-save relation operations. cause:" + res.cause().getMessage());
      } else {
        logger.debug("succeed to bulk-save relation operations. result:" + res.result());
      }
      tmpFuture.complete();
    });
    return tmpFuture;
  }

  public DataStore insertWithOptions(String clazz, JsonObject obj, InsertOption options,
                                     Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == obj || obj.size() < 1) {
      resultHandler.handle(new InvalidParameterResult("class or object is required."));
    } else {
      logger.debug("begin to insert object:" + obj);

      List<Map.Entry<String, Object>> relationOps = obj.stream()
              .filter(entry -> isRelationOperation(entry)).collect(Collectors.toList());
      obj = obj.stream().filter(entry -> !isRelationOperation(entry)).collect(JsonFactory.toJsonObject());

      logger.debug("insert object:" + obj);
      logger.debug("relation Operations: " + relationOps);

      JsonObject encodedObject = BsonTransformer.encode2BsonRequest(obj, BsonTransformer.REQUEST_OP.CREATE);
      final boolean returnNewDoc = (null != options)? options.isReturnNewDocument() : false;

      this.mongoClient.insertWithOptions(clazz, encodedObject, WriteOption.ACKNOWLEDGED, event -> {
        if (relationOps.size() > 0 && event.succeeded()) {
          String currentObjectId = event.result();
          List<Future> futures = relationOps.stream()
                  .map(entry -> processRelationOperationAsync(clazz, currentObjectId, entry))
                  .collect(Collectors.toList());
          CompositeFuture.all(futures).setHandler(a -> {
            logger.debug("relation operation result: " + a);
            if (null != resultHandler) {
              resultHandler.handle(event.map(objectId -> {
                if (returnNewDoc) {
                  return BsonTransformer.decodeBsonObject(new JsonObject().mergeIn(encodedObject));
                } else {
                  return new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID, objectId);
                }
              }));
            }
          });
        } else {
          if (null != resultHandler) {
            resultHandler.handle(event.map(objectId -> {
              if (returnNewDoc) {
                return BsonTransformer.decodeBsonObject(new JsonObject().mergeIn(encodedObject));
              } else {
                return new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID, objectId);
              }
            }));
          }
        }
      });
    }
    return this;
  }

  private static Handler<AsyncResult<JsonObject>> wrapResultHandler(Handler<AsyncResult<JsonObject>> rawHandler) {
    if (null == rawHandler) {
      return null;
    }
    return event -> rawHandler.handle(event.map(obj -> BsonTransformer.decodeBsonObject(obj)));
  }

  private static Handler<AsyncResult<List<JsonObject>>> wrapResultListHandler(Handler<AsyncResult<List<JsonObject>>> rawHandler) {
    if (null == rawHandler) {
      return null;
    }
    return event -> rawHandler.handle(event.map(obj -> obj.stream().map(BsonTransformer::decodeBsonObject).collect(Collectors.toList())));
  }

  public DataStore findOne(String clazz, JsonObject query, JsonObject fields, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);
      this.mongoClient.findOne(clazz, condition, fields, wrapResultHandler(resultHandler));
    }
    return this;
  }

  public DataStore updateWithOptions(String clazz, JsonObject query, JsonObject object, UpdateOption options,
                                     Handler<AsyncResult<Long>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query || null == object) {
      resultHandler.handle(new InvalidParameterResult("class or query or object is required."));
    } else {
      String currentObjectId = query.getString(ATTR_NAME_OBJECTID);

      List<Map.Entry<String, Object>> relationOps = object.stream()
              .filter(entry -> isRelationOperation(entry)).collect(Collectors.toList());
      object = object.stream().filter(entry -> !isRelationOperation(entry)).collect(JsonFactory.toJsonObject());

      logger.debug("update object:" + object);
      logger.debug("update relation Operations: " + relationOps);

      JsonObject encodedObject = BsonTransformer.encode2BsonRequest(object, BsonTransformer.REQUEST_OP.UPDATE);
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);

      UpdateOptions mongoOptions = null;
      if (null != options) {
        mongoOptions = new UpdateOptions();
        mongoOptions.setUpsert(options.isUpsert());
        mongoOptions.setReturningNewDocument(options.isReturnNewDocument());
      }

      this.mongoClient.updateCollectionWithOptions(clazz, condition, encodedObject, mongoOptions, event ->{
        if (event.succeeded()) {
          logger.debug(Json.encode(event.result()));
        }

        if (StringUtils.notEmpty(currentObjectId) && relationOps.size() > 0 && event.succeeded()) {
          logger.debug("try to save relation operations.");
          List<Future> futures = relationOps.stream()
                  .map(entry -> processRelationOperationAsync(clazz, currentObjectId, entry))
                  .collect(Collectors.toList());
          CompositeFuture.all(futures).setHandler(a -> {
            logger.debug("relation operation result: " + a);
            if (null != resultHandler) {
              resultHandler.handle(event.map(MongoClientUpdateResult::getDocModified));
            }
          });
        } else {
          if (null != resultHandler) {
            resultHandler.handle(event.map(MongoClientUpdateResult::getDocModified));
          }
        }
      });
    }
    return this;
  }

  public DataStore find(String clazz, JsonObject query, Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);
      this.mongoClient.find(clazz, condition, wrapResultListHandler(resultHandler));
    }
    return this;
  }

  public DataStore findWithOptions(String clazz, JsonObject query, QueryOption findOptions,
                                   Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);

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
      resultHandler.handle(new InvalidParameterResult("class or query or updateSingleObject is required."));
    } else {
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);
      JsonObject updateObject = BsonTransformer.encode2BsonRequest(update, BsonTransformer.REQUEST_OP.UPDATE);

      this.mongoClient.findOneAndUpdate(clazz, condition, updateObject, wrapResultHandler(resultHandler));
    }
    return this;
  }

  public DataStore findOneAndUpdateWithOptions(String clazz, JsonObject query, JsonObject update, QueryOption queryOption,
                                               UpdateOption updateOption, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (StringUtils.isEmpty(clazz) || null == query || null == update) {
      resultHandler.handle(new InvalidParameterResult("class or query or updateSingleObject is required."));
    } else {

      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);
      JsonObject updateObject = BsonTransformer.encode2BsonRequest(update, BsonTransformer.REQUEST_OP.UPDATE);

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
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);
      this.mongoClient.removeDocuments(clazz, condition, event -> {
        // TODO: remember to remove relation table if necessary.
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
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);

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
      JsonObject condition = BsonTransformer.encode2BsonRequest(query, BsonTransformer.REQUEST_OP.QUERY);

      this.mongoClient.count(clazz, condition, resultHandler);
    }
    return this;
  }

  public DataStore bulkWrite(String clazz, List<BulkOperation> operations, Handler<AsyncResult<JsonObject>> resultHandler) {
    if(StringUtils.isEmpty(clazz) || null == operations || operations.size() < 1) {
      resultHandler.handle(new InvalidParameterResult("class or query is required."));
    } else {
      List<io.vertx.ext.mongo.BulkOperation> mongoOperations = operations.stream().map(op -> {
        JsonObject doc = op.getDocument();
        BulkOperation.OpType type = op.getType();
        JsonObject filter = op.getFilter();
        io.vertx.ext.mongo.BulkOperation result;
        if (type == BulkOperation.OpType.INSERT || type == BulkOperation.OpType.UPDATE) {
          if (null != filter) {
            result = io.vertx.ext.mongo.BulkOperation.createUpdate(filter, doc, true, false);
          } else {
            result = io.vertx.ext.mongo.BulkOperation.createInsert(doc);
          }
        } else {
          result = io.vertx.ext.mongo.BulkOperation.createDelete(doc);
        }
        return result;
      }).collect(Collectors.toList());
      logger.debug("bulkWrite with mongoOperations: " + Json.encodePrettily(mongoOperations));
      this.mongoClient.bulkWrite(clazz, mongoOperations, event -> {
        if (null != resultHandler) {
          resultHandler.handle(event.map(mongoClientBulkWriteResult -> mongoClientBulkWriteResult.toJson()));
        }
      });
    }
    return this;
  }

  private <T> AsyncResult<T> generateDummyResult(T v) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return v;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }

  private void dropJoinClassIfNeed(String clazz, Handler<AsyncResult<Void>> resultHandler) {
    this.mongoClient.getCollections(response -> {
      if (response.succeeded()) {
        List<String> classes = response.result();
        List<Future> futures = classes.stream().filter(str -> str.startsWith("_Join:" + clazz) || str.endsWith(":" + clazz)).map(cls -> {
          Future future = Future.future();
          this.mongoClient.dropCollection(cls, dropResponse -> {
            if (dropResponse.failed()) {
              logger.warn("failed to drop Collection: " + cls + ", cause: " + dropResponse.cause().getMessage());
            } else {
              logger.info("succeed to drop Collection: " + cls);
            }
            future.complete();
          });
          return future;
        }).collect(Collectors.toList());
        if (futures.size() < 1) {
          if (null != resultHandler) {
            resultHandler.handle(generateDummyResult(null));
          }
        } else {
          CompositeFuture.all(futures).setHandler(res -> {
            if (null != resultHandler) {
              resultHandler.handle(generateDummyResult(null));
            }
          });
        }
      } else {
        if (null != resultHandler) {
          resultHandler.handle(generateDummyResult(null));
        }
      }
    });
  }

  public DataStore dropClass(String clazz, Handler<AsyncResult<Void>> resultHandler) {
    if (StringUtils.isEmpty(clazz)) {
      resultHandler.handle(new InvalidParameterResult<>("class is required."));
    } else {
      this.mongoClient.dropCollection(clazz, res -> {
        if (res.failed()) {
          if (null != resultHandler) {
            resultHandler.handle(res);
          }
        } else {
          dropJoinClassIfNeed(clazz, resultHandler);
        }
      });
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

  public DataStore listClasses(Handler<AsyncResult<JsonObject>> handler) {
    this.mongoClient.getCollections(response -> handler.handle(response.map(list -> new JsonObject().put("results", list))));
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

  public DataStore listIndices(String clazz, Handler<AsyncResult<JsonArray>> resultHandler) {
    if (StringUtils.isEmpty(clazz)) {
      resultHandler.handle(new InvalidParameterResult<>("class is required."));
    } else {
      this.mongoClient.listIndexes(clazz, resultHandler);
    }
    return this;
  }

  public DataStore findMetaInfo(String clazz, Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject query = new JsonObject().put("name", clazz);
    return this.findOne(Constraints.METADATA_CLASS, query, null, resultHandler);
  }

  public DataStore upsertMetaInfo(String clazz, JsonObject update, Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonObject query = new JsonObject().put("name", clazz);
    QueryOption queryOption = new QueryOption();
    UpdateOption option = new UpdateOption().setUpsert(true).setReturnNewDocument(true);
    return this.findOneAndUpdateWithOptions(Constraints.METADATA_CLASS, query, update, queryOption, option, resultHandler);
  }

  public DataStore listClassMetaInfo(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    return this.find(Constraints.METADATA_CLASS, new JsonObject(), resultHandler);
  }

  public DataStore removeMetaInfo(String clazz, Handler<AsyncResult<Long>> resultHandler) {
    JsonObject query = new JsonObject().put("name", clazz);
    return this.remove(Constraints.METADATA_CLASS, query, resultHandler);
  }

  public ReadStream<JsonObject> aggregate(String clazz, JsonArray pipeline) {
    return this.mongoClient.aggregateWithOptions(clazz, pipeline, new AggregateOptions().setBatchSize(100).setMaxAwaitTime(2000).setMaxTime(5000));
  }

  public void close() {
    this.mongoClient.close();
  }
}
