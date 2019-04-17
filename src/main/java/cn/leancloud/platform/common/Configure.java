package cn.leancloud.platform.common;

public class Configure {
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

  public static final String CLASS_ATTR_OBJECT_ID = "objectId";
  public static final String CLASS_ATTR_CREATED_TS = "createdAt";
  public static final String CLASS_ATTR_UPDATED_TS = "updatedAt";
  public static final String CLASS_ATTR_MONGO_ID = "_id";

  public static final String QINIU_ACCESS_KEY = System.getProperty("qiniu.accessKey", "access");
  public static final String QINIU_SECRET_KEY = System.getProperty("qiniu.secretKey", "secret");;
  public static final String FILE_DEFAULT_BUCKET = System.getProperty("file.bucket", "lc-unified");
  public static final String FILE_PROVIDER = System.getProperty("file.provider", "qiniu");
  public static final String FILE_DEFAULT_HOST = System.getProperty("file.host", "");
}
