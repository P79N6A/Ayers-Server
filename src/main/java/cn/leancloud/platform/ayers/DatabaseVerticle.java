package cn.leancloud.platform.ayers;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// for bigquery job.
//
public class DatabaseVerticle extends CommonVerticle {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

  private SQLClient mysqlClient = null;
  private static final String INIT_SQL = "create table if not exists sparkjobs (" +
          "  `id` varchar(64) NOT NULL," +
          "  `context_name` varchar(256)," +
          "  `classPath` varchar(256)," +
          "  `appId` varchar(64) NOT NULL," +
          "  `job_config` varchar(1024) NOT NULL," +
          "  `queued_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
          "  `start_time` TIMESTAMP NULL DEFAULT NULL," +
          "  `end_time` TIMESTAMP NULL DEFAULT NULL," +
          "  `status` int," +
          "  `error` varchar(1024)," +
          "  PRIMARY KEY (`id`)," +
          "  KEY `APP_ID_TIME` (`appId`,`queued_time`)" +
          ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
  private static final String SQL_GET_JOB_BY_APP = "select * from sparkjobs where appId = ? order by queued_time desc";
  private static final String SQL_GET_JOB = "select * from sparkjobs where id = ?";
  private static final String SQL_CREATE_JOB = "insert into sparkjobs(id, appId, job_config) values(?, ?, ?)";
  private static final String SQL_UPDATE_JOB = "update sparkjobs set start_time= ?, status = ? where id = ?";

  private Future<Void> prepareDatabase() {
    Future<Void> future = Future.future();
    JsonObject mySQLClientConfig = new JsonObject()
            .put("host", "127.0.0.1")
            .put("port", 13306)
            .put("username", "test")
            .put("password", "itsnothing")
            .put("database", "uluru")
            .put("charset", "utf-8")
            .put("connectTimeout", 10000)
            .put("testTimeout", 5000)
            .put("queryTimeout", 3000)
            .put("connectionRetryDelay", 5000)
            .put("maxPoolSize", 10);
    mysqlClient = MySQLClient.createShared(vertx, mySQLClientConfig, "MySQLPool");
    mysqlClient.getConnection(ar -> {
      if (ar.failed()) {
        logger.error("could not open a database connection. cause: ", ar.cause());
        future.fail(ar.cause());
      } else {
        SQLConnection connection = ar.result();
        connection.execute(INIT_SQL, cr -> {
          if (cr.failed()) {
            logger.error("database prepare error. cause: ", cr.cause());
            future.fail(cr.cause());
          } else {
            future.complete();
          }
        });
      }
    });
    return future;
  }

  public enum ErrorCodes {
    NO_OPERATION_SPECIFIED,
    BAD_OPERATION,
    DB_ERROR,
    NOT_FOUND_USER,
    PASSWORD_ERROR
  }

  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    logger.error("Database query error", cause);
    message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }

  public void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("operation")) {
      message.fail(ErrorCodes.NO_OPERATION_SPECIFIED.ordinal(), "no operation specified.");
      return;
    }
    String operation = message.headers().get("operation");
    JsonArray params = new JsonArray();
    JsonObject obj;
    switch (operation) {
      case "fetch":
        String jobId = message.body().getString("jobId");
        logger.debug("try to create job: " + jobId);
        params.add(jobId);
        mysqlClient.queryWithParams(SQL_GET_JOB, params, fetch -> {
          logger.debug("mysql client returned with result: " + fetch.succeeded());
          if (fetch.failed()) {
            reportQueryError(message, fetch.cause());
          } else {
            logger.debug("db result: " + fetch.result().toJson());
            message.reply(fetch.result().toJson());
          }
        });
        break;
      case "create":
        obj = message.body();
        logger.debug("try to create job: " + obj.toString());
        params.add(obj.getString("id"));
        params.add(obj.getString("appId"));
        params.add(obj.getJsonObject("jobConfig").toString());

        mysqlClient.updateWithParams(SQL_CREATE_JOB, params, updated -> {
          logger.debug("mysql client returned with result: " + updated.succeeded());
          if (updated.failed()) {
            reportQueryError(message, updated.cause());
          } else {
            message.reply(updated.result().toJson());
          }
        });
        break;
      case "update":
        obj = message.body();
        params.add(obj);
        mysqlClient.updateWithParams(SQL_UPDATE_JOB, params, updated -> {
          if (updated.failed()) {
            reportQueryError(message, updated.cause());
          } else {
            message.reply(updated.result());
          }
        });
        break;
      default:
        message.fail(ErrorCodes.BAD_OPERATION.ordinal(), "unknown operation.");
    }
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    logger.info("start DatabaseVerticle...");
    prepareDatabase().setHandler(ar -> {
      if (ar.failed()) {
        startFuture.fail(ar.cause());
      } else {
        String dbQueue = config().getString(RestServerVerticle.CONFIG_DB_QUEUE, "db.queue");
        vertx.eventBus().consumer(dbQueue, this::onMessage);
        startFuture.complete();;
      }
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    logger.info("stop DatabaseVerticle...");
    if (null != mysqlClient) {
      mysqlClient.close(ar -> stopFuture.complete());
    } else {
      stopFuture.complete();
    }
  }
}
