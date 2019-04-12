package org.tron.common.zksnark.sapling;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import java.awt.Point;
import java.io.File;
import org.tron.common.zksnark.sapling.zip32.HDSeed;
import org.tron.common.zksnark.sapling.zip32.HDSeed.RawHDSeed;

public class Librustzcash {

  private static ILibrustzcash INSTANCE;

  static {
    INSTANCE = (ILibrustzcash) Native.loadLibrary(getLibraryByName("librustzcash"), ILibrustzcash.class);
  }

  public interface ILibrustzcash extends Library {

    void librustzcash_zip32_xsk_master(byte[] data, int size, byte[] m_bytes);

    void librustzcash_zip32_xsk_derive(byte[] xsk_parent, int i, byte[] xsk_i);

    boolean librustzcash_zip32_xfvk_address(byte[] xfvk, byte[] j, byte[] j_ret, byte[] addr_ret);

    void librustzcash_ask_to_ak(byte[] ask, byte[] result);

    void librustzcash_nsk_to_nk(byte[] nsk, byte[] result);

    void librustzcash_crh_ivk(byte[] ak, byte[] nk, byte[] result);

    boolean librustzcash_sapling_ka_agree(byte[] p, byte[] sk, byte[] result);

    boolean librustzcash_check_diversifier(byte[] diversifier);

    boolean librustzcash_ivk_to_pkd(byte[] ivk, byte[] diversifier, byte[] result);

    boolean librustzcash_sapling_compute_cm(byte[] diversifier, byte[] pk_d, long value, byte[] r,
        byte[] result);
    //bool librustzcash_ivk_to_pkd(const unsigned char *ivk, const unsigned char *diversifier, unsigned char *result);
//    bool librustzcash_sapling_compute_cm(
//        const unsigned char *diversifier,
//        const unsigned char *pk_d,
//        const uint64_t value,
//        const unsigned char *r,
//        unsigned char *result
//    );

    Pointer librustzcash_sapling_proving_ctx_init();
    boolean librustzcash_sapling_spend_proof(
        Pointer ctx,
        byte[] ak,
        byte[] nsk,
        byte[] diversifier,
        byte[] rcm,
        byte[] ar,
        long value,
        byte[] anchor,
        byte[] witness,
        byte[] cv,
        byte[] rk,
        byte[] zkproof
    );
    boolean librustzcash_sapling_output_proof(
        Pointer ctx,
        byte[] esk,
        byte[] diversifier,
        byte[] pk_d,
        byte[] rcm,
        long value,
        byte[] cv,
        byte[] zkproof
    );
    boolean librustzcash_sapling_binding_sig(
        Pointer ctx,
        long valueBalance,
        byte[] sighash,
        byte[] result
    );
    void librustzcash_sapling_proving_ctx_free(Pointer ctx);

    Pointer librustzcash_sapling_verification_ctx_init();
    boolean librustzcash_sapling_check_spend(
        Pointer ctx,
        byte[] cv,
        byte[] anchor,
        byte[] nullifier,
        byte[] rk,
        byte[] zkproof,
        byte[] spendAuthSig,
        byte[] sighashValue
    );
    boolean librustzcash_sapling_check_output(
        Pointer ctx,
        byte[] cv,
        byte[] cm,
        byte[] ephemeralKey,
        byte[] zkproof
    );

    boolean librustzcash_sapling_final_check(
        Pointer ctx,
        long valueBalance,
        byte[] bindingSig,
        byte[] sighashValue
    );
    void librustzcash_sapling_verification_ctx_free(Pointer ctx);

  }

  // todo jni
  /*
  /// Derive the master ExtendedSpendingKey from a seed.
    void librustzcash_zip32_xsk_master(
        const unsigned char *seed,
        size_t seedlen,
        unsigned char *xsk_master
    );
  * */
  public static void librustzcashZip32XskMaster(byte[] data, int size, byte[] m_bytes) {
    INSTANCE.librustzcash_zip32_xsk_master(data, size, m_bytes);
  }

  /*
  /// Derive a child ExtendedSpendingKey from a parent.
    void librustzcash_zip32_xsk_derive(
        const unsigned char *xsk_parent,
        uint32_t i,
        unsigned char *xsk_i
    );
  * */
  public static void librustzcashZip32XskDerive(byte[] p_bytes, int i, byte[] m_bytes) {
    INSTANCE.librustzcash_zip32_xsk_derive(p_bytes, i, m_bytes);
  }

  // /// Derive a PaymentAddress from an ExtendedFullViewingKey.
  //     bool librustzcash_zip32_xfvk_address(
  //         const unsigned char *xfvk,
  //         const unsigned char *j,
  //         unsigned char *j_ret,
  //         unsigned char *addr_ret
  //     );
  public static boolean librustzcashZip32XfvkAddress(byte[] xfvk, byte[] j, byte[] j_ret,
      byte[] addr_ret) {
    return INSTANCE.librustzcash_zip32_xfvk_address(xfvk, j, j_ret, addr_ret);
  }


