package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.StringUtils;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class RequestParse {
  private static final Logger logger = LoggerFactory.getLogger(RequestParse.class);

  public static final String HEADER_APPID = "x-avoscloud-application-id";
  public static final String HEADER_APPID_SHORTER = "x-lc-id";

  public static final String HEADER_APPKEY = "x-avoscloud-application-key";
  public static final String HEADER_APPKEY_SHORTER = "x-lc-key";

  public static final String HEADER_MASTERKEY = "x-avoscloud-master-key";

  public static final String MASTERKEY_SUFFIX = ",master";

  public static final String HEADER_REQUEST_SIGN = "x-avoscloud-request-sign";
  public static final String HEADER_REQUEST_SIGN_SHORTER = "x-lc-sign";

  public static final String COOKIE_USER_NAME = "uluru_user";

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
