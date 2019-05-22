package cn.leancloud.platform.ayers.handler;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class ObjectQueryHandlerTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testProjectParse() throws Exception {
    String keys = "content";
    List<String> attrs = new ArrayList<>();
    attrs.add("author");
    JsonObject result = ObjectQueryHandler.parseProjectParam(keys, attrs);
    assertTrue(null != result);
    assertTrue(result.size() > 2);
  }

  public void testValidateNormalQuery() throws Exception {
    String query = "{\"createdAt\":\n" +
            "  {\"$gte\":{\"__type\":\"Date\",\"iso\":\"2015-06-29T00:00:00.000Z\"},\n" +
            "  \"$lt\":{\"__type\":\"Date\",\"iso\":\"2015-06-30T00:00:00.000Z\"}},\n" +
            "\"upvotes\":{\"$in\":[1,3,5,7,9]},\n" +
            "\"pubUser\":{\"$nin\":[\"LeanCloud官方客服\"]},\n" +
            "\"upvotes\":{\"$exists\":true},\n" +
            "\"author\": \"kingdom\",\n" +
            "\"title\":{\"$regex\":\"^WTO.*\",\"$options\":\"i\"},\n" +
            "\"arrayKey\":{\"$in\":[2,3,4]},\n" +
            "\"arrayKey2\":{\"$all\":[2,3,4]},\n" +
            "\"post\":{\"__type\":\"Pointer\",\"className\":\"Post\",\"objectId\":\"558e20cbe4b060308e3eb36c\"},\n" +
            "\"content\":{\"$in\":{\"where\":{\"image\":{\"$exists\":true}},\"className\":\"Post\"}},\n" +
            "\"$or\":[{\"pubUserCertificate\":{\"$gt\":2}},{\"pubUserCertificate\":{\"$lt\":3}}]\n" +
            "}";
    ObjectQueryHandler handler = new ObjectQueryHandler(null, null);
    JsonObject parsedQuery = handler.transformSubQuery("Object", new JsonObject(query));
    System.out.println(parsedQuery);
    assertTrue(null != parsedQuery);
  }

  public void testValidateNormalSubQuery() throws Exception {
    String query = "{\n" +
            "    \"author\": {\n" +
            "      \"$select\": {\n" +
            "        \"query\": { \n" +
            "          \"className\":\"_Followee\",\n" +
            "           \"where\": {\n" +
            "             \"user\":{\n" +
            "               \"__type\": \"Pointer\",\n" +
            "               \"className\": \"_User\",\n" +
            "               \"objectId\": \"55a39634e4b0ed48f0c1845c\" \n" +
            "             }\n" +
            "           }\n" +
            "        }, \n" +
            "        \"key\":\"followee\" \n" +
            "      }\n" +
            "    },\n" +
            "\"content\":{\"$inQuery\":{\"query\":{\"where\":{\"image\":{\"$exists\":true}},\"className\":\"Post\"},\"key\":\"Post\"}}\n" +
            "  }";
    ObjectQueryHandler handler = new ObjectQueryHandler(null, null);
    JsonObject parsedQuery = handler.transformSubQuery("Object", new JsonObject(query));
    System.out.println(parsedQuery);
    assertTrue(null != parsedQuery);
  }

  public void testValidateAbnormalSubQuery() throws Exception {
    String query = "{\"createdAt\":\n" +
            "  {\"$gte\":{\"__type\":\"Date\",\"iso\":\"2015-06-29T00:00:00.000Z\"},\n" +
            "  \"$lt\":{\"__type\":\"Date\",\"iso\":\"2015-06-30T00:00:00.000Z\"}},\n" +
            "\"upvotes\":{\"$in\":[1,3,5,7,9]},\n" +
            "\"pubUser\":{\"$nin\":[\"LeanCloud官方客服\"]},\n" +
            "\"upvotes\":{\"$exists\":true},\n" +
            "\"author\": {\n" +
            "      \"$select\": {\n" +
            "        \"query\": { \n" +
            "          \"className\":\"_Followee\",\n" +
            "           \"where\": {\n" +
            "             \"user\":{\n" +
            "               \"__type\": \"Pointer\",\n" +
            "               \"className\": \"_User\",\n" +
            "               \"objectId\": \"55a39634e4b0ed48f0c1845c\" \n" +
            "             }\n" +
            "           }\n" +
            "        }, \n" +
            "        \"keys\":\"followee\" \n" +
            "      }\n" +
            "    },\n" +
            "\"title\":{\"$regex\":\"^WTO.*\",\"$options\":\"i\"},\n" +
            "\"arrayKey\":{\"$in\":[2,3,4]},\n" +
            "\"arrayKey2\":{\"$all\":[2,3,4]},\n" +
            "\"post\":{\"__type\":\"Pointer\",\"className\":\"Post\",\"objectId\":\"558e20cbe4b060308e3eb36c\"},\n" +
            "\"content\":{\"$inQuery\":{\"where\":{\"image\":{\"$exists\":true}},\"className\":\"Post\"}},\n" +
            "\"$or\":[{\"pubUserCertificate\":{\"$gt\":2}},{\"pubUserCertificate\":{\"$lt\":3}}]\n" +
            "}";
    ObjectQueryHandler handler = new ObjectQueryHandler(null, null);
    try {
      JsonObject parsedQuery = handler.transformSubQuery("Object", new JsonObject(query));
      System.out.println(parsedQuery);
      fail();
    } catch (IllegalArgumentException ex) {
      System.out.println(ex.getMessage());
    }
  }
}
