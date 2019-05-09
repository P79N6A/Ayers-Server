package cn.leancloud.platform.utils;

import io.vertx.core.AsyncResult;

public class HandlerUtils {

  public static <T> AsyncResult<T> wrapActualResult(T value) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return value;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return true;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }

  public static <T> AsyncResult wrapErrorResult(Throwable throwable) {
    return new AsyncResult<T>() {
      @Override
      public T result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return throwable;
      }

      @Override
      public boolean succeeded() {
        return false;
      }

      @Override
      public boolean failed() {
        return true;
      }
    };
  }

}
