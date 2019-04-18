package cn.leancloud.platform.modules;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

public class RootUser implements User {
  public User isAuthorized(String var1, Handler<AsyncResult<Boolean>> var2) {
    return null;
  }

  public User clearCache() {
    return null;
  }

  public JsonObject principal() {
    return null;
  }

  public void setAuthProvider(AuthProvider var1) {
    ;
  }

}
