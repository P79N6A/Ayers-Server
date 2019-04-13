package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.StringUtils;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.CustomValidator;
import io.vertx.ext.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationAuthenticator implements CustomValidator {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationAuthenticator.class);

  // validate (appid, appkey/sign) through meta service, work for bigquery.
  // for api service, it's not useful. ignore.
  private final String metaServiceAuthKey;

  public ApplicationAuthenticator(String metaServiceAuthKey) {
    this.metaServiceAuthKey = metaServiceAuthKey;
  }

  private String getCurrentTs() {
    return String.valueOf(System.currentTimeMillis());
  }

  private String rpcAuthSign(String method, String ts) {
    String wholeInput = String.format("%s:%s:%s", this.metaServiceAuthKey, method, ts);
    return String.format("%s,%s,%s", this.metaServiceAuthKey, ts, StringUtils.computeSHA1(wholeInput, this.metaServiceAuthKey));
  }

  public void validate(RoutingContext context) throws ValidationException {
    String cookie = RequestParse.getCookie(context);
    String appId = RequestParse.getAppId(context);
    String appKey = RequestParse.getAppKey(context);
    String masterKey = RequestParse.getAppMasterKey(context);
    String requestSign = RequestParse.getAppSign(context);
    if (StringUtils.isEmpty(masterKey) && !StringUtils.isEmpty(appKey) && appKey.endsWith(RequestParse.MASTERKEY_SUFFIX)) {
      masterKey = appKey.substring(0, appKey.length() - RequestParse.MASTERKEY_SUFFIX.length());
    }
    if (StringUtils.isEmpty(appId) || (StringUtils.isEmpty(appKey) && StringUtils.isEmpty(masterKey) && StringUtils.isEmpty(requestSign))) {
      logger.warn("invalid request. appId=" + appId + ", appKey=" + appKey + ", masterkey=" + masterKey + ", sign=" + requestSign);
      throw new ValidationException("appKey authentication failed.");
    }
  }
}
