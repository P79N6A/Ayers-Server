package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.DataStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoDBDataStoreFactory extends DataStoreFactory {
  private Vertx vertx;
  private JsonObject mongoConfig;
  private String dataSourceName;

  public MongoDBDataStoreFactory(Vertx vertx, JsonObject options, String dataSourceName) {
    this.vertx = vertx;
    this.mongoConfig = options;
    this.dataSourceName = dataSourceName;
  }

  public DataStore getStore() {
    MongoClient client = MongoClient.createShared(vertx, this.mongoConfig, this.dataSourceName);
    return new MongoDBDataStore(client);
  }
}
