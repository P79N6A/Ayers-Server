package cn.leancloud.platform.modules;

import junit.framework.TestCase;

public class ObjectSpecificsTests extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testCheckPath() throws Exception {
    String[] paths = new String[]{"/1.1/classes/Post/Abc", "/1.1/classes", "/1.1/users/obj?", "/1.1/users",
            "/1.1/installations/fhei", "/1.1/installations", "/1.1/installations/", "/1.1/files",
            "/1.1/files/feifahie", "/1.1/files/fhaiefhe?afhie", "/1.1/roles", "/1.1/roles/hfiaeaihfhh?",
    "/1.1/class/fehia", "1.1/classes/test", "/1.1/test", "/1.1/classesfe/Post"};
    boolean[] expecteds = new boolean[]{true, true, true, true,
        true, true, true, true,
        true, true, true, true,
        false, false, false, false};
    for (int i = 0; i < paths.length; i++) {
      boolean tmp = ObjectSpecifics.validRequestPath(paths[i]);
      System.out.println("check " + paths[i] + ", result=" + tmp);
      assertTrue( expecteds[i] == tmp);
    }
  }
}
