package cn.leancloud.platform.common;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

public class TransformerTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testConvert2Bson() throws Exception {
    String param = "{\"birthday\":{\"iso\":\"2019-04-12T07:04:35.016Z\",\"__type\":\"Date\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"},\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonObject(paramObj);
    System.out.println(newObj.toString());
  }

  public void testConvertPointerArray2Bson() throws Exception {
    String param = "{\"birthday\":{\"iso\":\"2019-04-12T07:04:35.016Z\",\"__type\":\"Date\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":[{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"},{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"},{\"__type\":\"Pointer\",\"className\":\"Course\",\"objectId\":\"5cb03965c3a4593f60745a1a\"}],\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.encode2BsonObject(paramObj);
    System.out.println(newObj.toString());
  }

  public void testConvert2Rest() throws Exception {
    String param = "{\"birthday\":\"2019-04-12T07:04:35.016Z\",\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"},\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.decodeBsonObject(paramObj);
    System.out.println(newObj.toString());
  }

  public void testConvertRefArray2Rest() throws Exception {
    String param = "{\"birthday\":{\"iso\":\"2019-04-12T07:04:35.016Z\",\"__type\":\"Date\"},\"name\":\"Automatic Tester\",\"ACL\":{\"*\":{\"read\":true,\"write\":true}},\"favorite\":[{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"},{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"},{\"$ref\":\"Course\",\"$id\":\"5cb03965c3a4593f60745a1a\"}],\"age\":19}";
    JsonObject paramObj = new JsonObject(param);
    JsonObject newObj = Transformer.decodeBsonObject(paramObj);
    System.out.println(newObj.toString());
  }
}
