package org.tron.common.zksnark;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.common.zksnark.LibrustzcashParam.Zip32XskMasterParams;
import org.tron.core.exception.ZksnarkException;

@Slf4j
public class Librustzcash {

  private static ILibrustzcash INSTANCE;

  private static final Map<String, String> libraries = new ConcurrentHashMap<>();

  static {
    INSTANCE = (ILibrustzcash) Native
        .loadLibrary(getLibraryByName("librustzcash"), ILibrustzcash.class);
  }

  public interface ILibrustzcash extends Library {

    void librustzcash_init_zksnark_params(byte[] spend_path, int spend_path_len, String spend_hash,
        byte[] output_path, int output_path_len, String output_hash);

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

  public static void librustzcashZip32XskMaster(Zip32XskMasterParams params)
      throws ZksnarkException {
    params.valid();
    INSTANCE.librustzcash_zip32_xsk_master(params.getData(), params.getSize(), params.getM_bytes());
  }

  public static void librustzcashInitZksnarkParams(InitZksnarkParams params)
      throws ZksnarkException {
    params.valid();
    INSTANCE.librustzcash_init_zksnark_params(params.getSpend_path(), params.getSpend_path_len(),
        params.getSpend_hash(), params.getOutput_path(), params.getOutput_path_len(),
        params.getOutput_hash());
  }

  public static void librustzcashZip32XskDerive(byte[] p_bytes, int i, byte[] m_bytes) {
    if (!(p_bytes.length == 169 && m_bytes.length == 169)) {
      throw new RuntimeException("librustzcash_zip32_xsk_derive invalid array size");
    }
    INSTANCE.librustzcash_zip32_xsk_derive(p_bytes, i, m_bytes);
  }

  public static boolean librustzcashZip32XfvkAddress(byte[] xfvk, byte[] j, byte[] j_ret,
      byte[] addr_ret) {
    if (!(xfvk.length == 169 && j.length == 11 && j_ret.length == 11 && addr_ret.length == 43)) {
      throw new RuntimeException("librustzcash_zip32_xfvk_address invalid array size");
    }
    return INSTANCE.librustzcash_zip32_xfvk_address(xfvk, j, j_ret, addr_ret);
  }

  /*
  * ak: 32 bytes
  * nk: 32 bytes
  * ivk: 32 bytes
  */
  public static void librustzcashCrhIvk(byte[] ak, byte[] nk, byte[] ivk) {
    if (!(ak.length == 32 && nk.length == 32 && ivk.length == 32)) {
      throw new RuntimeException("librustzcash_crh_ivk invalid array size");
    }
    INSTANCE.librustzcash_crh_ivk(ak, nk, ivk);
  }

