package cn.leancloud.platform.cache;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;

// two level cache:
// L1. in-memory cache supported by ConcurrentLinkedHashMap
// L2. redis cache supported by redis cluster(sentinel)
//
public class UnifiedCache {

//  private SimpleRedisClient redisClient;
  private InMemoryLRUCache<String, JsonObject> memoryCache;
  private UnifiedCache(int capacity) {
    this.memoryCache = new InMemoryLRUCache<>(capacity);
//    this.redisClient = new SimpleRedisClient();
  }

  private static UnifiedCache globalInstance = new UnifiedCache(102400);
  public static UnifiedCache getGlobalInstance() {
    return globalInstance;
  }

  public void put(String key, JsonObject value) {
    if (StringUtils.notEmpty(key)) {
      this.memoryCache.put(key, value);
    }
  }

  public void putIfAbsent(String key, JsonObject value) {
    this.memoryCache.putIfAbsent(key, value);
  }

  public void remove(String key) {
    this.memoryCache.remove(key);
  }

  public JsonObject get(String key) {
    return this.memoryCache.get(key);
  }

}
