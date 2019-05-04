package cn.leancloud.platform.modules;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.List;

public class SchemaTests extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testAuthDataSchema() throws Exception {
    String userJsonString = "{" +
            "\"username\":\"hjiang001\",\n" +
            "\"password\":\"f32@ds*@&dsa\",\n" +
            "\"mobilePhoneNumber\":\"18612340000\",\n" +
            "\"authData\": {\n" +
            "  \"weixin\": {\n" +
            "    \"openid\": \"openid\",\n" +
            "    \"access_token\": \"access token\",\n" +
            "    \"expired\": \"expiredafe\",\n" +
            "    \"unionid\": \"faheifeunio\",\n" +
            "    \"platform\": \"weixin\",\n" +
            "    \"main_account\": \"true\"\n" +
            "  },\n" +
            " \"wqq\": {\n" +
            "    \"openid\": \"openidafhie\",\n" +
            "    \"access_token\": \"fearewesf\",\n" +
            "    \"expired\": \"faefwerewfdf\",\n" +
            "    \"unionid\": \"afhiefneiekke\",\n" +
            "    \"platform\": \"weixin\",\n" +
            "    \"main_account\": \"false\"\n" +
            "  }" +
            " }" +
            "}";
    JsonObject userJson = new JsonObject(userJsonString);
    LeanObject userObj = new LeanObject("_User", userJson);
    Schema userSchema = userObj.guessSchema();
    System.out.println(Json.encodePrettily(userSchema));
    assertTrue(null != userSchema);
    List<String> authDataIndex = userSchema.findAuthDataIndex();
    authDataIndex.stream().forEach(s -> System.out.println(s));
    assertTrue(authDataIndex.size() == 2);
  }

  public void testNoneAuthDataSchema() throws Exception {
    String userJsonString = "{" +
            "\"username\":\"hjiang001\",\n" +
            "\"password\":\"f32@ds*@&dsa\",\n" +
            "\"mobilePhoneNumber\":\"18612340000\"\n" +
            "}";
    JsonObject userJson = new JsonObject(userJsonString);
    LeanObject userObj = new LeanObject("_User", userJson);
    Schema userSchema = userObj.guessSchema();
    System.out.println(Json.encodePrettily(userSchema));
    assertTrue(null != userSchema);
    List<String> authDataIndex = userSchema.findAuthDataIndex();
    authDataIndex.stream().forEach(s -> System.out.println(s));
    assertTrue(authDataIndex.size() == 0);
  }

  public void testAnonymousAuthDataSchema() throws Exception {
    String userJsonString = "{" +
            "\"username\":\"hjiang001\",\n" +
            "\"password\":\"f32@ds*@&dsa\",\n" +
            "\"mobilePhoneNumber\":\"18612340000\",\n" +
            "\"authData\": {\n" +
            "  \"anonymous\": {\n" +
            "    \"id\": \"openid\"\n" +
            "  }" +
            " }" +
            "}";
    JsonObject userJson = new JsonObject(userJsonString);
    LeanObject userObj = new LeanObject("_User", userJson);
    Schema userSchema = userObj.guessSchema();
    System.out.println(Json.encodePrettily(userSchema));
    assertTrue(null != userSchema);
    List<String> authDataIndex = userSchema.findAuthDataIndex();
    authDataIndex.stream().forEach(s -> System.out.println(s));
    assertTrue(authDataIndex.size() == 1);
  }

}
