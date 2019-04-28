package cn.leancloud.platform.auth;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.CustomValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class SessionValidator implements CustomValidator {
  public void validate(RoutingContext context) throws ValidationException {
    // check session_token
    String sessionToken = context.request().getParam("session_token");
    if (StringUtils.isEmpty(sessionToken)) {
      sessionToken = context.request().getHeader(RequestParse.HEADER_AVOS_SESSION_TOKEN);
      if (StringUtils.isEmpty(sessionToken)) {
        throw new ValidationException("not found session token.");
      }
    }
  }
}
