package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.codec.Base64;
import cn.leancloud.platform.codec.MessageDigest;
import io.vertx.ext.web.RoutingContext;

public class UserHandler {
  public static void signup(RoutingContext context) {
    ;
  }
  public static void signin(RoutingContext context) {
    ;
  }

  public static String hashPassword(String password, String salt) {
    String newPassword = MessageDigest.compute(MessageDigest.ALGORITHM_SHA256, salt+ password);
    for (int i = 0; i < 256; i++) {
      newPassword = MessageDigest.compute(MessageDigest.ALGORITHM_SHA256, newPassword);
    }
    return new String(Base64.encode(newPassword));
  }
}
