package cn.leancloud.platform.integrate;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

  public void testPreIncludeQuery() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    String ReviewerFormat = "{\"name\":\"review no. %d\", \"tags\":[\"%s\", \"%s\"]}";
    String[] tags = new String[]{"designs", "read", "story", "web", "best", "comments", "Dragon Ball",
            "restful", "fruits", "layout", "sprite", "compare", "draw", "large", "think", "expensive"};

    String commentSourceText = "Tag clouds are typically represented using inline HTML elements. The tags can appear in alphabetical order, in a random order, they can be sorted by weight, and so on. Sometimes, further visual properties are manipulated in addition to font size, such as the font color, intensity, or weight.[18] Most popular is a rectangular tag arrangement with alphabetical sorting in a sequential line-by-line layout. The decision for an optimal layout should be driven by the expected user goals.[18] Some prefer to cluster the tags semantically so that similar tags will appear near each other[19][20][21] or use embedding techniques such as tSNE to position words.[12] Edges can be added to emphasize the co-occurrences of tags and visualize interactions.[12] Heuristics can be used to reduce the size of the tag cloud whether or not the purpose is to cluster the tags.[20]\n" +
            "\n" +
            "Tag cloud visual taxonomy is determined by a number of attributes: tag ordering rule (e.g. alphabetically, by importance, by context, randomly, ordered for visual quality), shape of the entire cloud (e.g. rectangular, circle, given map borders), shape of tag bounds (rectangle, or character body), tag rotation (none, free, limited), vertical tag alignment (sticking to typographical baselines, free). A tag cloud on the web must address problems of modeling and controlling aesthetics, constructing a two-dimensional layout of tags, and all these must be done in short time on volatile browser platform. Tags clouds to be used on the web must be in HTML, not graphics, to make them robot-readable, they must be constructed on the client side using the fonts available in the browser, and they must fit in a rectangular box";
    int commentSourceLength = commentSourceText.length();

    int tag_len = tags.length;
    int reviewerCount = 1;
    int CommentCount = 2;

    List<String> reviewerIds = new ArrayList<>(reviewerCount);

    List<Future> reviewFutures = new ArrayList<>(reviewerCount);
    for (int i = 0; i < reviewerCount; i++) {
      Future tmp = Future.future();
      reviewFutures.add(tmp);
      String tag1 = tags[rand.nextInt(tag_len)];
      String tag2 = tags[rand.nextInt(tag_len)];
      String reviewer = String.format(ReviewerFormat, i, tag1, tag2);
      post("/1.1/classes/Reviewer", new JsonObject(reviewer), res -> {
        if (res.failed()) {
          res.cause().printStackTrace();
          tmp.fail(res.cause());
        } else {
          reviewerIds.add(res.result().getString("objectId"));
          tmp.complete();
        }
      });
    }
    CompositeFuture.all(reviewFutures).setHandler(res -> {
      if (res.failed()) {
        res.cause().printStackTrace();
        latch.countDown();
      } else {
        // do next.
        List<Future> commentFutures = new ArrayList<>(CommentCount);
        for (int i = 0;i < CommentCount;i++) {
          int reviewerIndex = rand.nextInt(reviewerCount);
          int textBegin = rand.nextInt(commentSourceLength);
          int textEnd = rand.nextInt(commentSourceLength);
          if (textBegin > textEnd) {
            int t = textBegin;
            textBegin = textEnd;
            textEnd = t;
          }
          if (textEnd - textBegin > 30) {
            textEnd = textBegin + 30;
          }
          String slice = commentSourceText.substring(textBegin, textEnd);
          String oneObjectId = reviewerIds.get(reviewerIndex);
          JsonObject reviewJson = new JsonObject().put("__type", "Pointer").put("className", "Reviewer").put("objectId", oneObjectId);
          Future tmp = Future.future();
          commentFutures.add(tmp);
          post("/1.1/classes/Comment", new JsonObject().put("content", slice).put("author", new JsonObject().put("ptr", reviewJson)), res2 -> {
            if (res2.failed()) {
              res2.cause().printStackTrace();
              tmp.fail(res2.cause());
            } else {
              tmp.complete();
            }
          });
        }
        CompositeFuture.all(commentFutures).setHandler(fin -> {
          latch.countDown();
          testSuccessed = fin.succeeded();
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testPostFileObject() throws Exception {
    String json = "{\"metaData\": {\"owner\": \"unknown\", \"__source\": \"external\"},\n" +
            "\"name\": \"screen.jpg\",\n" +
            "\"url\": \"http://i1.wp.com/blog.avoscloud.com/wp-content/uploads/2014/05/screen568x568-1.jpg?resize=202%2C360\"}";
    post("/1.1/files/screen.jpg", new JsonObject(json), res -> {
      if (res.failed()) {
        System.out.println(res.cause().getMessage());
        latch.countDown();
      } else {
        testSuccessed = true;
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testIncludeQuery() throws Exception {
    JsonObject where = new JsonObject().put("writer", new JsonObject().put("$exists", true));
    JsonObject queryParam = new JsonObject();
    queryParam.put("limit", "5");
    queryParam.put("where", where.toString());
    queryParam.put("keys", "content");
    queryParam.put("include", "author,writer.ptr");
    queryParam.put("order", "updatedAt");
    get("/1.1/classes/Comment", queryParam, res -> {
      if (res.failed()) {
        System.out.println(res.cause().getMessage());
        latch.countDown();
      } else {
        JsonArray results = res.result().getJsonArray("results");
        testSuccessed = results.size() > 0;
        results.stream().forEach(a -> System.out.println(a));
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testSubQuery() throws Exception {

  }
}
