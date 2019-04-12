package cn.leancloud.platform.common;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtils {
  private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
  private static final String SHA1_ALGORITHM = "HmacSHA1";

  public static boolean isEmpty(String data) {
    return (null == data || data.trim().length() == 0);
  }

  public static boolean notEmpty(String data) {
    return !isEmpty(data);
  }

  public static String computeSHA1(String input, String key) {
    try {
      Mac sha1Instance = Mac.getInstance(SHA1_ALGORITHM);
      SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), SHA1_ALGORITHM);
      sha1Instance.init(signingKey);
      byte[] rawHmac = sha1Instance.doFinal(input.getBytes());
      byte[] hexBytes = new Hex().encode(rawHmac);
      return new String(hexBytes, "UTF-8");
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
      logger.warn(ex.getMessage());
    } catch (InvalidKeyException ex) {
      ex.printStackTrace();
      logger.warn(ex.getMessage());
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
      logger.warn(ex.getMessage());
    }
    return "";
  }

  public static String computeMD5(String input) {
    try {
      byte[] rawMD5 = MessageDigest.getInstance("MD5").digest(input.getBytes());
      byte[] hexBytes = new Hex().encode(rawMD5);
      String result = new String(hexBytes, "UTF-8");
      logger.debug("compute md5 for " + input + ", and result is " + result);
      return result;
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
      logger.warn(ex.getMessage());
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
      logger.warn(ex.getMessage());
    }
    return "";
  }
}
