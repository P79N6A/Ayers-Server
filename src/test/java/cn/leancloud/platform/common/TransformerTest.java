package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

public class TransformerTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testConvert2BsonCreate() throws Exception {
    String param = "{\"birthday\":{\"iso\":\"2019-04-12T07:04:35.016Z\",\"__type\":\"Date\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"},\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.CREATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonObject("birthday").getString("$date").length() > 0);
    assertTrue(newObj.getJsonObject("favorite").getString("$id").equals("5cb03965c3a4593f60745a1a"));
  }

  public void testConvertRole2BsonUpdate() throws Exception {
    String param = "{ \"_id\" : \"5cb4458ea8cb3d591a16308e\"," +
            " \"createdAt\" : \"2019-04-15T08:49:18.633Z\"," +
            " \"roles\" : { \"__op\" : \"AddRelation\", \"objects\" : [ { \"__type\" : \"Pointer\", \"className\" : \"_Role\", \"objectId\" : \"55a48351e4b05001a774a89f\" } ] }," +
            " \"name\" : \"CLevel\"," +
            " \"ACL\" : { \"*\" : { \"read\" : true } }," +
            " \"users\" : { \"__op\" : \"AddRelation\", \"objects\" : [ { \"__type\" : \"Pointer\", \"className\" : \"_User\", \"objectId\" : \"55a47496e4b05001a7732c5f\" } ] }," +
            " \"updatedAt\" : \"2019-04-15T08:49:18.633Z\" }";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.UPDATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonObject("$set").size() == 5);
    assertTrue(newObj.getJsonObject("$push").size() == 2);
    assertTrue(newObj.getJsonObject("$push").getJsonObject("roles").getJsonArray("$each").size() == 1);
    assertTrue(newObj.getJsonObject("$push").getJsonObject("users").getJsonArray("$each").size() == 1);
  }

  public void testConvertPointerArray2BsonCreate() throws Exception {
    String param = "{\"birthday\":{\"iso\":\"2019-04-12T07:04:35.016Z\",\"__type\":\"Date\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":[{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"},{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"},{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"}],\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.CREATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonArray("favorite").size() == 3);
  }

  public void testConvertGeoPoint2BsonUpdate() throws Exception {
    String param = "{\"location\":{\"$nearSphere\":{\"__type\":\"GeoPoint\",\"latitude\":39.9,\"longitude\":116.4}}}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.UPDATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonObject("$set").getJsonObject("location").getJsonObject("$nearSphere").getJsonArray("coordinates").size() == 2);
  }

  public void testConvertGeoPoint2BsonQuery() throws Exception {
    String param = "{\"location\":{\"$nearSphere\":{\"__type\":\"GeoPoint\",\"latitude\":39.9,\"longitude\":116.4}}}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.QUERY);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonObject("location").getJsonObject("$nearSphere").getJsonArray("coordinates").size() == 2);
  }

  public void testConvertComplexQuery() throws Exception {
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
            "        \"key\":\"followee\" \n" +
            "      }\n" +
            "    },\n" +
            "\"title\":{\"$regex\":\"^WTO.*\",\"$options\":\"i\"},\n" +
            "\"arrayKey\":{\"$in\":[2,3,4]},\n" +
            "\"arrayKey2\":{\"$all\":[2,3,4]},\n" +
            "\"post\":{\"__type\":\"Pointer\",\"className\":\"Post\",\"objectId\":\"558e20cbe4b060308e3eb36c\"},\n" +
            "\"content\":{\"$inQuery\":{\"where\":{\"image\":{\"$exists\":true}},\"className\":\"Post\"}},\n" +
            "\"$or\":[{\"pubUserCertificate\":{\"$gt\":2}},{\"pubUserCertificate\":{\"$lt\":3}}]\n" +
            "}";
    JsonObject paramObj = new JsonObject(query);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.QUERY);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonObject("createdAt").getJsonObject("$gte").getString("$date").equals("2015-06-29T00:00:00.000Z"));
    assertTrue(newObj.getJsonObject("post").getString("$id").equals("558e20cbe4b060308e3eb36c"));
  }

  public void testConvert2Rest() throws Exception {
    String param = "{\"birthday\":{\"$date\" : \"2019-04-12T10:34:52.274Z\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"},\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.decodeBsonObject(paramObj);
    System.out.println(newObj.toString());
  }

  public void testConvertRefArray2Rest() throws Exception {
    String param = "{\"birthday\":{\"$date\" : \"2019-04-12T10:34:52.274Z\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":[{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"},{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"},{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"}],\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.decodeBsonObject(paramObj);
    System.out.println(newObj.toString());
  }

  public void testConvertRestParam2BsonUpdate() throws Exception {
    String param = "{ \"roles\": {\n" +
            "          \"__op\": \"AddRelation\",\n" +
            "          \"objects\": [\n" +
            "            {\n" +
            "              \"__type\": \"Pointer\",\n" +
            "              \"className\": \"_Role\",\n" +
            "              \"objectId\": \"55a48351e4b05001a774a89f\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "  \"users\": {\n" +
            "          \"__op\": \"RemoveRelation\",\n" +
            "          \"objects\": [\n" +
            "            {\n" +
            "              \"__type\": \"Pointer\",\n" +
            "              \"className\": \"_User\",\n" +
            "              \"objectId\": \"55a47496e4b05001a7732c5f\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "  \"parents\": {\n" +
            "          \"__op\": \"AddUnique\",\n" +
            "          \"objects\": [\n" +
            "            {\n" +
            "              \"__type\": \"Pointer\",\n" +
            "              \"className\": \"_User\",\n" +
            "              \"objectId\": \"55a47496e4b05001a7732c5f\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "  \"tags\":{\"__op\":\"AddUnique\",\"objects\":[\"Frontend\",\"JavaScript\"]},\n" +
            "  \"upvotes\":{\"__op\":\"Increment\",\"amount\":1},\n" +
            "  \"flags\":{\"__op\":\"BitOr\",\"value\": 4},\n" +
            "  \"balance\":{\"__op\":\"Decrement\",\"amount\": 30},\n" +
            "  \"nickname\":{\"__op\":\"Delete\"},\n" +
            "  \"favor\": {\"__op\":\"Remove\",\"objects\":[\"Frontend\",\"JavaScript\"]}, \n" +
            " \"familyName\": \"Stark\"}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.CREATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonArray("roles").size() == 1);
    assertTrue(!newObj.containsKey("users"));
    assertTrue(newObj.getJsonArray("tags").size() == 2);
    assertTrue(newObj.getInteger("upvotes") == 1);
    assertTrue(newObj.getString("familyName").equals("Stark"));

    System.out.println("try to convert for Update...");
    JsonObject updateObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.UPDATE);
    System.out.println(updateObj.toString());
    assertTrue(updateObj.getJsonObject("$unset").getString("nickname").length() == 0);
    assertTrue(updateObj.getJsonObject("$inc").getInteger("balance") == -30);
    assertTrue(updateObj.getJsonObject("$inc").getInteger("upvotes") == 1);
    assertTrue(updateObj.getJsonObject("$pullAll").size() == 2);
    assertTrue(updateObj.getJsonObject("$push").size() == 1);
    assertTrue(updateObj.getJsonObject("$addToSet").size() == 2);
    assertTrue(updateObj.getJsonObject("$set").size() == 1);
    assertTrue(updateObj.getJsonObject("$set").getString("familyName").equals("Stark"));
    assertTrue(updateObj.getJsonObject("$bit").getJsonObject("flags").getInteger("or") == 4);
  }

  public void testConvertRestParamWithDuplicatedEle2BsonUpdate() throws Exception {
    String param = "{\"parents\": {\n" +
            "          \"__op\": \"AddUnique\",\n" +
            "          \"objects\": [\n" +
            "            {\n" +
            "              \"__type\": \"Pointer\",\n" +
            "              \"className\": \"_User\",\n" +
            "              \"objectId\": \"55a47496e4b05001a7732c5f\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"__type\": \"Pointer\",\n" +
            "              \"className\": \"_User\",\n" +
            "              \"objectId\": \"55a47496e4b05001a7732c5f\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"__type\": \"Pointer\",\n" +
            "              \"className\": \"_User\",\n" +
            "              \"objectId\": \"55a47496e4b05001a9932c5f\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "  \"tags\":{\"__op\":\"AddUnique\",\"objects\":[\"Frontend\",\"Frontend\",\"Frontend\",\"JavaScript\"]}\n" +
            "}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.CREATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonArray("tags").size() == 2);
    assertTrue(newObj.getJsonArray("parents").size() == 2);

    System.out.println("try to convert for Update...");
    JsonObject updateObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.UPDATE);
    System.out.println(updateObj.toString());
    assertTrue(updateObj.getJsonObject("$addToSet").size() == 2);
    assertTrue(updateObj.getJsonObject("$addToSet").getJsonObject("parents").getJsonArray("$each").size() == 2);
    assertTrue(updateObj.getJsonObject("$addToSet").getJsonObject("tags").getJsonArray("$each").size() == 2);
  }

  public void testEncodeNullValue() throws Exception {
    String param = "{\"class\":\"_File\",\"param\":{\"key\":\"b2d5cb4e988cd6f19198.js\",\"name\":\"stream-test.js\",\"metaData\":{\"owner\":\"unknown\"},\"url\":\"http://lc-ohqhxu3m.cn-n1.lcfile.com/b2d5cb4e988cd6f19198.js\",\"mime_type\":null,\"provider\":\"qiniu\",\"bucket\":\"ohqhxu3m\"}}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.UPDATE);
    System.out.println(newObj.toString());
    assertTrue(newObj.getJsonObject("$set").getJsonObject("param").getValue("mime_type") == null);
    assertTrue(newObj.getJsonObject("$set").getJsonObject("param").getString("provider").equals("qiniu"));
  }

  public void testJsonMerge() throws Exception {
    String input = "{\"name\":\"hallo\", \"first\": null}";
    JsonObject paramObj = new JsonObject(input);
    JsonObject createObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.CREATE);
    System.out.println(createObj.toString());
    assertTrue(createObj.getValue("first") == null);
    JsonObject newObj = Transformer.encode2BsonRequest(paramObj, Transformer.REQUEST_OP.UPDATE);
    System.out.println(newObj.toString());
    assertTrue(null == newObj.getJsonObject("$set").getValue("first"));
  }
}
