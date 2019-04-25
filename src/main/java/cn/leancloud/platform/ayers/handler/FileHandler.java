package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.utils.MimeUtils;
import com.qiniu.util.Auth;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class FileHandler extends CommonHandler {
  public static final String PARAM_FILE_UPLOAD_URL = "upload_url";
  public static final String PARAM_FILE_TOKEN = "token";

  public FileHandler(Vertx vertx, RoutingContext context) {
    super(vertx, context);
  }

  public void createFileToken(HttpMethod httpMethod, String name, String key, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
    if (!assertNotNull(name, handler))
      return;
    if (!assertNotNull(key, handler))
      return;
    if (!assertNotNull(body, handler))
      return;

    String mimeType = Constraints.DEFAULT_MIME_TYPE;
    String[] nameParts = name.split("\\.");
    if (nameParts.length > 1) {
      mimeType = MimeUtils.guessMimeTypeFromExtension(nameParts[nameParts.length - 1]);
    }

    Configure configure = Configure.getInstance();
    String bucketName = configure.fileBucket();

    Auth auth = Auth.create(configure.fileProviderAccessKey(), configure.fileProviderSecretKey());
    final String token = auth.uploadToken(bucketName, key);
    body.remove("__type");
    body.put(LeanObject.BUILTIN_ATTR_FILE_URL, configure.fileDefaultHost() + key)
            .put(LeanObject.BUILTIN_ATTR_FILE_MIMETYPE, mimeType)
            .put(LeanObject.BUILTIN_ATTR_FILE_PROVIDER,configure.fileProvideName())
            .put(LeanObject.BUILTIN_ATTR_FILE_BUCKET, bucketName);

    sendDataOperationWithOption(Constraints.FILE_CLASS, null, httpMethod.toString(), null, body, true, res -> {
      handler.handle(res.map(file -> file.mergeIn(body).put(PARAM_FILE_TOKEN, token).put(PARAM_FILE_UPLOAD_URL, configure.fileUploadHost())));
    });
  }

  public void uploadCallback(JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
    if (!assertNotNull(body, handler))
      return;
    boolean result = body.getBoolean("result");
    if (result) {
      // TODO: remove appId/objectId/token from cache.
    } else {
      // TODO: remove appId/objectId/token from cache, delete File document.
    }
  }
}
