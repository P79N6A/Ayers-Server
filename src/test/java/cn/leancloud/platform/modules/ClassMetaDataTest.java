package cn.leancloud.platform.modules;

import junit.framework.TestCase;

public class ClassMetaDataTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testDefaultSchema() throws Exception {
    ClassMetaData userMeta = new ClassMetaData("_User");
    assertTrue(null != userMeta.getSchema());
    assertTrue(userMeta.getSchema().getJsonObject("authData").getString("type").equals("Object"));
    assertTrue(userMeta.getSchema().getJsonObject("objectId").getString("type").equals("String"));
    assertTrue(userMeta.getSchema().getJsonObject("ACL").getString("type").equals("ACL"));
    assertTrue(userMeta.getSchema().getJsonObject("updatedAt").getString("type").equals("Date"));
    assertTrue(userMeta.getSchema().getJsonObject("createdAt").getString("type").equals("Date"));

    ClassMetaData fileMeta = new ClassMetaData("_File");
    assertTrue(null != fileMeta.getSchema());
    assertTrue(fileMeta.getSchema().getJsonObject("metaData").getString("type").equals("Object"));
    assertTrue(fileMeta.getSchema().getJsonObject("objectId").getString("type").equals("String"));
    assertTrue(fileMeta.getSchema().getJsonObject("ACL").getString("type").equals("ACL"));
    assertTrue(fileMeta.getSchema().getJsonObject("updatedAt").getString("type").equals("Date"));
    assertTrue(fileMeta.getSchema().getJsonObject("createdAt").getString("type").equals("Date"));

    ClassMetaData convMeta = new ClassMetaData("_Conversation");
    assertTrue(null != convMeta.getSchema());
    assertTrue(convMeta.getSchema().getJsonObject("mu").getString("type").equals("Array"));
    assertTrue(convMeta.getSchema().getJsonObject("objectId").getString("type").equals("String"));
    assertTrue(convMeta.getSchema().getJsonObject("ACL").getString("type").equals("ACL"));
    assertTrue(convMeta.getSchema().getJsonObject("updatedAt").getString("type").equals("Date"));
    assertTrue(convMeta.getSchema().getJsonObject("createdAt").getString("type").equals("Date"));

    ClassMetaData objectMeta = new ClassMetaData("Post");
    assertTrue(null != objectMeta.getSchema());
    assertTrue(objectMeta.getSchema().getJsonObject("objectId").getString("type").equals("String"));
    assertTrue(objectMeta.getSchema().getJsonObject("ACL").getString("type").equals("ACL"));
    assertTrue(objectMeta.getSchema().getJsonObject("updatedAt").getString("type").equals("Date"));
    assertTrue(objectMeta.getSchema().getJsonObject("createdAt").getString("type").equals("Date"));
  }
}
