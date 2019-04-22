package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.Schema;
import cn.leancloud.platform.persistence.DataStore;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

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
    JsonObject query = new JsonObject().put("_id", "5cbc637de7573f0226106226");
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
}
