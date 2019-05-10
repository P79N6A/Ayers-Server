package cn.leancloud.platform.modules;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

public class ACLTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testDefaultACL() {
    ACL defaultOne = ACL.publicRWInstance();
    System.out.println(defaultOne.toJsonString());
    System.out.println(defaultOne.toJson().toString());
  }

  public void testTemplateforOwnerRW() {
    String at = "{ \"_owner\" : { \"read\" : true, \"write\" : true }, \"*\" : { \"read\" : false, \"write\" : false } }";
    ObjectACLTemplate template = ObjectACLTemplate.Builder.build(new JsonObject(at));
    assertTrue(null != template);
    ACL acl1 = template.genACL4User(null, null);
    assertTrue(!acl1.getPublicReadAccess());
    assertTrue(!acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));
    acl1 = template.genACL4User("uid22", null);
    assertTrue(!acl1.getPublicReadAccess());
    assertTrue(!acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));

    acl1 = template.genACL4User("uid11", null);
    assertTrue(!acl1.getPublicReadAccess());
    assertTrue(!acl1.getPublicWriteAccess());
    assertTrue(acl1.getReadAccess("uid11"));
    assertTrue(acl1.getWriteAccess("uid11"));
  }

  public void testTemplateforPublicOwnerR() {
    String at = "{ \"_owner\" : { \"read\" : true, \"write\" : false }, \"*\" : { \"read\" : true, \"write\" : false } }";
    ObjectACLTemplate template = ObjectACLTemplate.Builder.build(new JsonObject(at));
    assertTrue(null != template);
    ACL acl1 = template.genACL4User(null, null);
    System.out.println(acl1.toJson());
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(!acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));
    acl1 = template.genACL4User("uid22", null);
    System.out.println(acl1.toJson());
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(!acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));

    acl1 = template.genACL4User("uid11", null);
    System.out.println(acl1.toJson());
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(!acl1.getPublicWriteAccess());
    assertTrue(acl1.getReadAccess("uid11"));
    assertTrue(!acl1.getWriteAccess("uid11"));
  }

  public void testTemplateforInvalidInput() {
    String at = "{ \"owner\" : { \"read\" : true, \"write\" : false }, \"public\" : { \"read\" : true, \"write\" : false } }";
    ObjectACLTemplate template = ObjectACLTemplate.Builder.build(new JsonObject(at));
    assertTrue(null != template);
    ACL acl1 = template.genACL4User(null, null);
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));
    acl1 = template.genACL4User("uid22", null);
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));

    acl1 = template.genACL4User("uid11", null);
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(acl1.getPublicWriteAccess());
    assertTrue(acl1.getReadAccess("uid11"));
    assertTrue(acl1.getWriteAccess("uid11"));
  }

  public void testTemplateforNullInput() {
    ObjectACLTemplate template = ObjectACLTemplate.Builder.build(null);
    assertTrue(null != template);
    ACL acl1 = template.genACL4User(null, null);
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));
    acl1 = template.genACL4User("uid22", null);
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(acl1.getPublicWriteAccess());
    assertTrue(!acl1.getReadAccess("uid"));
    assertTrue(!acl1.getWriteAccess("uid"));

    acl1 = template.genACL4User("uid11", null);
    assertTrue(acl1.getPublicReadAccess());
    assertTrue(acl1.getPublicWriteAccess());
    assertTrue(acl1.getReadAccess("uid11"));
    assertTrue(acl1.getWriteAccess("uid11"));
  }
}
