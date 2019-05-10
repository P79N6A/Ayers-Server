package cn.leancloud.platform.modules;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.util.Arrays;

public class ClassPermissionTest extends TestCase {
  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testSpecialPermissions() throws Exception {
    String permission = "{ \"create\" : { \"*\" : true, \"users\" : [ ], \"roles\" : [ ] }," +
            "\"find\" : { \"*\" : true, \"users\" : [ ], \"roles\" : [ ] }," +
            "\"get\" : { \"*\" : true }," +
            "\"update\" : { \"users\" : [ ], \"roles\" : [ \"Administrator\" ] }," +
            "\"delete\" : { \"users\" : \"5cd102ced3761600696fce99,abcfeifehw\", \"roles\" : \"\" }," +
            "\"add_fields\" : { \"onlySignInUsers\" : true, \"users\" : [ ], \"roles\" : [ ] } }";
    ClassPermission classPermission = new ClassPermission(new JsonObject(permission));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.FIND, null, null));
    assertTrue(!classPermission.checkOperation(ClassPermission.OP.UPDATE, null, null));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.UPDATE, "userid", Arrays.asList("Administrator", "Agent")));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.DELETE, "5cd102ced3761600696fce99", null));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.ADD_FIELDS, "5cd102ced3761600696fce99", null));
  }

  public void testCommonPermissions() throws Exception {
    String permission = "{ \"create\" : { \"*\" : true },\"find\" : { \"*\" : false }," +
            "\"get\" : { \"*\" : true },\"update\" : { \"*\" : true },\"delete\" : { \"*\" : false }, \"add_fields\" : { \"*\" : true } }";
    ClassPermission classPermission = new ClassPermission(new JsonObject(permission));
    assertTrue(!classPermission.checkOperation(ClassPermission.OP.FIND, null, null));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.UPDATE, null, null));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.UPDATE, "userid", Arrays.asList("Administrator", "Agent")));
    assertTrue(!classPermission.checkOperation(ClassPermission.OP.DELETE, "5cd102ced3761600696fce99", null));
    assertTrue(classPermission.checkOperation(ClassPermission.OP.ADD_FIELDS, "5cd102ced3761600696fce99", null));
  }
}
