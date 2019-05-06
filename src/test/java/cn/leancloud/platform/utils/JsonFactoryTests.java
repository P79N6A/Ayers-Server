package cn.leancloud.platform.utils;

import cn.leancloud.platform.utils.JsonFactory;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JsonFactoryTests extends TestCase {
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
    JsonObject beijing = JsonFactory.getJsonObject(tmp, "address.province.city");
    assertTrue(null != beijing);
    assertTrue(beijing.getString("name").equals("beijing"));
    JsonObject shanghai = JsonFactory.getJsonObject(tmp, "address.city.town.district");
    assertTrue(null == shanghai);

    JsonObject capital = new JsonObject().put("name", "beijing").put("area", 4223432).put("population", 432423212);
    boolean replaceResult = JsonFactory.replaceJsonValue(tmp, "address.city.town", capital);
    assertTrue(!replaceResult);
    replaceResult = JsonFactory.replaceJsonValue(tmp, "address.province.city", capital);
    assertTrue(replaceResult);
    replaceResult = JsonFactory.replaceJsonValue(tmp, "address.province.city.area", new JsonObject().put("long", 43.43));
    assertTrue(replaceResult);

    beijing = JsonFactory.getJsonObject(tmp, "address.province.city");
    assertTrue(null != beijing);
    assertTrue(beijing.getInteger("population") == 432423212);
  }
}
