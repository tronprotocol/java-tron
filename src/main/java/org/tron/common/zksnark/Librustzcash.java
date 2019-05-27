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
import org.tron.common.zksnark.LibrustzcashParam.CrhIvkParams;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.MerkleHashParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingBindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingCheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingCheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingComputeNfParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingFinalCheckParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingKaAgreeParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingKaDerivepublicParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingOutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingSpendProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingSpendSigParams;
import org.tron.common.zksnark.LibrustzcashParam.Zip32XfvkAddressParams;
import org.tron.common.zksnark.LibrustzcashParam.Zip32XskDeriveParams;
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
    INSTANCE.librustzcash_zip32_xsk_master(params.getData(), params.getSize(), params.getM_bytes());
  }

  public static void librustzcashInitZksnarkParams(InitZksnarkParams params)
      throws ZksnarkException {
    INSTANCE.librustzcash_init_zksnark_params(params.getSpend_path(), params.getSpend_path_len(),
        params.getSpend_hash(), params.getOutput_path(), params.getOutput_path_len(),
        params.getOutput_hash());
  }

  public static void librustzcashZip32XskDerive(Zip32XskDeriveParams params)
      throws ZksnarkException {
    INSTANCE.librustzcash_zip32_xsk_derive(params.getData(), params.getSize(), params.getM_bytes());
  }

  public static boolean librustzcashZip32XfvkAddress(Zip32XfvkAddressParams params)
      throws ZksnarkException {
    return INSTANCE.librustzcash_zip32_xfvk_address(params.getXfvk(), params.getJ(),
        params.getJ_ret(), params.getAddr_ret());
  }

  public static void librustzcashCrhIvk(CrhIvkParams params) {
    INSTANCE.librustzcash_crh_ivk(params.getAk(), params.getNk(), params.getIvk());
  }

  public static boolean librustzcashSaplingKaAgree(SaplingKaAgreeParams params) {
    return INSTANCE
        .librustzcash_sapling_ka_agree(params.getP(), params.getSk(), params.getResult());
  }

  public static boolean librustzcashSaplingComputeCm(SaplingComputeCmParams params) {
    return INSTANCE.librustzcash_sapling_compute_cm(params.getD(), params.getPk_d(),
        params.getValue(), params.getR(), params.getCm());
  }

  public static boolean librustzcashSaplingComputeNf(SaplingComputeNfParams params) {
    INSTANCE.librustzcash_sapling_compute_nf(params.getD(), params.getPk_d(), params.getValue(),
        params.getR(), params.getAk(), params.getNk(), params.getPosition(), params.getResult());
    return true;
  }

  // void librustzcash_ask_to_ak(const unsigned char *ask, unsigned char *result);
  public static byte[] librustzcashAskToAk(byte[] ask) throws ZksnarkException {
    if (!(ask.length == 32)) {
      throw new ZksnarkException("librustzcash_ask_to_ak invalid array size");
    }
    byte[] ak = new byte[32];
    INSTANCE.librustzcash_ask_to_ak(ask, ak);
    return ak;
  }

  // void librustzcash_nsk_to_nk(const unsigned char *nsk, unsigned char *result);
  public static byte[] librustzcashNskToNk(byte[] nsk) throws ZksnarkException {
    if (!(nsk.length == 32)) {
      throw new ZksnarkException("librustzcash_nsk_to_nk invalid array size");
    }
    byte[] nk = new byte[32];
    INSTANCE.librustzcash_nsk_to_nk(nsk, nk);
    return nk;
  }

  public static byte[] librustzcashSaplingGenerateR(byte[] r) throws ZksnarkException {
    if (!(r.length == 32)) {
      throw new ZksnarkException("librustzcash_sapling_generate_r invalid array size");
    }
    INSTANCE.librustzcash_sapling_generate_r(r);
    return r;
  }

  public static boolean librustzcashSaplingKaDerivepublic(SaplingKaDerivepublicParams params) {
    return INSTANCE.librustzcash_sapling_ka_derivepublic(params.getDiversifier(), params.getEsk(),
        params.getResult());
  }

  public static Pointer librustzcashSaplingProvingCtxInit() {
    return INSTANCE.librustzcash_sapling_proving_ctx_init();
  }

  public static boolean librustzcashCheckDiversifier(byte[] d) throws ZksnarkException {
    if (!(d.length == 11)) {
      throw new ZksnarkException("librustzcash_check_diversifier invalid array size");
    }
    return INSTANCE.librustzcash_check_diversifier(d);
  }

  public static boolean librustzcashSaplingSpendProof(SaplingSpendProofParams params) {
    return INSTANCE.librustzcash_sapling_spend_proof(params.getCtx(), params.getAk(),
        params.getNsk(), params.getD(), params.getR(), params.getAlpha(), params.getValue(),
        params.getAnchor(), params.getVoucherPath(), params.getCv(), params.getRk(),
        params.getZkproof());
  }

  public static boolean librustzcashSaplingOutputProof(SaplingOutputProofParams params) {
    return INSTANCE.librustzcash_sapling_output_proof(params.getCtx(), params.getEsk(),
        params.getD(), params.getPk_d(), params.getR(), params.getValue(), params.getCv(),
        params.getZkproof());
  }

  public static boolean librustzcashSaplingSpendSig(SaplingSpendSigParams params) {
    return INSTANCE.librustzcash_sapling_spend_sig(params.getAsk(), params.getAlpha(),
        params.getSigHash(), params.getResult());
  }

  public static boolean librustzcashSaplingBindingSig(SaplingBindingSigParams params) {
    return INSTANCE.librustzcash_sapling_binding_sig(params.getCtx(), params.getValueBalance(),
        params.getSighash(), params.getResult());
  }

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

  public static boolean librustzcashSaplingCheckSpend(SaplingCheckSpendParams params) {
    return INSTANCE.librustzcash_sapling_check_spend(params.getCtx(), params.getCv(),
        params.getAnchor(), params.getNullifier(), params.getRk(), params.getZkproof(),
        params.getSpendAuthSig(), params.getSighashValue());
  }

  public static boolean librustzcashSaplingCheckOutput(SaplingCheckOutputParams params) {
    return INSTANCE.librustzcash_sapling_check_output(params.getCtx(), params.getCv(),
        params.getCm(), params.getEphemeralKey(), params.getZkproof());
  }

  public static boolean librustzcashSaplingFinalCheck(SaplingFinalCheckParams params) {
    return INSTANCE.librustzcash_sapling_final_check(params.getCtx(), params.getValueBalance(),
        params.getBindingSig(), params.getSighashValue());
  }

  public static void librustzcashSaplingVerificationCtxFree(Pointer ctx) {
    INSTANCE.librustzcash_sapling_verification_ctx_free(ctx);
  }

  public static boolean librustzcashIvkToPkd(IvkToPkdParams params) {
    return INSTANCE.librustzcash_ivk_to_pkd(params.getIvk(), params.getD(), params.getPk_d());
  }

  public static void librustzcashMerkleHash(MerkleHashParams params) {
    INSTANCE.librustzcash_merkle_hash(params.getDepth(), params.getA(), params.getB(),
        params.getResult());
  }

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
