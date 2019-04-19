package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.codec.Base64;
import cn.leancloud.platform.codec.MessageDigest;
import cn.leancloud.platform.common.*;
import cn.leancloud.platform.modules.ACL;
import io.vertx.core.json.JsonObject;

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
        result.setMessage(ErrorCodes.INVALID_PARAMETER.getMessage());
      } else if (usernamePassSelected) {
        data.put(PARAM_USERNAME, username);
        data.put(PARAM_PASSWORD, password);
      } else if (mobilePassSelected) {
        data.put(PARAM_MOBILEPHONE, mobile);
        data.put(PARAM_PASSWORD, password);
      } else if (mobileSmscodeSelected) {
        // TODO： check smsCode is valid.
        data.put(PARAM_MOBILEPHONE, mobile);
      } else if (emailPassSelected) {
        data.put(PARAM_EMAIL, email);
        data.put(PARAM_PASSWORD, password);
      } else {
        data.put(Constraints.BUILTIN_ATTR_SESSION_TOKEN, sessionToken);
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
      result.setMessage(ErrorCodes.INVALID_PARAMETER.getMessage());
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
        result.setMessage(ErrorCodes.INVALID_PARAMETER.getMessage());
      } else if (!StringUtils.isEmpty(email) && !ObjectSpecifics.validEmail(email)) {
        result.setSuccess(false);
        result.setMessage(ErrorCodes.INVALID_EMAIL.getMessage());
      } else {
        if (usernamePassSelected || mobilePassSelected) {
          String salt = StringUtils.getRandomString(Constraints.SALT_LENGTH);
          String hashPassword = hashPassword(password, salt);
          data.put(PARAM_PASSWORD, hashPassword);
          data.put(Constraints.BUILTIN_ATTR_SALT, salt);
        } else {
          // TODO： check smsCode is valid.
          data.remove(PARAM_SMSCODE);
        }
        data.put(Constraints.BUILTIN_ATTR_ACL, getUserDefaultACL());
        data.put(Constraints.BUILTIN_ATTR_SESSION_TOKEN, StringUtils.getRandomString(Constraints.SESSION_TOKEN_LENGTH));
        data.put(Constraints.BUILTIN_ATTR_EMAIL_VERIFIED, false);
        data.put(Constraints.BUILTIN_ATTR_PHONENUM_VERIFIED, false);
      }
    }
    return result;
  }
}
