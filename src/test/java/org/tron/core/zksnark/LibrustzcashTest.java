package org.tron.core.zksnark;

import static org.tron.common.zksnark.JLibrustzcash.librustzcashCheckDiversifier;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashComputeCm;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashIvkToPkd;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashNskToNk;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingBindingSig;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingProvingCtxInit;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingSpendProof;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingSpendSig;
import static org.tron.common.zksnark.Libsodium.crypto_aead_chacha20poly1305_IETF_NPUBBYTES;

import com.sun.jna.Pointer;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.BindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.ComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendProofParams;
import org.tron.common.zksnark.LibrustzcashParam.MerkleHashParams;
import org.tron.common.zksnark.LibrustzcashParam.OutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
import org.tron.common.zksnark.Libsodium;
import org.tron.core.Wallet;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.SpendDescriptionInfo;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.Note.NotePlaintextEncryptionResult;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.protos.Contract.PedersenHash;

@Slf4j
public class LibrustzcashTest {

  private static String dbPath = "output_Librustzcash_test";
  private static String dbDirectory = "db_Librustzcash_test";
  private static String indexDirectory = "index_Librustzcash_test";
  private static AnnotationConfigApplicationContext context;
  private static Wallet wallet;

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w",
            "--debug"
        },
        "config-test-mainnet.conf"
    );

    context = new TronApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    wallet = context.getBean(Wallet.class);
    Args.getInstance().setAllowShieldedTransaction(true);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

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

    boolean res = librustzcashComputeCm(new ComputeCmParams(d, pk_d, value, r, cm));
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
        new SpendSigParams(ask, alpha, sighash, sigRes));
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

    long ctx = librustzcashSaplingProvingCtxInit();
    byte[] resbindSig = new byte[64];
    boolean boolBindSig = librustzcashSaplingBindingSig(
        new BindingSigParams(ctx, value, sighash, resbindSig));
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
      JLibrustzcash.librustzcashInitZksnarkParams(
          new InitZksnarkParams(spendPath, spendHash, outputPath, outputHash));
    } catch (ZksnarkException e) {
    }
  }

  public long benchmarkVerifySpend() throws ZksnarkException {
    String spend = "8c6cf86bbb83bf0d075e5bd9bb4b5cd56141577be69f032880b11e26aa32aa5ef09fd00899e4b469fb11f38e9d09dc0379f0b11c23b5fe541765f76695120a03f0261d32af5d2a2b1e5c9a04200cd87d574dc42349de9790012ce560406a8a876a1e54cfcdc0eb74998abec2a9778330eeb2a0ac0e41d0c9ed5824fbd0dbf7da930ab299966ce333fd7bc1321dada0817aac5444e02c754069e218746bf879d5f2a20a8b028324fb2c73171e63336686aa5ec2e6e9a08eb18b87c14758c572f4531ccf6b55d09f44beb8b47563be4eff7a52598d80959dd9c9fee5ac4783d8370cb7d55d460053d3e067b5f9fe75ff2722623fb1825fcba5e9593d4205b38d1f502ff03035463043bd393a5ee039ce75a5d54f21b395255df6627ef96751566326f7d4a77d828aa21b1827282829fcbc42aad59cdb521e1a3aaa08b99ea8fe7fff0a04da31a52260fc6daeccd79bb877bdd8506614282258e15b3fe74bf71a93f4be3b770119edf99a317b205eea7d5ab800362b97384273888106c77d633600";
    String dataToBeSigned = "2c596ec7f2d580471e0769fcc4a0b96b908394710cac0fd8cba7887bfe83bf2d";

    long startTime = System.currentTimeMillis();
    long ctx = librustzcashSaplingProvingCtxInit();

    CheckSpendParams checkSpendParams = CheckSpendParams.decode(ctx,
        ByteArray.fromHexString(spend),
        ByteArray.fromHexString(dataToBeSigned));

    boolean ok = JLibrustzcash.librustzcashSaplingCheckSpend(checkSpendParams);

    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;

    System.out.println("--- time is: " + time + ", result is " + ok);
    return time;
  }

  public long benchmarkCreateSpend() throws ZksnarkException {

    long ctx = librustzcashSaplingProvingCtxInit();

      byte[] ak = HexBin.decode("2021c369f4b901cc4f37d80eac2d676aa41beb2a2d835d5120005714bc687657");
      byte[] nsk = HexBin
          .decode("48ea637742229ee87b8ebffd435b27469bee46ecb7732a6e3fb27939d442c006");

      byte[] d = HexBin.decode("5aafbda15b790d38637017");
      long value = 10 * 1000000;
      byte[] rcm = HexBin
          .decode("26328c28c46fb3c3a5e0648e5fc6b312a93f9fa93b5275cf79d4f71a30cd4d00");
      byte[] alpha = HexBin
          .decode("994f6f29a8205747c510406e331d2a49faa1b517e630a4c55d9fe3856a9e030b");
      byte[] anchor = HexBin
          .decode("f2097ce0e430f74a87d5d6c574f483165c781bd6b2423ec4824505890606554f");
      byte[] voucherPath = HexBin.decode(
          "2020b2eed031d4d6a4f02a097f80b54cc1541d4163c6b6f5971f88b6e41d35c538142012935f14b676509b81eb49ef25f39269ed72309238b4c145803544b646dca62d20e1f34b034d4a3cd28557e2907ebf990c918f64ecb50a94f01d6fda5ca5c7ef722028e7b841dcbc47cceb69d7cb8d94245fb7cb2ba3a7a6bc18f13f945f7dbd6e2a20a5122c08ff9c161d9ca6fc462073396c7d7d38e8ee48cdb3bea7e2230134ed6a20d2e1642c9a462229289e5b0e3b7f9008e0301cbb93385ee0e21da2545073cb582016d6252968971a83da8521d65382e61f0176646d771c91528e3276ee45383e4a20fee0e52802cb0c46b1eb4d376c62697f4759f6c8917fa352571202fd778fd712204c6937d78f42685f84b43ad3b7b00f81285662f85c6a68ef11d62ad1a3ee0850200769557bc682b1bf308646fd0b22e648e8b9e98f57e29f5af40f6edb833e2c492008eeab0c13abd6069e6310197bf80f9c1ea6de78fd19cbae24d4a520e6cf3023208d5fa43e5a10d11605ac7430ba1f5d81fb1b68d29a640405767749e841527673206aca8448d8263e547d5ff2950e2ed3839e998d31cbc6ac9fd57bc6002b15921620cd1c8dbf6e3acc7a80439bc4962cf25b9dce7c896f3a5bd70803fc5a0e33cf00206edb16d01907b759977d7650dad7e3ec049af1a3d875380b697c862c9ec5d51c201ea6675f9551eeb9dfaaa9247bc9858270d3d3a4c5afa7177a984d5ed1be245120d6acdedf95f608e09fa53fb43dcd0990475726c5131210c9e5caeab97f0e642f20bd74b25aacb92378a871bf27d225cfc26baca344a1ea35fdd94510f3d157082c201b77dac4d24fb7258c3c528704c59430b630718bec486421837021cf75dab65120ec677114c27206f5debc1c1ed66f95e2b1885da5b7be3d736b1de98579473048204777c8776a3b1e69b73a62fa701fa4f7a6282d9aee2c7a6b82e7937d7081c23c20ba49b659fbd0b7334211ea6a9d9df185c757e70aa81da562fb912b84f49bce722043ff5457f13b926b61df552d4e402ee6dc1463f99a535f9a713439264d5b616b207b99abdc3730991cc9274727d7d82d28cb794edbc7034b4f0053ff7c4b68044420d6c639ac24b46bd19341c91b13fdcab31581ddaf7f1411336a271f3d0aa52813208ac9cf9c391e3fd42891d27238a81a8a5c1d3a72b1bcbea8cf44a58ce738961320912d82b2c2bca231f71efcf61737fbf0a08befa0416215aeef53e8bb6d23390a20e110de65c907b9dea4ae0bd83a4b0a51bea175646a64c12b4c9f931b2cb31b4920d8283386ef2ef07ebdbb4383c12a739a953a4d6e0d6fb1139a4036d693bfbb6c20ffe9fc03f18b176c998806439ff0bb8ad193afdb27b2ccbc88856916dd804e3420817de36ab2d57feb077634bca77819c8e0bd298c04f6fed0e6a83cc1356ca1552001000000000000000000000000000000000000000000000000000000000000000000000000000000");
      byte[] cv = new byte[32];
      byte[] rk = new byte[32];
      byte[] zkproof = new byte[192];

      long start = System.currentTimeMillis();
        boolean ret;
        ret =  librustzcashSaplingSpendProof(new SpendProofParams(ctx, ak,
                nsk,
                d,
                rcm,
                alpha,
                value,
                anchor,
                voucherPath,
                cv,
                rk,
                zkproof));

      long time =  (System.currentTimeMillis() - start);


    System.out.println("--- time is: " + time +",ok," + ret );
    return time;
  }

  @Test
  public void calBenchmarkSpendConcurrent() throws Exception{
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 100;

    CountDownLatch countDownLatch = new CountDownLatch(count);

    int availableProcessors = Runtime.getRuntime().availableProcessors();
    logger.info("availableProcessors:" + availableProcessors);

    ExecutorService generatePool =
        Executors.newFixedThreadPool(
            availableProcessors,
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                return new Thread(r, "generate-transaction");
              }
            });

    long startGenerate = System.currentTimeMillis();
    LongStream.range(0L, count)
        .forEach(
            l -> {
              generatePool.execute(
                  () -> {
                    try {
                      benchmarkCreateSpend();
                    } catch (Exception ex) {
                      ex.printStackTrace();
                      logger.error("", ex);
                    }
                  });
            });

    countDownLatch.await();

    logger.info("generate cost time:" + (System.currentTimeMillis() - startGenerate));
  }

  @Test
  public void calBenchmarkSpend() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 100;
    long min_time = 10000;
    long max_time = 0;
    double total_time = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkCreateSpend();
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
    System.out.println("---- avg_time is: " + total_time / count);

  }

  public long benchmarkVerifyOut() throws ZksnarkException {
    // expect true
    String spend = "add742af18857e5ec2d71d346a7fe2ac97c137339bd5268eea86d32e0ff4f38f76213fa8cfed3347ac4e8572dd88aff395c0c10a59f8b3f49d2bc539ed6c726667e29d4763f914ddd0abf1cdfa84e44de87c233434c7e69b8b5b8f4623c8aa444163425bae5cef842972fed66046c1c6ce65c866ad894d02e6e6dcaae7a962d9f2ef95757a09c486928e61f0f7aed90ad0a542b0d3dc5fe140dfa7626b9315c77e03b055f19cbacd21a866e46f06c00e0c7792b2a590a611439b510a9aaffcf1073bad23e712a9268b36888e3727033eee2ab4d869f54a843f93b36ef489fb177bf74b41a9644e5d2a0a417c6ac1c8869bc9b83273d453f878ed6fd96b82a5939903f7b64ecaf68ea16e255a7fb7cc0b6d8b5608a1c6b0ed3024cc62c2f0f9c5cfc7b431ae6e9d40815557aa1d010523f9e1960de77b2274cb6710d229d475c87ae900183206ba90cb5bbc8ec0df98341b82726c705e0308ca5dc08db4db609993a1046dfb43dfd8c760be506c0bed799bb2205fc29dc2e654dce731034a23b0aaf6da0199248702ee0523c159f41f4cbfff6c35ace4dd9ae834e44e09c76a0cbdda1d3f6a2c75ad71212daf9575ab5f09ca148718e667f29ddf18c8a330a86ace18a86e89454653902aa393c84c6b694f27d0d42e24e7ac9fe34733de5ec15f5066081ce912c62c1a804a2bb4dedcef7cc80274f6bb9e89e2fce91dc50d6a73c8aefb9872f1cf3524a92626a0b8f39bbf7bf7d96ca2f770fc04d7f457021c536a506a187a93b2245471ddbfb254a71bc4a0d72c8d639a31c7b1920087ffca05c24214157e2e7b28184e91989ef0b14f9b34c3dc3cc0ac64226b9e337095870cb0885737992e120346e630a416a9b217679ce5a778fb15779c136bcecca5efe79012013d77d90b4e99dd22c8f35bc77121716e160d05bd30d288ee8886390ee436f85bdc9029df888a3a3326d9d4ddba5cb5318b3274928829d662e96fea1d601f7a306251ed8c6cc4e5a3a7a98c35a3650482a0eee08f3b4c2da9b22947c96138f1505c2f081f8972d429f3871f32bef4aaa51aa6945df8e9c9760531ac6f627d17c1518202818a91ca304fb4037875c666060597976144fcbbc48a776a2c61beb9515fa8f3ae6d3a041d320a38a8ac75cb47bb9c866ee497fc3cd13299970c4b369c1c2ceb4220af082fbecdd8114492a8e4d713b5a73396fd224b36c1185bd5e20d683e6c8db35346c47ae7401988255da7cfffdced5801067d4d296688ee8fe424b4a8a69309ce257eefb9345ebfda3f6de46bb11ec94133e1f72cd7ac54934d6cf17b3440800e70b80ebc7c7bfc6fb0fc2c";

    long startTime = System.currentTimeMillis();
    long ctx = librustzcashSaplingProvingCtxInit();

    CheckOutputParams checkOutputParams = CheckOutputParams.decodeZ(ctx,
        ByteArray.fromHexString(spend));

    boolean ok = JLibrustzcash.librustzcashSaplingCheckOutput(checkOutputParams);

    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;

    System.out.println("--- time is: " + time + ", result is " + ok);
    return time;
  }

  @Test
  public void calBenchmarkVerifyOutput() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 100;
    long min_time = 500;
    long max_time = 0;
    double total_time = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkVerifyOut();
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
    System.out.println("---- avg_time is: " + total_time / count);

  }

  public long benchmarkCreateSaplingSpend() throws BadItemException, ZksnarkException {

    long startTime = System.currentTimeMillis();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey spendingKey = SpendingKey.random();
    ExpandedSpendingKey expsk = spendingKey.expandedSpendingKey();
    PaymentAddress address = spendingKey.defaultAddress();

    long value = 100; // TODO random
    Note note = new Note(address, value);
    byte[] cm = note.cm();

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(cm));
    PedersenHash a = compressCapsule1.getInstance();
    tree.append(a);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();

    byte[] anchor = voucher.root().getContent().toByteArray();

    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);

    long proofContext = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(spend, proofContext);
    JLibrustzcash.librustzcashSaplingProvingCtxFree(proofContext);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;
    System.out.println("time is: " + time + "ms, result is: " + ByteArray
        .toHexString(spendDescriptionCapsule.getData()));

    return time;
  }

  @Test
  public void calBenchmarkCreateSaplingSpend() throws BadItemException, ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 10;
    long min_time = 1000000;
    long max_time = 0;
    double total_time = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkCreateSaplingSpend();
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
    System.out.println("---- avg_time is: " + total_time / count);

  }


  public long benchmarkCreateSaplingOutput() throws BadItemException, ZksnarkException {
    long startTime = System.currentTimeMillis();

    SpendingKey spendingKey = SpendingKey.random();
    PaymentAddress paymentAddress = spendingKey.defaultAddress();

    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    long value = 100; // TODO random
    Note note = new Note(paymentAddress, value);
    note.setMemo(new byte[512]);

    byte[] cm = note.cm();
    if (ByteArray.isEmpty(cm)) {
      throw new ZksnarkException("Output is invalid");
    }

    Optional<NotePlaintextEncryptionResult> res = note.encrypt(note.pkD);
    if (!res.isPresent()) {
      throw new ZksnarkException("Failed to encrypt note");
    }

    NotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.noteEncryption;

    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    boolean result = JLibrustzcash.librustzcashSaplingOutputProof(
        new OutputProofParams(ctx,
            encryptor.esk,
            note.d.data,
            note.pkD,
            note.rcm,
            note.value,
            cv,
            zkProof));

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);

    Assert.assertTrue(result);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;
    System.out.println("time is: " + time + "ms");

    return time;
  }

  @Test
  public void calBenchmarkCreateSaplingOutPut() throws BadItemException, ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 10;
    long min_time = 1000000;
    long max_time = 0;
    double total_time = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkCreateSaplingOutput();
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
    System.out.println("---- avg_time is: " + total_time / count);

  }

  @Test
  public void checkVerifyOutErr() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    // expect fail
    String spend = "0252dff2688fc9eb4645f85a9602dd9c0459663d1e43ade8ae1fdf5d289953b49ab041943b828fea6e0002cf67fd85437e88b14bbe35b57e46e0e2d8b354fd4164fcac491a4f9cacdd5ebcac2dcb4515cd2efc128b1e656ca4a24ab0f05b469099cbc68c2c5839959f770a20ff12184e17b9f5558936b15e7d8bc8812abb668655700fc8fca1c0ee62f5c08690433745992b96a36b21809073d26fcac04ead3f807050c480e7c1103c77992382a3a5946504fc32edef2d530f937a2975b1d43c130e20340a02c1c3e74d4d6d1fce343605c76f7e8b0fe1817430469748205382bc1307a769e5b854d6669fd1a71712909993ada53f65080990ad28de1566e8c4f05b5e49a22bc1ceed376b736b25f4ff3595802d4ac4a5def46ec20d6ba21d40";

    long ctx = librustzcashSaplingProvingCtxInit();

    CheckOutputParams checkOutputParams = CheckOutputParams.decode(ctx,
        ByteArray.fromHexString(spend));

    boolean result = JLibrustzcash.librustzcashSaplingCheckOutput(checkOutputParams);

    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);

    Assert.assertFalse(result);
  }

  @Test
  public void testGenerateNote() throws Exception {

    int total = 100;
    int success = 0;
    int fail = 0;

    for (int i = 0; i < total; i++) {

      SpendingKey spendingKey = SpendingKey
          .decode("044ce61616fc962c9fb3ac3a71ce8bfc6dfd42d414eb8b64c3f7306861a7db36");
      // SpendingKey spendingKey = SpendingKey.random();

      DiversifierT diversifierT = new DiversifierT().random();
      System.out.println("d is: " + ByteArray.toHexString(diversifierT.getData()));
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

      try {
        Optional<PaymentAddress> op = incomingViewingKey.address(diversifierT);
        // PaymentAddress op = spendingKey.defaultAddress();

        Note note = new Note(op.get(), 100);
        note.rcm = ByteArray
            .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02");

        byte[] cm = note.cm();
        if (cm != null) {
          success++;
        } else {
          fail++;
        }
        System.out.println("note is " + note.cm());
      } catch (ZksnarkException e) {
        System.out.println("failed: " + e.getMessage());
        fail++;
        // continue;
      }
    }
    System.out.println("total is: " + total);
    System.out.println("success is: " + success);
    System.out.println("fail is: " + fail);
  }

  @Test
  public void testGenerateNoteWithDefault() throws Exception {

    int total = 1000;
    int success = 0;
    int fail = 0;

    for (int i = 0; i < total; i++) {

      SpendingKey spendingKey = SpendingKey
          .decode("044ce61616fc962c9fb3ac3a71ce8bfc6dfd42d414eb8b64c3f7306861a7db36");
      // SpendingKey spendingKey = SpendingKey.random();

      try {
        PaymentAddress address = spendingKey.defaultAddress();
        System.out.println("d is: " + ByteArray.toHexString(address.getD().getData()));
        System.out.println("pkd is: " + ByteArray.toHexString(address.getPkD()));

        Note note = new Note(address, 100);
        note.rcm = ByteArray
            .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02");

        byte[] cm = note.cm();
        if (cm != null) {
          success++;
        } else {
          fail++;
        }
        System.out.println("note is " + ByteArray.toHexString(note.cm()));
      } catch (ZksnarkException e) {
        System.out.println("failed: " + e.getMessage());
        fail++;
        // continue;
      }
    }
    System.out.println("total is: " + total);
    System.out.println("success is: " + success);
    System.out.println("fail is: " + fail);
  }

  @Test
  public void testGenerateNoteWithConstant() throws Exception {

    SpendingKey spendingKey = SpendingKey
        .decode("044ce61616fc962c9fb3ac3a71ce8bfc6dfd42d414eb8b64c3f7306861a7db36");

    DiversifierT diversifierT = new DiversifierT();
    // a2e62b198564fce9dd2c5c ok
    // 3072b1623134181197d82f error
    diversifierT.setData(ByteArray.fromHexString("a2e62b198564fce9dd2c5c"));
    System.out.println("d is: " + ByteArray.toHexString(diversifierT.getData()));
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    try {
      Optional<PaymentAddress> op = incomingViewingKey.address(diversifierT);
      // PaymentAddress op = spendingKey.defaultAddress();

      Note note = new Note(op.get(), 100);
      note.rcm = ByteArray
          .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02");

      byte[] cm = note.cm();
      System.out.println("note is " + note.cm());
    } catch (ZksnarkException e) {
      System.out.println("failed: " + e.getMessage());
    }

  }


  @Test
  public void testPedersenHash() throws Exception {
    byte[] a = ByteArray
        .fromHexString("05655316a07e6ec8c9769af54ef98b30667bfb6302b32987d552227dae86a087");
    byte[] b = ByteArray
        .fromHexString("06041357de59ba64959d1b60f93de24dfe5ea1e26ed9e8a73d35b225a1845ba7");

    byte[] res = new byte[32];
    JLibrustzcash.librustzcashMerkleHash(new MerkleHashParams(25, a, b, res));

    Assert.assertEquals("61a50a5540b4944da27cbd9b3d6ec39234ba229d2c461f4d719bc136573bf45b",
        ByteArray.toHexString(res));
  }
}
