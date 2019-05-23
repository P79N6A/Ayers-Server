package cn.leancloud.platform.ayers.handler;

import cn.leancloud.platform.modules.Relation;
import cn.leancloud.platform.modules.Role;
import cn.leancloud.platform.persistence.DataStore;
import cn.leancloud.platform.persistence.impl.MongoDBDataStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;
import org.bson.types.ObjectId;

import java.util.concurrent.CountDownLatch;

public class UserRoleQueryHandlerTest extends TestCase {
  private Vertx vertx = null;
  private MongoDBDataStoreFactory factory = null;
  private DataStore dataStore = null;

  private boolean testSuccessed = false;
  private CountDownLatch latch = null;

  private String targetUserId = "5cd644af85dac202c058cc07";

  private String targetManagerUserId = "5ce65961c470821ebdc91b86";

  public UserRoleQueryHandlerTest() {
    vertx = Vertx.vertx();
    JsonObject mongoConfig = new JsonObject()
            .put("host", "localhost")
            .put("port", 27027)
            .put("db_name", "uluru-testSchema")
            .put("maxPoolSize", 3)
            .put("minPoolSize", 1)
            .put("keepAlive", true);
    factory = new MongoDBDataStoreFactory(vertx, mongoConfig, "TestMongoDB");
    vertx.runOnContext(res -> {});
  }

  private void prepareData() throws Exception {
    CountDownLatch tmpLatch = new CountDownLatch(1);
    DataStore.InsertOption insertOption = new DataStore.InsertOption().setReturnNewDocument(true);
    JsonObject managerRole = new JsonObject().put("name", "Manager");
    JsonObject ceoRole = new JsonObject().put("name", "Agent");
    dataStore.insertWithOptions("_Role", managerRole, insertOption, res1 -> {
      if (res1.succeeded()) {
        System.out.println("succeed to create ManagerRole. result:" + res1.result());

        JsonObject manager = res1.result();
        String mangerObjectId = manager.getString("objectId");

        JsonObject managerUserRoleRelation = new JsonObject()
                .put(Relation.BUILTIN_ATTR_RELATION_OWNING_ID, new JsonObject().put("$ref", "_Role").put("$id", new ObjectId(mangerObjectId).toString()))
                .put(Relation.BUILTIN_ATTR_RELATIONN_RELATED_ID, new JsonObject().put("$ref", "_User").put("$id", new ObjectId(targetManagerUserId).toString()));
        dataStore.insertWithOptions(Role.getUserRelationTable(), managerUserRoleRelation, insertOption, res-> {
          if (res.failed()) {
            System.out.println("failed to create managerRole Relationn. cause:" + res.cause());
            tmpLatch.countDown();
          } else {
            dataStore.insertWithOptions("_Role", ceoRole, insertOption, res2 -> {
              if (res2.succeeded()) {
                System.out.println("succeed to create AgentRole. result:" + res2.result());

                JsonObject ceo = res2.result();
                String ceoObjectId = ceo.getString("objectId");

                JsonObject ceoManagerRoleRelation = new JsonObject()
                        .put(Relation.BUILTIN_ATTR_RELATION_OWNING_ID, new JsonObject().put("$ref", "_Role").put("$id", new ObjectId(ceoObjectId).toString()))
                        .put(Relation.BUILTIN_ATTR_RELATIONN_RELATED_ID, new JsonObject().put("$ref", "_Role").put("$id", new ObjectId(mangerObjectId).toString()));
                JsonObject ceoUserRoleRelation = new JsonObject()
                        .put(Relation.BUILTIN_ATTR_RELATION_OWNING_ID, new JsonObject().put("$ref", "_Role").put("$id", new ObjectId(ceoObjectId).toString()))
                        .put(Relation.BUILTIN_ATTR_RELATIONN_RELATED_ID, new JsonObject().put("$ref", "_User").put("$id", new ObjectId(targetUserId).toString()));
                dataStore.insertWithOptions(Role.getRoleRelationTable(), ceoManagerRoleRelation, insertOption, res3 -> {
                  if (res3.failed()) {
                    System.out.println("failed to create Role Relation. cause:" + res3.cause());
                  }else {
                    System.out.println("succeed to create Role Relation. " + res3.result());
                  }
                  dataStore.insertWithOptions(Role.getUserRelationTable(), ceoUserRoleRelation, insertOption, res4 -> {
                    if (res4.failed()) {
                      System.out.println("failed to create User Relation. cause:" + res4.cause());
                    } else {
                      System.out.println("succeed to create User Relation. " + res4.result());
                    }
                    tmpLatch.countDown();
                  });
                });
              } else {
                System.out.println("failed to create AgentRole. cause:" + res2.cause());
                tmpLatch.countDown();
              }
            });
          }
        });
      } else {
        System.out.println("failed to create managerRole. cause:" + res1.cause());
        tmpLatch.countDown();
      }
    });
    tmpLatch.await();
  }

  private void cleanupData() throws Exception {
    CountDownLatch tmpLatch = new CountDownLatch(1);
    dataStore.dropClass("_Role", res -> {
      if (res.failed()) {
        System.out.println("failed to drop _Role. cause:" + res.cause());
      } else {
        System.out.println("succeed to drop _Role.");
      }
      dataStore.dropClass(Role.getRoleRelationTable(), res2 -> {
        if (res.failed()) {
          System.out.println("failed to drop " + Role.getRoleRelationTable() + ". cause:" + res2.cause());
        } else {
          System.out.println("succeed to drop " + Role.getRoleRelationTable());
        }
        dataStore.dropClass(Role.getUserRelationTable(), res3 -> {
          if (res.failed()) {
            System.out.println("failed to drop " + Role.getUserRelationTable() + ". cause:" + res3.cause());
          } else {
            System.out.println("succeed to drop " + Role.getUserRelationTable());
          }
          tmpLatch.countDown();
        });
      });
    });
    tmpLatch.await();
  }

  @Override
  protected void setUp() throws Exception {
    dataStore = factory.getStore();
    latch = new CountDownLatch(1);
    testSuccessed = false;
    prepareData();
  }

  @Override
  protected void tearDown() throws Exception {
    if (null != dataStore) {
      cleanupData();
      dataStore.close();
    }
  }

  public void testWithNotExistedUser() throws Exception {
    UserRoleQueryHandler handler = new UserRoleQueryHandler();
    handler.queryUserRoles(dataStore, "5cd6437185dac202c058cbff", res -> {
      if (res.failed()) {
        System.out.println("failed to query User roles with not existed user. cause:" + res.cause().getMessage());
        latch.countDown();
      } else {
        testSuccessed = res.result().size() < 1;
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testWithExisted2RoleUser() throws Exception {
    UserRoleQueryHandler handler = new UserRoleQueryHandler();
    handler.queryUserRoles(dataStore, targetUserId, res -> {
      if (res.failed()) {
        System.out.println("failed to query User roles with not existed user. cause:" + res.cause().getMessage());
        latch.countDown();
      } else {
        testSuccessed = res.result().size() == 2;
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }

  public void testWithExisted1RoleUser() throws Exception {
    UserRoleQueryHandler handler = new UserRoleQueryHandler();
    handler.queryUserRoles(dataStore, targetManagerUserId, res -> {
      if (res.failed()) {
        System.out.println("failed to query User roles with not existed user. cause:" + res.cause().getMessage());
        latch.countDown();
      } else {
        testSuccessed = res.result().size() == 1;
        latch.countDown();
      }
    });
    latch.await();
    assertTrue(testSuccessed);
  }
}
