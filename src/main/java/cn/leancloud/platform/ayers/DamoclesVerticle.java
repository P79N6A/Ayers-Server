package cn.leancloud.platform.ayers;

import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.cache.UnifiedCache;
import cn.leancloud.platform.modules.*;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.common.ErrorCodes;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import cn.leancloud.platform.utils.JsonFactory;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.leancloud.platform.common.ErrorCodes.ACL_VIOLATION;
import static cn.leancloud.platform.common.ErrorCodes.SCHEMA_VIOLATION;

/**
 * Class permission / Object ACL rule / Data consistency defender.
 *
 */
public class DamoclesVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DamoclesVerticle.class);
  private InMemoryLRUCache<String, JsonObject> classMetaCache;
  private DataStoreFactory dataStoreFactory = null;

  private void saveSchema(String clazz, Schema schema, ClassMetaData classMetaData) {
    Objects.requireNonNull(classMetaData);
    Objects.requireNonNull(schema);
    classMetaData.setSchema(schema);
    this.classMetaCache.put(clazz, classMetaData);

    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.upsertMetaInfo(clazz, classMetaData, event -> {
      if (event.failed()) {
        logger.warn("failed to upsert class schema. cause: " + event.cause());
      } else {
        logger.info("succeed to save schema. clazz=" + clazz + ", schema=" + schema);
      }
    });

    final JsonArray existedIndices = classMetaData.getIndices();
    final JsonArray newIndices = new JsonArray();

    Future<Boolean> future = Future.succeededFuture(true);
    future.compose(res -> {
      // make sure 2dsphere
      String  geoPointAttrPath = schema.findGeoPointAttr();
      Future<Boolean> sphereFuture = Future.future();
      if (StringUtils.notEmpty(geoPointAttrPath)) {
        JsonObject indexJson = new JsonObject().put(geoPointAttrPath, "2dsphere");
        DataStore.IndexOption indexOption = new DataStore.IndexOption().setSparse(true).setName(geoPointAttrPath);
        makeSureIndexCreated(clazz, indexJson, indexOption, existedIndices, dataStore, response -> {
          if (response.failed()) {
            logger.warn("failed to createSingleObject index. attr=" + geoPointAttrPath + ". cause: " + response.cause());
            future.fail(response.cause());
          } else {
            if (response.result()) {
              logger.info("success to createSingleObject index. attr=" + geoPointAttrPath);
              newIndices.add(indexOption.toJson());
            }
            future.complete(response.result());
          }
        });
      } else {
        sphereFuture.complete();
      }
      return sphereFuture;
    })
    .compose(res -> makeSureDefaultIndex(clazz, LeanObject.ATTR_NAME_UPDATED_TS, false, existedIndices, newIndices, dataStore))
    .compose(res -> makeSureDefaultIndex(clazz, LeanObject.ATTR_NAME_CREATED_TS, false, existedIndices, newIndices, dataStore))
    .compose(res -> {
      if (Constraints.USER_CLASS.equals(clazz)) {
        return makeSureDefaultIndex(clazz, LeanObject.BUILTIN_ATTR_EMAIL, true, existedIndices, newIndices, dataStore);
      } else {
        return Future.succeededFuture(true);
      }
    }).compose(res -> {
        if (Constraints.USER_CLASS.equals(clazz)) {
          return makeSureDefaultIndex(clazz, LeanObject.BUILTIN_ATTR_USERNAME, true, existedIndices, newIndices, dataStore);
        } else {
          return Future.succeededFuture(true);
        }
    }).compose(res -> {
      if (Constraints.USER_CLASS.equals(clazz)) {
        return makeSureDefaultIndex(clazz, LeanObject.BUILTIN_ATTR_MOBILEPHONE, true, existedIndices, newIndices, dataStore);
      } else {
        return Future.succeededFuture(true);
      }
    }).compose(res -> {
      if (Constraints.USER_CLASS.equals(clazz)) {
        List<String> authIndexPaths = schema.findAuthDataIndex();
        Future<Boolean> composedFuture = Future.succeededFuture(true);
        for (String attr : authIndexPaths) {
          composedFuture = composedFuture.compose(r -> makeSureDefaultIndex(clazz, attr, true, existedIndices, newIndices, dataStore));
        }
        return composedFuture;
      } else {
        return Future.succeededFuture(true);
      }
    }).setHandler(response -> {
      dataStore.close();
      if (newIndices.size() > 0) {
        if (null != existedIndices)
          newIndices.addAll(existedIndices);
        classMetaData.setIndices(newIndices);
        this.classMetaCache.put(clazz, classMetaData);
      }
    });
  }

  private Future<Boolean> makeSureDefaultIndex(String clazz, String attr, boolean isUnique, JsonArray existedIndex, JsonArray newIndices,
                                               DataStore dataStore) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(attr);
    Objects.requireNonNull(newIndices);
    Future<Boolean> defaultFuture = Future.future();
    JsonObject indexJson = new JsonObject().put(attr, 1);
    DataStore.IndexOption indexOption = new DataStore.IndexOption().setSparse(true).setUnique(isUnique).setName(attr);
    makeSureIndexCreated(clazz, indexJson, indexOption, existedIndex, dataStore, response -> {
      if (response.failed()) {
        logger.warn("failed to createSingleObject index. attr=" + attr + ". cause: " + response.cause());
        defaultFuture.fail(response.cause());
      } else {
        if (response.result()) {
          logger.info("success to createSingleObject index. attr=" + attr);
          newIndices.add(indexOption.toJson());
        }
        defaultFuture.complete(response.result());
      }
    });

    return defaultFuture;
  }

  private void makeSureIndexCreated(String clazz, JsonObject indexJson, DataStore.IndexOption indexOption,
                                    JsonArray existedIndex, DataStore dataStore, Handler<AsyncResult<Boolean>> handler) {
    Objects.requireNonNull(indexJson);
    Objects.requireNonNull(indexOption);
    Objects.requireNonNull(dataStore);
    Objects.requireNonNull(handler);
    if (indexJson.size() < 1) {
      throw new IllegalArgumentException("indexJson is empty.");
    }

    String attrPath = indexJson.stream().findFirst().get().getKey();
    boolean existed = (null == existedIndex) ? false :
            existedIndex.stream().filter(json -> attrPath.equals(((JsonObject)json).getString("name"))).count() > 0;
    if (existed) {
      handler.handle(new AsyncResult<Boolean>() {
        @Override
        public Boolean result() {
          return false;
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
      });
    } else {
      dataStore.createIndexWithOptions(clazz, indexJson, indexOption, res -> handler.handle(res.map(res.succeeded())));
    }
  }

  protected Future<Boolean> checkRolePermission(List<String> roles, JsonObject currentUser) {
    // TODO: finish me.
    Future<Boolean> result = Future.failedFuture("can't check role, not implement yet...");
    return result;
  }

  protected Future<Boolean> checkClassPermission(String clazz, JsonObject body, RequestParse.RequestHeaders request, ClassPermission.OP op) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(request);

    Future<Boolean> result = Future.future();
    if (request.isUseMasterKey()) {
      // master key is GOD
      result.complete(true);
      return result;
    }

    JsonObject classMeta = Configure.getInstance().getClassMetaDataCache().get(clazz);
    if (null == classMeta) {
      // always allow create class.
      result.complete(true);
      return result;
    }

    ClassPermission classPermission = ClassPermission.fromJson(ClassMetaData.fromJson(classMeta).getClassPermissions());
    // check public permission at first.
    if (classPermission.checkOperation(op, null, null)) {
      result.complete(true);
      return result;
    }

    String sessionToken = request.getSessionToken();
    if (StringUtils.isEmpty(sessionToken)) {
      // unauth user.
      result.fail("user isn't login.");
      return result;
    }

    JsonObject user = UnifiedCache.getGlobalInstance().get(sessionToken);
    if (null != user) {
      boolean allowed = classPermission.checkOperation(op, user.getString(LeanObject.ATTR_NAME_OBJECTID), null);
      if (allowed) {
        result.complete(true);
      } else {
        List<String> roles = classPermission.getOperationRoles(op);
        if (null == roles || roles.size() < 1) {
          result.fail("no permission for current user.");
        } else {
          return checkRolePermission(roles, user);
        }
      }
      return result;
    }
    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.findOne(Constraints.USER_CLASS, new JsonObject().put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, request.getSessionToken()),
            null, response -> {
      dataStore.close();
      if (response.failed() || null == response.result()) {
        result.fail("failed to fetch user with sessionToken:" + request.getSessionToken());
      } else {
        JsonObject newUser = response.result();
        boolean allowed = classPermission.checkOperation(op, newUser.getString(LeanObject.ATTR_NAME_OBJECTID), null);
        if (allowed) {
          result.complete(true);
        } else {
          List<String> roles = classPermission.getOperationRoles(op);
          if (null == roles || roles.size() < 1) {
            result.fail("no permission for current user.");
          } else {
            checkRolePermission(roles, user).setHandler(res -> result.handle(res));
          }
        }
      }
    });
    return result;
  }

  protected Future<Boolean> checkObjectACL(String clazz, String objectId, RequestParse.RequestHeaders request, ClassPermission.OP op) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(objectId);
    Objects.requireNonNull(request);

    Future<Boolean> result = Future.future();

    if (request.isUseMasterKey()) {
      // for masterkey, any operation is allowed.
      result.complete(true);
      return result;
    }

    DataStore dataStore = dataStoreFactory.getStore();
    JsonObject query = new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID, objectId);
    JsonObject field = new JsonObject().put(LeanObject.BUILTIN_ATTR_ACL, 1);
    dataStore.findOne(clazz, query, field, response -> {
              if (response.failed()) {
                // database error.
                dataStore.close();
                result.fail(response.cause().getMessage());
                return;
              } else if (null == response.result() || response.result().size() < 1) {
                // not found.
                dataStore.close();
                result.complete(true);
                return;
              } else {
                LeanObject targetObject = LeanObject.fromJson(clazz, response.result());
                boolean isWriteOp = op.equals(ClassPermission.OP.UPDATE) || op.equals(ClassPermission.OP.DELETE);
                String sessionToken = request.getSessionToken();
                if (StringUtils.isEmpty(sessionToken)) {
                  dataStore.close();
                  boolean allowed = targetObject.checkOperationByACL(isWriteOp, null);
                  if (allowed) {
                    result.complete(true);
                  } else {
                    result.fail("user isn't login.");
                  }
                  return;
                } else {
                  JsonObject user = UnifiedCache.getGlobalInstance().get(sessionToken);
                  if (null != user) {
                    dataStore.close();
                    boolean allowed = targetObject.checkOperationByACL(isWriteOp, user.getString(LeanObject.ATTR_NAME_OBJECTID));
                    if (allowed) {
                      result.complete(true);
                    } else {
                      List<String> roles = targetObject.getOperationRoles(isWriteOp);
                      if (null == roles || roles.size() < 1) {
                        result.fail("no permission for current user.");
                      } else {
                        checkRolePermission(roles, user).setHandler(result::handle);
                      }
                    }
                    return;
                  } else {
                    dataStore.findOne(Constraints.USER_CLASS, new JsonObject().put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, sessionToken),
                            null, res2 -> {
                              dataStore.close();
                              if (res2.failed()) {
                                result.fail(res2.cause().getMessage());
                                return;
                              } else {
                                JsonObject newUser = res2.result();
                                if (null != newUser) {
                                  UnifiedCache.getGlobalInstance().put(sessionToken, newUser);
                                }
                                String userObjectId = null == newUser ? null : newUser.getString(LeanObject.ATTR_NAME_OBJECTID);
                                boolean allowed = targetObject.checkOperationByACL(isWriteOp, userObjectId);
                                if (allowed) {
                                  result.complete(true);
                                } else {
                                  List<String> roles = targetObject.getOperationRoles(isWriteOp);
                                  if (null == roles || roles.size() < 1) {
                                    result.fail("no permission for current user.");
                                  } else {
                                    checkRolePermission(roles, newUser).setHandler(result::handle);
                                  }
                                }
                                return;
                              }
                            });
                  }
                }
              }
            });
    return result;
  }

  /**
   * main process loop
   * @param message
   */
  public void onMessage(Message<JsonObject> message) {
    JsonObject messageBody = message.body();
    String clazz = messageBody.getString(INTERNAL_MSG_ATTR_CLASS, "");
    String objectId = messageBody.getString(INTERNAL_MSG_ATTR_OBJECT_ID);
    JsonObject param = messageBody.getJsonObject(INTERNAL_MSG_ATTR_UPDATE_PARAM);
    RequestParse.RequestHeaders requestHeaders = RequestParse.RequestHeaders.fromJson(messageBody.getJsonObject(INTERNAL_MSG_ATTR_REQUESTHEADERS));

    if (StringUtils.isEmpty(clazz)) {
      logger.warn("clazz name is empty, ignore schema check...");
      message.fail(ErrorCodes.INVALID_PARAMETER.getCode(), "className is required.");
    } else {
      String operation = message.headers().get(INTERNAL_MSG_HEADER_OP).toUpperCase();
      final ClassMetaData cachedClassMetaData = (ClassMetaData)this.classMetaCache.get(clazz);

      LeanObject object = null != param? new LeanObject(param) : null;

      if (RequestParse.OP_FIND_SCHEMA.equals(operation)) {
        if (null == cachedClassMetaData) {
          message.reply(new JsonObject());
        } else {
          message.reply(cachedClassMetaData.getSchema());
        }
        return;
      } else if (null == object) {
        message.fail(ErrorCodes.INVALID_PARAMETER.getCode(), "json param is required.");
        return;
      } else {
        final Schema inputSchema;
        switch (operation) {
          case RequestParse.OP_ADD_SCHEMA:
            inputSchema = object.guessSchema();
            if (inputSchema.size() < 1) {
              // can't add empty schema.
              message.reply(new JsonObject().put("result", false));
            } else if (null == cachedClassMetaData || null == cachedClassMetaData.getSchema()) {
              // schema not existed.
              saveSchema(clazz, inputSchema, null == cachedClassMetaData? new ClassMetaData(clazz) : cachedClassMetaData);
              message.reply(new JsonObject().put("result", true));
            } else {
              // schema has existed, failed to add again.
              message.reply(new JsonObject().put("result", false));
            }
            break;
          case RequestParse.OP_TEST_SCHEMA:
            inputSchema = object.guessSchema();
            if (null != cachedClassMetaData) {
              Schema cachedSchema = cachedClassMetaData.getSchema();
              try {
                Schema.CompatResult compatResult = inputSchema.compatibleWith(cachedSchema);
                if (compatResult == Schema.CompatResult.NOT_MATCHED) {
                  message.reply(new JsonObject().put("result", false));
                } else {
                  message.reply(new JsonObject().put("result", true));
                }
              } catch (ConsistencyViolationException ex) {
                message.reply(new JsonObject().put("result", false));
              }
            } else {
              // anything is matched if first occurring.
              message.reply(new JsonObject().put("result", true));
            }
            break;
          case RequestParse.OP_USER_SIGNIN:
          case RequestParse.OP_USER_SIGNUP:
            inputSchema = object.guessSchema();
            Schema cachedSchema = null == cachedClassMetaData ? null : cachedClassMetaData.getSchema();
            try {
              Schema.CompatResult compatResult = inputSchema.compatibleWith(cachedSchema);
              if (compatResult != Schema.CompatResult.MATCHED) {
                saveSchema(clazz, inputSchema, cachedClassMetaData);
              }
              message.reply(new JsonObject().put("result", true));
            } catch (ConsistencyViolationException ex) {
              logger.warn("failed to parse object schema. cause: " + ex.getMessage());
              message.fail(SCHEMA_VIOLATION.getCode(), ex.getMessage());
            }
            break;
          case RequestParse.OP_OBJECT_DELETE:
          case RequestParse.OP_OBJECT_PUT:
          case RequestParse.OP_OBJECT_POST:
          case RequestParse.OP_OBJECT_GET:
            // object CRUD.
            final ClassPermission.OP op;
            boolean checkSchemaFirst = false;
            boolean dontCheckACL = false;

            Schema.CompatResult compatResult = Schema.CompatResult.MATCHED;
            if (RequestParse.OP_OBJECT_POST.equals(operation)) {
              op = ClassPermission.OP.CREATE;
              checkSchemaFirst = true;
              dontCheckACL = true;
            } else if (RequestParse.OP_OBJECT_PUT.equals(operation)) {
              op = ClassPermission.OP.UPDATE;
              checkSchemaFirst = true;
            } else if (RequestParse.OP_OBJECT_GET.equals(operation)) {
              op = StringUtils.isEmpty(objectId)? ClassPermission.OP.FIND : ClassPermission.OP.GET;
            } else if (RequestParse.OP_OBJECT_DELETE.equals(operation)) {
              op = ClassPermission.OP.DELETE;
            } else {
              op = ClassPermission.OP.FIND;
            }
            if (checkSchemaFirst) {
              try {
                inputSchema = object.guessSchema();
                Schema existedSchema = null == cachedClassMetaData ? null : cachedClassMetaData.getSchema();
                compatResult = inputSchema.compatibleWith(existedSchema);
              } catch (ConsistencyViolationException ex) {
                logger.warn("failed to parse object schema. cause: " + ex.getMessage());
                message.fail(SCHEMA_VIOLATION.getCode(), ex.getMessage());
                return;
              }
            } else {
              inputSchema = null;
            }
            final boolean needSaveSchema = (compatResult != Schema.CompatResult.MATCHED) && (null != inputSchema);

            List<Future> futures = new ArrayList<>();
            if (compatResult == Schema.CompatResult.OVER_MATCHED_ON_FIELD_LAYER) {
              // check add_field permission at first.
              futures.add(checkClassPermission(clazz, param, requestHeaders, ClassPermission.OP.ADD_FIELDS));
            }
            // check class permissions.
            futures.add(checkClassPermission(clazz, param, requestHeaders, op));

            // check object ACL
            if (!dontCheckACL) {
              futures.add(checkObjectACL(clazz, objectId, requestHeaders, op));
            }

            CompositeFuture.all(futures).setHandler(response -> {
              if (response.failed()) {
                message.fail(ACL_VIOLATION.getCode(), response.cause().getMessage());
              } else {
                message.reply(new JsonObject().put("result", true));

                if (needSaveSchema) {
                  saveSchema(clazz, inputSchema, cachedClassMetaData);
                }
              }
            });
            break;
          default:
            message.reply(new JsonObject().put("result", false));
            break;
        }
      }
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    logger.info("start Damocles Verticle...");

    dataStoreFactory = Configure.getInstance().getDataStoreFactory();
    classMetaCache = Configure.getInstance().getClassMetaDataCache();
    DataStore dataStore = dataStoreFactory.getStore();
    dataStore.listClassMetaInfo(event -> {
      if (event.failed()) {
        logger.warn("failed to fetch class meta info, cause: " + event.cause());
        dataStore.close();
      } else {
        event.result().forEach(object  -> {
          if (null == object) {
            return;
          }
          ClassMetaData metaData = ClassMetaData.fromJson(object);
          String clazz = metaData.getName();
          if (StringUtils.isEmpty(clazz)) {
            logger.warn("found invalid class meta data. " + object);
          } else {
            logger.info("found class meta data. clazz=" + clazz + ", metaInfo=" + metaData);
            dataStore.listIndices(clazz, res-> {
              JsonArray indices = null;
              if (res.failed()) {
                logger.warn("failed to load indices for class:" + clazz + ", cause: " + res.cause());
              } else {
                indices = res.result().stream().filter(json -> !((JsonObject)json).getString("name").equals("_id_"))
                        .collect(JsonFactory.toJsonArray());
                logger.info("found indices. clazz=" + clazz + ", indices=" + indices.toString());
              }
              if (null == indices) {
                indices = new JsonArray();
              }
              metaData.setIndices(indices);
              classMetaCache.putIfAbsent(clazz, metaData);
            });
          }
        });
      }

      logger.info("begin to consume address: " + Configure.MAIL_ADDRESS_DAMOCLES_QUEUE);
      vertx.eventBus().consumer(Configure.MAIL_ADDRESS_DAMOCLES_QUEUE, this::onMessage);

      startFuture.complete();
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop Damocles Verticle...");
    stopFuture.complete();
  }
}
