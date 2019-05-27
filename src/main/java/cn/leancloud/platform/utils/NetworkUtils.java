package cn.leancloud.platform.utils;

import cn.leancloud.platform.cache.ExpirationLRUCache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.util.*;

public class NetworkUtils {
  private static final String[] ChinaDomains = new String[]{"qq.com.", "netease.com.", "sina.com.cn.", "163.com.",
          "126.com.", "dangdang.com.", "sohu.com.cn.", "aliyun.com.", "2980.com.", "baidu.com.", "alibaba-inc.com.",
          "jd.com.", "huawei.com.", "mxhichina.com.", "263xmail.com."};
  private static final Set<String> ChinaDomainSet= new HashSet<>();
  private static final ExpirationLRUCache<String, List<String>> MXRecordCache = new ExpirationLRUCache<>(1024, 24 * 3600 * 7 * 1000);

  static {
    ChinaDomainSet.addAll(Arrays.asList(ChinaDomains));
  }

  public static List<String> getMXRecord(String domain) {
    try {
      List result = new ArrayList();
      Lookup lookup = new Lookup(domain, Type.MX);
      Record[] records = lookup.run();
      for (Record record : records) {
        String[] parts = record.rdataToString().split(" ");
        result.add(parts[1]);
      }
      return result;
    } catch (TextParseException ex) {
      ex.printStackTrace();
    }
    return new ArrayList();
  }

  public static boolean isDomesticProvider(String email) {
    if (StringUtils.isEmpty(email) || email.indexOf("@") < 0) {
      return false;
    }
    String[] parts = email.split("@");
    if (parts.length <= 1) {
      return false;
    }
    String domain = parts[1];
    List<String> records = MXRecordCache.get(domain);
    if (null == records || records.size() < 1) {
      records = getMXRecord(domain);
      if (records.size() > 0) {
        MXRecordCache.put(domain, records);
      }
    }
    if (records.size() < 1) {
      return false;
    }
    long domesticRecordCount = records.stream().filter(s ->
            Arrays.stream(ChinaDomains).filter(suffix -> s.endsWith(suffix)).count() > 0
         ).count();
    return domesticRecordCount > 0;
  }
}
