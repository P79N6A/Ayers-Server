package cn.leancloud.platform.engine;

public enum HookType {
  BeforeUpdate(0, "before_update"), AfterUpdate(1, "after_update"),
  BeforeSave(3, "before_save"), AfterSave(4, "after_save"),
  BeforeDelete(5, "before_delete"), AfterDetele(6, "after_delete"),
  onVerified(7, "on_verified"), onLogin(8, "on_login");

  String name;
  int code;
  HookType(int code, String name) {
    this.code = code;
    this.name = name;
  }
  public String getName() {
    return this.name;
  }
  public int getCode() {
    return this.code;
  }
}
