package cn.leancloud.platform.common;

import junit.framework.TestCase;

import java.util.Date;

public class StringUtilTests extends TestCase {
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
}
