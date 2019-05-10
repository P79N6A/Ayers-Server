package cn.leancloud.platform.utils;

import cn.leancloud.platform.utils.JsonFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JsonFactoryTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testMapToJsonObject() throws Exception {
    Map<String, Object> input = new HashMap<>();
    input.put("name", "Nicole");
    input.put("age", 10);
    JsonObject json = Stream.of(input.entrySet()).flatMap(entrySet -> entrySet.stream()).collect(JsonFactory.toJsonObject());
    System.out.println(json);
  }

  public void testJsonToJsonObject() throws Exception {
    JsonObject input = new JsonObject();
    input.put("name", "Nicole");
    input.put("age", 10);
    input.put("location", new JsonObject().put("province", "beijing").put("city", "beijing"));
    Object nullObj = null;
    input.put("favorite", nullObj);
    JsonObject res = input.stream().collect(JsonFactory.toJsonObject());
    System.out.println(res);
  }

  public void testGetJsonObject() throws Exception {
    JsonObject tmp = new JsonObject();
    tmp.put("address", new JsonObject().put("province", new JsonObject().put("city", new JsonObject().put("name", "beijing"))));
    JsonObject beijing = JsonUtils.getJsonObject(tmp, "address.province.city");
    assertTrue(null != beijing);
    assertTrue(beijing.getString("name").equals("beijing"));
    JsonObject shanghai = JsonUtils.getJsonObject(tmp, "address.city.town.district");
    assertTrue(null == shanghai);

    JsonObject capital = new JsonObject().put("name", "beijing").put("area", 4223432).put("population", 432423212);
    boolean replaceResult = JsonUtils.replaceJsonValue(tmp, "address.city.town", capital);
    assertTrue(!replaceResult);
    replaceResult = JsonUtils.replaceJsonValue(tmp, "address.province.city", capital);
    assertTrue(replaceResult);
    replaceResult = JsonUtils.replaceJsonValue(tmp, "address.province.city.area", new JsonObject().put("long", 43.43));
    assertTrue(replaceResult);

    beijing = JsonUtils.getJsonObject(tmp, "address.province.city");
    assertTrue(null != beijing);
    assertTrue(beijing.getInteger("population") == 432423212);
  }

  public void testDeepMerge() throws Exception {
    String oldSchema = "{ \"updatedAt\" : { \"type\" : \"Date\" }, \"_r\" : { \"type\" : \"Array\" }, \"ACL\" : { \"type\" : \"ACL\" }," +
            " \"objectId\" : { \"type\" : \"String\" }, \"inboxType\" : { \"type\" : \"String\" }, " +
            " \"createdAt\" : { \"type\" : \"Date\" }, \"messages\" : { \"type\" : \"Array\" }," +
            " \"_w\" : { \"type\" : \"Array\" }, \"sequence\" : { \"type\" : \"Number\" }, \"owner\" : { \"type\" : \"Any\" } }";
    String newSchema = "{ \"name\" : { \"type\" : \"String\" }, \"_r\" : { \"type\" : \"Array\" }, \"ACL\" : { \"type\" : \"ACL\" }," +
            " \"_w\" : { \"type\" : \"Array\" }, \"verified\" : { \"type\" : \"Boolean\" }, \"owner\" : { \"type\" : \"Any\" } }";
    JsonObject left = new JsonObject(newSchema);
    JsonObject right = new JsonObject(oldSchema);
    JsonObject result = JsonUtils.deepMergeIn(left, right);
    System.out.println(Json.encodePrettily(result));
    assertTrue(null != result);
    assertTrue(result.getJsonObject("name").getString("type").equals("String"));
    assertTrue(result.getJsonObject("objectId").getString("type").equals("String"));
    assertTrue(result.getJsonObject("verified").getString("type").equals("Boolean"));
    assertTrue(result.getJsonObject("messages").getString("type").equals("Array"));

    oldSchema = "{ \"salt\" : { \"type\" : \"String\" }, \"email\" : { \"type\" : \"String\" }, \"sessionToken\" : { \"type\" : \"String\" }," +
            " \"updatedAt\" : { \"type\" : \"Date\" }, \"_r\" : { \"type\" : \"Array\" }, \"ACL\" : { \"type\" : \"ACL\" }," +
            " \"password\" : { \"type\" : \"String\" }, \"objectId\" : { \"type\" : \"String\" }, \"username\" : { \"type\" : \"String\" }," +
            " \"createdAt\" : { \"type\" : \"Date\" }, \"emailVerified\" : { \"type\" : \"Boolean\" }, \"_w\" : { \"type\" : \"Array\" }," +
            " \"mobilePhoneNumber\" : { \"type\" : \"String\" }, \"authData\" : { \"type\" : \"Object\", \"user_private\" : true }," +
            " \"mobilePhoneVerified\" : { \"type\" : \"Boolean\" }, \"comment\" : { \"type\" : \"File\", \"v\" : 2 } }";
    newSchema = "{\n" +
            "  \"username\" : {\n" +
            "    \"type\" : \"String\"\n" +
            "  },\n" +
            "  \"password\" : {\n" +
            "    \"type\" : \"String\"\n" +
            "  },\n" +
            "  \"mobilePhoneNumber\" : {\n" +
            "    \"type\" : \"String\"\n" +
            "  },\n" +
            "  \"authData\" : {\n" +
            "    \"type\" : \"Object\",\n" +
            "    \"schema\" : {\n" +
            "      \"anonymous\" : {\n" +
            "        \"type\" : \"Object\",\n" +
            "        \"schema\" : {\n" +
            "          \"id\" : {\n" +
            "            \"type\" : \"String\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    result = JsonUtils.deepMergeIn(new JsonObject(newSchema), new JsonObject(oldSchema));
    System.out.println(Json.encodePrettily(result));
    assertTrue(null != result);
    assertTrue(result.getJsonObject("authData").getString("type").equals("Object"));
    assertTrue(result.getJsonObject("authData").getBoolean("user_private"));
    assertTrue(result.getJsonObject("authData").getJsonObject("schema").getJsonObject("anonymous").getString("type").equals("Object"));
    assertTrue(result.getJsonObject("username").getString("type").equals("String"));
    assertTrue(result.getJsonObject("createdAt").getString("type").equals("Date"));
    assertTrue(result.getJsonObject("ACL").getString("type").equals("ACL"));
  }
}
