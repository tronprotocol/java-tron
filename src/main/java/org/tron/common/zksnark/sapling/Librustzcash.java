package org.tron.common.zksnark.sapling;

import java.util.List;

public class Librustzcash {

  // todo jni
  public static void librustzcashZip32XskMaster(List data, int size, byte[] m_bytes) {
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
}
