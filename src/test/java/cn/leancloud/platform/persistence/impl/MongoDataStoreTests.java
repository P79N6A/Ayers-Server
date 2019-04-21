package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.persistence.DataStore;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.time.Instant;
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
}
