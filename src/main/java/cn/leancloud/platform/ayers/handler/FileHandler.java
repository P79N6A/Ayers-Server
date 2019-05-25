package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.ayers.RequestParse;
import cn.leancloud.platform.cache.InMemoryLRUCache;
import cn.leancloud.platform.common.Configure;
import cn.leancloud.platform.common.Constraints;
import cn.leancloud.platform.modules.LeanObject;
import cn.leancloud.platform.utils.HandlerUtils;
import cn.leancloud.platform.utils.MimeUtils;
import cn.leancloud.platform.utils.StringUtils;
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
  public static final String PARAM_RESULT = "result";
  private static final InMemoryLRUCache<String, JsonObject> fileTokenCache = new InMemoryLRUCache<>(10000);

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
    body.remove(LeanObject.ATTR_NAME_TYPE);
    body.put(LeanObject.BUILTIN_ATTR_FILE_URL, configure.fileDefaultHost() + key)
            .put(LeanObject.BUILTIN_ATTR_FILE_MIMETYPE, mimeType)
            .put(LeanObject.BUILTIN_ATTR_FILE_PROVIDER,configure.fileProvideName())
            .put(LeanObject.BUILTIN_ATTR_FILE_BUCKET, bucketName);

    RequestParse.RequestHeaders headers = RequestParse.extractLCHeaders(this.routingContext);

    sendDataOperationWithOption(Constraints.FILE_CLASS, null, httpMethod.toString(), null, body,
            true, headers, res -> {
      if (res.succeeded()) {
        String objectId = res.result().getString(LeanObject.ATTR_NAME_OBJECTID);
        fileTokenCache.put(token, new JsonObject().put(LeanObject.ATTR_NAME_OBJECTID, objectId));
      }
      handler.handle(res.map(file -> file.mergeIn(body).put(PARAM_FILE_TOKEN, token).put(PARAM_FILE_UPLOAD_URL, configure.fileUploadHost())));
    });
  }

  // request body as following:
  // {
  //  "result":true,
  //  "token":"w6ZYeC-arS2yNzZ9"
  // }
  public void uploadCallback(JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
    if (!assertNotNull(body, handler))
      return;
    boolean result = body.getBoolean(PARAM_RESULT);
    String token = body.getString(PARAM_FILE_TOKEN);
    if (StringUtils.isEmpty(token)) {
      handler.handle(HandlerUtils.wrapErrorResult(new IllegalArgumentException("token is required.")));
      return;
    }
    JsonObject fileObject = fileTokenCache.get(token);
    if (result) {
      fileTokenCache.remove(token);
      handler.handle(HandlerUtils.wrapActualResult(new JsonObject().put(PARAM_RESULT, "ok")));
    } else if (null == fileObject) {
      handler.handle(HandlerUtils.wrapErrorResult(new IllegalAccessException("token is expired.")));
    } else {
      sendDataOperationWithoutCheck(Constraints.FILE_CLASS, fileObject.getString(LeanObject.ATTR_NAME_OBJECTID),
              RequestParse.OP_OBJECT_DELETE, fileObject, null, false, null, res -> {
        handler.handle(res);
              });
    }
  }
}
