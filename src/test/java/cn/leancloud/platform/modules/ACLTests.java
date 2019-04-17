package cn.leancloud.platform.modules;

import junit.framework.TestCase;

public class ACLTests extends TestCase {
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
}
