package cn.leancloud.platform.engine;

import cn.leancloud.platform.common.Configure;

import java.util.concurrent.ConcurrentHashMap;

/**
 * we use zookeeper to synchronize LeanEngine Hook meta info.
 * after started, EngineMetaStore will watch /{appid}-hook-meta directory.
  */

public class EngineMetaStore {
  private static final String HOOK_FUNC_FORMAT = "__%s_for_%s";
  private static final String HOOK_FUNC_REQUEST_PATH = "/1.1/functions/%s/%s";
  private static EngineMetaStore instance = new EngineMetaStore();

  private EngineMetaStore() {
  }

  public static EngineMetaStore getInstance() {
    return instance;
  }

  private ConcurrentHashMap<String, Integer> hookFuntions = new ConcurrentHashMap<>(50);

  /**
   * Notice: should be invoked at main loop.
   */
  public void initialize() {
    // TODO: connect to zookeeper, sync and listen hook meta data.

  }

  /**
   * get LeanEngine host, it is configurable.
   * @return
   */
  public String getEngineHost() {
    return Configure.getInstance().getUluruEngineHost();
  }

  /**
   * get leanengine port, it's configurable.
   * @return
   */
  public int getEnginePort() {
    return Configure.getInstance().getUluruEnginePort();
  }

  /**
   * get hook function path.
   * if no hook function found. return null, otherwise return valid leanengine path, such as "__after_save_for_Clazz".
   *
   * @param clazz
   * @param type
   * @return
   */
  public String getHookFunctionPath(String clazz, HookType type) {
    String key = String.format(HOOK_FUNC_FORMAT, type.getName(), clazz);
    if (hookFuntions.containsKey(key)) {
      return String.format(HOOK_FUNC_REQUEST_PATH, clazz, type.getRequestPath());
    } else {
      return null;
    }
  }
}
