package cn.leancloud.platform.modules;

public class ConsistencyViolationException extends Exception {
  public ConsistencyViolationException() {
    super();
  }

  public ConsistencyViolationException(String message) {
    super(message);
  }

  public ConsistencyViolationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConsistencyViolationException(Throwable cause) {
    super(cause);
  }

  protected ConsistencyViolationException(String message, Throwable cause,
                      boolean enableSuppression,
                      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
