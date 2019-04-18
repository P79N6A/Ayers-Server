package cn.leancloud.platform.common;

public class Configure {

  public static final String QINIU_ACCESS_KEY = System.getProperty("qiniu.accessKey", "access");
  public static final String QINIU_SECRET_KEY = System.getProperty("qiniu.secretKey", "secret");;
  public static final String FILE_DEFAULT_BUCKET = System.getProperty("file.bucket", "lc-unified");
  public static final String FILE_PROVIDER = System.getProperty("file.provider", "qiniu");
  public static final String FILE_UPLOAD_URL = System.getProperty("file.upload.url", "https://upload.qiniup.com");
  public static final String FILE_DEFAULT_HOST = System.getProperty("file.host", "");
}
