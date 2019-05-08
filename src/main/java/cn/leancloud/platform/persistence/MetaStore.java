package cn.leancloud.platform.persistence;

import org.apache.commons.lang3.tuple.Pair;

public interface MetaStore {
  Pair<String, String> getApplicationKey(String appId);
  String getFileBucket(String appId);
  String getAllowOrigin(String appId);
  Boolean enableCreateClassFromClient(String appId);
}
