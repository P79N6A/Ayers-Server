package cn.leancloud.platform.modules;

import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.time.Instant;
import java.util.Arrays;

import cn.leancloud.platform.modules.Schema.CompatResult;

public class LeanObjectTests extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testGetSchema() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    object.put("location", new JsonObject().put("latitude", 34.5).put("longitude", -87.4));
    Schema schema = object.guessSchema();
    System.out.println(schema);
  }

  public void testSchemaCheckSimple() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    LeanObject object2 = new LeanObject("Post");
    object2.put("title", "LeanCloud");
    object2.put("publishTime", Instant.now());
    object2.put("commentCounts", 199);
    object2.put("dislike", -199);
    object2.put("spam", "false");
    try {
      object.guessSchema().compatibleWith(object2.guessSchema());
      fail();
    } catch (ConsistencyViolationException ex) {
      ;
    }
  }

  public void testSchemaCheckPointer() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    LeanObject object2 = new LeanObject("Post");
    object2.put("title", "LeanCloud");
    object2.put("publishTime", Instant.now());
    object2.put("commentCounts", 199);
    object2.put("dislike", -199);
    object2.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    assertTrue(object.guessSchema().compatibleWith(object2.guessSchema()) != CompatResult.NOT_MATCHED);
  }

  public void testSchemaCheckWithDifferentPointer() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    System.out.println(object.guessSchema());
    LeanObject object2 = new LeanObject("Post");
    object2.put("title", "LeanCloud");
    object2.put("publishTime", Instant.now());
    object2.put("commentCounts", 199);
    object2.put("dislike", -199);
    object2.put("author", new JsonObject().put("__type", "Pointer").put("className", "Person").put("objectId", "dhfiafheiire"));
    System.out.println(object2.guessSchema());
    try {
      CompatResult result = object.guessSchema().compatibleWith(object2.guessSchema());
      fail();
    } catch (ConsistencyViolationException ex) {

    }
  }

  public void testSchemaFindGeoPointWithNone() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    Schema schema = object.guessSchema();
    System.out.println(schema);
    String geoPointAttrPath = schema.findGeoPointAttr();
    assertTrue(StringUtils.isEmpty(geoPointAttrPath));
  }

  public void testSchemaFindGeoPointWithFirstLayer() throws Exception {
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    object.put("location", new JsonObject().put("__type", "GeoPoint").put("latitude", 34.5).put("longitude", -87.4));
    Schema schema = object.guessSchema();
    System.out.println(schema);
    String geoPointAttrPath = schema.findGeoPointAttr();
    assertTrue(geoPointAttrPath.equals("location"));
  }

  public void testSchemaFindGeoPointWithSubLayer() throws Exception {
    JsonObject location = new JsonObject().put("location",
            new JsonObject().put("__type", "GeoPoint").put("latitude", 34.5).put("longitude", -87.4));
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    object.put("hometown", location);
    Schema schema = object.guessSchema();
    System.out.println(schema);
    String geoPointAttrPath = schema.findGeoPointAttr();
    System.out.println(geoPointAttrPath);
    assertTrue(geoPointAttrPath.equals("hometown.location"));
  }

  public void testSchemaFindGeoPointWithSubLayer2() throws Exception {
    JsonObject location = new JsonObject().put("location",
            new JsonObject().put("__type", "GeoPoint").put("latitude", 34.5).put("longitude", -87.4));
    LeanObject object = new LeanObject("Post");
    object.put("title", "LeanCloud");
    object.put("publishTime", Instant.now());
    object.put("commentCounts", 199);
    object.put("dislike", -199);
    object.put("spam", false);
    object.put("likes", Arrays.asList("One", "Two", "Three"));
    object.put("author", new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", "dhfiafheiire"));
    object.put("hometown", new JsonObject().put("province", "beijing").put("geo", location));
    Schema schema = object.guessSchema();
    System.out.println(schema);
    String geoPointAttrPath = schema.findGeoPointAttr();
    System.out.println(geoPointAttrPath);
    assertTrue(geoPointAttrPath.equals("hometown.geo.location"));
  }
}
