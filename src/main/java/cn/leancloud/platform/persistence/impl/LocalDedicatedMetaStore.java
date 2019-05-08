package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.persistence.MetaStore;
import org.apache.commons.lang3.tuple.Pair;

public class LocalDedicatedMetaStore implements MetaStore {
  public Pair<String, String> getApplicationKey(String appId) {
    return null;
  }
  public String getFileBucket(String appId) {
    return null;
  }
  public String getAllowOrigin(String appId) {
    return null;
  }
  public Boolean enableCreateClassFromClient(String appId) {
    return true;
  }
}
