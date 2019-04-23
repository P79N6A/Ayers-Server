package cn.leancloud.platform.ayers;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RequestParse {
  private static final Logger logger = LoggerFactory.getLogger(RequestParse.class);

  public static final String HEADER_APPID = "X-AVOSCloud-Application-Id";
  public static final String HEADER_APPID_SHORTER = "X-LC-Id";

  public static final String HEADER_APPKEY = "X-AVOSCloud-Application-Key";
  public static final String HEADER_APPKEY_SHORTER = "X-LC-Key";

  public static final String HEADER_MASTERKEY = "X-AVOSCloud-Master-Key";

  public static final String MASTERKEY_SUFFIX = ",master";

  public static final String HEADER_REQUEST_SIGN = "X-AVOSCloud-Request-Sign";
  public static final String HEADER_REQUEST_SIGN_SHORTER = "X-LC-Sign";

  public static final String HEADER_SESSION_TOKEN = "X-AVOSCloud-Session-Token";
  public static final String HEADER_SESSION_TOKEN_SHORTER = "X-LC-Session";

  public static final String HEADER_USERAGENT = "X-LC-UA";

  public static final String COOKIE_USER_NAME = "uluru_user";

  public static final String HEADER_HOOK_KEY = "X-LC-Hook-Key";
  public static final String HEADER_PROD_KEY = "X-LC-Prod";
  public static final String HEADER_REQUEST_WITH = "X-Requested-With";
  public static final String HEADER_ULURU_APPID = "X-Uluru-Application-Id";
  public static final String HEADER_ULURU_APPKEY = "X-Uluru-Application-Key";
  public static final String HEADER_ULURU_APP_PROD = "X-Uluru-Application-Production";
  public static final String HEADER_ULURU_CLIENT_VERSION = "X-Uluru-Client-Version";
  public static final String HEADER_ULURU_SESSION_TOKEN = "X-Uluru-Session-Token";

  public static final String[] ALLOWED_HEADERS = new String[]{HEADER_APPID, HEADER_APPID_SHORTER, HEADER_APPKEY,
          HEADER_USERAGENT, HEADER_APPKEY_SHORTER, HEADER_MASTERKEY, HEADER_REQUEST_SIGN, HEADER_REQUEST_SIGN_SHORTER,
          HEADER_SESSION_TOKEN, HEADER_SESSION_TOKEN_SHORTER, HEADER_HOOK_KEY, HEADER_PROD_KEY, HEADER_REQUEST_WITH,
          HEADER_ULURU_APPID, HEADER_ULURU_APPKEY, HEADER_ULURU_APP_PROD, HEADER_ULURU_CLIENT_VERSION, HEADER_ULURU_SESSION_TOKEN
  };

//  public static final String ALLOWED_HEADERS_STRING = String.join(",", ALLOWED_HEADERS);
  public static final Set<String> ALLOWED_HEADERS_SET = new HashSet<>(Arrays.asList(ALLOWED_HEADERS));

  private static String getHeader(RoutingContext context, String first, String second) {
    String value = context.request().getHeader(first);
    if (StringUtils.isEmpty(value) && !StringUtils.isEmpty(second)) {
      value = context.request().getHeader(second);
    }
    return value;
  }

  public static String getAppId(RoutingContext context) {
    return getHeader(context, HEADER_APPID, HEADER_APPID_SHORTER);
  }

  public static String getAppKey(RoutingContext context) {
    return getHeader(context, HEADER_APPKEY, HEADER_APPKEY_SHORTER);
  }

  public static String getAppMasterKey(RoutingContext context) {
    return getHeader(context, HEADER_MASTERKEY, null);
  }

  public static String getAppSign(RoutingContext context) {
    return getHeader(context, HEADER_REQUEST_SIGN, HEADER_REQUEST_SIGN_SHORTER);
  }

  public static String getSessionToken(RoutingContext context) {
    return getHeader(context, HEADER_SESSION_TOKEN, null);
  }

  public static String getCookie(RoutingContext context) {
    Cookie cookie = context.getCookie(COOKIE_USER_NAME);
    if (null != cookie) {
      try {
        return URLDecoder.decode(cookie.getValue(), "UTF-8");
      } catch (UnsupportedEncodingException ex) {
        logger.warn("failed to decode cookie, cause: ", ex);
        return null;
      }
    } else {
      return null;
    }
  }
}
