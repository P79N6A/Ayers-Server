package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;

public enum ErrorCodes {
  NO_ERROR(0, "no error"),

  INTERNAL_ERROR(1, "Internal server error. No information available."),
  INVALID_PARAMETER(2, "Invalid request parameter."),

  INVALID_GEOPOINT(302, "Invalid GeoPoint values."),
  INVALID_LINKED_SESSION(251, "Invalid linked session."),
  INVALID_WEIXIN_SESSION(252, "Invalid Weixin session."),
  INVALID_USERNAME(217, "Invalid username, it must be a non-blank string."),
  INVALID_EMAIL(125, "The email address was invalid."),
  INVALID_PASSWORD(218, "Invalid password, it must be a non-blank string."),
  INVALID_ROLENAME(139, "Role's name is invalid."),
  INVALID_OBJECTID(104, "Missing or invalid objectId."),
  INVALID_CLASSNAME(103, "Missing or invalid classname. Classname is case-sensitive, must start with a letter, and a-zA-Z0-9_ are the only valid characters."),
  INVALID_JSON_KEY(121, "Keys in NSDictionary values may not include '#39; or '.'."),
  INVALID_JSON_FORMAT(107, "Malformed json object. A json dictionary is expected."),

  OBJECT_TOO_LARGE(116, "The object is too large."),
  USER_NOT_FOUND(211, "Could not find user."),
  EMAIL_NOT_VERIFIED(216, "Email address isn't verified."),

  OBJECT_NOT_FOUND(101, ""),
  OPERATION_NOT_SUPPORT(3, "operation not support yet."),
  PASSWORD_WRONG(210, "The username and password mismatch."),

  DATABASE_ERROR(304, "Database error"),
  NO_EFFECT_OPERATION(305, "No effect on updating/deleting a document"),

  UNAUTHORIZED(401, "Unauthorized."),

  SERVER_OFFLINE(502, "Server is in maintenance."),

  TOO_MANY_SMS(601, ""),
  FAILED_TO_SEND_SMS(602, ""),
  INVALID_SMS_CODE(603, "smsCode is invalid."),
  INVALID_SMS_TOKEN(608, ""),
  QUERY_KEY_NOT_EXIST(700, "");

  private int code = 0;
  private String message;
  ErrorCodes(int code, String message) {
    this.code = code;
    this.message = message;
  }
  public int getCode() {
    return this.code;
  }
  public String getMessage() {
    return this.message;
  }
  public JsonObject toJson() {
    return new JsonObject().put("code", this.code).put("error", this.message);
  }
}
