package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.utils.HandlerUtils;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Objects;

public class MailHandler extends CommonHandler {
  public MailHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public void sendMail(String host, String user, String passwd, String from, List<String> to, List<String> cc, List<String> bcc,
                       String subject, String bodyContentType, String bodyContent, Handler<AsyncResult<String>> handler) {
    Objects.requireNonNull(handler);
    if (StringUtils.isEmpty(host) || StringUtils.isEmpty(user) || StringUtils.isEmpty(passwd) || StringUtils.isEmpty(from)
            || StringUtils.isEmpty(subject) || StringUtils.isEmpty(bodyContent) || StringUtils.isEmpty(bodyContentType)) {
      handler.handle(HandlerUtils.wrapErrorResult(new IllegalArgumentException("parameter is invalid.")));
      return;
    }
    boolean validRecipient = (null != to && to.size() > 0) || (null != cc && cc.size() > 0) || (null != bcc && bcc.size() > 0);
    if (!validRecipient) {
      handler.handle(HandlerUtils.wrapErrorResult(new IllegalArgumentException("not found any valid recipient.")));
      return;
    }
    MailConfig config = new MailConfig();
    config.setHostname(host);
    config.setStarttls(StartTLSOptions.REQUIRED);
    config.setUsername(user);
    config.setPassword(passwd);
    MailClient mailClient = MailClient.createShared(vertx, config);
    MailMessage message = new MailMessage();
    message.setFrom(from);
    if (null != to) {
      message.setTo(to);
    }
    if (null != cc) {
      message.setCc(cc);
    }
    if (null != bcc) {
      message.setBcc(bcc);
    }
    message.setSubject(subject);
    if (bodyContentType.indexOf("html") < 0) {
      message.setText(bodyContent);
    } else {
      message.setHtml(bodyContent);
    }
    mailClient.sendMail(message, response -> handler.handle(response.map(mailResult -> mailResult.toString())));
  }

}
