package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Optional;

public class User extends LeanObject {
  public User(JsonObject value) {
    super(Constraints.USER_CLASS, value);
  }

  public static class AuthDataParseResult {
    public boolean isMainAccount = false;
    public String currentPlatform = null;
    public String unionId = null;
    public String unionPlatform = null;
    public JsonObject query = null;
  }

  /**
   * example for authData:
   *    {"weixin": {
   *      "openid": "",
   *      "access_token": "",
   *      "expired": "",
   *      "unionid": "",
   *      "platform": "",
   *      "main_account": "",
   *    }}
   * @param authData
   * @return
   */
  public static AuthDataParseResult parseAuthData(JsonObject authData) {
    AuthDataParseResult result = new AuthDataParseResult();
    Optional<String> platform = authData.fieldNames().stream().findFirst();
    if (!platform.isPresent()) {
      logger.warn("invalid authData: empty map.");
      return result;
    }
    JsonObject authMap = authData.getJsonObject(platform.get());
    Optional<String> idOption = authMap.fieldNames().stream()
            .filter(str -> StringUtils.notEmpty(str) && !"unionid".equalsIgnoreCase(str) && str.toLowerCase().endsWith("id"))
            .findFirst();
    boolean isMainAccount = authMap.getBoolean("main_account", false);
    String unionId = authMap.getString("unionid");
    String unionPlatform = authMap.getString("platform");
    if (!idOption.isPresent()) {
      logger.warn("invalid authData: no id field found.");
      return result;
    }
    String platformIdPath = String.format("authData.%s.%s", platform.get(), idOption.get());
    JsonObject platformQuery = new JsonObject().put(platformIdPath, authMap.getString(idOption.get()));
    if (StringUtils.isEmpty(unionId)) {
      result.currentPlatform = platform.get();
      result.query = platformQuery;
      return result;
    }
    if (StringUtils.isEmpty(unionPlatform)) {
      logger.warn("invalid authData: unionid and unionplatform are required.");
      return result;
    }
    String unionIdPath = String.format("authData._%s_unionid.uid", unionPlatform);
    JsonObject unionidQuery = new JsonObject().put(unionIdPath, unionId);
    result.query = new JsonObject().put("$or", new JsonArray(Arrays.asList(unionidQuery, platformQuery)));
    result.isMainAccount = isMainAccount;
    result.unionId = unionId;
    result.unionPlatform = unionPlatform;
    result.currentPlatform = platform.get();
    return result;
  }
}
