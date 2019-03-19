package org.tron.common.zksnark.sapling;

import java.util.ArrayList;
import java.util.List;
import com.sun.jna.Library;
import com.sun.jna.Native;
import org.tron.common.zksnark.sapling.zip32.HDSeed;
import org.tron.common.zksnark.sapling.zip32.HDSeed.RawHDSeed;

public class Librustzcash {

  private static ILibrustzcash INSTANCE;

  static {
    // System.load("/Users/tron/Documents/codes/java-tron/src/main/resources/librustzcash/librustzcash.dylib");
    INSTANCE = (ILibrustzcash)Native.loadLibrary("/Users/tron/Documents/codes/java-tron/src/main/resources/librustzcash/librustzcash.dylib", ILibrustzcash.class);
  }

  public interface ILibrustzcash extends Library {
    void librustzcash_zip32_xsk_master(byte[] data, int size, byte[] m_bytes);
  }

  // todo jni
  public static void librustzcashZip32XskMaster(byte[] data, int size, byte[] m_bytes) {
    INSTANCE.librustzcash_zip32_xsk_master(data, size, m_bytes);
  }

  public static void librustzcashZip32XskDerive(byte[] p_bytes, int i, byte[] m_bytes) {
  }

  public static void librustzcashCrhIvk(byte[] ak, byte[] nk, byte[] ivk) {
  }

  public static boolean librustzcashSaplingComputeCm(
      byte[] d, byte[] pk_d, long value_, byte[] r, byte[] cm) {
    return true;
  }

  public static boolean librustzcashSaplingComputeNf(
      byte[] d,
      byte[] pk_d,
      long value_,
      byte[] r,
      byte[] ak,
      byte[] nk,
      long position,
      byte[] result) {
    return true;
  }

  public static byte[] librustzcashAskToAk(byte[] aks) {
    return null;
  }

  public static byte[] librustzcashNskToNk(byte[] nsk) {
    return null;
  }

  public static byte[] librustzcashSaplingGenerateR(byte[] r) {
    return null;
  }

  public static ProvingContext librustzcashSaplingProvingCtxInit() {

    return null;
  }

  public static boolean librustzcashCheckDiversifier(byte[] b) {
    return true;
  }

  public static ProvingContext librustzcashIvkToPkd(byte[] value, byte[] data, byte[] pk_d) {

    return null;
  }

  public static void librustzcash_to_scalar(byte[] value, byte[] data) {
  }

  public static ProvingContext librustzcashSaplingProvingCtxFree() {
    return null;
  }

  public static void main(String[] args) throws Exception {
    Librustzcash librustzcash = new Librustzcash();

    byte [] aa = {0x16,      0x52,      0x52};

    HDSeed seed = KeyStore.seed;
    RawHDSeed rawHDSeed = new RawHDSeed();
    seed.seed = rawHDSeed;
    seed.seed.data = aa;
    RawHDSeed rawSeed = seed.getSeed();

    int ZIP32_XSK_SIZE = 169; // byte
    byte[] m_bytes = new byte[ZIP32_XSK_SIZE];
    librustzcash.librustzcashZip32XskMaster(rawSeed.getData(), rawSeed.getData().length, m_bytes);

  }

}