  /*
  * p: 32 bytes
  * sk: 32 bytes
  * result: 32 bytes
  */
  public static boolean librustzcashSaplingKaAgree(byte[] p, byte[] sk, byte[] result) {
    if (!(p.length == 32 && sk.length == 32 && result.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_ka_agree invalid array size");
    }
    return INSTANCE.librustzcash_sapling_ka_agree(p, sk, result);
  }

  /*
  * d: diversifier, 11 bytes
  * pk_d: 32 bytes
  * value: 64 bits integer
  * r: rcm,  32 bytes
  * cm: note commitment, 32 bytes
  */
  public static boolean librustzcashSaplingComputeCm(byte[] d, byte[] pk_d, long value, byte[] r,
      byte[] cm) {
    if (!(d.length == 11 && pk_d.length == 32 && r.length == 32 && cm.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_compute_cm invalid array size");
    }
    return INSTANCE.librustzcash_sapling_compute_cm(d, pk_d, value, r, cm);
  }

  /*
  * d: diversifier, 11 bytes
  * pk_d, 32 bytes
  * value: 64 bits integer
  * r: rcm,  32 bytes
  * ak: 32 bytes
  * nk: 32 bytes
  * result: nullifier, 32 bytes
  */
  public static boolean librustzcashSaplingComputeNf(byte[] d, byte[] pk_d, long value_, byte[] r,
      byte[] ak, byte[] nk, long position, byte[] result) {
    if (!(d.length == 11 && pk_d.length == 32 && r.length == 32
        && ak.length == 32 && nk.length == 32 && result.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_compute_nf invalid array size");
    }
    INSTANCE.librustzcash_sapling_compute_nf(d, pk_d, value_, r, ak, nk, position, result);
    return true;
  }

  // void librustzcash_ask_to_ak(const unsigned char *ask, unsigned char *result);
  /*
  * ask: 32 bytes
  @ return 32 bytes
  */
  public static byte[] librustzcashAskToAk(byte[] ask) {
    if (!(ask.length == 32)) {
      throw new RuntimeException("librustzcash_ask_to_ak invalid array size");
    }
    byte[] ak = new byte[32];
    INSTANCE.librustzcash_ask_to_ak(ask, ak);
    return ak;
  }

  // void librustzcash_nsk_to_nk(const unsigned char *nsk, unsigned char *result);
  /*
  * nsk: 32 bytes
  @ return  32 bytes
  */
  public static byte[] librustzcashNskToNk(byte[] nsk) {
    if (!(nsk.length == 32)) {
      throw new RuntimeException("librustzcash_nsk_to_nk invalid array size");
    }
    byte[] nk = new byte[32];
    INSTANCE.librustzcash_nsk_to_nk(nsk, nk);
    return nk;
  }

  /*
  * r: return random number,  32 bytes
  */
  public static byte[] librustzcashSaplingGenerateR(byte[] r) {
    if (!(r.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_generate_r invalid array size");
    }
    INSTANCE.librustzcash_sapling_generate_r(r);
    return r;
  }

  /*
  * diversifier: d, 11 bytes
  * esk: 32 bytes
  * result: return 32 bytes
  */
  public static boolean librustzcashSaplingKaDerivepublic(byte[] diversifier, byte[] esk,
      byte[] result) {
    if (!(diversifier.length == 11 && esk.length == 32 && result.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_ka_derivepublic invalid array size");
    }
    return INSTANCE.librustzcash_sapling_ka_derivepublic(diversifier, esk, result);
  }

  public static Pointer librustzcashSaplingProvingCtxInit() {
    return INSTANCE.librustzcash_sapling_proving_ctx_init();
  }

  /*
  * check validity of d
  * d: 11 bytes
  */
  public static boolean librustzcashCheckDiversifier(byte[] d) {
    if (!(d.length == 11)) {
      throw new RuntimeException("librustzcash_check_diversifier invalid array size");
    }
    return INSTANCE.librustzcash_check_diversifier(d);
  }

  /*
  * calculate spend proof
  * ak: 32 bytes
  * nsk: 32 bytes
  * d: 11 bytes
  * r: rcm, 32 bytes
  * alpha: 32 bytes
  * value: 64 bits integer
  * anchor: 32 bytes
  * voucherPath: (1 + 33 * 32 + 8) bytes,  voucherPath[0]=0x20, voucherPath[1+i*33]=0x20,i=0,1,...31.
  * cv: value commitments, 32 bytes
  * rk: spendAuthSig.PublicKey 32 bytes
  * zkproof: spend proof, 192 bytes
  */
  public static boolean librustzcashSaplingSpendProof(Pointer ctx, byte[] ak, byte[] nsk, byte[] d,
      byte[] r, byte[] alpha, long value, byte[] anchor, byte[] voucherPath, byte[] cv, byte[] rk,
      byte[] zkproof) {

    if (!(ak.length == 32 && nsk.length == 32 && d.length == 11 && r.length == 32
        && alpha.length == 32 && anchor.length == 32 && voucherPath.length == 1 + 33 * 32 + 8
        && cv.length == 32 && rk.length == 32 && zkproof.length == 192)) {
      throw new RuntimeException("librustzcash_sapling_spend_proof invalid array size");
    }
    return INSTANCE
        .librustzcash_sapling_spend_proof(ctx, ak, nsk, d, r, alpha, value, anchor, voucherPath, cv,
            rk, zkproof);
  }

  /*
  * esk: 32 bytes
  * d: 11 bytes
  * pk_d: 32 bytes
  * r: rcm, 32 bytes
  * value: 64 bits integer
  * cv: value commitment, 32 bytes
  * zkproof: receive proof, 192 bytes
  */
  public static boolean librustzcashSaplingOutputProof(Pointer ctx, byte[] esk, byte[] d,
      byte[] pk_d, byte[] r, long value, byte[] cv, byte[] zkproof) {
    if (!(esk.length == 32 && d.length == 11 && pk_d.length == 32 && r.length == 32
        && cv.length == 32 && zkproof.length == 192)) {
      throw new RuntimeException("librustzcash_sapling_output_proof invalid array size");
    }
    return INSTANCE.librustzcash_sapling_output_proof(ctx, esk, d, pk_d, r, value, cv, zkproof);
  }

  /*
  * ask: 32 bytes
  * alpha: random number, 32 bytes
  * sigHash: 32 bytes
  * result: spendAuthSig, 64 bytes
  */
  public static boolean librustzcashSaplingSpendSig(byte[] ask, byte[] alpha, byte[] sigHash,
      byte[] result) {
    if (!(ask.length == 32 && alpha.length == 32 && sigHash.length == 32 && result.length == 64)) {
      throw new RuntimeException("librustzcash_sapling_spend_sig invalid array size");
    }
    return INSTANCE.librustzcash_sapling_spend_sig(ask, alpha, sigHash, result);
  }

  /*
  * valueBalance: 64 bits integer
  * sigHash: 32 bytes
  * result: binding signature, 64 bytes
  */
  public static boolean librustzcashSaplingBindingSig(
      Pointer ctx, long valueBalance, byte[] sighash, byte[] result) {
    if (!(sighash.length == 32 && result.length == 64)) {
      throw new RuntimeException("librustzcash_sapling_binding_sig invalid array size");
    }
    return INSTANCE.librustzcash_sapling_binding_sig(ctx, valueBalance, sighash, result);
  }

  /*
  * convert value to 32-byte scalar
  * value: 64 bytes
  * data: return, 32 bytes
  */
  public static void librustzcashToScalar(byte[] value, byte[] data) {
    if (!(value.length == 64 && data.length == 32)) {
      throw new RuntimeException("librustzcash_to_scalar invalid array size");
    }
    INSTANCE.librustzcash_to_scalar(value, data);
  }

  public static void librustzcashSaplingProvingCtxFree(Pointer ctx) {
    INSTANCE.librustzcash_sapling_proving_ctx_free(ctx);
  }

  public static Pointer librustzcashSaplingVerificationCtxInit() {
    return INSTANCE.librustzcash_sapling_verification_ctx_init();
  }

  /*
  * cv: value commitments, 32 bytes
  * anchor: 32 bytes
  * nullifier: 32 bytes
  * rk: spendAuthSig.PublicKey 32 bytes
  * zkproof: spend proof, 192 bytes
  * spendAuthSig: 64 bytes
  * sigHash: 32 bytes
  */
  public static boolean librustzcashSaplingCheckSpend(Pointer ctx, byte[] cv, byte[] anchor,
      byte[] nullifier, byte[] rk, byte[] zkproof, byte[] spendAuthSig, byte[] sighashValue) {
    if (!(cv.length == 32 && anchor.length == 32 && nullifier.length == 32 && rk.length == 32
        && zkproof.length == 192 && spendAuthSig.length == 64 && sighashValue.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_check_spend invalid array size");
    }
    return INSTANCE
        .librustzcash_sapling_check_spend(ctx, cv, anchor, nullifier, rk, zkproof, spendAuthSig,
            sighashValue);
  }

  /*
  * cv: value commitments, 32 bytes
  * cm: note commitment, 32 bytes
  * ephemeralKey: 32 bytes
  * zkproof: 192 bytes
  */
  public static boolean librustzcashSaplingCheckOutput(Pointer ctx, byte[] cv, byte[] cm,
      byte[] ephemeralKey, byte[] zkproof) {
    if (!(cv.length == 32 && cm.length == 32 && ephemeralKey.length == 32
        && zkproof.length == 192)) {
      throw new RuntimeException("librustzcash_sapling_check_output invalid array size");
    }
    return INSTANCE.librustzcash_sapling_check_output(ctx, cv, cm, ephemeralKey, zkproof);
  }

  /*
  * bindingSig: 64 bytes
  * sighashValue: 32 bytes
  */
  public static boolean librustzcashSaplingFinalCheck(Pointer ctx, long valueBalance,
      byte[] bindingSig, byte[] sighashValue) {
    if (!(bindingSig.length == 64 && sighashValue.length == 32)) {
      throw new RuntimeException("librustzcash_sapling_final_check invalid array size");
    }
    return INSTANCE.librustzcash_sapling_final_check(ctx, valueBalance, bindingSig, sighashValue);
  }

  public static void librustzcashSaplingVerificationCtxFree(Pointer ctx) {
    INSTANCE.librustzcash_sapling_verification_ctx_free(ctx);
  }

  /*
  * ivk: 32 bytes
  * d: 11 bytes
  * pk_d: 32 bytes
  */
  public static boolean librustzcashIvkToPkd(byte[] ivk, byte[] d, byte[] pk_d) {
    if (!(ivk.length == 32 && d.length == 11 && pk_d.length == 32)) {
      throw new RuntimeException("librustzcash_ivk_to_pkd invalid array size");
    }
    return INSTANCE.librustzcash_ivk_to_pkd(ivk, d, pk_d);
  }

  /*
  * a: 32 bytes
  * b: 32 bytes
  * result: 32 bytes
  */
  public static void librustzcashMerkleHash(int depth, byte[] a, byte[] b, byte[] result) {
    if (!(a.length == 32 && b.length == 32 && result.length == 32)) {
      throw new RuntimeException("librustzcash_merkle_hash invalid array size");
    }
    INSTANCE.librustzcash_merkle_hash(depth, a, b, result);
  }

  /*
  * result: uncommitted value, 32 bytes
   */
  public static void librustzcash_tree_uncommitted(byte[] result) {
    if (!(result.length == 32)) {
      throw new RuntimeException("librustzcash_tree_uncommitted invalid array size");
    }
    INSTANCE.librustzcash_tree_uncommitted(result);
  }

  public static String getLibraryByName(String name) {
    return libraries.computeIfAbsent(name, Librustzcash::getLibrary);
  }

  private static String getLibrary(String name) {
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
    InputStream in = Librustzcash.class.getClassLoader().getResourceAsStream(
        "native-package" + File.separator + platform + File.separator + name + extension);
    File fileOut = new File(
        System.getProperty("java.io.tmpdir") + File.separator + name + "-" + System
            .currentTimeMillis() + extension);
    try {
      FileUtils.copyToFile(in, fileOut);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    return fileOut.getAbsolutePath();
  }


}
