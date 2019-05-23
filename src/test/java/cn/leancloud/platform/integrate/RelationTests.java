package cn.leancloud.platform.integrate;

import cn.leancloud.platform.modules.ACL;
import cn.leancloud.platform.modules.Role;
import cn.leancloud.platform.utils.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * curl -X POST \
 -H "X-LC-Id: heQFQ0SwoQqiI3gEAcvKXjeR-gzGzoHsz" \
 -H "X-LC-Key: lNSjPPPDohJjYMJcQSxi9qAm" \
 -H "Content-Type: application/json" \
 -d '{
 "name": "Manager",
 "ACL": {
 "*": {
 "read": true
 }
 }
 }' \
 https://heqfq0sw.api.lncld.net/1.1/roles

 * new record in _Role:
 * { "_id" : ObjectId("5ce3f011c8959c0069c10322"), "name" : "Manager", "ACL" : { "*" : { "read" : true } }, "_r" : [ "*" ], "_w" : [ ], "createdAt" : ISODate("2019-05-21T12:33:21.709Z"), "updatedAt" : ISODate("2019-05-21T12:33:21.709Z") }
 *
 *
 *
 curl -X POST \
 -H "X-LC-Id: heQFQ0SwoQqiI3gEAcvKXjeR-gzGzoHsz" \
 -H "X-LC-Key: lNSjPPPDohJjYMJcQSxi9qAm" \
 -H "Content-Type: application/json" \
 -d '{
 "name": "CLevel",
 "ACL": {
 "*": {
 "read": true
 }
 },
 "roles": {
 "__op": "AddRelation",
 "objects": [
 {
 "__type": "Pointer",
 "className": "_Role",
 "objectId": "5ce3f011c8959c0069c10322"
 }
 ]
 },
 "users": {
 "__op": "AddRelation",
 "objects": [
 {
 "__type": "Pointer",
 "className": "_User",
 "objectId": "55a47496e4b05001a7732c5f"
 }
 ]
 }
 }' \
 https://heqfq0sw.api.lncld.net/1.1/roles

 * got result in _Role:
 * { "_id" : ObjectId("5ce3f0947b968a00730fbfc6"), "name" : "CLevel", "ACL" : { "*" : { "read" : true } }, "_r" : [ "*" ], "_w" : [ ], "createdAt" : ISODate("2019-05-21T12:35:32.300Z"), "updatedAt" : ISODate("2019-05-21T12:35:32.303Z") }
 * got result in _Join:_Role:roles:_Role
 * { "_id" : ObjectId("5ce3f0947b968a00730fbfc9"), "owningId" : DBRef("_Role", ObjectId("5ce3f0947b968a00730fbfc6")), "relatedId" : DBRef("_Role", ObjectId("5ce3f011c8959c0069c10322")) }
 * got result in _Join:_User:users:_Role
 * { "_id" : ObjectId("5ce3f0947b968a00730fbfca"), "owningId" : DBRef("_Role", ObjectId("5ce3f0947b968a00730fbfc6")), "relatedId" : DBRef("_User", ObjectId("55a47496e4b05001a7732c5f")) }
 *
 *
 * curl -X GET \
 -H "X-LC-Id: heQFQ0SwoQqiI3gEAcvKXjeR-gzGzoHsz" \
 -H "X-LC-Key: lNSjPPPDohJjYMJcQSxi9qAm" \
 -G \
 --data-urlencode 'where={"$relatedTo":{"object":{"__type":"Pointer","className":"_Role","objectId":"5ce3f0947b968a00730fbfc6"},"key":"users"}}' \
 https://heqfq0sw.api.lncld.net/1.1/users

 * curl -X GET \
 -H "X-LC-Id: heQFQ0SwoQqiI3gEAcvKXjeR-gzGzoHsz" \
 -H "X-LC-Key: lNSjPPPDohJjYMJcQSxi9qAm" \
 -G \
 --data-urlencode 'where={"$relatedTo":{"object":{"__type":"Pointer","className":"_Role","objectId":"5ce3f0947b968a00730fbfc6"},"key":"roles"}}' \
 https://heqfq0sw.api.lncld.net/1.1/roles
 * got result as following:
 * {"results":[{"name":"Manager","createdAt":"2019-05-21T12:33:21.709Z","updatedAt":"2019-05-21T12:33:21.709Z","objectId":"5ce3f011c8959c0069c10322","roles":{"__type":"Relation","className":"_Role"},"users":{"__type":"Relation","className":"_User"}}]}
 */

