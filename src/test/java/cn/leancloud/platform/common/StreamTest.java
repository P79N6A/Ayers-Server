package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.time.LocalTime;

public class StreamTest extends TestCase {
  public void testJsonStreamParallel() throws Exception {
    JsonObject json = new JsonObject();
    for (int i = 0; i < 100; i++) {
      json.put(String.format("name %d", i), i);
    }
    json.stream().parallel().forEach(entry -> {
      System.out.println(LocalTime.now() + " - value: " + entry.getKey() +
              " - thread: " + Thread.currentThread().getName());
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    System.out.println("MainThread:" + Thread.currentThread().getName());
  }

  public void testJsonStreamSequential() throws Exception {
    JsonObject json = new JsonObject();
    for (int i = 0; i < 100; i++) {
      json.put(String.format("name %d", i), i);
    }
    json.stream().sequential().forEach(entry -> {
      System.out.println(LocalTime.now() + " - value: " + entry.getKey() +
              " - thread: " + Thread.currentThread().getName());
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    System.out.println("MainThread:" + Thread.currentThread().getName());
  }
}
