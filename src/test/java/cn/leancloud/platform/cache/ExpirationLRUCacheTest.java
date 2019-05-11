package cn.leancloud.platform.cache;

import cn.leancloud.platform.utils.StringUtils;
import com.google.gson.JsonObject;
import junit.framework.TestCase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExpirationLRUCacheTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testSimpleGet() throws Exception {
    ExpirationLRUCache<String, JsonObject> cache = new ExpirationLRUCache<>(100, 60000);
    String key = Instant.now().toString();
    JsonObject value = new JsonObject();
    value.addProperty("objectId", "abc");
    cache.put(key, value);
    JsonObject cachedValue = cache.get(key);
    assertTrue(null != cachedValue);
    assertTrue(cachedValue.get("objectId").getAsString().equals("abc"));
  }

  public void testExpirationGet() throws Exception {
    ExpirationLRUCache<String, JsonObject> cache = new ExpirationLRUCache<>(100, 60000);
    String key = Instant.now().toString();
    JsonObject value = new JsonObject();
    value.addProperty("objectId", "abc");
    cache.put(key, value, 5000);
    JsonObject cachedValue = cache.get(key);
    assertTrue(null != cachedValue);
    assertTrue(cachedValue.get("objectId").getAsString().equals("abc"));
    Thread.sleep(6000);
    cachedValue = cache.get(key);
    assertTrue(null == cachedValue);
    System.out.println(cache.weightedSize());

    assertTrue(cache.weightedSize() > 0);
    String newKey = Instant.now().toString();
    cachedValue = cache.get(newKey);
    assertTrue(null == cachedValue);
    cache.put(newKey, new JsonObject());
    cachedValue = cache.get(newKey);
    assertTrue(null != cachedValue);
    for (int i = 0; i < 100; i++) {
      Thread.sleep(5);
      cache.put(Instant.now().toString(), new JsonObject());
    }
    cachedValue = cache.get(newKey);
    assertTrue(null == cachedValue);
  }

  public void testMultiThreadRW() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    ExpirationLRUCache<String, String> cache = new ExpirationLRUCache<>(1000, 60000);
    int totalKeyNumber = 800;
    List<String> keys = new ArrayList<>(totalKeyNumber);
    for (int j = 0; j < totalKeyNumber; j++) {
      keys.add(String.format("key-%d", j));
    }

    int threadNum = 50;
    List<Thread> threads = new ArrayList<>(threadNum);
    List<Boolean> threadRWResult = new ArrayList<Boolean>(threadNum);
    for (int i = 0; i< threadNum; i++) {
      threadRWResult.add(true);
    }

    for (int i = 0; i< threadNum; i++) {
      final int curThreadIdx = i;
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          String allV = Thread.currentThread().getName();
          System.out.println("all value: " + allV + ". thread:" + Thread.currentThread().getName());
          int rwKeyNumber = 40;
          List<String> rwKeys = new ArrayList<>(rwKeyNumber);
          for (int k = 0; k < rwKeyNumber; k++) {
            String tmpK = keys.get(rand.nextInt(totalKeyNumber));
            rwKeys.add(tmpK);
            cache.put(tmpK, allV);
          }

          try {
            Thread.sleep(rand.nextInt(10000));
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          for (int k = 0; k < rwKeyNumber; k++) {
            String tmpK = rwKeys.get(k);
            String v = cache.get(tmpK);
            if (StringUtils.isEmpty(v)) {
              System.out.println("value not found!! key=" + tmpK);
              threadRWResult.set(curThreadIdx, false);
            }
          }
          try {
            Thread.sleep(120000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          System.out.println("all kv cache expired. thread:" + Thread.currentThread().getName());
          for (int k = 0; k < rwKeyNumber; k++) {
            String tmpK = rwKeys.get(k);
            String v = cache.get(tmpK);
            if (StringUtils.notEmpty(v)) {
              System.out.println("value not expirated!! key=" + tmpK);
              threadRWResult.set(curThreadIdx, false);
            }
          }
        }
      });
      threads.add(t);
      t.start();
    }
    threads.stream().forEach(t -> {
      try {
        t.join();
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
    });
    boolean failed = threadRWResult.stream().anyMatch(v -> !v);
    assertTrue(!failed);
  }
}
