package cn.leancloud.platform.integrate;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpStatus;

public class UserFunctionTests extends WebClientTests {
  private static final String anonymousId = StringUtils.getRandomString(24);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testLoginWithSmsCode() throws Exception {
    JsonObject data = new JsonObject().put("mobilePhoneNumber", "18700983452").put("smsCode", "123456");
    post("/1.1/login", data, response -> {
      if (response.failed()) {
        testSuccessed = true;
      } else {
        System.out.println(response.result());
        testSuccessed = response.result().size() > 0;
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testAnonymousLogin() throws Exception {
    JsonObject authRequest = new JsonObject().put("authData",
            new JsonObject().put("anonymous", new JsonObject().put("id", anonymousId)));
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
          testSuccessed = true;
        }
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testWeiboAuth() throws Exception {
    String authString = "{\n" +
            "  \"authData\": {\n" +
            "    \"weibo\": {\n" +
            "      \"uid\": \"123456789\",\n" +
            "      \"access_token\": \"2.00vs3XtCI5FevCff4981adb5jj1lXE\",\n" +
            "      \"expiration_in\": \"36000\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("updatedAt")) {
          testSuccessed = true;
        }
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testQQAuth() throws Exception {
    String authString = "{\n" +
            "  \"authData\": {\n" +
            "    \"qq\": {\n" +
            "      \"openid\": \"0395BA18A5CD6255E5BA185E7BEBA242\",\n" +
            "      \"access_token\": \"12345678-SaMpLeTuo3m2avZxh5cjJmIrAfx4ZYyamdofM7IjU\",\n" +
            "      \"expires_in\": 1382686496\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
          testSuccessed = true;
        }
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testWeixinAuth() throws Exception {
    String authString = "{\n" +
            "  \"authData\": {\n" +
            "    \"weixin\": {\n" +
            "      \"openid\": \"0395BA18A5CD6255E5BA185E7BEBA242\",\n" +
            "      \"access_token\": \"12345678-SaMpLeTuo3m2avZxh5cjJmIrAfx4ZYyamdofM7IjU\",\n" +
            "      \"expires_in\": 1382686496\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
          testSuccessed = true;
        }
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testWeixinUnionAuth() throws Exception {
    String authString = "{" +
            "  \"authData\": {\n" +
            "    \"weixin\": {\n" +
              "    \"access_token\" : \"access_token\",\n" +
              "    \"expires_in\" : 7200,\n" +
              "    \"openid\" : \"openid\",\n" +
              "    \"refresh_token\" : \"refresh_token\",\n" +
              "    \"scope\" : \"snsapi_userinfo\",\n" +
              "    \"unionid\" : \"ox7NLs-e-32ZyHg2URi_F2iPEI2U\",\n" +
              "    \"main_account\": true, \"platform\":\"weixin\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
           String firstObjectId = resultUser.getString("objectId");
           String authString2 = "{" +
                   "  \"authData\": {\n" +
                   "    \"weixin2\": {\n" +
                   "    \"access_token\" : \"access_token\",\n" +
                   "    \"expires_in\" : 7200,\n" +
                   "    \"openid\" : \"weixin2_openid\",\n" +
                   "    \"refresh_token\" : \"refresh_token\",\n" +
                   "    \"scope\" : \"snsapi_userinfo\",\n" +
                   "    \"unionid\" : \"ox7NLs-e-32ZyHg2URi_F2iPEI2U\",\n" +
                   "    \"main_account\":true, \"platform\":\"weixin\"\n" +
                   "    }\n" +
                   "  }\n" +
                   "}";
           post("/1.1/users", new JsonObject(authString2), res -> {
             if (res.succeeded()) {
               JsonObject tmpUser = res.result();
               System.out.println(tmpUser);
               if (tmpUser.containsKey("objectId") && tmpUser.containsKey("sessionToken")) {
                 testSuccessed = firstObjectId.equals(tmpUser.getString("objectId"));
               }
             }
             latch.countDown();
           });
        } else {
          latch.countDown();
        }
      } else {
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testComplexUnionIdAuth() throws Exception {
    String openId = StringUtils.getRandomString(20);
    String unionId = StringUtils.getRandomString(24);
    String authString = "{" +
            "  \"authData\": {\n" +
            "    \"weixin\": {\n" +
            "    \"access_token\" : \"access_token\",\n" +
            "    \"expires_in\" : 7200,\n" +
            "    \"openid\" : \""+ openId + "\",\n" +
            "    \"refresh_token\" : \"refresh_token\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
          String firstObjectId = resultUser.getString("objectId");
          String authString2 = "{" +
                  "  \"authData\": {\n" +
                  "    \"weixin\": {\n" +
                  "    \"access_token\" : \"access_token\",\n" +
                  "    \"expires_in\" : 7200,\n" +
                  "    \"openid\" : \""+ openId + "\",\n" +
                  "    \"refresh_token\" : \"refresh_token\",\n" +
                  "    \"scope\" : \"snsapi_userinfo\",\n" +
                  "    \"unionid\" : \"" + unionId + "\",\n" +
                  "    \"main_account\":true, \"platform\":\"weixin\"\n" +
                  "    }\n" +
                  "  }\n" +
                  "}";
          post("/1.1/users", new JsonObject(authString2), res -> {
            if (res.succeeded()) {
              JsonObject tmpUser = res.result();
              System.out.println(tmpUser);
              if (tmpUser.containsKey("objectId") && tmpUser.containsKey("sessionToken")) {
                testSuccessed = firstObjectId.equals(tmpUser.getString("objectId"));
              }
            }
            latch.countDown();
          });
        } else {
          latch.countDown();
        }
      } else {
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testMultiAccountAuth() throws Exception {
    String openId = StringUtils.getRandomString(20);
    String unionId = StringUtils.getRandomString(24);
    String authString = "{" +
            "  \"authData\": {\n" +
            "    \"weixin\": {\n" +
            "    \"access_token\" : \"access_token\",\n" +
            "    \"expires_in\" : 7200,\n" +
            "    \"openid\" : \""+ openId + "\",\n" +
            "    \"refresh_token\" : \"refresh_token\",\n" +
            "    \"scope\" : \"snsapi_userinfo\",\n" +
            "    \"unionid\" : \"" + unionId + "\",\n" +
            "    \"main_account\":true, \"platform\":\"weixin\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
          String firstObjectId = resultUser.getString("objectId");
          String authString2 = "{" +
                  "  \"authData\": {\n" +
                  "    \"weixin2\": {\n" +
                  "    \"access_token\" : \"access_token\",\n" +
                  "    \"expires_in\" : 7200,\n" +
                  "    \"openid\" : \""+ openId + "\",\n" +
                  "    \"refresh_token\" : \"refresh_token\",\n" +
                  "    \"scope\" : \"snsapi_userinfo\",\n" +
                  "    \"unionid\" : \"" + unionId + "\",\n" +
                  "    \"main_account\":false, \"platform\":\"weixin\"\n" +
                  "    }\n" +
                  "  }\n" +
                  "}";
          post("/1.1/users", new JsonObject(authString2), res -> {
            if (res.succeeded()) {
              JsonObject tmpUser = res.result();
              System.out.println(tmpUser);
              if (tmpUser.containsKey("objectId") && tmpUser.containsKey("sessionToken")) {
                testSuccessed = firstObjectId.equals(tmpUser.getString("objectId"));
              }
            }
            latch.countDown();
          });
        } else {
          latch.countDown();
        }
      } else {
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testOtherAuth() throws Exception {
    String authString = "{\n" +
            "  \"authData\": {\n" +
            "    \"facebook\": {\n" +
            "      \"id\": \"0395BA18A5CD6255E5BA185E7BEBA242\",\n" +
            "      \"access_token\": \"在第三方平台的 access token\",\n" +
            "      \"expires_in\": 1382686496\n" +
            "    }\n" +
            "  }\n" +
            "}";
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, response -> {
      if (response.succeeded()) {
        JsonObject resultUser = response.result();
        System.out.println(resultUser);
        if (resultUser.containsKey("objectId") && resultUser.containsKey("sessionToken") && resultUser.containsKey("createdAt")) {
          testSuccessed = true;
        }
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testVerifyEmail() throws Exception {
    get("/emailVerify/5ce3f0947b968a00730fbfca", null, res -> {
      if (res.succeeded()) {
        testSuccessed = true;
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }
}
