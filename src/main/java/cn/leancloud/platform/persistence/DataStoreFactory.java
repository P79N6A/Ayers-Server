package cn.leancloud.platform.persistence;

import cn.leancloud.platform.persistence.impl.MongoDBDataStoreFactory;
import cn.leancloud.platform.persistence.impl.MysqlDataStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public abstract class DataStoreFactory {
  public static final String SUPPORT_DATA_SOURCE_MYSQL = "mysql";
  public static final String SUPPORT_DATA_SOURCE_MONGO = "mongo";

  public abstract DataStore getStore();

  public static class Builder {
    private String dataStoreType = null;
    private JsonObject options = null;
    private String dataSourceName = null;

    public Builder setType(String dataStoreType) {
      this.dataStoreType = dataStoreType;
      return this;
    }
    public Builder setOptions(JsonObject options) {
      this.options = options;
      return this;
    }
    public Builder setSourceName(String sourceName) {
      this.dataSourceName = sourceName;
      return this;
    }
    public DataStoreFactory build(Vertx vertx) {
      if (SUPPORT_DATA_SOURCE_MYSQL.equals(this.dataStoreType)) {
        return new MysqlDataStoreFactory(vertx, this.options, this.dataSourceName);
      } else if (SUPPORT_DATA_SOURCE_MONGO.equals(this.dataStoreType)) {
        return new MongoDBDataStoreFactory(vertx, this.options, this.dataSourceName);
      } else {
        return null;
      }
    }
  }
}
