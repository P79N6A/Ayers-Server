package cn.leancloud.platform.modules;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * for example: { "_owner" : { "read" : true, "write" : true }, "*" : { "read" : true, "write" : true } }
 *              { "_owner" : { "read" : true, "write" : false }, "*" : { "read" : false, "write" : false } }
 */
public class ObjectACLTemplate {
  private boolean ownerReadEnable;
  private boolean ownerWriteEnable;
  private boolean publicReadEnable;
  private boolean publicWriteEnable;

  private ObjectACLTemplate(boolean ownerR, boolean ownerW, boolean publicR, boolean publicW) {
    this.ownerReadEnable = ownerR;
    this.ownerWriteEnable = ownerW;
    this.publicReadEnable = publicR;
    this.publicWriteEnable = publicW;
  }

  private ObjectACLTemplate() {
    this(true, true, true, true);
  }

  public ACL genACL4User(String currentUserObjectId, List<String> roles) {
    ACL result = new ACL();
    result.setPublicReadAccess(this.publicReadEnable);
    result.setPublicWriteAccess(this.publicWriteEnable);
    if (StringUtils.notEmpty(currentUserObjectId)) {
      result.setWriteAccess(currentUserObjectId, this.ownerWriteEnable);
      result.setReadAccess(currentUserObjectId, this.ownerReadEnable);
    }
    return result;
  }

  public static class Builder {
    public static ObjectACLTemplate build(JsonObject json) {
      if (null == json) {
        return new ObjectACLTemplate();
      }
      JsonObject ownerJson = json.getJsonObject("_owner");
      JsonObject publicJson = json.getJsonObject("*");
      boolean publicRead = null == publicJson ? true : publicJson.getBoolean("read", true);
      boolean publicWrite = null == publicJson ? true : publicJson.getBoolean("write", true);
      boolean ownerRead = null == ownerJson ? true : ownerJson.getBoolean("read", true);
      boolean ownerWrite = null == ownerJson ? true : ownerJson.getBoolean("write", true);
      return new ObjectACLTemplate(ownerRead, ownerWrite, publicRead, publicWrite);
    }
  }
}
