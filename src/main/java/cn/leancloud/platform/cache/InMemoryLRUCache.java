package cn.leancloud.platform.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.Set;

public class InMemoryLRUCache<K, V> {
  private ConcurrentLinkedHashMap<K, V> cache;

  public InMemoryLRUCache(long capacity) {
    cache = new ConcurrentLinkedHashMap.Builder<K, V>()
            .maximumWeightedCapacity(capacity)
            .build();
  }

  public void put(K key, V value) {
    this.cache.put(key, value);
  }

  public void putIfAbsent(K key, V value) {
    this.cache.putIfAbsent(key, value);
  }

  public void remove(K key) {
    this.cache.remove(key);
  }

  public V get(K key) {
    return this.cache.get(key);
  }

  public V getOrDefault(K key, V defaultV) {
    return this.cache.getOrDefault(key, defaultV);
  }

  public V getQuietly(K key) {
    return this.cache.getQuietly(key);
  }

  public long capacity() {
    return cache.capacity();
  }

  public long size() {
    return this.cache.size();
  }

  public long weightedSize() {
    return this.cache.weightedSize();
  }

  public boolean isFull() {
    return size() >= capacity();
  }

  public Set<K> keySet() {
    return this.cache.keySet();
  }

  public void clear() {
    this.cache.clear();
  }
}
