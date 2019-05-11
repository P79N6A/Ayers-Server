package cn.leancloud.platform.modules;

import cn.leancloud.platform.common.Constraints;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * class meta data.
 * for example:
 * 1. { "_id" : ObjectId("5cd036160237d7006f42228f"), "schema" : { "objectId" : { "type" : "String" }, "ACL" : { "type" : "ACL" }, "_r" : { "type" : "Array" }, "_w" : { "type" : "Array" }, "createdAt" : { "type" : "Date" }, "updatedAt" : { "type" : "Date" }, "age" : { "type" : "Number" }, "content" : { "type" : "String" } }, "updatedAt" : ISODate("2019-05-07T01:48:39.682Z"), "permissions" : { "createSingleObject" : { "*" : true }, "findMetaInfo" : { "*" : true }, "get" : { "*" : true }, "updateSingleObject" : { "*" : true }, "deleteSingleObject" : { "*" : true }, "add_fields" : { "*" : true } }, "v" : NumberLong(1), "name" : "OwnerStrictReadWrite", "createdAt" : ISODate("2019-05-06T13:26:46Z"), "class-type" : "normal", "at" : { "_owner" : { "read" : true, "write" : true }, "*" : { "read" : false, "write" : false } }, "options" : { "row-cache" : false }, "custom-acl" : true }
 * 2. { "_id" : ObjectId("5caed4cbc8959c0073d57eac"), "schema" : { "objectId" : { "type" : "String" }, "ACL" : { "type" : "ACL" }, "_r" : { "type" : "Array" }, "_w" : { "type" : "Array" }, "createdAt" : { "type" : "Date" }, "updatedAt" : { "type" : "Date" }, "content" : { "type" : "String" }, "pubUser" : { "type" : "String" }, "pubTimestamp" : { "type" : "Number" } }, "updatedAt" : ISODate("2019-04-12T03:02:29.683Z"), "permissions" : { "createSingleObject" : { "*" : true }, "findMetaInfo" : { "*" : true }, "get" : { "*" : true }, "updateSingleObject" : { "*" : true }, "deleteSingleObject" : { "*" : true }, "add_fields" : { "*" : true } }, "v" : NumberLong(1), "name" : "Post", "createdAt" : ISODate("2019-04-11T05:46:51Z"), "class-type" : "normal", "at" : null, "options" : { "row-cache" : false } }
 */
public class ClassMetaData extends LeanObject {
  private static final String ATTR_NAME = "name";
  private static final String ATTR_SCHEMA = "schema";
  private static final String ATTR_INDICES = "indices";
  private static final String ATTR_ACL_TEMPLATE = "at";
  private static final String ATTR_CLASS_PERMISSIONS = "permissions";
  private static final String ATTR_OPTIONS = "options";
  private static final String ATTR_CUSTOM_ACL = "custom-acl";
  private static final String ATTR_CLASS_TYPE = "class-type";

  private static final JsonObject defaultClassPermissions =
          new JsonObject("{ \"create\" : { \"*\" : true }, \"find\" : { \"*\" : true }, \"get\" : { \"*\" : true }, " +
                  "\"update\" : { \"*\" : true }, \"delete\" : { \"*\" : true }, \"add_fields\" : { \"*\" : true } }");
  private static final JsonObject defaultUserClassPermissions =
          new JsonObject("{ \"create\" : { \"*\" : true }, \"find\" : { \"*\" : false }, \"get\" : { \"*\" : true }, " +
                  "\"update\" : { \"onlySignInUsers\" : true }, \"delete\" : { \"*\" : false }, \"add_fields\" : { \"*\" : true } }");
  private static final String defaultClassType = "normal";
  private static final JsonObject defaultOptions = new JsonObject("{ \"row-cache\" : false }");
  private static final JsonObject defaultSchema = new JsonObject("{\"objectId\":{\"type\":\"String\"}," +
          "\"ACL\":{\"type\":\"ACL\"},\"createdAt\":{\"type\":\"Date\"},\"updatedAt\":{\"type\":\"Date\"}}");

  private static final JsonObject defaultFileSchema = new JsonObject("{\"mime_type\":{\"type\":\"String\"},\"ACL\":{\"type\":\"ACL\"}," +
          "\"updatedAt\":{\"type\":\"Date\"},\"key\":{\"type\":\"String\"},\"name\":{\"type\":\"String\"}," +
          "\"objectId\":{\"type\":\"String\"},\"createdAt\":{\"type\":\"Date\"},\"url\":{\"type\":\"String\"}," +
          "\"provider\":{\"type\":\"String\"},\"metaData\":{\"type\":\"Object\"},\"bucket\":{\"type\":\"String\"}}");

