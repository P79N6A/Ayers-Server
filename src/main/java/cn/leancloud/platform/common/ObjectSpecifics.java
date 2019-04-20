package cn.leancloud.platform.common;

import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

public class ObjectSpecifics {
  private static final String REG_PATTERN_ROLE_NAME = "[a-zA-Z0-9_]+";
  private static final String REG_PATTERN_CLASSNAME = "^\\w[a-zA-Z0-9_]*";
  private static final String REG_PATTERN_ATTRNAME = "[a-zA-Z0-9_]+";
  private static final String REG_PATTERN_REQUEST_PATH = "^\\/1\\.1\\/classes\\/.+";
  private static final String REG_PATTERN_EMAIL = "(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";

  public static final String[] builtinClazz = new String[]{"_User", "_Installation", "_Conversation",
          "_File", "_Follower", "_Followee", "_Role"};
  public static final Set<String> builtinClazzSet = new ConcurrentHashSet<String>();

  static {
    builtinClazzSet.addAll(Arrays.asList(builtinClazz));
  }

  public static boolean isBuiltinClass(String clazz) {
    return builtinClazzSet.contains(clazz);
  }

  public static boolean validRoleName(String name) {
    return Pattern.matches(REG_PATTERN_ROLE_NAME, name);
  }

  public static boolean validEmail(String email) {
    return Pattern.matches(REG_PATTERN_EMAIL, email);
  }

  public static boolean validClassName(String className) {
    return Pattern.matches(REG_PATTERN_CLASSNAME, className) || isBuiltinClass(className);
  }

  public static boolean validAttrName(String attr) {
    return Pattern.matches(REG_PATTERN_ATTRNAME, attr);
  }

  public static boolean validRequestPath(String path) {
    return Pattern.matches(REG_PATTERN_REQUEST_PATH, path);
  }
}
