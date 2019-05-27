package cn.leancloud.platform.ayers;

import cn.leancloud.platform.common.Configure;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MailVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.eventBus().consumer(Configure.MAIL_ADDRESS_EMAIL_QUEUE, this::onMessage);
    logger.info("start MailVerticle...");
    startFuture.complete();
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop MailVerticle...");
    stopFuture.complete();
  }

  public void onMessage(Message<JsonObject> message) {
    message.reply("");
  }
}