  private static final JsonObject defaultUserSchema = new JsonObject("{\"salt\":{\"type\":\"String\"}," +
          "\"email\":{\"type\":\"String\"},\"sessionToken\":{\"type\":\"String\"},\"updatedAt\":{\"type\":\"Date\"}," +
          "\"password\":{\"type\":\"String\"},\"name\":{\"type\":\"String\"},\"objectId\":{\"type\":\"String\"}," +
          "\"username\":{\"type\":\"String\"},\"createdAt\":{\"type\":\"Date\"},\"emailVerified\":{\"type\":\"Boolean\"}," +
          "\"mobilePhoneNumber\":{\"type\":\"String\"},\"authData\":{\"type\":\"Object\",\"hidden\":true}," +
          "\"mobilePhoneVerified\":{\"type\":\"Boolean\"}, \"ACL\":{\"type\":\"ACL\"}}");
  private static final JsonObject defaultConversationSchema = new JsonObject("{\"unique\":{\"type\":\"Boolean\"}," +
          "\"updatedAt\":{\"type\":\"Date\"},\"name\":{\"type\":\"String\"},\"objectId\":{\"type\":\"String\"}," +
          "\"m\":{\"type\":\"Array\"},\"tr\":{\"type\":\"Boolean\"},\"createdAt\":{\"type\":\"Date\"},\"ACL\":{\"type\":\"ACL\"}," +
          "\"lm\":{\"type\":\"Date\"},\"uniqueId\":{\"type\":\"String\"},\"mu\":{\"type\":\"Array\"},\"sys\":{\"type\":\"Boolean\"}}");

  public ClassMetaData(JsonObject data) {
    super(Constraints.METADATA_CLASS, data);
  }

  public static ClassMetaData fromJson(JsonObject data) {
    return new ClassMetaData(data);
  }
  public static Schema getDefaultSchema() {
    return new Schema(defaultSchema);
  }

  public ClassMetaData() {
    super(Constraints.METADATA_CLASS);
    put(ATTR_OPTIONS, defaultOptions);
    put(ATTR_CLASS_PERMISSIONS, defaultClassPermissions);
    put(ATTR_CLASS_TYPE, defaultClassType);
    put(ATTR_SCHEMA, defaultSchema);
  }

  public ClassMetaData(String className) {
    this();
    setName(className);
    if (Constraints.USER_CLASS.equals(className)) {
      put(ATTR_CLASS_PERMISSIONS, defaultUserClassPermissions);
      put(ATTR_SCHEMA, defaultUserSchema);
    } else if (Constraints.FILE_CLASS.equals(className)) {
      put(ATTR_SCHEMA, defaultFileSchema);
    } else if (Constraints.CONVERSATION_CLASS.equals(className)) {
      put(ATTR_SCHEMA, defaultConversationSchema);
    }
  }

  public ClassMetaData(String className, JsonObject schema, JsonArray indices) {
    this(className);
    if (null != schema) {
      setSchema(schema);
    }
    setIndices(indices);
  }

  public String getName() {
    return this.getString(ATTR_NAME);
  }
  public void setName(String name) {
    this.put(ATTR_NAME, name);
  }

  public Schema getSchema() {
    return new Schema(this.getJsonObject(ATTR_SCHEMA));
  }
  public void setSchema(JsonObject schema) {
    this.put(ATTR_SCHEMA, schema);
  }

  public JsonArray getIndices() {
    return this.getJsonArray(ATTR_INDICES);
  }

  public void setIndices(JsonArray array) {
    this.put(ATTR_INDICES, array);
  }

  public void addIndex(JsonObject index) {
    JsonArray indices = getIndices();
    if (null == indices) {
      indices = new JsonArray();
    }
    indices.add(index);
  }

  public JsonObject getACLTemplate() {
    return this.getJsonObject(ATTR_ACL_TEMPLATE);
  }

  public void setACLTemplate(JsonObject at) {
    this.put(ATTR_ACL_TEMPLATE, at);
    if (null != at) {
      this.put(ATTR_CUSTOM_ACL, true);
    }
  }

  public JsonObject getOptions() {
    return this.getJsonObject(ATTR_OPTIONS);
  }

  public JsonObject getClassPermissions() {
    return this.getJsonObject(ATTR_CLASS_PERMISSIONS);
  }

  public void setClassPermissions(JsonObject permissions) {
    this.put(ATTR_CLASS_PERMISSIONS, permissions);
  }

  public void setClassType(String classType) {
    put(ATTR_CLASS_TYPE, classType);
  }

  public boolean isCustomACL() {
    return this.getBoolean(ATTR_CUSTOM_ACL, false);
  }
}
