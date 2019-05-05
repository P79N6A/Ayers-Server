package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.utils.JsonFactory;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import junit.framework.TestCase;
import scala.collection.immutable.StreamIterator;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class MongoDataStoreTests extends TestCase {
  private Vertx vertx = null;
  private MongoDBDataStoreFactory factory = null;
  private DataStore dataStore = null;

  private boolean testSuccessed = false;
  private CountDownLatch latch = null;

  public MongoDataStoreTests() {
    vertx = Vertx.vertx();
    JsonObject mongoConfig = new JsonObject()
            .put("host", "localhost")
            .put("port", 27027)
            .put("db_name", "uluru-test")
            .put("maxPoolSize", 3)
            .put("minPoolSize", 1)
            .put("keepAlive", true);
    factory = new MongoDBDataStoreFactory(vertx, mongoConfig, "TestMongoDB");
    vertx.runOnContext(res -> {});
  }

  @Override
  protected void setUp() throws Exception {
    dataStore = factory.getStore();
    testSuccessed = false;
    latch = new CountDownLatch(1);
  }

  @Override
  protected void tearDown() throws Exception {
    if (null != dataStore) {
      dataStore.close();
    }
  }

  public void testInsertDocument() throws Exception {
    JsonObject document = new JsonObject();
    document.put("time", System.currentTimeMillis());
    dataStore.insertWithOptions("test", document, null, res -> {
      if (res.succeeded()) {
        System.out.println(res.result());
        testSuccessed = true;
      }
      latch.countDown();
    });
    latch.await();
  }

  public void testFindDocument() throws Exception {
    JsonObject filter = new JsonObject().put("time", new JsonObject().put("$lt", System.currentTimeMillis()));
    DataStore.QueryOption option = new DataStore.QueryOption();
    option.setLimit(100);
    dataStore.findWithOptions("test", filter, option, res -> {
      if (res.succeeded()) {
        System.out.println(res.result());
        testSuccessed = res.result().size() > 1;
      }
      latch.countDown();
    });
    latch.await();
  }

  public void testUpdateWithOptions() throws Exception {
    JsonObject query = new JsonObject().put("objectId", "5cbc637de7573f0226106226");
    JsonObject update = new JsonObject().put("updatedAt", Instant.now()).put("source", "modified by unit test");
    DataStore.QueryOption option = new DataStore.QueryOption();
    option.setLimit(100);
    DataStore.UpdateOption updateOption = new DataStore.UpdateOption();
    updateOption.setReturnNewDocument(true);
    dataStore.findOneAndUpdateWithOptions("test", query, update, option, updateOption, res -> {
      if (res.succeeded()) {
        System.out.println(res.result());
        testSuccessed = res.result() != null;
      }
      latch.countDown();
    });
    latch.await();
  }

  public void testSchemaOperation() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);

    Schema schema1 = object.guessSchema();

    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    object.put("location", new JsonObject().put("latitude", 34.5).put("longitude", -87.4));

    Schema schema2 = object.guessSchema();

    dataStore.upsertSchema("Post", schema1, event -> {
      if (event.failed()) {
        System.out.println("failed to upsert first schema. cause: " + event.cause());
        latch.countDown();
      } else {
        System.out.println("succeed to upsert first schema. result: " + event.result());
        dataStore.listSchemas(event2 -> {
          if (event2.failed()) {
            System.out.println("failed to list schema after first upsert. cause: " + event2.cause());
            latch.countDown();
          } else if (null == event2.result() || event2.result().size() < 1) {
            System.out.println("list schema result is wrong: " + event2.result().size() + ", expected=1");
            latch.countDown();
          } else {
            System.out.println("succeed to list schema after first upsert. result: " + event2.result());
            dataStore.upsertSchema("Post", schema2, event1 -> {
              if (event1.failed()) {
                System.out.println("failed to upsert schema again. cause: " + event1.cause());
                latch.countDown();
              } else {
                System.out.println("succeed to upsert schema again. result: " + event1.result());
                dataStore.removeSchema("Post", event3 -> {
                  if (event3.failed()) {
                    System.out.println("failed to remove schema. cause: " + event3.cause());
                    latch.countDown();
                  } else {
                    if (event3.result() == 1) {
                      testSuccessed = true;
                    } else {
                      System.out.println("remove schema count is wrong: " + event3.result() + ", expected=1");
                    }
                    latch.countDown();
                  }
                });
              }
            });
          }
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testListEmptyIndex() throws Exception {
    dataStore.listIndices("test", res -> {
      if (res.succeeded()) {
        System.out.println(res.result().toString());
        JsonArray result = res.result().stream().filter(json -> !"_id_".equals(((JsonObject)json).getString("name")))
                .collect(JsonFactory.toJsonArray());
        System.out.println("net result: " + result.toString());
        testSuccessed = true;
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testInsertWithPointer() throws Exception {
    LeanObject object = new LeanObject("Article");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    dataStore.insertWithOptions("Article", object, new DataStore.InsertOption().setReturnNewDocument(true), res -> {
      if (res.failed()) {
        System.out.println("failed to insert Article document. cause: " + res.cause());
        latch.countDown();
      } else {
        LeanObject comment1 = new LeanObject("Comment");
        comment1.put("article", new JsonObject().put("$ref", "Article").put("$id", res.result().getString("objectId")));
        comment1.put("publishTime", Instant.now());
        LeanObject comment2 = new LeanObject("Comment");
        comment2.put("article", new JsonObject().put("$ref", "Article").put("$id", res.result().getString("objectId")));
        comment2.put("publishTime", Instant.now());
        dataStore.insertWithOptions("Comment", comment1, new DataStore.InsertOption().setReturnNewDocument(true),
                res1 -> {
          if (res1.failed()) {
            System.out.println("failed to insert first Comment document. cause: " + res1.cause());
            latch.countDown();
          } else {
            dataStore.insertWithOptions("Comment", comment2, new DataStore.InsertOption().setReturnNewDocument(true),
                    res2 ->{
              if (res2.failed()) {
                System.out.println("failed to insert second Comment document. cause: " + res2.cause());
                latch.countDown();
              } else {
                testSuccessed = true;
                latch.countDown();
              }
            });
          }});
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testFindDocumentWithPointer() throws Exception {
    JsonObject filter = new JsonObject();
    DataStore.QueryOption option = new DataStore.QueryOption();
    option.setLimit(100);
    option.setFields(new JsonObject().put("author", 1).put("_id", 1));
    dataStore.findWithOptions("Comment", filter, option, res -> {
      if (res.succeeded()) {
        res.result().forEach(a -> System.out.println(a));
        testSuccessed = res.result().size() > 1;
      }
      latch.countDown();
    });
    latch.await();
  }

  public void testAggregation() throws Exception {
    MongoDBDataStore mongoDBDataStore = (MongoDBDataStore)dataStore;
    String articleId = "5cc308ce875e6360c03d000f";
    JsonObject lookup = new JsonObject().put("$lookup",
            new JsonObject().put("from", "Article").put("localField", "article").put("foreignField", "_id").put("as", "article"));
    JsonObject match = new JsonObject().put("$match",
            new JsonObject().put("$exist", "publishTime"));
    ReadStream<JsonObject> result = mongoDBDataStore.aggregate("Comment", new JsonArray().add(lookup).add(match));
    result.handler(res -> {
      System.out.println(res);
      latch.countDown();
    });
    latch.await();
  }

  public void testIndexCRUD() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("location", new JsonObject().put("__type", "GeoPoint").put("latitude", 34.5).put("longitude", -87.4));
    dataStore.insertWithOptions("Post", object, new DataStore.InsertOption().setReturnNewDocument(true), res -> {
      if (res.failed()) {
        System.out.println("failed to insert document. cause: " + res.cause());
        latch.countDown();
      } else {
        DataStore.IndexOption indexOption = new DataStore.IndexOption();
        indexOption.setName("location");
        indexOption.setSparse(true);
        dataStore.createIndexWithOptions("Post", new JsonObject().put("localtion", "2dsphere"), indexOption,
                res2 -> {
          if (res2.failed()) {
            System.out.println("failed to create index. cause: " + res2.cause());
            latch.countDown();
          } else {
            dataStore.listIndices("Post", res3 -> {
              if (res3.failed()) {
                System.out.println("failed to list indexes document. cause: " + res3.cause());
                latch.countDown();
              } else {
                testSuccessed = res3.result().size() > 0;
                System.out.println("index results: " + res3.result().toString());
                dataStore.dropIndex("Post", "location", res4 -> {
                  if (res4.failed()) {
                    System.out.println("failed to drop indexes document. cause: " + res4.cause());
                  } else {
                    testSuccessed &= true;
                  }
                  latch.countDown();
                });
              }
            });
          }
                });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testCreateIndexTwice() throws Exception {
    DataStore.IndexOption indexOption = new DataStore.IndexOption();
    indexOption.setName("updatedAt");
    indexOption.setSparse(true);
    dataStore.createIndexWithOptions("_User", new JsonObject().put("updatedAt", 1), indexOption, res -> {
      if (res.failed()) {
        dataStore.close();
        System.out.println("failed to create index for updatedAt at first.");
        res.cause().printStackTrace();
        latch.countDown();
      } else {
        System.out.println("succeed to create index for updatedAt at first.");
        dataStore.createIndexWithOptions("_User", new JsonObject().put("updatedAt", 1), indexOption, res2 -> {
          dataStore.close();
          if (res2.failed()) {
            System.out.println("failed to create index for updatedAt again.");
            res2.cause().printStackTrace();
          } else {
            System.out.println("succeed to create index for updatedAt again.");
            testSuccessed = true;
          }
          latch.countDown();
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }
}
