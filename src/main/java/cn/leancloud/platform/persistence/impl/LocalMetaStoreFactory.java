package cn.leancloud.platform.persistence.impl;

import cn.leancloud.platform.persistence.MetaStore;
import cn.leancloud.platform.persistence.MetaStoreFactory;

public class LocalMetaStoreFactory extends MetaStoreFactory {

  public MetaStore getStore() {
    return new LocalDedicatedMetaStore();
  }
}
