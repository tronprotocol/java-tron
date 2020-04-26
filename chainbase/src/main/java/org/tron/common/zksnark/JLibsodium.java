package org.tron.common.zksnark;

import org.tron.common.parameter.CommonParameter;
import org.tron.common.zksnark.JLibsodiumParam.Black2bSaltPersonalParams;
import org.tron.common.zksnark.JLibsodiumParam.Blake2bFinalParams;
import org.tron.common.zksnark.JLibsodiumParam.Blake2bInitSaltPersonalParams;
import org.tron.common.zksnark.JLibsodiumParam.Blake2bUpdateParams;
import org.tron.common.zksnark.JLibsodiumParam.Chacha20Poly1305IetfEncryptParams;
import org.tron.common.zksnark.JLibsodiumParam.Chacha20poly1305IetfDecryptParams;

public class JLibsodium {

  public static final int CRYPTO_GENERICHASH_BLAKE2B_PERSONALBYTES = 16;
  public static final int CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES = 12;
  private static Libsodium INSTANCE;

  public static int cryptoGenerichashBlake2bInitSaltPersonal(Blake2bInitSaltPersonalParams params) {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE
        .cryptoGenerichashBlake2BInitSaltPersonal(params.getState(), params.getKey(),
            params.getKeyLen(), params.getOutLen(), params.getSalt(), params.getPersonal());
  }

  public static int cryptoGenerichashBlake2bUpdate(Blake2bUpdateParams params) {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE
        .cryptoGenerichashBlake2BUpdate(params.getState(), params.getIn(), params.getInLen());
  }

  public static int cryptoGenerichashBlake2bFinal(Blake2bFinalParams params) {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE.cryptoGenerichashBlake2BFinal(params.getState(),
        params.getOut(), params.getOutLen());
  }

  public static int cryptoGenerichashBlack2bSaltPersonal(Black2bSaltPersonalParams params) {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE.cryptoGenerichashBlake2BSaltPersonal(params.getOut(), params.getOutLen(),
        params.getIn(), params.getInLen(), params.getKey(), params.getKeyLen(),
        params.getSalt(),
        params.getPersonal());
  }

  public static int cryptoAeadChacha20poly1305IetfDecrypt(
      Chacha20poly1305IetfDecryptParams params) {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE
        .cryptoAeadChacha20Poly1305IetfDecrypt(params.getM(), params.getMLenP(),
            params.getNSec(),
            params.getC(), params.getCLen(), params.getAd(),
            params.getAdLen(), params.getNPub(), params.getK());
  }

  public static int cryptoAeadChacha20Poly1305IetfEncrypt(
      Chacha20Poly1305IetfEncryptParams params) {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE
        .cryptoAeadChacha20Poly1305IetfEncrypt(params.getC(), params.getCLenP(), params.getM(),
            params.getMLen(), params.getAd(), params.getAdLen(),
            params.getNSec(), params.getNPub(), params.getK());
  }

  public static long initState() {
    if (!isOpenZen()) {
      return 0;
    }
    return INSTANCE.cryptoGenerichashBlake2BStateInit();
  }

  public static void freeState(long state) {
    if (!isOpenZen()) {
      return;
    }
    INSTANCE.cryptoGenerichashBlake2BStateFree(state);
  }

  private static boolean isOpenZen() {
    boolean res = CommonParameter.getInstance()
        .isFullNodeAllowShieldedTransactionArgs();
    if (res) {
      INSTANCE = LibsodiumWrapper.getInstance();
    }
    return res;
  }
}
