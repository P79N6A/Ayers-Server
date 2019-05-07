package cn.leancloud.platform.modules;

import cn.leancloud.platform.ayers.handler.ObjectQueryHandler;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

public class ObjectSpecifics {
  private static final String REG_PATTERN_ROLE_NAME = "[a-zA-Z0-9_]+";
  private static final String REG_PATTERN_CLASSNAME = "^\\w[a-zA-Z0-9_]*$";
  private static final String REG_PATTERN_ATTRNAME = "[a-zA-Z0-9_]+$";
  private static final String REG_PATTERN_REQUEST_PATH = "^\\/1\\.1\\/(classes|users|installations|roles|files)($|\\/.*)";
  private static final String REG_PATTERN_EMAIL = "(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";
  private static final String REG_PATTERN_DATE = "\\d+-\\d+-\\d+-\\d+T\\d+:\\d+:\\d+.\\d+Z";

  public static final String[] builtinClazz = new String[]{"_User", "_Installation", "_Conversation",
          "_File", "_Follower", "_Followee", "_Role", "_Class"};

  public static final Set<String> builtinClazzSet = new ConcurrentHashSet<String>();

  static {
    builtinClazzSet.addAll(Arrays.asList(builtinClazz));
  }

  public static boolean isBuiltinClass(String clazz) {
    return builtinClazzSet.contains(clazz);
  }

  public static boolean validateRoleName(String name) {
    return Pattern.matches(REG_PATTERN_ROLE_NAME, name);
  }

  public static boolean validateEmail(String email) {
    return Pattern.matches(REG_PATTERN_EMAIL, email);
  }

  public static boolean validateClassName(String className) {
    return Pattern.matches(REG_PATTERN_CLASSNAME, className) || isBuiltinClass(className);
  }

  public static boolean validateAttrName(String attr) {
    return Pattern.matches(REG_PATTERN_ATTRNAME, attr);
  }

  public static boolean validateRequestPath(String path) {
    return Pattern.matches(REG_PATTERN_REQUEST_PATH, path);
  }

  public static boolean validateObject(JsonObject data) {
    if (null == data) {
      return true;
    }
    boolean result = data.stream().map(entry -> {
      String key = entry.getKey();
      Object v = entry.getValue();
      boolean isValid = validateAttrName(key);
      if (!isValid) {
        return isValid;
      }
      if (null != v && v instanceof JsonObject) {
        isValid = validateObject((JsonObject) v);
      }
      return isValid;
    }).anyMatch(v -> !v);
    return !result;
  }
}
