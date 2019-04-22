package cn.leancloud.platform.codec;

import cn.leancloud.platform.utils.StringUtils;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MessageDigest {
  private static final String DEFAULT_CHARSET = "UTF-8";
  public static final String ALGORITHM_MD5 = "MD5";
  public static final String ALGORITHM_SHA1 = "SHA-1";
  public static final String ALGORITHM_SHA256 = "SHA-256";
  private static final String ALGORITHM_HMACSHA1 = "HmacSHA1";

  public static String compute(String algo, String input) {
    if (StringUtils.isEmpty(algo) || StringUtils.isEmpty(input)) {
      return "";
    }
    try {
      byte[] data = input.getBytes(DEFAULT_CHARSET);
      java.security.MessageDigest md = java.security.MessageDigest.getInstance(algo);
      md.update(data, 0, data.length);
      return new String(new Hex().encode(md.digest()), DEFAULT_CHARSET);
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
    }
    return "";
  }

  public static String computeSHA1WithKey(String input, String key) {
    try {
      Mac sha1Instance = Mac.getInstance(ALGORITHM_HMACSHA1);
      SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), ALGORITHM_HMACSHA1);
      sha1Instance.init(signingKey);
      byte[] rawHmac = sha1Instance.doFinal(input.getBytes());
      byte[] hexBytes = new Hex().encode(rawHmac);
      return new String(hexBytes, DEFAULT_CHARSET);
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
    } catch (InvalidKeyException ex) {
      ex.printStackTrace();
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
    }
    return "";
  }
}
