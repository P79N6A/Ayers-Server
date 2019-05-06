package cn.leancloud.platform.modules;

public class ObjectSecurityLevel {
  public static final ObjectSecurityLevel PublicReadWrite = new ObjectSecurityLevel(0);
  public static final ObjectSecurityLevel PublicRead = new ObjectSecurityLevel(1);
  public static final ObjectSecurityLevel OwnerReadWrite = new ObjectSecurityLevel(2);
  public static final ObjectSecurityLevel OwnerRead = new ObjectSecurityLevel(3);

  private int level = 0;
  private ObjectSecurityLevel(int level) {
    this.level = level;
  }

  public ACL generateACL4User(String currentUserObjectId) {
    ACL result = new ACL();
    switch (this.level) {
      case 0:
        result.setPublicReadAccess(true);
        result.setPublicWriteAccess(true);
        break;
      case 1:
        result.setPublicReadAccess(true);
        result.setPublicWriteAccess(false);
        result.setReadAccess(currentUserObjectId, true);
        result.setWriteAccess(currentUserObjectId, true);
        break;
      case 2:
        result.setPublicReadAccess(false);
        result.setPublicWriteAccess(false);
        result.setReadAccess(currentUserObjectId, true);
        result.setWriteAccess(currentUserObjectId, true);
        break;
      case 3:
        result.setPublicReadAccess(false);
        result.setPublicWriteAccess(false);
        result.setReadAccess(currentUserObjectId, true);
        result.setWriteAccess(currentUserObjectId, false);
        break;
    }
    return result;
  }
}
