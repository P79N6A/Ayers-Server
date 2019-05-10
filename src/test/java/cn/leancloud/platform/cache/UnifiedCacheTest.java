package cn.leancloud.platform.cache;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

public class UnifiedCacheTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testSaveAndRead() throws Exception {
    UnifiedCache unifiedCache = UnifiedCache.getGlobalInstance();
    unifiedCache.put("sessionToken", new JsonObject().put("objectId", "321fhiehfiei").put("sessionToken", "sessionToken"));
    assertTrue(null != unifiedCache.get("sessionToken"));
    JsonObject user = (JsonObject) unifiedCache.get("sessionToken");
    assertTrue(user.getString("objectId").equals("321fhiehfiei"));
  }
}
