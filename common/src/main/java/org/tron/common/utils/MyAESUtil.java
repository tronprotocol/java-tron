package org.tron.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.nio.charset.StandardCharsets;

public class MyAESUtil {

  public static String encrypt(String sSrc, String sKey) throws Exception {
    if (sKey == null) {
      System.out.print("Key is null");
      return null;
    }
    if (sKey.length() != 16) {
      System.out.print("key Length less than 16");
      return null;
    }
    byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
    byte[] encrypted = cipher.doFinal(sSrc.getBytes(StandardCharsets.UTF_8));
    return new BASE64Encoder().encode(encrypted);
  }


  public static String encrypt2(byte[] sSrc, String sKey) throws Exception {
    if (sKey == null) {
      System.out.print("Key is null");
      return null;
    }
    if (sKey.length() != 16) {
      System.out.print("Kkey length less than 16");
      return null;
    }
    byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
    byte[] encrypted = cipher.doFinal(sSrc);
    return new BASE64Encoder().encode(encrypted);
  }

  public static byte[] decrypt2(String sSrc, String sKey) throws Exception {
    try {
      if (sKey == null) {
        System.out.print("Key is null");
        return null;
      }
      if (sKey.length() != 16) {
        System.out.print("key length less than 16");
        return null;
      }
      byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
      SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec);
      byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);
      byte[] original = null;
      try {
        original = cipher.doFinal(encrypted1);
      } catch (Exception e) {
        System.out.println(e.toString());
        return null;
      }
      return original;
    } catch (Exception ex) {
      System.out.println(ex.toString());
      return null;
    }
  }

  public static String decrypt(String sSrc, String sKey) throws Exception {
    try {
      if (sKey == null) {
        System.out.print("Key is null");
        return null;
      }
      if (sKey.length() != 16) {
        System.out.print("key length less than 16");
        return null;
      }
      byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
      SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec);
      byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);
      try {
        byte[] original = cipher.doFinal(encrypted1);
        return new String(original, StandardCharsets.UTF_8);
      } catch (Exception e) {
        System.out.println(e.toString());
        return null;
      }
    } catch (Exception ex) {
      System.out.println(ex.toString());
      return null;
    }
  }
}
