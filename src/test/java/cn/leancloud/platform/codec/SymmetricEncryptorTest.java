package cn.leancloud.platform.codec;

import io.vertx.core.json.JsonObject;
import junit.framework.TestCase;

import java.net.URLDecoder;
import java.net.URLEncoder;

public class SymmetricEncryptorTest extends TestCase {
  public void testEncodeWithDES() throws Exception {
    String secretKey = "lcserver";
    String plainText = "thismy3desdemotest";
    String encryptData = SymmetricEncryptor.encodeWithDES(plainText, secretKey);
    String urlencodedEncryptData = URLEncoder.encode(encryptData, "utf-8");
    String urldecodedEncryptData = URLDecoder.decode(urlencodedEncryptData, "utf-8");
    String decryptData = SymmetricEncryptor.decodeWithDES(urldecodedEncryptData, secretKey);
    System.out.println("origin=" + plainText + ", encrypt=" + encryptData + ", decrypt=" + decryptData);
    assertTrue(plainText.equals(decryptData));
  }

  public void testJsonEncodeWithDES() throws Exception {
    JsonObject json = new JsonObject().put("appId", "ayers-app").put("objectId", "5ce3f0947b968a00730fbfca").put("dl", System.currentTimeMillis());
    String secretKey = "lcserver";
    String plainText = json.toString();
    String encryptData = SymmetricEncryptor.encodeWithDES(plainText, secretKey);
    String urlencodedEncryptData = URLEncoder.encode(encryptData, "utf-8");
    String urldecodedEncryptData = URLDecoder.decode(urlencodedEncryptData, "utf-8");
    String decryptData = SymmetricEncryptor.decodeWithDES(urldecodedEncryptData, secretKey);
    System.out.println("origin=" + plainText + ", encrypt=" + encryptData + ", decrypt=" + decryptData);
    assertTrue(plainText.equals(decryptData));
  }
}
