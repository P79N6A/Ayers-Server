package cn.leancloud.platform.modules;

public class Relation {
  public static final String BUILTIN_ATTR_RELATION_OWNING_ID = "owningId";
  public static final String BUILTIN_ATTR_RELATIONN_RELATED_ID = "relatedId";

  public static final String RELATEDTO_QUERY_FORMAT = "{\"$relatedTo\":" +
          "{\"object\":{" +
          "    \"__type\":\"Pointer\"," +
          "    \"className\":\"%s\"," +
          "    \"objectId\":\"%s\"}," +
          " \"key\":\"%s\"}}";

  public static final String RELATEDBY_QUERY_FORMAT = "{\"$relatedBy\":" +
          "{\"object\":{" +
          "    \"__type\":\"Pointer\"," +
          "    \"className\":\"%s\"," +
          "    \"objectId\":\"%s\"}," +
          " \"key\":\"%s\"}}";
}
