package cn.leancloud.platform.integrate;

import cn.leancloud.platform.ayers.RequestParse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ObjectACLTests extends WebClientTests {
  private static Map<String, Object> authData = new HashMap<>();
  private static final String PLATFORM = "weixinapp-test";
  static {
    authData.put("access_token", "weixin access token from test");
    authData.put("expires_in", 3123321378374l);
    authData.put("openid", "weixinopenid from test");
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
    final String adminRole = "Administrator_Test";
    JsonObject roleQuery = new JsonObject().put("name", adminRole);
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
          // createSingleObject Role
          JsonArray userObjects = new JsonArray();
          userObjects.add(new JsonObject().put("__type", "Pointer").put("className", "_User").put("objectId", userObjectId));
          JsonObject roleJson = new JsonObject();
          roleJson.put("name", adminRole);
          roleJson.put("ACL", new JsonObject("{\"*\":{\"read\":true,\"write\":true}}"));
          roleJson.put("users", new JsonObject().put("__op", "AddRelation").put("objects", userObjects));
          post("/1.1/roles", roleJson, roleRes -> {
            if (roleRes.failed()) {
              System.out.println("failed to createSingleObject Role. cause:" + roleRes.cause().getMessage());
              latch.countDown();
              return;
            }
            System.out.println(roleRes.result());

            JsonObject classMetaJson = new JsonObject().put("class_type", "normal");
            JsonObject publicRW = new JsonObject()
                    .put("*", new JsonObject().put("read", true).put("write", true))
                    .put("_owner", new JsonObject().put("read", true).put("write", true));
            JsonObject ownerRW = new JsonObject()
                    .put("*", new JsonObject().put("read", true).put("write", false))
                    .put("_owner", new JsonObject().put("read", true).put("write", true));
            JsonObject ownerStrictRW = new JsonObject()
                    .put("*", new JsonObject().put("read", false).put("write", false))
                    .put("_owner", new JsonObject().put("read", true).put("write", true));
            JsonObject ownerOnlyR = new JsonObject()
                    .put("*", new JsonObject().put("read", false).put("write", false))
                    .put("_owner", new JsonObject().put("read", true).put("write", false));

            classMetaJson.put("class_name", "PublicReadWrite");
            classMetaJson.put("acl_template", publicRW);
            post("/1.1/meta/classes", classMetaJson, classCreateRes1 -> {
              if (classCreateRes1.failed()) {
                System.out.println("failed to create class " + classMetaJson.getString("class_name")
                        + ", cause: " + classCreateRes1.cause().getMessage());
                latch.countDown();
                return;
              }
              classMetaJson.put("class_name", "OnlyOwnerReadWrite");
              classMetaJson.put("acl_template", ownerRW);
              post("/1.1/meta/classes", classMetaJson, classCreateRes2 -> {
                if (classCreateRes2.failed()) {
                  System.out.println("failed to create class " + classMetaJson.getString("class_name")
                          + ", cause: " + classCreateRes2.cause().getMessage());
                  latch.countDown();
                  return;
                }
                classMetaJson.put("class_name", "OwnerStrictReadWrite");
                classMetaJson.put("acl_template", ownerStrictRW);
                post("/1.1/meta/classes", classMetaJson, classCreateRes3 -> {
                  if (classCreateRes3.failed()) {
                    System.out.println("failed to create class " + classMetaJson.getString("class_name")
                            + ", cause: " + classCreateRes3.cause().getMessage());
                    latch.countDown();
                    return;
                  }
                  classMetaJson.put("class_name", "StrictAllReadWrite");
                  classMetaJson.put("acl_template", ownerOnlyR);
                  post("/1.1/meta/classes", classMetaJson, classCreateRes4 -> {
                    if (classCreateRes4.failed()) {
                      System.out.println("failed to create class " + classMetaJson.getString("class_name")
                              + ", cause: " + classCreateRes4.cause().getMessage());
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
        System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
            System.out.println("failed to updateSingleObject object. cause:" + updatedRes.cause().getMessage());
            latch.countDown();
            return;
          }
          delete(classPath + "/" + objectId, null, deleteRes -> {
            if (deleteRes.failed()) {
              System.out.println("failed to deleteSingleObject object. cause:" + deleteRes.cause().getMessage());
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
        System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
        latch.countDown();
        return;
      }
      final String objectId = objRes.result().getString("objectId");
      get(classPath + "/" + objectId, null, getRes -> {
        if (getRes.succeeded() && null != getRes.result() && getRes.result().size() > 0) {
          JsonObject updated = new JsonObject().put("age", 28);
          put(classPath + "/" + objectId, updated, updatedRes -> {
            if (updatedRes.succeeded()) {
              System.out.println("succeed to updateSingleObject object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.succeeded()) {
                System.out.println("succeed to deleteSingleObject object. it is not expected bcz ACL should not allowed.");
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
        System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
            System.out.println("succeed to updateSingleObject object. it is not expected bcz ACL should not allowed.");
            latch.countDown();
            return;
          }
          delete(classPath + "/" + objectId, null, deleteRes -> {
            if (deleteRes.succeeded()) {
              System.out.println("succeed to deleteSingleObject object. it is not expected bcz ACL should not allowed.");
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
        System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
            System.out.println("succeed to updateSingleObject object. it is not expected bcz ACL should not allowed.");
            latch.countDown();
            return;
          }
          delete(classPath + "/" + objectId, null, deleteRes -> {
            if (deleteRes.succeeded()) {
              System.out.println("succeed to deleteSingleObject object. it is not expected bcz ACL should not allowed.");
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
          System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
              System.out.println("failed to updateSingleObject object. cause:" + updatedRes.cause().getMessage());
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.failed()) {
                System.out.println("failed to deleteSingleObject object. cause:" + deleteRes.cause().getMessage());
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
          System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
              System.out.println("failed to updateSingleObject object. cause:" + updatedRes.cause().getMessage());
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.failed()) {
                System.out.println("failed to deleteSingleObject object. cause:" + deleteRes.cause().getMessage());
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
          System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
              System.out.println("succeed to updateSingleObject object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.succeeded()) {
                System.out.println("succeed to deleteSingleObject object. it is not expected bcz ACL should not allowed.");
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
          System.out.println("failed to createSingleObject object. cause:" + objRes.cause().getMessage());
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
              System.out.println("succeed to updateSingleObject object. it is not expected bcz ACL should not allowed.");
              latch.countDown();
              return;
            }
            delete(classPath + "/" + objectId, null, deleteRes -> {
              if (deleteRes.succeeded()) {
                System.out.println("succeed to deleteSingleObject object. it is not expected bcz ACL should not allowed.");
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
