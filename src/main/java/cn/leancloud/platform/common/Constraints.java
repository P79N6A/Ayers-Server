package cn.leancloud.platform.common;

public class Constraints {

  public static final String INSTALLATION_CLASS = "_Installation";
  public static final String USER_CLASS = "_User";
  public static final String FILE_CLASS = "_File";
  public static final String ROLE_CLASS = "_Role";
  public static final String CONVERSATION_CLASS = "_Conversation";
  public static final String FOLLOWSHIP_CLASS = "_Follow";
  public static final String METADATA_CLASS = "_Class";

  public static final int SESSION_TOKEN_LENGTH = 24;
  public static final int SALT_LENGTH = 24;
  public static final int MAX_CLASS_COUNT = 500;

  public static final int MAX_OBJECT_SIZE = 16*1024*1024;
  public static final int MAX_QUERY_RESULT_COUNT = 1000;

  public static final String EMAIL_VERIFY_PATH = "/emailVerify/";

  public static final String RELATION_TABLE_FORMAT = "_Join:%s:%s:%s";

  public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  public static String getRelationTable(String fromClazz, String toClazz, String field) {
    return String.format(RELATION_TABLE_FORMAT, toClazz, field, fromClazz);
  }
}
