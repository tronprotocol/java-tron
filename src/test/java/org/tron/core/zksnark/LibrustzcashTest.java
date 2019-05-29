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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingBindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingCheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingSpendSigParams;
import org.tron.common.zksnark.Libsodium;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;

public class LibrustzcashTest {

  @Test
  public void testLibsodium() throws ZksnarkException {
    byte[] K = new byte[32];
    byte[] ovk = new byte[32];
    byte[] cv = new byte[32];
    byte[] cm = new byte[32];
    byte[] epk = new byte[32];
    test(K, ovk, cv, cm, epk);
  }

  @Test
  public void testZcashParam() throws ZksnarkException {
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
    Assert.assertTrue(check_d);

    ivk[31] = (byte) 0x10;
    boolean check_pkd = librustzcashIvkToPkd(new IvkToPkdParams(ivk, d, pk_d));
    System.out.println("pk_d is\n");
    for (int j = 0; j < 32; j++) {
      System.out.printf("%x ", pk_d[j]);
      if ((j + 1) % 16 == 0) {
        System.out.printf("\n");
      }
    }
    Assert.assertTrue(check_pkd);

    boolean res = librustzcashSaplingComputeCm(new SaplingComputeCmParams(d, pk_d, value, r, cm));
    Assert.assertFalse(res);

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
    Assert.assertFalse(boolSigRes);

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
        new SaplingBindingSigParams(ctx, value, sighash, resbindSig));
    Assert.assertFalse(boolBindSig);
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
    Assert.assertTrue(Libsodium.cryptoGenerichashBlack2bSaltPersonal(K, 32,
        block, 128,
        null, 0, // No key.
        null,    // No salt.
        personalization) == 0);

    byte[] cipher_nonce = new byte[crypto_aead_chacha20poly1305_IETF_NPUBBYTES];

    Assert.assertTrue(Libsodium.cryptoAeadChacha20poly1305IetfDecrypt(
        new byte[1024], null,
        null,
        new byte[1024], 1024,
        null,
        0,
        cipher_nonce, K) != 0);
  }

  private String getParamsFile(String fileName) {
    InputStream in = FullNodeHttpApiService.class.getClassLoader()
        .getResourceAsStream("params" + File.separator + fileName);
    File fileOut = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
    try {
      FileUtils.copyToFile(in, fileOut);
    } catch (IOException e) {
    }
    return fileOut.getAbsolutePath();
  }

  private void librustzcashInitZksnarkParams() {

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    try {
      Librustzcash.librustzcashInitZksnarkParams(
          new InitZksnarkParams(spendPath.getBytes(), spendPath.length(), spendHash,
              outputPath.getBytes(), outputPath.length(), outputHash));
    } catch (ZksnarkException e) {
    }
  }

  public long benchmarkVerifySaplingSpend() throws ZksnarkException {
    String spend = "8c6cf86bbb83bf0d075e5bd9bb4b5cd56141577be69f032880b11e26aa32aa5ef09fd00899e4b469fb11f38e9d09dc0379f0b11c23b5fe541765f76695120a03f0261d32af5d2a2b1e5c9a04200cd87d574dc42349de9790012ce560406a8a876a1e54cfcdc0eb74998abec2a9778330eeb2a0ac0e41d0c9ed5824fbd0dbf7da930ab299966ce333fd7bc1321dada0817aac5444e02c754069e218746bf879d5f2a20a8b028324fb2c73171e63336686aa5ec2e6e9a08eb18b87c14758c572f4531ccf6b55d09f44beb8b47563be4eff7a52598d80959dd9c9fee5ac4783d8370cb7d55d460053d3e067b5f9fe75ff2722623fb1825fcba5e9593d4205b38d1f502ff03035463043bd393a5ee039ce75a5d54f21b395255df6627ef96751566326f7d4a77d828aa21b1827282829fcbc42aad59cdb521e1a3aaa08b99ea8fe7fff0a04da31a52260fc6daeccd79bb877bdd8506614282258e15b3fe74bf71a93f4be3b770119edf99a317b205eea7d5ab800362b97384273888106c77d633600";
    String dataToBeSigned = "2c596ec7f2d580471e0769fcc4a0b96b908394710cac0fd8cba7887bfe83bf2d";

    long startTime = System.currentTimeMillis();
    Pointer ctx = librustzcashSaplingProvingCtxInit();

    SaplingCheckSpendParams saplingCheckSpendParams = SaplingCheckSpendParams.decode(ctx,
        ByteArray.fromHexString(spend),
        ByteArray.fromHexString(dataToBeSigned));

    boolean ok = Librustzcash.librustzcashSaplingCheckSpend(saplingCheckSpendParams);

    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;

    System.out.println("--- time is: " + time + ", result is " + ok);
    return time;
  }

  // @Test
  public void calBenchmarkVerifySaplingSpend() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 100;
    long min_time = 500;
    long max_time = 0;
    double total_time = 0.0;

    for (int i =0; i < count; i++) {
      long time = benchmarkVerifySaplingSpend();
      if (time < min_time) {
        min_time = time;
      }
      if (time > max_time) {
        max_time = time;
      }
      total_time += time;
    }

    System.out.println("---- result ----");
    System.out.println("---- max_time is: " + max_time);
    System.out.println("---- min_time is: " + min_time);
    System.out.println("---- avg_time is: " + total_time/count);

  }

}
