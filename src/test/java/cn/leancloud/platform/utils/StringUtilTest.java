package cn.leancloud.platform.utils;

import cn.leancloud.platform.modules.ObjectSpecifics;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Date;

public class StringUtilTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testMD5() {
    String appId = "appId";
    String sql = "select count(*) from table";
    long curHourTime = new Date().getTime() / 3600000;
    String jobId = StringUtils.computeMD5(String.format("%s-%d-%s", appId, curHourTime, sql));
    assertTrue(jobId.length() > 0);
  }

  public void testPathValidator() {
    String[] paths = new String[]{"/1.1/classes/Post", "/1.1/classeses/Post", "/api/1.1/classes/Post", "/1.1/classes/", "/1.1/classes", "/1.1/classes/Post/fhei"};
    boolean[] expected = new boolean[]{true, false, false, false, true, true};

    for (int i = 0; i < paths.length; i++) {
      boolean ret = ObjectSpecifics.validateRequestPath(paths[i]);
      assertTrue(ret == expected[i]);
      System.out.println("passed " + paths[i]);
    }
  }

  public void testMongoPoint() {

    JsonArray jsonArray = new JsonArray(Arrays.asList(124.6682391, -17.8978304));
    JsonObject jobj = new JsonObject().put("type", "Point");
    jobj.put("coordinates", jsonArray);

    // below  jsonObject_loc contains the jsonobject as you want..
    JsonObject jsonObject_loc = new JsonObject();
    jsonObject_loc.put("loc", jobj);
    System.out.println(jsonObject_loc);
  }
}
