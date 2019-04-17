package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.codec.Base64;
import cn.leancloud.platform.codec.MessageDigest;
import cn.leancloud.platform.common.CommonResult;
import cn.leancloud.platform.common.ObjectSpecifics;
import cn.leancloud.platform.common.StringUtils;
import cn.leancloud.platform.modules.ACL;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserHandler {
  public static final String PARAM_USERNAME = "username";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_EMAIL = "email";
  public static final String PARAM_MOBILEPHONE = "mobilePhoneNumber";
  public static final String PARAM_SMSCODE = "smsCode";
  public static final String PARAM_SESSION_TOKEN = "session_token";

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

  public static CommonResult fillSigninParam(JsonObject param) {
    //signin by username and password
    //signin by session_token
    //signin by mobilePhoneNumber and password
    //signin by mobilePhoneNumber and smsCode
    //signin by email and password
    CommonResult result = new CommonResult();
    if (null == param) {
      result.setSuccess(false);
      result.setMessage("parameter is empty.");
    } else {
      JsonObject data = new JsonObject();
      result.setObject(data);

      String username = param.getString(PARAM_USERNAME);
      String password = param.getString(PARAM_PASSWORD);
      String email = param.getString(PARAM_EMAIL);
      String mobile = param.getString(PARAM_MOBILEPHONE);
      String smsCode = param.getString(PARAM_SMSCODE);
      String sessionToken = param.getString(PARAM_SESSION_TOKEN);
      boolean emailPassSelected = !StringUtils.isEmpty(email) && !StringUtils.isEmpty(password);
      boolean usernamePassSelected = !StringUtils.isEmpty(username) && !StringUtils.isEmpty(password);
      boolean mobilePassSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(password);
      boolean mobileSmscodeSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(smsCode);
      boolean sessionTokenSelected = !StringUtils.isEmpty(sessionToken);
      if (!emailPassSelected && !mobilePassSelected && !usernamePassSelected && !mobileSmscodeSelected && !sessionTokenSelected) {
        result.setSuccess(false);
        result.setMessage("parameter is invalid.");
      } else if (usernamePassSelected) {
        data.put(PARAM_USERNAME, username);
        data.put(PARAM_PASSWORD, password);
      } else if (mobilePassSelected) {
        data.put(PARAM_MOBILEPHONE, mobile);
        data.put(PARAM_PASSWORD, password);
      } else if (mobileSmscodeSelected) {
        // TODO： check smsCode is valid.
        data.put(PARAM_MOBILEPHONE, mobile);
      } else if (sessionTokenSelected) {
        data.put("sessionToken", sessionToken);
      } else {
        data.put(PARAM_EMAIL, email);
        data.put(PARAM_PASSWORD, password);
      }
    }
    return result;
  }

  public static CommonResult fillSignupParam(JsonObject param) {
    // support two paths to signup or login:
    // 1. username + password
    // 2. mobilephone + smscode
    // 3. mobilephone + password

    CommonResult result = new CommonResult();
    if (null == param) {
      result.setSuccess(false);
      result.setMessage("parameter is empty.");
    } else {
      JsonObject data = new JsonObject(param.getMap());
      result.setObject(data);

      String username = param.getString(PARAM_USERNAME);
      String password = param.getString(PARAM_PASSWORD);
      String email = param.getString(PARAM_EMAIL);
      String mobile = param.getString(PARAM_MOBILEPHONE);
      String smsCode = param.getString(PARAM_SMSCODE);
      boolean usernamePassSelected = !StringUtils.isEmpty(username) && !StringUtils.isEmpty(password);
      boolean mobileSmscodeSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(smsCode);
      boolean mobilePassSelected = !StringUtils.isEmpty(mobile) && !StringUtils.isEmpty(password);
      if (!usernamePassSelected && !mobileSmscodeSelected && !mobilePassSelected) {
        result.setSuccess(false);
        result.setMessage( "only support by signup with username + password, or mobilePhoneNumber + password, or mobilePhoneNumber + smsCode.");
      } else if (!StringUtils.isEmpty(email) && !ObjectSpecifics.validEmail(email)) {
        result.setSuccess(false);
        result.setMessage( "email is invalid.");
      } else {
        if (usernamePassSelected || mobilePassSelected) {
          String salt = StringUtils.getRandomString(16);
          String hashPassword = hashPassword(password, salt);
          data.put(PARAM_PASSWORD, hashPassword);
          data.put("salt", salt);
        } else {
          // TODO： check smsCode is valid.
          data.remove(PARAM_SMSCODE);
        }
        data.put("ACL", getUserDefaultACL());
        data.put("sessionToken", StringUtils.getRandomString(16));
        data.put("emailVerified", false);
        data.put("mobilePhoneVerified", false);
      }
    }
    return result;
  }
}