  // void librustzcash_crh_ivk(const unsigned char *ak, const unsigned char *nk, unsigned char
  // *result);
  public static void librustzcashCrhIvk(byte[] ak, byte[] nk, byte[] ivk) {
    System.out.println("just a test");
    INSTANCE.librustzcash_crh_ivk(ak, nk, ivk);
  }

  public static boolean librustzcashSaplingKaAgree(byte[] p, byte[] sk, byte[] result) {
    return INSTANCE.librustzcash_sapling_ka_agree(p, sk, result);
  }

  public static boolean librustzcashSaplingComputeCm(
      byte[] d, byte[] pk_d, long value, byte[] r, byte[] cm) {
    return INSTANCE.librustzcash_sapling_compute_cm(d, pk_d, value, r, cm);
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

  // void librustzcash_ask_to_ak(const unsigned char *ask, unsigned char *result);
  public static byte[] librustzcashAskToAk(byte[] aks) {
    byte[] ak = new byte[32];
    INSTANCE.librustzcash_ask_to_ak(aks, ak);
    return ak;
  }

  // void librustzcash_nsk_to_nk(const unsigned char *nsk, unsigned char *result);
  public static byte[] librustzcashNskToNk(byte[] nsk) {
    byte[] nk = new byte[32];
    INSTANCE.librustzcash_nsk_to_nk(nsk, nk);
    return nk;
  }

  public static byte[] librustzcashSaplingGenerateR(byte[] r) {
    return null;
  }

  public static Pointer librustzcashSaplingProvingCtxInit() {
    return INSTANCE.librustzcash_sapling_proving_ctx_init();
  }

  public static boolean librustzcashCheckDiversifier(byte[] b) {
    // TODO
    return INSTANCE.librustzcash_check_diversifier(b);
  }


  public static boolean librustzcashSaplingSpendProof(
      Pointer ctx,
      byte[] ak,
      byte[] nsk,
      byte[] d,
      byte[] r,
      byte[] alpha,
      long value,
      byte[] anchor,
      byte[] voucherPath,
      byte[] cv,
      byte[] rk,
      byte[] zkproof) {
    return INSTANCE.librustzcash_sapling_spend_proof(ctx, ak, nsk, d, r, alpha, value, anchor, voucherPath, cv, rk, zkproof);
  }

  public static boolean librustzcashSaplingOutputProof(
      Pointer ctx,
      byte[] esk,
      byte[] d,
      byte[] pk_d,
      byte[] r,
      long value,
      byte[] cv,
      byte[] zkproof) {
    return INSTANCE.librustzcash_sapling_output_proof(ctx, esk, d, pk_d, r, value, cv, zkproof);
  }

  public static boolean librustzcashSaplingSpendSig(
      byte[] ask,
      byte[] ar,
      byte[] sighash,
      byte[] result) {
    return false;
  }

  public static boolean librustzcashSaplingBindingSig(
      Pointer ctx, long valueBalance, byte[] sighash, byte[] result) {
    return INSTANCE.librustzcash_sapling_binding_sig(ctx, valueBalance, sighash, result);
  }

  public static void librustzcashToScalar(byte[] value, byte[] data) {
  }

  public static void librustzcashSaplingProvingCtxFree(Pointer ctx) {
    INSTANCE.librustzcash_sapling_proving_ctx_free(ctx);
  }

  public static boolean librustzcashIvkToPkd(byte[] ivk, byte[] d, byte[] pk_d) {

    return INSTANCE.librustzcash_ivk_to_pkd(ivk, d, pk_d);
  }

  public static String getLibraryByName(String name) {
    String platform;
    String extension;
    if (Platform.isLinux()) {
      platform = "linux";
      extension = ".so";
    } else if (Platform.isWindows()) {
      platform = "windows";
      extension = ".dll";
    } else if (Platform.isMac()) {
      platform = "macos";
      extension = ".dylib";
    } else {
      throw new RuntimeException("unsupportedPlatformException");
    }

    return Librustzcash.class.getClassLoader().getResource(
        "native-package" + File.separator + platform + File.separator + name +extension).getFile();
  }

  public static void main(String[] args) throws Exception {
    Librustzcash librustzcash = new Librustzcash();

    byte[] aa = {0x16, 0x52, 0x52};

    HDSeed seed = KeyStore.seed;
    RawHDSeed rawHDSeed = new RawHDSeed();
    seed.rawSeed = rawHDSeed;
    seed.rawSeed.data = aa;
    RawHDSeed rawSeed = seed.getRawSeed();

    int ZIP32_XSK_SIZE = 169; // byte
    byte[] m_bytes = new byte[ZIP32_XSK_SIZE];
    librustzcash.librustzcashZip32XskMaster(rawSeed.getData(), rawSeed.getData().length, m_bytes);

  }

}
