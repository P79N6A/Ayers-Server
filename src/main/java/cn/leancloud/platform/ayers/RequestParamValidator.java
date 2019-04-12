package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.StringUtils;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.CustomValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class RequestParamValidator implements CustomValidator {
  private String[] paramNames;
  public RequestParamValidator(String[] paramNames) {
    this.paramNames = paramNames;
  }
  public void validate(RoutingContext context) throws ValidationException {
    for (String name : paramNames) {
      if (StringUtils.isEmpty(context.request().getParam(name))) {
        throw new ValidationException(name + " is necessary.");
      }
    }
  }
}
