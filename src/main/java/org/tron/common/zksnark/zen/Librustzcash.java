package org.tron.common.zksnark.zen;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import java.io.File;
import org.tron.common.zksnark.zen.zip32.HDSeed;
import org.tron.common.zksnark.zen.zip32.HDSeed.RawHDSeed;

public class Librustzcash {

  private static ILibrustzcash INSTANCE;

  static {
    INSTANCE = (ILibrustzcash) Native
        .loadLibrary(getLibraryByName("librustzcash"), ILibrustzcash.class);
  }

  public interface ILibrustzcash extends Library {

    void librustzcash_init_zksnark_params(byte[] spend_path, int spend_path_len, byte[] spend_hash,
        byte[] output_path, int output_path_len, byte[] output_hash, byte[] sprout_path,
        int sprout_path_len, byte[] sprout_hash);

    void librustzcash_zip32_xsk_master(byte[] data, int size, byte[] m_bytes);

    void librustzcash_zip32_xsk_derive(byte[] xsk_parent, int i, byte[] xsk_i);

    boolean librustzcash_zip32_xfvk_address(byte[] xfvk, byte[] j, byte[] j_ret, byte[] addr_ret);

    void librustzcash_ask_to_ak(byte[] ask, byte[] result);

    void librustzcash_sapling_compute_nf(byte[] d, byte[] pk_d, long value_, byte[] r, byte[] ak,
        byte[] nk, long position, byte[] result);

    void librustzcash_nsk_to_nk(byte[] nsk, byte[] result);

    void librustzcash_sapling_generate_r(byte[] r);

    boolean librustzcash_sapling_ka_derivepublic(byte[] diversifier, byte[] esk, byte[] result);

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

    boolean librustzcash_sapling_spend_proof(Pointer ctx, byte[] ak, byte[] nsk, byte[] diversifier,
        byte[] rcm, byte[] ar, long value, byte[] anchor, byte[] witness, byte[] cv, byte[] rk,
        byte[] zkproof);

    boolean librustzcash_sapling_output_proof(Pointer ctx, byte[] esk, byte[] diversifier,
        byte[] pk_d, byte[] rcm, long value, byte[] cv, byte[] zkproof);

    boolean librustzcash_sapling_spend_sig(byte[] ask, byte[] ar, byte[] sighash, byte[] result);

    boolean librustzcash_sapling_binding_sig(Pointer ctx, long valueBalance, byte[] sighash,
        byte[] result);

    void librustzcash_sapling_proving_ctx_free(Pointer ctx);

    Pointer librustzcash_sapling_verification_ctx_init();

    boolean librustzcash_sapling_check_spend(Pointer ctx, byte[] cv, byte[] anchor,
        byte[] nullifier, byte[] rk, byte[] zkproof, byte[] spendAuthSig, byte[] sighashValue);

    boolean librustzcash_sapling_check_output(Pointer ctx, byte[] cv, byte[] cm,
        byte[] ephemeralKey, byte[] zkproof);

    boolean librustzcash_sapling_final_check(Pointer ctx, long valueBalance, byte[] bindingSig,
        byte[] sighashValue);

    void librustzcash_sapling_verification_ctx_free(Pointer ctx);

    /// Computes a merkle tree hash for a given depth.
    /// The `depth` parameter should not be larger than
    /// 62.
    ///
    /// `a` and `b` each must be of length 32, and must each
    /// be scalars of BLS12-381.
    ///
    /// The result of the merkle tree hash is placed in
    /// `result`, which must also be of length 32.
    void librustzcash_merkle_hash(int depth, byte[] a, byte[] b, byte[] result
    );

    /// Writes the "uncommitted" note value for empty leaves
    /// of the merkle tree. `result` must be a valid pointer
    /// to 32 bytes which will be written.
    void librustzcash_tree_uncommitted(byte[] result);

    void librustzcash_to_scalar(byte[] input, byte[] result);
  }

  public static void librustzcashZip32XskMaster(byte[] data, int size, byte[] m_bytes) {
    INSTANCE.librustzcash_zip32_xsk_master(data, size, m_bytes);
  }

  public static void librustzcashInitZksnarkParams(byte[] spend_path, int spend_path_len,
      byte[] spend_hash, byte[] output_path, int output_path_len, byte[] output_hash,
      byte[] sprout_path, int sprout_path_len, byte[] sprout_hash) {
    INSTANCE.librustzcash_init_zksnark_params(spend_path, spend_path_len, spend_hash,
        output_path, output_path_len, output_hash, sprout_path, sprout_path_len,
        sprout_hash);
  }

  public static void librustzcashZip32XskDerive(byte[] p_bytes, int i, byte[] m_bytes) {
    INSTANCE.librustzcash_zip32_xsk_derive(p_bytes, i, m_bytes);
  }

  public static boolean librustzcashZip32XfvkAddress(byte[] xfvk, byte[] j, byte[] j_ret,
      byte[] addr_ret) {
    return INSTANCE.librustzcash_zip32_xfvk_address(xfvk, j, j_ret, addr_ret);
  }

