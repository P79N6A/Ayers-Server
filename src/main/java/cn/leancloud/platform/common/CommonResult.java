package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;

public class CommonResult {
  private boolean success = true;
  private int errorCode = 0;
  private String message = null;
  private JsonObject object = null;

  public CommonResult() {
  }

  public boolean isSuccess() {
    return success;
  }

  public boolean isFailed() {
    return !isSuccess();
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public JsonObject getObject() {
    return object;
  }

  public void setObject(JsonObject object) {
    this.object = object;
  }
}
