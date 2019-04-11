package cn.leancloud.platform.cache;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.redis.client.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpleRedisClient {
  private static final Logger logger = LoggerFactory.getLogger(SimpleRedisClient.class);
  private Redis client;
  private RedisAPI redisAPI;

  private <T> AsyncResult<T> createFakeResponse(final Throwable cause) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return cause;
      }

      @Override
      public boolean succeeded() {
        return false;
      }

      @Override
      public boolean failed() {
        return true;
      }
    };
  }

  public void connect(Vertx vertx, String host, int port, Handler<AsyncResult<Void>> handler) {
    // TODO:
    // we can change options to support redis cluster or sentinel.
    //
    RedisOptions options = new RedisOptions();
    options.setEndpoint(SocketAddress.inetSocketAddress(port, host));
    Redis.createClient(vertx, options).connect(onConnect -> {
      if (onConnect.succeeded()) {
        this.client = onConnect.result();
        this.redisAPI = RedisAPI.api(this.client);
        logger.info("succeed connect to redis host " + host + ":" + port);
      } else {
        logger.warn("failed to connect redis server. cause: ", onConnect.cause());
      }
      if (null != handler) {
        handler.handle(new AsyncResult<Void>() {
          @Override
          public Void result() {
            return null;
          }

          @Override
          public Throwable cause() {
            return onConnect.cause();
          }

          @Override
          public boolean succeeded() {
            return onConnect.succeeded();
          }

          @Override
          public boolean failed() {
            return onConnect.failed();
          }
        });
      }
    });
  }

  public void close() {
    if (null != this.client) {
      this.client.close();
    }
    this.client = null;
    this.redisAPI = null;
  }

  public void get(String key, Handler<AsyncResult<Response>> handler) {
    if (null != this.redisAPI) {
      this.redisAPI.get(key, handler);
    } else {
      if (null != handler) {
        handler.handle(this.createFakeResponse(new RuntimeException("redis client initializer failed.")));
      }
    }
  }

  public void set(List<String> args, Handler<AsyncResult<Response>> handler) {
    if (null != this.redisAPI) {
      this.redisAPI.set(args, handler);
    } else {
      if (null != handler) {
        handler.handle(this.createFakeResponse(new RuntimeException("redis client initializer failed.")));
      }
    }
  }

  public void subscriber(Handler<Response> handler) {
    if (null != this.client) {
      this.client.handler(handler);
    } else if (null != handler) {
      handler.handle(null);
    }
  }

  public void publish(String channel, String message, Handler<AsyncResult<Response>> handler) {
    if (null != this.client) {
      this.client.send(Request.cmd(Command.PUBLISH).arg(channel, message), res -> {
        if (null != handler) {
          handler.handle(res);
        }
      });
    } else if (null != handler) {
      handler.handle(this.createFakeResponse(new RuntimeException("redis client initializer failed.")));
    }
  }
}