  public static void librustzcashCrhIvk(byte[] ak, byte[] nk, byte[] ivk) {
    INSTANCE.librustzcash_crh_ivk(ak, nk, ivk);
  }

  public static boolean librustzcashSaplingKaAgree(byte[] p, byte[] sk, byte[] result) {
    return INSTANCE.librustzcash_sapling_ka_agree(p, sk, result);
  }

  public static boolean librustzcashSaplingComputeCm(byte[] d, byte[] pk_d, long value, byte[] r,
      byte[] cm) {
    return INSTANCE.librustzcash_sapling_compute_cm(d, pk_d, value, r, cm);
  }

  public static boolean librustzcashSaplingComputeNf(byte[] d, byte[] pk_d, long value_, byte[] r,
      byte[] ak, byte[] nk, long position, byte[] result) {
    INSTANCE.librustzcash_sapling_compute_nf(d, pk_d, value_, r, ak, nk, position, result);
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
    INSTANCE.librustzcash_sapling_generate_r(r);
    return r;
  }

  public static boolean librustzcashSaplingKaDerivepublic(byte[] diversifier, byte[] esk,
      byte[] result) {
    return INSTANCE.librustzcash_sapling_ka_derivepublic(diversifier, esk, result);
  }

  public static Pointer librustzcashSaplingProvingCtxInit() {
    return INSTANCE.librustzcash_sapling_proving_ctx_init();
  }

  public static boolean librustzcashCheckDiversifier(byte[] b) {
    return INSTANCE.librustzcash_check_diversifier(b);
  }

  public static boolean librustzcashSaplingSpendProof(Pointer ctx, byte[] ak, byte[] nsk, byte[] d,
      byte[] r, byte[] alpha, long value, byte[] anchor, byte[] voucherPath, byte[] cv, byte[] rk,
      byte[] zkproof) {
    return INSTANCE
        .librustzcash_sapling_spend_proof(ctx, ak, nsk, d, r, alpha, value, anchor, voucherPath, cv,
            rk, zkproof);
  }

  public static boolean librustzcashSaplingOutputProof(Pointer ctx, byte[] esk, byte[] d,
      byte[] pk_d, byte[] r, long value, byte[] cv, byte[] zkproof) {
    return INSTANCE.librustzcash_sapling_output_proof(ctx, esk, d, pk_d, r, value, cv, zkproof);
  }

  public static boolean librustzcashSaplingSpendSig(byte[] ask, byte[] alpha, byte[] sigHash,
      byte[] result) {
    return INSTANCE.librustzcash_sapling_spend_sig(ask, alpha, sigHash, result);
  }

  public static boolean librustzcashSaplingBindingSig(
      Pointer ctx, long valueBalance, byte[] sighash, byte[] result) {
    return INSTANCE.librustzcash_sapling_binding_sig(ctx, valueBalance, sighash, result);
  }

  public static void librustzcashToScalar(byte[] value, byte[] data) {
    INSTANCE.librustzcash_to_scalar(value, data);
  }

  public static void librustzcashSaplingProvingCtxFree(Pointer ctx) {
    INSTANCE.librustzcash_sapling_proving_ctx_free(ctx);
  }

  public static Pointer librustzcashSaplingVerificationCtxInit() {
    return INSTANCE.librustzcash_sapling_verification_ctx_init();
  }

  public static boolean librustzcashSaplingCheckSpend(Pointer ctx, byte[] cv, byte[] anchor,
      byte[] nullifier, byte[] rk, byte[] zkproof, byte[] spendAuthSig, byte[] sighashValue) {
    return INSTANCE
        .librustzcash_sapling_check_spend(ctx, cv, anchor, nullifier, rk, zkproof, spendAuthSig,
            sighashValue);
  }

  public static boolean librustzcashSaplingCheckOutput(Pointer ctx, byte[] cv, byte[] cm,
      byte[] ephemeralKey, byte[] zkproof) {
    return INSTANCE.librustzcash_sapling_check_output(ctx, cv, cm, ephemeralKey, zkproof);
  }

  public static boolean librustzcashSaplingFinalCheck(Pointer ctx, long valueBalance,
      byte[] bindingSig, byte[] sighashValue) {
    return INSTANCE.librustzcash_sapling_final_check(ctx, valueBalance, bindingSig, sighashValue);
  }

  public static void librustzcashSaplingVerificationCtxFree(Pointer ctx) {
    INSTANCE.librustzcash_sapling_verification_ctx_free(ctx);
  }

  public static boolean librustzcashIvkToPkd(byte[] ivk, byte[] d, byte[] pk_d) {
    return INSTANCE.librustzcash_ivk_to_pkd(ivk, d, pk_d);
  }

  public static void librustzcashMerkleHash(int depth, byte[] a, byte[] b, byte[] result) {
    INSTANCE.librustzcash_merkle_hash(depth, a, b, result);
  }

  public static void librustzcash_tree_uncommitted(byte[] result) {
    INSTANCE.librustzcash_tree_uncommitted(result);
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
        "native-package" + File.separator + platform + File.separator + name + extension).getFile();
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
