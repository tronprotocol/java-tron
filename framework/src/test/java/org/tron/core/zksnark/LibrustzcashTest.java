package org.tron.core.zksnark;

import static org.tron.common.zksnark.JLibrustzcash.librustzcashCheckDiversifier;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashComputeCm;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashIvkToPkd;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashNskToNk;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingBindingSig;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingProvingCtxInit;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingSpendProof;
import static org.tron.common.zksnark.JLibrustzcash.librustzcashSaplingSpendSig;
import static org.tron.common.zksnark.JLibsodium.CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.JLibsodium;
import org.tron.common.zksnark.JLibsodiumParam.Black2bSaltPersonalParams;
import org.tron.common.zksnark.JLibsodiumParam.Chacha20poly1305IetfDecryptParams;
import org.tron.common.zksnark.LibrustzcashParam.BindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.ComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.MerkleHashParams;
import org.tron.common.zksnark.LibrustzcashParam.OutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
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
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.Note.NotePlaintextEncryptionResult;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.protos.contract.ShieldContract.PedersenHash;

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
    Args.setFullNodeAllowShieldedTransaction(true);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }

  public static void test(byte[] K, byte[] ovk, byte[] cv, byte[] cm, byte[] epk)
      throws ZksnarkException {
    byte[] block = new byte[128];

    System.arraycopy(ovk, 0, block, 0, 32);
    System.arraycopy(cv, 0, block, 32, 32);
    System.arraycopy(cm, 0, block, 64, 32);
    System.arraycopy(epk, 0, block, 96, 32);

    byte[] personalization = new byte[16];
    byte[] aa = "Zcash_Derive_ock".getBytes();
    System.arraycopy(aa, 0, personalization, 0, aa.length);
    Assert.assertTrue(
        JLibsodium.cryptoGenerichashBlack2bSaltPersonal(
            new Black2bSaltPersonalParams(K, 32, block, 128, null, 0, // No key.
                null,    // No salt.
                personalization)) == 0);

    byte[] cipher_nonce = new byte[CRYPTO_AEAD_CHACHA20POLY1305_IETF_NPUBBYTES];
    Assert.assertTrue(JLibsodium
        .cryptoAeadChacha20poly1305IetfDecrypt(new Chacha20poly1305IetfDecryptParams(
            new byte[1024], null, null, new byte[1024], 1024,
            null, 0, cipher_nonce, K)) != 0);
  }

  public static void librustzcashInitZksnarkParams() {

    FullNodeHttpApiService.librustzcashInitZksnarkParams();
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
    //byte[] pkD = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    byte[] ivk = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15};
    byte[] pkD = new byte[32];
    long value = 1;
    byte[] r = {(byte) 0xb7, 0x2c, (byte) 0xf7, (byte) 0xd6, 0x5e, 0x0e, (byte) 0x97, (byte) 0xd0,
        (byte) 0x82, 0x10, (byte) 0xc8, (byte) 0xcc, (byte) 0x93, 0x20, 0x68, (byte) 0xa6, 0x00,
        0x3b, 0x34, 0x01, 0x01, 0x3b, 0x67, 0x06, (byte) 0xa9, (byte) 0xaf, 0x33, 0x65, (byte) 0xea,
        (byte) 0xb4, 0x7d, 0x0e};
    byte[] cm = new byte[32];
    boolean check_d = librustzcashCheckDiversifier(d);
    Assert.assertTrue(check_d);

    //Most significant five bits of ivk must be 0.
    ivk[31] = (byte) 0x07;
    boolean check_pkd = librustzcashIvkToPkd(new IvkToPkdParams(ivk, d, pkD));
    System.out.println("pkD is\n");
    for (int j = 0; j < 32; j++) {
      System.out.printf("%x ", pkD[j]);
      if ((j + 1) % 16 == 0) {
        System.out.printf("\n");
      }
    }
    Assert.assertTrue(check_pkd);

    boolean res = librustzcashComputeCm(new ComputeCmParams(d, pkD, value, r, cm));
    Assert.assertFalse(res);

    //check range of alpha
    byte[] ask = {(byte) 0xb7, 0x2c, (byte) 0xf7, (byte) 0xd6, 0x5e, 0x0e, (byte) 0x97, (byte) 0xd0,
        (byte) 0x82, 0x10, (byte) 0xc8, (byte) 0xcc, (byte) 0x93, 0x20, 0x68, (byte) 0xa6, 0x00,
        0x3b, 0x34, 0x01, 0x01, 0x3b, 0x67, 0x06, (byte) 0xa9, (byte) 0xaf, 0x33, 0x65,
        (byte) 0xea, (byte) 0xb4, 0x7d, 0x0e};

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
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  public long benchmarkCreateSpend() throws ZksnarkException {

    long ctx = librustzcashSaplingProvingCtxInit();

    byte[] ak = ByteUtil.hexToBytes(
        "2021c369f4b901cc4f37d80eac2d676aa41beb2a2d835d5120005714bc687657");
    byte[] nsk = ByteUtil.hexToBytes(
        "48ea637742229ee87b8ebffd435b27469bee46ecb7732a6e3fb27939d442c006");

    byte[] d = ByteUtil.hexToBytes("5aafbda15b790d38637017");
    long value = 10 * 1000000;
    byte[] rcm = ByteUtil.hexToBytes(
        "26328c28c46fb3c3a5e0648e5fc6b312a93f9fa93b5275cf79d4f71a30cd4d00");
    byte[] alpha = ByteUtil.hexToBytes(
        "994f6f29a8205747c510406e331d2a49faa1b517e630a4c55d9fe3856a9e030b");
    byte[] anchor = ByteUtil.hexToBytes(
        "f2097ce0e430f74a87d5d6c574f483165c781bd6b2423ec4824505890606554f");
    byte[] voucherPath = ByteUtil.hexToBytes(
        "2020b2eed031d4d6a4f02a097f80b54cc1541d4163c6b6f5971f88b6e41d35c538142012935f14b676509b8"
            + "1eb49ef25f39269ed72309238b4c145803544b646dca62d20e1f34b034d4a3cd28557e2907ebf990c918"
            + "f64ecb50a94f01d6fda5ca5c7ef722028e7b841dcbc47cceb69d7cb8d94245fb7cb2ba3a7a6bc18f13f9"
            + "45f7dbd6e2a20a5122c08ff9c161d9ca6fc462073396c7d7d38e8ee48cdb3bea7e2230134ed6a20d2e16"
            + "42c9a462229289e5b0e3b7f9008e0301cbb93385ee0e21da2545073cb582016d6252968971a83da8521d"
            + "65382e61f0176646d771c91528e3276ee45383e4a20fee0e52802cb0c46b1eb4d376c62697f4759f6c89"
            + "17fa352571202fd778fd712204c6937d78f42685f84b43ad3b7b00f81285662f85c6a68ef11d62ad1a3e"
            + "e0850200769557bc682b1bf308646fd0b22e648e8b9e98f57e29f5af40f6edb833e2c492008eeab0c13a"
            + "bd6069e6310197bf80f9c1ea6de78fd19cbae24d4a520e6cf3023208d5fa43e5a10d11605ac7430ba1f5"
            + "d81fb1b68d29a640405767749e841527673206aca8448d8263e547d5ff2950e2ed3839e998d31cbc6ac9"
            + "fd57bc6002b15921620cd1c8dbf6e3acc7a80439bc4962cf25b9dce7c896f3a5bd70803fc5a0e33cf002"
            + "06edb16d01907b759977d7650dad7e3ec049af1a3d875380b697c862c9ec5d51c201ea6675f9551eeb9d"
            + "faaa9247bc9858270d3d3a4c5afa7177a984d5ed1be245120d6acdedf95f608e09fa53fb43dcd0990475"
            + "726c5131210c9e5caeab97f0e642f20bd74b25aacb92378a871bf27d225cfc26baca344a1ea35fdd9451"
            + "0f3d157082c201b77dac4d24fb7258c3c528704c59430b630718bec486421837021cf75dab65120ec677"
            + "114c27206f5debc1c1ed66f95e2b1885da5b7be3d736b1de98579473048204777c8776a3b1e69b73a62f"
            + "a701fa4f7a6282d9aee2c7a6b82e7937d7081c23c20ba49b659fbd0b7334211ea6a9d9df185c757e70aa"
            + "81da562fb912b84f49bce722043ff5457f13b926b61df552d4e402ee6dc1463f99a535f9a713439264d5"
            + "b616b207b99abdc3730991cc9274727d7d82d28cb794edbc7034b4f0053ff7c4b68044420d6c639ac24b"
            + "46bd19341c91b13fdcab31581ddaf7f1411336a271f3d0aa52813208ac9cf9c391e3fd42891d27238a81"
            + "a8a5c1d3a72b1bcbea8cf44a58ce738961320912d82b2c2bca231f71efcf61737fbf0a08befa0416215a"
            + "eef53e8bb6d23390a20e110de65c907b9dea4ae0bd83a4b0a51bea175646a64c12b4c9f931b2cb31b492"
            + "0d8283386ef2ef07ebdbb4383c12a739a953a4d6e0d6fb1139a4036d693bfbb6c20ffe9fc03f18b176c9"
            + "98806439ff0bb8ad193afdb27b2ccbc88856916dd804e3420817de36ab2d57feb077634bca77819c8e0b"
            + "d298c04f6fed0e6a83cc1356ca1552001000000000000000000000000000000000000000000000000000"
            + "000000000000000000000000000");
    byte[] cv = new byte[32];
    byte[] rk = new byte[32];
    byte[] zkproof = new byte[192];

    long start = System.currentTimeMillis();
    boolean ret;
    ret = librustzcashSaplingSpendProof(new SpendProofParams(ctx, ak,
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

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);

    long time = (System.currentTimeMillis() - start);

    System.out.println("--- time is: " + time + ", result is " + ret);
    return time;
  }

  // @Test
  public void calBenchmarkSpendConcurrent() throws Exception {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 2;

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
    LongStream.range(0L, count).forEach(l -> {
      generatePool.execute(() -> {
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

    int count = 2;
    long minTime = 10000;
    long maxTime = 0;
    double totalTime = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkCreateSpend();
      if (time < minTime) {
        minTime = time;
      }
      if (time > maxTime) {
        maxTime = time;
      }
      totalTime += time;
    }

    System.out.println("---- result ----");
    System.out.println("---- maxTime is: " + maxTime);
    System.out.println("---- minTime is: " + minTime);
    System.out.println("---- avgTime is: " + totalTime / count);

  }

  public long benchmarkCreateSaplingSpend() throws BadItemException, ZksnarkException {

    long startTime = System.currentTimeMillis();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey spendingKey = SpendingKey.random();
    ExpandedSpendingKey expsk = spendingKey.expandedSpendingKey();
    PaymentAddress address = spendingKey.defaultAddress();

    long value = randomInt(100, 100000);
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

    int count = 2;
    long minTime = 1000000;
    long maxTime = 0;
    double totalTime = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkCreateSaplingSpend();
      if (time < minTime) {
        minTime = time;
      }
      if (time > maxTime) {
        maxTime = time;
      }
      totalTime += time;
    }

    System.out.println("---- result ----");
    System.out.println("---- maxTime is: " + maxTime);
    System.out.println("---- minTime is: " + minTime);
    System.out.println("---- avgTime is: " + totalTime / count);

  }


  public long benchmarkCreateSaplingOutput() throws BadItemException, ZksnarkException {
    long startTime = System.currentTimeMillis();

    SpendingKey spendingKey = SpendingKey.random();
    PaymentAddress paymentAddress = spendingKey.defaultAddress();

    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    long value = randomInt(100, 100000);
    Note note = new Note(paymentAddress, value);
    note.setMemo(new byte[512]);

    byte[] cm = note.cm();
    if (ByteArray.isEmpty(cm)) {
      throw new ZksnarkException("Output is invalid");
    }

    Optional<NotePlaintextEncryptionResult> res = note.encrypt(note.getPkD());
    if (!res.isPresent()) {
      throw new ZksnarkException("Failed to encrypt note");
    }

    NotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.getNoteEncryption();

    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    boolean result = JLibrustzcash.librustzcashSaplingOutputProof(
        new OutputProofParams(ctx,
            encryptor.getEsk(),
            note.getD().getData(),
            note.getPkD(),
            note.getRcm(),
            note.getValue(),
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

    int count = 2;
    long minTime = 1000000;
    long maxTime = 0;
    double totalTime = 0.0;

    for (int i = 0; i < count; i++) {
      long time = benchmarkCreateSaplingOutput();
      if (time < minTime) {
        minTime = time;
      }
      if (time > maxTime) {
        maxTime = time;
      }
      totalTime += time;
    }

    System.out.println("---- result ----");
    System.out.println("---- maxTime is: " + maxTime);
    System.out.println("---- minTime is: " + minTime);
    System.out.println("---- avgTime is: " + totalTime / count);

  }

  @Test
  public void checkVerifyOutErr() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    // expect fail
    String spend = "0252dff2688fc9eb4645f85a9602dd9c0459663d1e43ade8ae1fdf5d289953b49ab041943b828fe"
        + "a6e0002cf67fd85437e88b14bbe35b57e46e0e2d8b354fd4164fcac491a4f9cacdd5ebcac2dcb4515cd2efc1"
        + "28b1e656ca4a24ab0f05b469099cbc68c2c5839959f770a20ff12184e17b9f5558936b15e7d8bc8812abb668"
        + "655700fc8fca1c0ee62f5c08690433745992b96a36b21809073d26fcac04ead3f807050c480e7c1103c77992"
        + "382a3a5946504fc32edef2d530f937a2975b1d43c130e20340a02c1c3e74d4d6d1fce343605c76f7e8b0fe18"
        + "17430469748205382bc1307a769e5b854d6669fd1a71712909993ada53f65080990ad28de1566e8c4f05b5e4"
        + "9a22bc1ceed376b736b25f4ff3595802d4ac4a5def46ec20d6ba21d40";

    long ctx = librustzcashSaplingProvingCtxInit();

    CheckOutputParams checkOutputParams = CheckOutputParams.decode(ctx,
        ByteArray.fromHexString(spend));

    boolean result = JLibrustzcash.librustzcashSaplingCheckOutput(checkOutputParams);

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);

    Assert.assertFalse(result);
  }

  @Test
  public void testGenerateNote() throws Exception {

    int total = 10;
    int success = 0;
    int fail = 0;

    for (int i = 0; i < total; i++) {

      SpendingKey spendingKey = SpendingKey
          .decode("044ce61616fc962c9fb3ac3a71ce8bfc6dfd42d414eb8b64c3f7306861a7db36");
      // SpendingKey spendingKey = SpendingKey.random();

      DiversifierT diversifierT = DiversifierT.random();
      System.out.println("d is: " + ByteArray.toHexString(diversifierT.getData()));
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

      try {
        Optional<PaymentAddress> op = incomingViewingKey.address(diversifierT);
        // PaymentAddress op = spendingKey.defaultAddress();

        Note note = new Note(op.get(), 100);
        note.setRcm(ByteArray
            .fromHexString(
                "bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02"));

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

    Assert.assertEquals(0, fail);
  }

  @Test
  public void testGenerateNoteWithDefault() throws Exception {

    int total = 10;
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

        Note note = new Note(address, randomInt(100, 100000));
        note.setRcm(ByteArray.fromHexString(
            "bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02"));

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

    Assert.assertEquals(0, fail);
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

      Note note = new Note(op.get(), randomInt(100, 100000));
      note.setRcm(ByteArray
          .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02"));

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
