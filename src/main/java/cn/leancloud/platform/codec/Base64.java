package cn.leancloud.platform.codec;

import cn.leancloud.platform.common.StringUtils;

public class Base64 {
  public static byte[] decode(String input) {
    if (StringUtils.isEmpty(input)) {
      return null;
    }
    return decode(input.getBytes());
  }
  public static byte[] decode(byte[] input) {
    if (null == input) {
      return null;
    }
    return java.util.Base64.getDecoder().decode(input);
  }

  public static byte[] encode(String input) {
    if (StringUtils.isEmpty(input)) {
      return null;
    }
    return encode(input.getBytes());
  }
  public static byte[] encode(byte[] input) {
    if (null == input) {
      return null;
    }
    return java.util.Base64.getEncoder().encode(input);
  }
}
