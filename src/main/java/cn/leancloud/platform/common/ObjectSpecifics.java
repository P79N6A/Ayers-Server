package cn.leancloud.platform.common;

import java.util.regex.Pattern;

public class ObjectSpecifics {
  private static final String REG_PATTERN_ROLE_NAME = "[a-zA-Z0-9_]+";
  private static final String REG_PATTERN_CLASSNAME = "^\\w[a-zA-Z0-9_]*";
  private static final String REG_PATTERN_REQUEST_PATH = "^\\/1\\.1\\/classes\\/.+";
  private static final String REG_PATTERN_EMAIL = "(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";

  public static boolean validRoleName(String name) {
    return Pattern.matches(REG_PATTERN_ROLE_NAME, name);
  }

  public static boolean validEmail(String email) {
    return Pattern.matches(REG_PATTERN_EMAIL, email);
  }

  public static boolean validClassName(String className) {
    return Pattern.matches(REG_PATTERN_CLASSNAME, className) || Constraints.isBuiltinClass(className);
  }

  public static boolean validRequestPath(String path) {
    return Pattern.matches(REG_PATTERN_REQUEST_PATH, path);
  }
}
