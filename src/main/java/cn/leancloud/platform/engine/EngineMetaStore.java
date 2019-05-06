package cn.leancloud.platform.engine;

// use zookeeper to synchronize LeanEngine Hook meta info.
// after started, EngineMetaStore will watch /{appid}-hook-meta directory.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class EngineMetaStore {
  private static final Logger logger = LoggerFactory.getLogger(EngineMetaStore.class);
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
    logger.debug("add hook func: __before_save_for_Review");
    hookFuntions.put("__before_save_for_Review", 1);
    hookFuntions.put("__after_save_for_Review", 1);
  }

  /**
   * get LeanEngine host, it is configurable.
   * @return
   */
  public String getEngineHost() {
    return "localhost";
  }

  public int getEnginePort() {
    return 3000;
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
//    logger.debug("look for key: " + key);
    if (hookFuntions.containsKey(key)) {
      String path = String.format(HOOK_FUNC_REQUEST_PATH, clazz, type.getRequestPath());
//      logger.debug("found path: " + path);
      return path;
    } else {
      return null;
    }
  }
}
