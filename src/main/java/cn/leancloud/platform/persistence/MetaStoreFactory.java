package cn.leancloud.platform.persistence;

import cn.leancloud.platform.persistence.impl.LocalMetaStoreFactory;

public abstract class MetaStoreFactory {

  public abstract MetaStore getStore();

  public static class Builder {
    public MetaStoreFactory build() {
      return new LocalMetaStoreFactory();
    }
  }
}
