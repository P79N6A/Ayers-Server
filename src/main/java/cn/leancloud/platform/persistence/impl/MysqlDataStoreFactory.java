package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;

public class MysqlDataStoreFactory extends DataStoreFactory {
  private Vertx vertx;
  private JsonObject mysqlConfig;
  private String dataSourceName;
  public MysqlDataStoreFactory(Vertx vertx, JsonObject options, String dataSourceName) {
    this.vertx = vertx;
    this.mysqlConfig = options;
    this.dataSourceName = dataSourceName;
  }
  public DataStore getStore() {
    AsyncSQLClient mysqlClient = MySQLClient.createShared(this.vertx, this.mysqlConfig, this.dataSourceName);
    return new MysqlDataStore(mysqlClient);
  }
}
