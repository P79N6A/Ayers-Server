package cn.leancloud.platform.mongo;

import cn.leancloud.platform.common.BsonTransformer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class QuickTour {
  public static void main(final String[] args) throws InterruptedException {
    String database = "uluru-test";
    String collectionName = "test";
    Vertx vertx = Vertx.vertx();
    MongoClient client = MongoClient.createShared(vertx,
            new JsonObject().put("host", "127.0.0.1").put("port", 27027).put("db_name", database));
    JsonObject doc = new JsonObject().put("name", "MongoDB").put("type", "database").put("count", 1)
            .put("city", Arrays.asList("changsha", "xian"));
    CountDownLatch latch = new CountDownLatch(1);
    client.insert(collectionName, new JsonObject().put("$set", doc), res -> {
      System.out.println("insert: " + res.result());
      JsonObject tmp = new JsonObject("{\"city\": {\"__op\":\"AddRelation\", \"objects\":[\"beijing\",\"shanghai\"]}, \"$set\":{\"count\": 10}}");
      JsonObject updated = BsonTransformer.encode2BsonRequest(tmp, BsonTransformer.REQUEST_OP.UPDATE);
      System.out.println(updated.toString());
      //new JsonObject().put("$push", new JsonObject().put("city", new JsonObject().put("$each", Arrays.asList("beijing", "shanghai"))));
      client.updateCollection(collectionName, new JsonObject().put("_id", res.result()), updated, res1 -> {
        if (res1.succeeded()) {
          System.out.println(res1.result().toJson());
        } else {
          res1.cause().printStackTrace();
        }
        latch.countDown();
      });
    });
    latch.await();
  }
}
