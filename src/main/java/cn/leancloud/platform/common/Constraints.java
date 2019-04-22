package cn.leancloud.platform.common;

import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constraints {
  public static final String OP_OBJECT_UPSERT = "upsert";
  public static final String OP_OBJECT_DELETE = "delete";
  public static final String OP_OBJECT_QUERY = "query";
  public static final String OP_USER_SIGNIN = "login";

  public static final String INSTALLATION_CLASS = "_Installation";
  public static final String USER_CLASS = "_User";
  public static final String FILE_CLASS = "_File";
  public static final String ROLE_CLASS = "_Role";
  public static final String FOLLOWSHIP_CLASS = "_Follow";

  public static final String QUERY_KEY_COUNT = "count";
  public static final String QUERY_KEY_WHERE = "where";
  public static final String QUERY_KEY_LIMIT = "limit";
  public static final String QUERY_KEY_SKIP = "skip";
  public static final String QUERY_KEY_ORDER = "order";
  public static final String QUERY_KEY_INCLUDE = "include";
  public static final String QUERY_KEY_KEYS = "keys";

  public static final String INTERNAL_MSG_ATTR_CLASS = "class";
  public static final String INTERNAL_MSG_ATTR_OBJECT_ID = "objectId";
  public static final String INTERNAL_MSG_ATTR_PARAM = "param";
  public static final String INTERNAL_MSG_HEADER_OP = "operation";
  public static final String INTERNAL_MSG_ATTR_FETCHWHENSAVE = "fetchWhenSave";

  public static final String CLASS_ATTR_OBJECT_ID = "objectId";
  public static final String CLASS_ATTR_CREATED_TS = "createdAt";
  public static final String CLASS_ATTR_UPDATED_TS = "updatedAt";
  public static final String CLASS_ATTR_MONGO_ID = "_id";

  public static final String PARAM_FILE_UPLOAD_URL = "upload_url";
  public static final String PARAM_FILE_TOKEN = "token";

  public static final String BUILTIN_ATTR_SESSION_TOKEN = "sessionToken";
  public static final String BUILTIN_ATTR_ACL = "ACL";
  public static final String BUILTIN_ATTR_SALT = "salt";
  public static final String BUILTIN_ATTR_PASSWORD = "password";
  public static final String BUILTIN_ATTR_EMAIL_VERIFIED = "emailVerified";
  public static final String BUILTIN_ATTR_PHONENUM_VERIFIED = "mobilePhoneVerified";

  public static final String BUILTIN_ATTR_FILE_URL = "url";
  public static final String BUILTIN_ATTR_FILE_MIMETYPE = "mime_type";
  public static final String BUILTIN_ATTR_FILE_PROVIDER = "provider";
  public static final String BUILTIN_ATTR_FILE_BUCKET = "bucket";

  public static final int SESSION_TOKEN_LENGTH = 24;
  public static final int SALT_LENGTH = 24;

  public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
}
