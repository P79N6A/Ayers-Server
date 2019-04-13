package cn.leancloud.platform.common;

import java.util.Random;

import cn.leancloud.platform.codec.MessageDigest;

public class StringUtils {
  static Random random = new Random(System.nanoTime());

  private static final String ALL_LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  public static boolean isEmpty(String data) {
    return (null == data || data.trim().length() == 0);
  }

  public static boolean notEmpty(String data) {
    return !isEmpty(data);
  }

  public static String getRandomString(int length) {

    StringBuilder randomString = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      randomString.append(ALL_LETTERS.charAt(random.nextInt(ALL_LETTERS.length())));
    }

    return randomString.toString();
  }

  public static String computeSHA1(String input, String key) {
    return MessageDigest.computeSHA1WithKey(input, key);
  }

  public static String computeMD5(String input) {
    return MessageDigest.compute(MessageDigest.ALGORITHM_MD5, input);
  }
}
