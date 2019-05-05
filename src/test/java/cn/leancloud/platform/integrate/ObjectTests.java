package cn.leancloud.platform.integrate;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ObjectTests extends WebClientTests {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testSimpleOperation() throws Exception {
    JsonObject data = new JsonObject().put("content", "每个 Java 程序员必备的 8 个开发工具")
            .put("pubUser", "LeanCloud官方客服").put("pubTimestamp", 1435541999);
    post("/1.1/classes/Post", data, response -> {
      if (response.failed()) {
        System.out.println("failed to create object, cause:" + response.cause());
        latch.countDown();
      } else {
        JsonObject result = response.result();
        System.out.println("create result: " + result);
        String objectId = result.getString("objectId");
        assertTrue(StringUtils.notEmpty(objectId) && result.getString("createdAt") != null);
        get("/1.1/classes/Post/" + objectId, null, res -> {
          if (res.failed()) {
            System.out.println("failed to fetch object, cause:" + res.cause());
            latch.countDown();
          } else {
            System.out.println("get result: " + res.result());

            assertTrue(res.result().getString("objectId").equals(objectId));
            JsonObject update = new JsonObject().put("content", "每个 JavaScript 程序员必备的 9 个开发工具");
            put("/1.1/classes/Post/" + objectId, update, res3 -> {
              if (res3.failed()) {
                System.out.println("failed to update object, cause:" + res3.cause());
                latch.countDown();
              } else {
                System.out.println("update result: " + res3.result());
                delete("/1.1/classes/Post/" + objectId, null, res4 -> {
                  if (res4.failed()) {
                    System.out.println("failed to delete object, cause:" + res4.cause());
                    latch.countDown();
                  } else {
                    System.out.println("delete result: " + res4.result());
                    testSuccessed = true;
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

  public void testBatchOperations() throws Exception {
    String opString = "{\n" +
            "        \"requests\": [\n" +
            "          {\n" +
            "            \"method\": \"POST\",\n" +
            "            \"path\": \"/1.1/classes/Post\",\n" +
            "            \"body\": {\n" +
            "              \"content\": \"近期 LeanCloud 的文档已经支持评论功能，如果您有疑问、意见或是其他想法，都可以直接在我们文档中提出。\",\n" +
            "              \"pubUser\": \"LeanCloud官方客服\"\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"method\": \"POST\",\n" +
            "            \"path\": \"/1.1/classes/Post\",\n" +
            "            \"body\": {\n" +
            "              \"content\": \"很多用户表示很喜欢我们的文档风格，我们已将 LeanCloud 所有文档的 Markdown 格式的源码开放出来。\",\n" +
            "              \"pubUser\": \"LeanCloud官方客服\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }";
    JsonObject data = new JsonObject(opString);
    postWithResultArray("/1.1/batch", data, response -> {
      if (response.failed()) {
        latch.countDown();
      } else {
        System.out.println(response.result());
        JsonArray results = response.result();
        JsonArray deleteOpArray = new JsonArray();
        results.stream().forEach(json -> {
          JsonObject tmp = ((JsonObject) json).getJsonObject("success");
          if (null != tmp) {
            JsonObject deleteOne = new JsonObject().put("method", "DELETE").put("path", "/1.1/classes/Post/" + tmp.getString("objectId"));
            deleteOpArray.add(deleteOne);
          }
        });
        postWithResultArray("/1.1/batch", new JsonObject().put("requests", deleteOpArray), res2 -> {
          if (res2.succeeded()) {
            System.out.println(res2.result());
            testSuccessed = true;
          }
          latch.countDown();
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testSimpleQuery() throws Exception {
    String queryParam = "{\"pubUser\":\"LeanCloud官方客服\"}";
    get("/1.1/classes/Post", new JsonObject().put("where", queryParam), res -> {
      if (res.failed()) {
        System.out.println("failed to call simple query. cause: " + res.cause());
        latch.countDown();
      } else {
        System.out.println(res.result());
        String query2 = "{\"createdAt\":{\"$gte\":{\"__type\":\"Date\",\"iso\":\"2015-06-29T00:00:00.000Z\"},\"$lt\":{\"__type\":\"Date\",\"iso\":\"2020-06-30T00:00:00.000Z\"}}}";
        get("/1.1/classes/Post", new JsonObject().put("where", query2), res2 -> {
          if (res2.succeeded()) {
            System.out.println(res.result());
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
