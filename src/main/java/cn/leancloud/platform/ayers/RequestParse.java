package cn.leancloud.platform.ayers;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequestParse {
  private static final Logger logger = LoggerFactory.getLogger(RequestParse.class);

  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

  public static final String HEADER_LC_APPID = "X-LC-Id";
  public static final String HEADER_AVOS_APPID = "X-AVOSCloud-Application-Id";
  public static final String HEADER_ULURU_APPID = "X-Uluru-Application-Id";

  public static final String HEADER_LC_APPKEY = "X-LC-Key";
  public static final String HEADER_AVOS_APPKEY = "X-AVOSCloud-Application-Key";
  public static final String HEADER_ULURU_APPKEY = "X-Uluru-Application-Key";

  public static final String HEADER_LC_REQUEST_SIGN = "X-LC-Sign";
  public static final String HEADER_AVOS_REQUEST_SIGN = "X-AVOSCloud-Request-Sign";

  public static final String HEADER_LC_SESSION_TOKEN = "X-LC-Session";
  public static final String HEADER_AVOS_SESSION_TOKEN = "X-AVOSCloud-Session-Token";
  public static final String HEADER_ULURU_SESSION_TOKEN = "X-Uluru-Session-Token";

  public static final String HEADER_PROD_KEY = "X-LC-Prod";
  public static final String HEADER_ULURU_APP_PROD = "X-Uluru-Application-Production";

  public static final String HEADER_HOOK_KEY = "X-LC-Hook-Key";

  public static final String HEADER_USERAGENT = "X-LC-UA";
  public static final String HEADER_MASTERKEY = "X-AVOSCloud-Master-Key";
  public static final String MASTERKEY_SUFFIX = ",master";
  public static final String HEADER_REQUEST_WITH = "X-Requested-With";
  public static final String HEADER_ULURU_CLIENT_VERSION = "X-Uluru-Client-Version";

  public static final String[] ALLOWED_HEADERS = new String[]{HEADER_AVOS_APPID, HEADER_LC_APPID, HEADER_AVOS_APPKEY,
          HEADER_USERAGENT, HEADER_LC_APPKEY, HEADER_MASTERKEY, HEADER_AVOS_REQUEST_SIGN, HEADER_LC_REQUEST_SIGN,
          HEADER_AVOS_SESSION_TOKEN, HEADER_LC_SESSION_TOKEN, HEADER_HOOK_KEY, HEADER_PROD_KEY, HEADER_REQUEST_WITH,
          HEADER_ULURU_APPID, HEADER_ULURU_APPKEY, HEADER_ULURU_APP_PROD, HEADER_ULURU_CLIENT_VERSION, HEADER_ULURU_SESSION_TOKEN
  };

//  public static final String ALLOWED_HEADERS_STRING = String.join(",", ALLOWED_HEADERS);
  public static final Set<String> ALLOWED_HEADERS_SET = new HashSet<>(Arrays.asList(ALLOWED_HEADERS));

  public static final String HTTP_POST = "POST";
  public static final String HTTP_PUT = "PUT";
  public static final String HTTP_GET = "GET";
  public static final String HTTP_DELETE = "DELETE";
  public static final String OP_USER_SIGNIN = "LOGIN";
  public static final String OP_USER_SIGNUP = "SIGNUP";
  public static final String OP_USER_AUTH_LOGIN = "AUTH_LOGIN";

  public static class RequestHeaders {
    private String appId;
    private String appKey;
    private String requestSign;
    private String requestTimestamp;
    private String sessionToken;
    private boolean useMasterKey;

    public String getAppId() {
      return appId;
    }

    public RequestHeaders setAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public String getAppKey() {
      return appKey;
    }

    public RequestHeaders setAppKey(String appKey) {
      this.appKey = appKey;
      return this;
    }

    public String getRequestSign() {
      return requestSign;
    }

    public RequestHeaders setRequestSign(String requestSign) {
      this.requestSign = requestSign;
      return this;
    }

    public String getRequestTimestamp() {
      return requestTimestamp;
    }

    public RequestHeaders setRequestTimestamp(String requestTimestamp) {
      this.requestTimestamp = requestTimestamp;
      return this;
    }

    public String getSessionToken() {
      return sessionToken;
    }

    public RequestHeaders setSessionToken(String sessionToken) {
      this.sessionToken = sessionToken;
      return this;
    }

    public boolean isUseMasterKey() {
      return useMasterKey;
    }

    public RequestHeaders setUseMasterKey(boolean useMasterKey) {
      this.useMasterKey = useMasterKey;
      return this;
    }

    public JsonObject toHeaders() {
      return new JsonObject().put(HEADER_LC_APPID, this.appId).put(HEADER_LC_APPKEY, this.appKey)
              .put(HEADER_LC_REQUEST_SIGN, this.requestSign).put(HEADER_LC_SESSION_TOKEN, this.sessionToken);
    }
  }

  public static RequestHeaders extractRequestHeaders(RoutingContext context) {
    if (null == context) {
      return null;
    }
    String appId = getHeader(context, HEADER_LC_APPID, HEADER_ULURU_APPID, HEADER_AVOS_APPID);
    String appKey = getHeader(context, HEADER_LC_APPKEY, HEADER_ULURU_APPKEY, HEADER_AVOS_APPKEY);
    String requestSign = getHeader(context, HEADER_LC_REQUEST_SIGN, HEADER_AVOS_REQUEST_SIGN);
    String requestTimestamp = "";
    String sessionToken = getHeader(context, HEADER_LC_SESSION_TOKEN, HEADER_ULURU_SESSION_TOKEN, HEADER_AVOS_SESSION_TOKEN);
    String masterKey = getHeader(context, HEADER_MASTERKEY);
    boolean useMasterKey = StringUtils.notEmpty(masterKey);
    if (StringUtils.isEmpty(masterKey) && StringUtils.notEmpty(appKey) && appKey.endsWith(MASTERKEY_SUFFIX)) {
      masterKey = appKey.substring(0, appKey.length() - MASTERKEY_SUFFIX.length());
      useMasterKey = true;
    } else if (StringUtils.notEmpty(requestSign)) {
      String[] signParts = requestSign.split(",");

      if (signParts.length > 1) {
        requestSign = signParts[0];
        requestTimestamp = signParts[1];
        if (signParts.length == 3 || signParts[2].equalsIgnoreCase("master")) {
          useMasterKey = true;
        }
      } else {
        logger.warn("invalid request sign: " + requestSign);
        requestSign = "";
      }
    }
    RequestHeaders result = new RequestHeaders()
            .setAppId(appId).setAppKey(StringUtils.notEmpty(masterKey)? masterKey : appKey)
            .setRequestSign(requestSign).setRequestTimestamp(requestTimestamp).setSessionToken(sessionToken)
            .setUseMasterKey(useMasterKey);
    return result;
  }

  private static String getHeader(RoutingContext context, String... headers) {
    for (String header : headers) {
      String value = context.request().getHeader(header);
      if (StringUtils.notEmpty(value)) {
        return value;
      }
    }
    return "";
  }

  public static String getSessionToken(RoutingContext context) {
    return getHeader(context, HEADER_LC_SESSION_TOKEN, HEADER_AVOS_SESSION_TOKEN, HEADER_LC_SESSION_TOKEN);
  }

//  public static String getCookie(RoutingContext context) {
//    Cookie cookie = context.getCookie(COOKIE_USER_NAME);
//    if (null != cookie) {
//      try {
//        return URLDecoder.decode(cookie.getValue(), "UTF-8");
//      } catch (UnsupportedEncodingException ex) {
//        logger.warn("failed to decode cookie, cause: ", ex);
//        return null;
//      }
//    } else {
//      return null;
//    }
//  }
}
