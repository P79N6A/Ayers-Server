package cn.leancloud.platform.ayers.handler;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;

public class CommonHandlerTests extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testParseBatchPath() throws Exception {
    String[] paths = new String[]{"1.1/classes/Post//", "", "/1.1/user/fhiea", "/1.1/class/Post/hfiea", "/classes/Post/"};
    boolean[] expected = new boolean[]{false, false, false, false, false};
    for (int i = 0; i < paths.length; i++) {
      Pair<String, String> p = CommonHandler.parseClazzAndObjectId(paths[i]);
      System.out.println("testSchema " + paths[i] + "||| pair: " + p.getLeft() + "," + p.getRight());
      assertTrue(expected[i] == p.getLeft().length() > 0);
    }
  }

  public void testRegexMatch() throws Exception {
    String[] paths = new String[]{"/1.1/classes/Post/Abc", "/1.1/classes", "/1.1/users/obj?", "/1.1/users",
            "/1.1/installations/fhei", "/1.1/installations", "/1.1/installations/", "/1.1/files",
            "/1.1/files/feifahie", "/1.1/files/fhaiefhe?afhie", "/1.1/roles", "/1.1/roles/hfiaeaihfhh?",
            "/1.1/class/fehia", "1.1/classes/testSchema", "/1.1/testSchema", "/1.1/classesfe/Post"};
    boolean[] expecteds = new boolean[]{true, false, true, true,
            true, true, true, true,
            true, true, true, true,
            false, false, false, false};
    for (int i = 0; i < paths.length; i++) {
      Pair<String, String> p = CommonHandler.parseClazzAndObjectId(paths[i]);
      System.out.println("testSchema " + paths[i] + "||| pair: " + p.getLeft() + "," + p.getRight());
      assertTrue(expecteds[i] == p.getLeft().length() > 0);
    }
  }
}
