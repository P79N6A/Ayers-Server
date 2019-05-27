package cn.leancloud.platform.utils;

import junit.framework.TestCase;

import java.util.List;

public class NetworkUtilsTest extends TestCase{

  public void testGetMXRecords() {
    List<String> results = NetworkUtils.getMXRecord("leancloud.rocks");
    assertTrue(results.size() > 0);

    results = NetworkUtils.getMXRecord("163.com");
    assertTrue(results.size() > 0);
  }

  public void testEmailProvider() {
    boolean result = NetworkUtils.isDomesticProvider("jfeng@avoscloud.com");
    assertTrue(!result);
    result = NetworkUtils.isDomesticProvider("jfeng@163.com");
    assertTrue(result);
  }

}
