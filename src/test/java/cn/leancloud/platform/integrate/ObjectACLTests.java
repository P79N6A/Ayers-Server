package cn.leancloud.platform.integrate;

import cn.leancloud.platform.ayers.RequestParse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ObjectACLTests extends WebClientTests {
  private static Map<String, Object> authData = new HashMap<>();
  private static final String PLATFORM = "weixinapp";
  static {
    authData.put("access_token", "weixin access token");
    authData.put("expires_in", 3123321378374l);
    authData.put("openid", "weixinopenid");
    authData.put("platform", PLATFORM);
  }


  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testPrepareACL() throws Exception {
    JsonObject roleQuery = new JsonObject().put("name", "Administrator");
    get("/1.1/roles", roleQuery, response -> {
      if (response.failed()) {
        System.out.println("failed to query role. cause:" + response.cause().getMessage());
        latch.countDown();
        return;
      } else {
        JsonArray results = response.result().getJsonArray("results");
        if (null != results && results.size() > 0) {
          testSuccessed = true;
          latch.countDown();
          return;
        }
        JsonObject user = new JsonObject().put("username", "Automatic Test User").put("mobilePhoneNumber","12345678900");
        user.put("authData", new JsonObject().put(PLATFORM, new JsonObject(authData)));
        post("/1.1/login", user, userRes -> {
          if (userRes.failed()) {
            System.out.println("failed to loginWithAuthData. cause:" + userRes.cause().getMessage());
            latch.countDown();
            return;
          }
          String userObjectId = userRes.result().getString("objectId");
          // create Role
          JsonArray userObjects = new JsonArray();
          userObjects.add(new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", userObjectId));
          JsonObject roleJson = new JsonObject();
          roleJson.put("name", "Administrator");
          roleJson.put("ACL", new JsonObject("{\"*\":{\"read\":true,\"write\":true}}"));
          roleJson.put("users", new JsonObject().put("__op", "AddRelation").put("objects", userObjects));
          post("/1.1/roles", roleJson, roleRes -> {
            if (roleRes.failed()) {
              System.out.println("failed to create Role. cause:" + roleRes.cause().getMessage());
              latch.countDown();
              return;
            }
            System.out.println(roleRes.result());
            testSuccessed = true;
            latch.countDown();
          });
        });
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testPublicReadWriteWithUnauth() throws Exception {
    final String classPath = "/1.1/classes/PublicReadWrite";
    JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
    post(classPath, publicRWObj, objRes -> {
      if (objRes.failed()) {
        System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
        latch.countDown();
        return;
      }
      String objectId = objRes.result().getString("objectId");
      get(classPath + "/" + objectId, null, getResponse -> {
        if (getResponse.failed()) {
          System.out.println("failed to get object. cause:" + getResponse.cause().getMessage());
          latch.countDown();
          return;
        }
        JsonObject updated = new JsonObject().put("age", 28);
        put(classPath + "/" + objectId, updated, updatedRes -> {
          if (updatedRes.failed()) {
            System.out.println("failed to update object. cause:" + updatedRes.cause().getMessage());
            latch.countDown();
            return;
          }
          delete(classPath + "/" + objectId, null, deleteRes -> {
            if (deleteRes.failed()) {
              System.out.println("failed to delete object. cause:" + deleteRes.cause().getMessage());
              latch.countDown();
              return;
            }
            testSuccessed = true;
            latch.countDown();
          });
        });
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }


  public void testOwnerReadWriteWithUnauth() throws Exception {
    final String classPath = "/1.1/classes/OnlyOwnerReadWrite";
    JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
    post(classPath, publicRWObj, objRes -> {
      if (objRes.failed()) {
        System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
        latch.countDown();
        return;
      }
      final String objectId = objRes.result().getString("objectId");
      get(classPath + "/" + objectId, null, getRes -> {
        if (getRes.succeeded() && null != getRes.result() && getRes.result().size() > 0) {
          JsonObject updated = new JsonObject().put("age", 28);
          put(classPath + "/" + objectId, updated, updatedRes -> {
            if (updatedRes.succeeded()) {
              System.out.println("succeed to update object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.succeeded()) {
                System.out.println("succeed to delete object. it is not expected bcz ACL should not allowed.");
                latch.countDown();
                return;
              }
              testSuccessed = true;
              latch.countDown();
            });
          });
        } else {
          System.out.println("can't fetch object with Id.");
          latch.countDown();
          return;
        }
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testOwnerStrictReadWriteWithUnauth() throws Exception {
    final String classPath = "/1.1/classes/OwnerStrictReadWrite";
    JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
    post(classPath, publicRWObj, objRes -> {
      if (objRes.failed()) {
        System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
        latch.countDown();
        return;
      }
      final String objectId = objRes.result().getString("objectId");
      get(classPath + "/" + objectId, null, getRes -> {
        if (getRes.succeeded() && null != getRes.result() && getRes.result().size() > 0) {
          // get result, wtf
          System.out.println("succeed to fetch object. it is not expected bcz ACL should not allowed.");
          latch.countDown();
          return;
        }
        JsonObject updated = new JsonObject().put("age", 28);
        put(classPath + "/" + objectId, updated, updatedRes -> {
          if (updatedRes.succeeded()) {
            System.out.println("succeed to update object. it is not expected bcz ACL should not allowed.");
            latch.countDown();
            return;
          }
          delete(classPath + "/" + objectId, null, deleteRes -> {
            if (deleteRes.succeeded()) {
              System.out.println("succeed to delete object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            testSuccessed = true;
            latch.countDown();
          });
        });
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testStrictAllReadWithUnauth() throws Exception {
    final String classPath = "/1.1/classes/StrictAllReadWrite";
    JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
    post(classPath, publicRWObj, objRes -> {
      if (objRes.failed()) {
        System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
        latch.countDown();
        return;
      }
      final String objectId = objRes.result().getString("objectId");
      get(classPath + "/" + objectId, null, getRes -> {
        if (getRes.succeeded() && null != getRes.result() && getRes.result().size() > 0) {
          // get result, wtf
          System.out.println("succeed to fetch object. it is not expected bcz ACL should not allowed.");
          latch.countDown();
          return;
        }
        JsonObject updated = new JsonObject().put("age", 28);
        put(classPath + "/" + objectId, updated, updatedRes -> {
          if (updatedRes.succeeded()) {
            System.out.println("succeed to update object. it is not expected bcz ACL should not allowed.");
            latch.countDown();
            return;
          }
          delete(classPath + "/" + objectId, null, deleteRes -> {
            if (deleteRes.succeeded()) {
              System.out.println("succeed to delete object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            testSuccessed = true;
            latch.countDown();
          });
        });
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testPublicReadWriteWithAuthUser() throws Exception {
    final String classPath = "/1.1/classes/PublicReadWrite";
    // need to login at first.
    JsonObject user = new JsonObject().put("username", "Automatic Test User").put("mobilePhoneNumber","12345678900");
    user.put("authData", new JsonObject().put(PLATFORM, new JsonObject(authData)));
    post("/1.1/login", user, userRes -> {
      if (userRes.failed()) {
        System.out.println("failed to login. cause:" + userRes.cause().getMessage());
        latch.countDown();
        return;
      }
      String sessionToken = userRes.result().getString("sessionToken");
      addHttpHeader(RequestParse.HEADER_LC_SESSION_TOKEN, sessionToken);

      JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
      post(classPath, publicRWObj, objRes -> {
        if (objRes.failed()) {
          System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
          latch.countDown();
          return;
        }
        final String objectId = objRes.result().getString("objectId");
        get(classPath + "/" + objectId, null, getRes -> {
          if (getRes.failed()) {
            System.out.println("failed to get object. cause:" + getRes.cause().getMessage());
            latch.countDown();
            return;
          }
          JsonObject updated = new JsonObject().put("age", 28);
          put(classPath + "/" + objectId, updated, updatedRes -> {
            if (updatedRes.failed()) {
              System.out.println("failed to update object. cause:" + updatedRes.cause().getMessage());
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.failed()) {
                System.out.println("failed to delete object. cause:" + deleteRes.cause().getMessage());
                latch.countDown();
                return;
              }
              testSuccessed = true;
              latch.countDown();
            });
          });
        });
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testOwnerReadWriteWithAuthUser() throws Exception {
    String classPath = "/1.1/classes/OnlyOwnerReadWrite";
    // need to login at first.
    JsonObject user = new JsonObject().put("username", "Automatic Test User").put("mobilePhoneNumber","12345678900");
    user.put("authData", new JsonObject().put(PLATFORM, new JsonObject(authData)));
    post("/1.1/login", user, userRes -> {
      if (userRes.failed()) {
        System.out.println("failed to login. cause:" + userRes.cause().getMessage());
        latch.countDown();
        return;
      }
      String sessionToken = userRes.result().getString("sessionToken");
      addHttpHeader(RequestParse.HEADER_LC_SESSION_TOKEN, sessionToken);

      JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
      post(classPath, publicRWObj, objRes -> {
        if (objRes.failed()) {
          System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
          latch.countDown();
          return;
        }
        String objectId = objRes.result().getString("objectId");
        get(classPath + "/" + objectId, null, getRes -> {
          if (getRes.failed()) {
            System.out.println("failed to get object. cause:" + getRes.cause().getMessage());
            latch.countDown();
            return;
          }
          JsonObject updated = new JsonObject().put("age", 28);
          put(classPath + "/" + objectId, updated, updatedRes -> {
            if (updatedRes.failed()) {
              System.out.println("failed to update object. cause:" + updatedRes.cause().getMessage());
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.failed()) {
                System.out.println("failed to delete object. cause:" + deleteRes.cause().getMessage());
                latch.countDown();
                return;
              }
              testSuccessed = true;
              latch.countDown();
            });
          });
        });
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testOwnerStrictReadWriteWithAuthUser() throws Exception {
    String classPath = "/1.1/classes/OwnerStrictReadWrite";
    // need to login at first.
    JsonObject user = new JsonObject().put("username", "Automatic Test User").put("mobilePhoneNumber","12345678900");
    user.put("authData", new JsonObject().put(PLATFORM, new JsonObject(authData)));
    post("/1.1/login", user, userRes -> {
      if (userRes.failed()) {
        System.out.println("failed to login. cause:" + userRes.cause().getMessage());
        latch.countDown();
        return;
      }
      String sessionToken = userRes.result().getString("sessionToken");
      addHttpHeader(RequestParse.HEADER_LC_SESSION_TOKEN, sessionToken);

      JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
      post(classPath, publicRWObj, objRes -> {
        if (objRes.failed()) {
          System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
          latch.countDown();
          return;
        }
        String objectId = objRes.result().getString("objectId");
        get(classPath + "/" + objectId,null, getRes -> {
          if (getRes.failed()) {
            System.out.println("failed to get object. cause:" + getRes.cause().getMessage());
            latch.countDown();
            return;
          }
          JsonObject updated = new JsonObject().put("age", 28);
          put(classPath + "/" + objectId, updated, updatedRes -> {
            if (updatedRes.succeeded()) {
              System.out.println("succeed to update object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.succeeded()) {
                System.out.println("succeed to delete object. it is not expected bcz ACL should not allowed.");
                latch.countDown();
                return;
              }
              testSuccessed = true;
              latch.countDown();
            });
          });
        });
      });
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testStrictAllReadWithAuthUser() throws Exception {
    final String classPath = "/1.1/classes/StrictAllReadWrite";

    // need to login at first.
    JsonObject user = new JsonObject().put("username", "Automatic Test User").put("mobilePhoneNumber","12345678900");
    user.put("authData", new JsonObject().put(PLATFORM, new JsonObject(authData)));
    post("/1.1/login", user, userRes -> {
      if (userRes.failed()) {
        System.out.println("failed to login. cause:" + userRes.cause().getMessage());
        latch.countDown();
        return;
      }
      String sessionToken = userRes.result().getString("sessionToken");
      addHttpHeader(RequestParse.HEADER_LC_SESSION_TOKEN, sessionToken);

      JsonObject publicRWObj = new JsonObject().put("content", "Automatic Tester").put("age", 20);
      post(classPath, publicRWObj, objRes -> {
        if (objRes.failed()) {
          System.out.println("failed to create object. cause:" + objRes.cause().getMessage());
          latch.countDown();
          return;
        }
        final String objectId = objRes.result().getString("objectId");
        get(classPath + "/" + objectId, null, getRes -> {
          if (getRes.failed()) {
            // get result, wtf
            System.out.println("failed to fetch object. cause:" + getRes.cause().getMessage());
            latch.countDown();
            return;
          }
          JsonObject updated = new JsonObject().put("age", 28);
          put(classPath + "/" + objectId, updated, updatedRes -> {
            if (updatedRes.succeeded()) {
              System.out.println("succeed to update object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.succeeded()) {
                System.out.println("succeed to delete object. it is not expected bcz ACL should not allowed.");
                latch.countDown();
                return;
              }
              testSuccessed = true;
              latch.countDown();
            });
          });
        });
      });
    });

    latch.await();
    assertTrue(testSuccessed);
  }
}
