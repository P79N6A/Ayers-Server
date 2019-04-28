package cn.leancloud.platform.auth;

import cn.leancloud.platform.ayers.RequestParse;
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
    RequestParse.RequestHeaders headers = RequestParse.extractRequestHeaders(context);
    if (null == headers) {
      throw new ValidationException("application authentication data not found in request header.");
    }
    String appId = headers.getAppId();
    String appKey = headers.getAppKey();
    boolean useMasterKey = headers.isUseMasterKey();
    String requestSign = headers.getRequestSign();
    if (StringUtils.isEmpty(appId) || (StringUtils.isEmpty(appKey) && StringUtils.isEmpty(requestSign))) {
      logger.warn("request. appId=" + appId + ", appKey=" + appKey + ", useMasterKey=" + useMasterKey + ", sign=" + requestSign);
      throw new ValidationException("invalid application authentication.");
    }
  }
}
