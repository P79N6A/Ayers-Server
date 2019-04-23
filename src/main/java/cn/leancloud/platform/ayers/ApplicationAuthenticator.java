package cn.leancloud.platform.ayers;

import cn.leancloud.platform.codec.MessageDigest;
import cn.leancloud.platform.utils.StringUtils;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.CustomValidator;
import io.vertx.ext.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationAuthenticator implements CustomValidator {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationAuthenticator.class);

  private String computeMD5Sign(String key, String timestamp) {
    return MessageDigest.compute("MD5", timestamp + key);
  }

  public void validate(RoutingContext context) throws ValidationException {
    if (context.request().method() == HttpMethod.OPTIONS) {
      return;
    }
    String appId = RequestParse.getAppId(context);
    String appKey = RequestParse.getAppKey(context);
    String masterKey = RequestParse.getAppMasterKey(context);
    String requestSign = RequestParse.getAppSign(context);
    if (StringUtils.isEmpty(masterKey) && !StringUtils.isEmpty(appKey) && appKey.endsWith(RequestParse.MASTERKEY_SUFFIX)) {
      masterKey = appKey.substring(0, appKey.length() - RequestParse.MASTERKEY_SUFFIX.length());
    }
    logger.debug("request. appId=" + appId + ", appKey=" + appKey + ", masterkey=" + masterKey + ", sign=" + requestSign);
    if (StringUtils.isEmpty(appId) || (StringUtils.isEmpty(appKey) && StringUtils.isEmpty(masterKey) && StringUtils.isEmpty(requestSign))) {
      logger.warn("invalid request. appId=" + appId + ", appKey=" + appKey + ", masterkey=" + masterKey + ", sign=" + requestSign);
      throw new ValidationException("appKey authentication failed.");
    }
  }
}
