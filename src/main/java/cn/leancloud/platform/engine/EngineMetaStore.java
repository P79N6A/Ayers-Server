package cn.leancloud.platform.engine;

// use zookeeper to synchronize LeanEngine Hook meta info.
// after started, EngineMetaStore will watch /{appid}-hook-meta directory.

public class EngineMetaStore {
  private static EngineMetaStore instance = new EngineMetaStore();
  public static EngineMetaStore getInstance() {
    return instance;
  }

  /**
   * Notice: should be invoked at main loop.
   */
  public void initialize() {
  }

  /**
   * get LeanEngine host, it is configurable.
   * @return
   */
  public String getEngineHost() {
    return "localhost:9090";
  }

  /**
   * get hook function path.
   * if no hook function found. return null, otherwise return valid leanengine path, such as "__after_save_for_Clazz".
   *
   * @param clazz
   * @param type
   * @return
   */
  public String getHookFunction(String clazz, HookType type) {
    return null;
  }
}
