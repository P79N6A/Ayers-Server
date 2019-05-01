package cn.leancloud.platform.engine;

public enum HookType {
  BeforeUpdate(0, "before_update", "beforeUpdate"), AfterUpdate(1, "after_update", "afterUpdate"),
  BeforeSave(3, "before_save", "beforeSave"), AfterSave(4, "after_save", "afterSave"),
  BeforeDelete(5, "before_delete", "beforeDelete"), AfterDetele(6, "after_delete", "afterDelete"),
  onVerified(7, "on_verified", "onVerified"), onLogin(8, "on_login", "onLogin");

  String name;
  int code;
  String requestPath;
  HookType(int code, String name, String requestPath) {
    this.code = code;
    this.name = name;
    this.requestPath = requestPath;
  }
  public String getName() {
    return this.name;
  }
  public String getRequestPath() {
    return this.requestPath;
  }
  public int getCode() {
    return this.code;
  }
}
