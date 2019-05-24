package cn.leancloud.platform.codec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;

public class SymmetricEncryptor {
  private final static String DES_ALGORITHM = "DES";
  private final static String DES_MODE_ECB = "DES/ECB/pkcs5padding";
  private final static String DES_MODE_EDE = "desede/CBC/PKCS5Padding";

  private final static String encoding = "utf-8";

  public static String encodeWithDES(String plainText, String secretKey) throws Exception {
    SecureRandom sr = new SecureRandom();
    DESKeySpec dks = new DESKeySpec(secretKey.getBytes(encoding));
    SecretKeyFactory skf = SecretKeyFactory.getInstance(DES_ALGORITHM);
    SecretKey sk = skf.generateSecret(dks);
    Cipher cipher = Cipher.getInstance(DES_MODE_ECB);
    cipher.init(Cipher.ENCRYPT_MODE, sk, sr);
    byte[] encryptData = cipher.doFinal(plainText.getBytes(encoding));
    return new String(Base64.encode(encryptData));
  }

  public static String decodeWithDES(String encryptText, String secretKey) throws Exception {
    SecureRandom sr = new SecureRandom();
    DESKeySpec dks = new DESKeySpec(secretKey.getBytes(encoding));
    SecretKeyFactory skf = SecretKeyFactory.getInstance(DES_ALGORITHM);
    SecretKey sk = skf.generateSecret(dks);
    Cipher cipher = Cipher.getInstance(DES_MODE_ECB);
    cipher.init(Cipher.DECRYPT_MODE, sk, sr);
    byte[] decryptData = cipher.doFinal(Base64.decode(encryptText));
    return new String(decryptData);
  }
}