public class RelationTests extends WebClientTests {
  private final String authString = "{\n" +
          "  \"authData\": {\n" +
          "    \"weibo\": {\n" +
          "      \"uid\": \"123456789\",\n" +
          "      \"access_token\": \"2.00vs3XtCI5FevCff4981adb5jj1lXE\",\n" +
          "      \"expiration_in\": \"36000\"\n" +
          "    }\n" +
          "  }\n" +
          "}";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCreateRelationsRole() throws Exception {
    ACL acl = new ACL();
    acl.setPublicReadAccess(true);
    acl.setPublicWriteAccess(false);

    JsonObject data = new JsonObject().put("name", "Manager")
            .put("ACL", acl.toJson());
    post("/1.1/roles", data, response -> {
      if (response.failed()) {
        System.out.println("failed to create role, cause:" + response.cause());
        latch.countDown();
      } else {
        JsonObject result = response.result();
        final String targetRoleObjectId = result.getString("objectId");
        JsonObject authRequest = new JsonObject(authString);
        post("/1.1/users", authRequest, userRes -> {
          if (userRes.failed()) {
            System.out.println("failed to login with authdata. cause: " + userRes.cause());
            latch.countDown();
          } else {
            JsonObject userObj = userRes.result();
            final String targetUserObjectId = userObj.getString("objectId");
            acl.setPublicWriteAccess(true);
            JsonObject createRoleParam = new JsonObject().put("name", "CLevel").put("ACL", acl.toJson());
            JsonObject rolesAttr = new JsonObject().put("__op", "AddRelation").put("objects",
                    Arrays.asList(new JsonObject().put("__type", "Pointer").put("className", "_Role")
                            .put("objectId", targetRoleObjectId)));
            JsonObject usersAttr = new JsonObject().put("__op", "AddRelation").put("objects",
                    Arrays.asList(new JsonObject().put("__type", "Pointer").put("className", "_User")
                            .put("objectId", targetUserObjectId)));
            createRoleParam.put("roles", rolesAttr);
            createRoleParam.put("users", usersAttr);
            post("/1.1/roles", createRoleParam, roleRes -> {
              if (roleRes.failed()) {
                System.out.println("failed to create role with relations, cause:" + roleRes.cause());
              } else {
                System.out.println(roleRes.result());
                testSuccessed = true;
              }
              latch.countDown();
            });
          }
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testUpdateRoleWithRelation() throws Exception {
    JsonObject queryCond = new JsonObject().put("where", new JsonObject().put("name", "CLevel"));
    get("/1.1/roles", queryCond, response-> {
      if (response.failed()) {
        System.out.println("failed to query roles. cause:" + response.cause().getMessage());
        latch.countDown();
      } else {
        JsonArray results = response.result().getJsonArray("results");
        if (null == results || results.size() < 1) {
          latch.countDown();
          return;
        }
        JsonObject targetRole = results.getJsonObject(0);
        final String targetRoleObjectId = targetRole.getString("objectId");
        String targetUserObjectId = "558e20cbe4b060308e3eb36c";
        JsonObject usersAttr = new JsonObject().put("__op", "AddRelation").put("objects",
                Arrays.asList(new JsonObject().put("__type", "Pointer").put("className", "_User")
                        .put("objectId", targetUserObjectId)));
        JsonObject updateRoleParam = new JsonObject().put("users", usersAttr);
        put("/1.1/roles/" + targetRoleObjectId, updateRoleParam, roleRes1 -> {
          if (roleRes1.failed()) {
            System.out.println("failed to add more relation for roles. cause:" + roleRes1.cause().getMessage());
            latch.countDown();
          } else {
            JsonObject deleteUsersAttr = new JsonObject().put("__op", "RemoveRelation").put("objects",
                    Arrays.asList(new JsonObject().put("__type", "Pointer").put("className", "_User")
                            .put("objectId", targetUserObjectId)));
            updateRoleParam.put("users", deleteUsersAttr);
            put("/1.1/roles/" + targetRoleObjectId, updateRoleParam, roleRes2 -> {
              if (roleRes2.failed()) {
                System.out.println("failed to remove relation for roles. cause:" + roleRes2.cause().getMessage());
                latch.countDown();
              } else {
                testSuccessed = true;
                latch.countDown();
              }
            });
          }
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  private Future makeSureRelationQuery(String from, String to, String field, String objectId) {
    String relationFormat = "{\"$relatedTo\":{\"object\":{\"__type\":\"Pointer\",\"className\":\"%s\",\"objectId\":\"%s\"},\"key\":\"%s\"}}";
    String relationQuery = String.format(relationFormat, from, objectId, field);
    JsonObject relationQueryJson = new JsonObject().put("where", new JsonObject(relationQuery));
    Future result = Future.future();
    get("/1.1/classes/" + to, relationQueryJson, response -> {
      if (response.failed()) {
        System.out.println(response.cause());
        result.fail(response.cause());
      } else {
        JsonArray resultArray = response.result().getJsonArray("results");
        long occCount = null == resultArray? 0: resultArray.size();
        //.filter( obj -> ((JsonObject)obj).getString("objectId").equals(objectId)).count();
        result.complete(occCount > 0);
      }
    });
    return result;
  }

  public void testRelationRoleQuery() throws Exception {
    get("/1.1/roles", new JsonObject().put("where", new JsonObject().put("name", "CLevel")), roleResponse -> {
      if (roleResponse.failed()) {
        latch.countDown();
        return;
      }
      JsonArray roles = roleResponse.result().getJsonArray("results");
      if (null == roles || roles.size() < 1) {
        latch.countDown();
        return;
      }
      String roleObjectId = roles.getJsonObject(0).getString("objectId");
      makeSureRelationQuery("_Role", "_Role", "roles", roleObjectId).setHandler(res -> {
        testSuccessed = true;
        latch.countDown();
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testRelationUserQuery() throws Exception {
    JsonObject authRequest = new JsonObject(authString);
    post("/1.1/users", authRequest, userRes -> {
      if (userRes.failed()) {
        System.out.println("failed to login. cause:" + userRes.cause().getMessage());
        latch.countDown();
      } else {
        JsonObject userObj = userRes.result();
        final String targetUserObjectId = userObj.getString("objectId");
        if (StringUtils.isEmpty(targetUserObjectId)) {
          System.out.println("user object is null!!!");
          latch.countDown();
        } else {
          get("/1.1/roles", new JsonObject().put("where", new JsonObject().put("name", "Manager")), roleResponse -> {
            if (roleResponse.failed()) {
              latch.countDown();
              return;
            }
            JsonArray roles = roleResponse.result().getJsonArray("results");
            if (null == roles || roles.size() < 1) {
              latch.countDown();
              return;
            }
            String roleObjectId = roles.getJsonObject(0).getString("objectId");
            makeSureRelationQuery("_Role", "_User", Role.BUILTIN_ATTR_RELATION_USER, roleObjectId).setHandler(res -> {
              testSuccessed = true;
              latch.countDown();
            });
          });
        }
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testRelationObjectDelete() throws Exception {

  }

  public void testRelationQueryAndValidateResult() throws Exception {
    get("/1.1/roles", new JsonObject().put("where", new JsonObject().put("name", "Manager")), roleResponse -> {
      if (roleResponse.failed()) {
        latch.countDown();
        return;
      }
      JsonArray roles = roleResponse.result().getJsonArray("results");
      if (null == roles || roles.size() < 1) {
        latch.countDown();
        return;
      }
      String roleObjectId = roles.getJsonObject(0).getString("objectId");
      JsonObject userObject = roles.getJsonObject(0).getJsonObject("users");
      JsonObject roleObject = roles.getJsonObject(0).getJsonObject("roles");
      if (StringUtils.notEmpty(roleObjectId) && null != userObject && null != roleObject) {
        if ("Relation".equals(userObject.getString("__type")) && "Relation".equals(roleObject.getString("__type"))) {
          if ("_User".equals(userObject.getString("className")) && "_Role".equals(roleObject.getString("className"))) {
            testSuccessed = true;
          }
        }
      }
      latch.countDown();
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testRelationClassDelete() throws Exception {
    delete("/1.1/meta/classes/_Role", null, response -> {
      if (response.failed()) {
        latch.countDown();
        return;
      }
      get("/1.1/meta/classes", null, classesRes -> {
        if (classesRes.failed()) {
          latch.countDown();
        } else {
          long invalidClassCount = classesRes.result().getJsonArray("results").stream()
                  .filter(cls -> ((String)cls).indexOf("_Role") >= 0).count();
          testSuccessed = invalidClassCount < 1;
          latch.countDown();
        }
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }
}
