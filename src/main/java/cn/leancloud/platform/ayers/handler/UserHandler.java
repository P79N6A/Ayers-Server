package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.cache.UnifiedCache;
import cn.leancloud.platform.codec.Base64;
import cn.leancloud.platform.codec.MessageDigest;
import cn.leancloud.platform.common.*;
import cn.leancloud.platform.modules.ACL;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.modules.ObjectSpecifics;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserHandler extends CommonHandler {
  public static final String PARAM_USERNAME = "username";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_EMAIL = "email";
  public static final String PARAM_MOBILEPHONE = "mobilePhoneNumber";
  public static final String PARAM_SMSCODE = "smsCode";
  public static final String PARAM_SESSION_TOKEN = "session_token";
  public static final String PARAM_AUTH_DATA = "authData";
  public static final String PARAM_AUTH_ANONYMOUS = "anonymous";

  public UserHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public static String hashPassword(String password, String salt) {
    String newPassword = MessageDigest.compute(MessageDigest.ALGORITHM_SHA256, salt+ password);
    for (int i = 0; i < 256; i++) {
      newPassword = MessageDigest.compute(MessageDigest.ALGORITHM_SHA256, newPassword);
    }
    return new String(Base64.encode(newPassword));
  }

  public static JsonObject getUserDefaultACL() {
    return ACL.publicRWInstance().toJson();
  }

  /**
   * support signin with:
   *   1. username + password
   *   2. mobilePhoneNumber + password
   *   3. mobilePhoneNumber + smscode
   *   4. email + password
   *   5. session_token
   *   6. authdata(anonymous or thirdparty)
   * @param param
   * @return
   */
  public static CommonResult parseSigninParam(JsonObject param) {
    CommonResult result = new CommonResult();
    if (null == param) {
      result.setSuccess(false);
      result.setMessage("parameter is null.");
    } else {
      String username = param.getString(PARAM_USERNAME);
      String password = param.getString(PARAM_PASSWORD);
      String email = param.getString(PARAM_EMAIL);
      String mobile = param.getString(PARAM_MOBILEPHONE);
      String smsCode = param.getString(PARAM_SMSCODE);
      String sessionToken = param.getString(PARAM_SESSION_TOKEN);
      JsonObject authData = param.getJsonObject(PARAM_AUTH_DATA);

      boolean emailPassSelected = !StringUtils.isEmpty(email) && !StringUtils.isEmpty(password);
      boolean usernamePassSelected = !StringUtils.isEmpty(username) && !StringUtils.isEmpty(password);
      boolean mobilePassSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(password);
      boolean mobileSmscodeSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(smsCode);
      boolean sessionTokenSelected = !StringUtils.isEmpty(sessionToken);
      boolean thirdPartyLogin = authData != null && authData.size() > 0;

      if (!emailPassSelected && !mobilePassSelected && !usernamePassSelected && !mobileSmscodeSelected
              && !sessionTokenSelected && !thirdPartyLogin) {
        result.setSuccess(false);
        result.setMessage(ErrorCodes.INVALID_PARAMETER.getMessage());
      } else {
        JsonObject signJson = new JsonObject();
        if (usernamePassSelected) {
          signJson.put(PARAM_USERNAME, username);
          signJson.put(PARAM_PASSWORD, password);
        } else if (mobilePassSelected) {
          signJson.put(PARAM_MOBILEPHONE, mobile);
          signJson.put(PARAM_PASSWORD, password);
        } else if (mobileSmscodeSelected) {
          signJson.put(PARAM_MOBILEPHONE, mobile);
          signJson.put(PARAM_SMSCODE, smsCode);
        } else if (emailPassSelected) {
          signJson.put(PARAM_EMAIL, email);
          signJson.put(PARAM_PASSWORD, password);
        } else if (thirdPartyLogin) {
          signJson.put(PARAM_AUTH_DATA, authData);
        } else {
          signJson.put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, sessionToken);
        }
        result.setObject(signJson);
      }
    }
    return result;
  }

  /**
   * support signup with:
   *   1. username + password
   *   2. mobilePhoneNumber + password
   *   3. mobilePhoneNumber + smscode
   *   4. authdata(anonymous or thirdparty)
   * @param param
   * @return
   */
  public static CommonResult parseSignupParam(JsonObject param) {
    CommonResult result = new CommonResult();
    if (null == param) {
      result.setSuccess(false);
      result.setMessage("parameter is null.");
    } else {
      String username = param.getString(PARAM_USERNAME);
      String password = param.getString(PARAM_PASSWORD);
      String email = param.getString(PARAM_EMAIL);
      String mobile = param.getString(PARAM_MOBILEPHONE);
      String smsCode = param.getString(PARAM_SMSCODE);
      JsonObject authData = param.getJsonObject(PARAM_AUTH_DATA);

      boolean usernamePassSelected = !StringUtils.isEmpty(username) && !StringUtils.isEmpty(password);
      boolean mobileSmscodeSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(smsCode);
      boolean mobilePassSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(password);
      boolean thirdPartyLogin = authData != null && authData.size() > 0;

      if (!usernamePassSelected && !mobileSmscodeSelected && !mobilePassSelected && !thirdPartyLogin) {
        result.setSuccess(false);
        result.setMessage(ErrorCodes.INVALID_PARAMETER.getMessage());
      } else if (StringUtils.notEmpty(email) && !ObjectSpecifics.validateEmail(email)) {
        result.setSuccess(false);
        result.setMessage(ErrorCodes.INVALID_EMAIL.getMessage());
      } else {
        JsonObject data = new JsonObject(param.getMap());
        result.setObject(data);

        if (usernamePassSelected || mobilePassSelected) {
          String salt = StringUtils.getRandomString(Constraints.SALT_LENGTH);
          String hashPassword = hashPassword(password, salt);
          data.put(PARAM_PASSWORD, hashPassword);
          data.put(LeanObject.BUILTIN_ATTR_SALT, salt);
        }
        data.put(LeanObject.BUILTIN_ATTR_ACL, getUserDefaultACL());
        data.put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, StringUtils.getRandomString(Constraints.SESSION_TOKEN_LENGTH));
        data.put(LeanObject.BUILTIN_ATTR_EMAIL_VERIFIED, false);
        data.put(LeanObject.BUILTIN_ATTR_PHONENUM_VERIFIED, false);
      }
    }
    return result;
  }

  public void signup(JsonObject param, Handler<AsyncResult<JsonObject>> handler) {
    String operation = RequestParse.OP_USER_SIGNUP;
    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(this.routingContext);
    sendDataOperationWithOption(Constraints.USER_CLASS, null, operation, null, param, true, headers, res -> {
      if (res.succeeded() && null != res.result()) {
        JsonObject user = res.result();
        UnifiedCache.getGlobalInstance().put(user.getString(LeanObject.BUILTIN_ATTR_SESSION_TOKEN), user);
      }
      handler.handle(res);
    });
  }

  public void signin(JsonObject param, Handler<AsyncResult<JsonObject>> handler) {
    String operation = RequestParse.OP_USER_SIGNIN;
    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(this.routingContext);
    sendDataOperationWithOption(Constraints.USER_CLASS, null, operation, null, param, true, headers, res -> {
      if (res.succeeded() && null != res.result()) {
        JsonObject user = res.result();
        UnifiedCache.getGlobalInstance().put(user.getString(LeanObject.BUILTIN_ATTR_SESSION_TOKEN), user);
      }
      handler.handle(res);
    });
  }

  public void validateSessionToken(String sessionToken, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject body = new JsonObject().put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, sessionToken);
    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(this.routingContext);
    sendDataOperation(Constraints.USER_CLASS, null, HttpMethod.GET.toString(), body, null, headers, res -> {
      if (res.succeeded() && null != res.result()) {
        JsonObject user = res.result();
        UnifiedCache.getGlobalInstance().put(user.getString(LeanObject.BUILTIN_ATTR_SESSION_TOKEN), user);
      }
      handler.handle(res);
    });
  }

  public void updateSessionToken(String objectId, String sessionToken, String newSessionToken, Handler<AsyncResult<JsonObject>> handler) {
    // findMetaInfo and updateSingleObject.
    JsonObject query = new JsonObject().put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, sessionToken).put(LeanObject.ATTR_NAME_OBJECTID, objectId);
    JsonObject update = new JsonObject().put(LeanObject.BUILTIN_ATTR_SESSION_TOKEN, newSessionToken);
    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(this.routingContext);

    sendDataOperationWithOption(Constraints.USER_CLASS, objectId, HttpMethod.PUT.toString(), query, update, true, headers, res -> {
      if (res.succeeded() && null != res.result()) {
        JsonObject user = res.result();
        UnifiedCache.getGlobalInstance().put(user.getString(LeanObject.BUILTIN_ATTR_SESSION_TOKEN), user);
      }
      handler.handle(res);
    });
  }


}
