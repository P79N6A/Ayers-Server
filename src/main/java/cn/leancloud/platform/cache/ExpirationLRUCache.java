package cn.leancloud.platform.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Set;

public class ExpirationLRUCache<K, V>{
  private static final Logger logger = LoggerFactory.getLogger(ExpirationLRUCache.class);

  private static class ValueNode<V> {
    V data;
    long ttlMS;
    public ValueNode(V data, long ttlMS) {
      this.data = data;
      this.ttlMS = ttlMS;
    }
    public V value() {
      if (Instant.now().toEpochMilli() > this.ttlMS) {
        return null;
      }
      return this.data;
    }
    public long ttl() {
      return this.ttlMS;
    }
  }
  private int maxTTL;
  private InMemoryLRUCache<K, ValueNode<V>> memoryCache;
  private Thread timer;

  public ExpirationLRUCache(long capacity, int maxTTL) {
    this.memoryCache = new InMemoryLRUCache<>(capacity);
    this.maxTTL = maxTTL;
    this.timer = new Thread(new Runnable() {
      @Override
      public void run() {
        while(true) {
          try {
            Thread.sleep( 60000);
            logger.debug("start expiration check...");
            long now = Instant.now().toEpochMilli();
            memoryCache.keySet().stream().forEach(k -> {
              ValueNode value = getRaw(k);
              if (null == value || now > value.ttl()) {
                logger.debug("eviction expired key=" + k);
                remove(k);
              }
            });
          } catch (InterruptedException ex) {
            return;
          }
        }
      }
    });
    this.timer.start();
  }

  public void put(K key, V value) {
    put(key, value, maxTTL);
  }

  public void put(K key, V value, int ttl) {
    if (null == value) {
      remove(key);
      return;
    }
    if (ttl <= 0) {
      return;
    }
    Instant now = Instant.now();
    this.memoryCache.put(key, new ValueNode<V>(value, now.toEpochMilli() + ttl));
  }

  public void putIfAbsent(K key, V value, int ttl) {
    Instant now = Instant.now();
    ValueNode v = new ValueNode<V>(value, now.toEpochMilli() + ttl);
    this.memoryCache.putIfAbsent(key, v);
  }

  public void remove(K key) {
    this.memoryCache.remove(key);
  }

  private ValueNode<V> getRaw(K key) {
    return this.memoryCache.get(key);
  }

  public V get(K key) {
    ValueNode<V> node = this.memoryCache.get(key);
    if (null == node) {
      return null;
    }
    return node.value();
  }

  public V getOrDefault(K key, V defaultV) {
    V res = get(key);
    if (null == res) {
      return defaultV;
    }
    return res;
  }

  public V getQuietly(K key) {
    ValueNode<V> node = this.memoryCache.getQuietly(key);
    if (null == node) {
      return null;
    }
    return node.value();
  }

  public long capacity() {
    return this.memoryCache.capacity();
  }

  public long size() {
    return this.memoryCache.size();
  }

  public boolean isFull() {
    return this.memoryCache.isFull();
  }

  public Set<K> keySet() {
    return this.memoryCache.keySet();
  }

  public void clear() {
    this.memoryCache.clear();
  }

  public long weightedSize() {
    return this.memoryCache.weightedSize();
  }
}
