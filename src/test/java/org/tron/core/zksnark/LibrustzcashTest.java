package org.tron.core.zksnark;

import static org.tron.common.zksnark.Librustzcash.librustzcashCheckDiversifier;
import static org.tron.common.zksnark.Librustzcash.librustzcashIvkToPkd;
import static org.tron.common.zksnark.Librustzcash.librustzcashNskToNk;
import static org.tron.common.zksnark.Librustzcash.librustzcashSaplingBindingSig;
import static org.tron.common.zksnark.Librustzcash.librustzcashSaplingComputeCm;
import static org.tron.common.zksnark.Librustzcash.librustzcashSaplingProvingCtxInit;
import static org.tron.common.zksnark.Librustzcash.librustzcashSaplingSpendSig;
import static org.tron.common.zksnark.Libsodium.crypto_aead_chacha20poly1305_IETF_NPUBBYTES;

import com.sun.jna.Pointer;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingBindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingSpendSigParams;
import org.tron.common.zksnark.Libsodium;
import org.tron.core.exception.ZksnarkException;

public class LibrustzcashTest {

  public static void main(String[] args) throws ZksnarkException {
    testZcashParam();

    byte[] K = new byte[32];
    byte[] ovk = new byte[32];
    byte[] cv = new byte[32];
    byte[] cm = new byte[32];
    byte[] epk = new byte[32];

    test(K, ovk, cv, cm, epk);
  }

  private static void testZcashParam() throws ZksnarkException {
    byte[] d = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    //byte[] d ={};
    //byte[] pk_d = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    byte[] ivk = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15};
    byte[] pk_d = new byte[32];
    long value = 1;
    byte[] r = {(byte) 0xb7, 0x2c, (byte) 0xf7, (byte) 0xd6, 0x5e, 0x0e, (byte) 0x97, (byte) 0xd0,
        (byte) 0x82, 0x10, (byte) 0xc8, (byte) 0xcc, (byte) 0x93, 0x20, 0x68, (byte) 0xa6, 0x00,
        0x3b, 0x34, 0x01, 0x01, 0x3b, 0x67, 0x06, (byte) 0xa9, (byte) 0xaf, 0x33, 0x65, (byte) 0xea,
        (byte) 0xb4, 0x7d, 0x0e};
    byte[] cm = new byte[32];
    boolean check_d = librustzcashCheckDiversifier(d);
    System.out.println("d is " + check_d);

    ivk[31] = (byte) 0x10;
    boolean check_pkd = librustzcashIvkToPkd(new IvkToPkdParams(ivk, d, pk_d));
    System.out.println("pk_d is\n");
    for (int j = 0; j < 32; j++) {
      System.out.printf("%x ", pk_d[j]);
      if ((j + 1) % 16 == 0) {
        System.out.printf("\n");
      }
    }

    boolean res = librustzcashSaplingComputeCm(new SaplingComputeCmParams(d, pk_d, value, r, cm));
    System.out.println("cm is" + res);

    //check range of alpha
    byte[] ask = {(byte) 0xb7, 0x2c, (byte) 0xf7, (byte) 0xd6, 0x5e, 0x0e, (byte) 0x97, (byte) 0xd0,
        (byte) 0x82, 0x10, (byte) 0xc8, (byte) 0xcc, (byte) 0x93, 0x20, 0x68, (byte) 0xa6, 0x00,
        0x3b, 0x34, 0x01, 0x01, 0x3b, 0x67, 0x06, (byte) 0xa9, (byte) 0xaf, 0x33, 0x65, (byte) 0xea,
        (byte) 0xb4, 0x7d, 0x0e};
    byte[] alpha = {(byte) 0xb6, 0x2c, (byte) 0xf7, (byte) 0xd6, 0x5e, 0x0e, (byte) 0x97,
        (byte) 0xd0, (byte) 0x82, 0x10, (byte) 0xc8, (byte) 0xcc, (byte) 0x93, 0x20, 0x68,
        (byte) 0xa6, 0x00, 0x3b, 0x34, 0x01, 0x01, 0x3b, 0x67, 0x06, (byte) 0xa9, (byte) 0xaf, 0x33,
        0x65, (byte) 0xea, (byte) 0xb4, 0x7d, 0x0e};
    byte[] sighash = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7,
        8, 9, 10, 11, 12, 13, 14, 15};
    byte[] sigRes = new byte[64];
    boolean boolSigRes = librustzcashSaplingSpendSig(
        new SaplingSpendSigParams(ask, alpha, sighash, sigRes));
    System.out.println("sig result " + boolSigRes);

    byte[] nsk = {(byte) 0xb6, 0x2c, (byte) 0xf7, (byte) 0xd6, 0x5e, 0x0e, (byte) 0x97, (byte) 0xd0,
        (byte) 0x82, 0x10, (byte) 0xc8, (byte) 0xcc, (byte) 0x93, 0x20, 0x68, (byte) 0xa6, 0x00,
        0x3b, 0x34, 0x01, 0x01, 0x3b, 0x67, 0x06, (byte) 0xa9, (byte) 0xaf, 0x33, 0x65, (byte) 0xea,
        (byte) 0xb4, 0x7d, 0x0e};
    byte[] nk = new byte[32];
    nk = librustzcashNskToNk(nsk);

    for (int j = 0; j < 32; j++) {
      System.out.printf("%x ", nk[j]);
      if ((j + 1) % 16 == 0) {
        System.out.printf("\n");
      }
    }

    Pointer ctx = librustzcashSaplingProvingCtxInit();
    byte[] resbindSig = new byte[64];
    boolean boolBindSig = librustzcashSaplingBindingSig(
        new SaplingBindingSigParams(ctx, value, null, resbindSig));
    System.out.println("binding sig result " + boolBindSig);
  }

  public static void test(byte[] K, byte[] ovk, byte[] cv, byte[] cm, byte[] epk) {
    byte[] block = new byte[128];

    System.arraycopy(ovk, 0, block, 0, 32);
    System.arraycopy(cv, 0, block, 32, 32);
    System.arraycopy(cm, 0, block, 64, 32);
    System.arraycopy(epk, 0, block, 96, 32);

    byte[] personalization = new byte[32];
    byte[] aa = "Zcash_Derive_ock".getBytes();
    System.arraycopy(aa, 0, personalization, 0, aa.length);
    if (Libsodium.cryptoGenerichashBlack2bSaltPersonal(K, 32,
        block, 128,
        null, 0, // No key.
        null,    // No salt.
        personalization
    ) != 0) {
      System.out.println("cryptoGenerichashBlack2bSaltPersonal return pok...");
      //throw new RuntimeException("hash function failure");
    } else {
      System.out.println("cryptoGenerichashBlack2bSaltPersonal return ok....");
      for (int i = 0; i < personalization.length; i++) {
        System.out.print(personalization[i] + " ");
      }
      System.out.println();
    }

    byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];

    if (Libsodium.cryptoAeadChacha20poly1305IetfDecrypt(
        new byte[1024], null,
        null,
        new byte[1024], 1024,
        null,
        0,
        cipher_nonce, K) != 0) {
      System.out.println("cryptoAeadChacha20poly1305IetfDecrypt return true.");
    } else {
      System.out.println("cryptoAeadChacha20poly1305IetfDecrypt return false.");
    }

    return;
  }
}
