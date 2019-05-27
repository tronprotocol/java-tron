package org.tron.core.zksnark;

import static org.tron.core.zen.note.NotePlaintext.decrypt;

import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jna.Pointer;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.collections.Lists;
import org.tron.api.GrpcAPI;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.zksnark.ZksnarkUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingBindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingCheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingCheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingFinalCheckParams;
import org.tron.common.zksnark.LibrustzcashParam.SaplingSpendSigParams;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.IncrementalMerkleVoucherCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.capsule.ReceiveDescriptionCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.SpendDescriptionInfo;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer.EmptyMerkleRoots;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.merkle.MerklePath;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.NotePlaintext;
import org.tron.core.zen.note.NotePlaintext.NotePlaintextEncryptionResult;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.Contract;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;

public class SendCoinShieldTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static String dbPath = "output_ShieldedTransaction_test";
  private static String dbDirectory = "db_ShieldedTransaction_test";
  private static String indexDirectory = "index_ShieldedTransaction_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
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

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    wallet = context.getBean(Wallet.class);
    //init energy
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(100_000L);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }


  @Test
  public void testNote() throws ZksnarkException {
    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    long value = 100;
    Note note = new Note(address, value);
    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);
    long position = 1000_000;
    byte[] cm = note.cm();
    byte[] nf = note.nullifier(expsk.fullViewingKey(), position);
    if (ByteArray.isEmpty(cm) || ByteArray.isEmpty(nf)) {
      throw new RuntimeException("Spend is invalid");
    }
  }

  @Test
  public void testPathMock() throws ZksnarkException{
    List<List<Boolean>> authenticationPath = Lists.newArrayList();
    Boolean[] authenticationArray = {true, false, true, false, true, false};
    for (int i = 0; i < 6; i++) {
      authenticationPath.add(Lists.newArrayList(authenticationArray));
    }

    Boolean[] indexArray = {true, false, true, false, true, false};
    List<Boolean> index = Lists.newArrayList(Arrays.asList(indexArray));

    MerklePath path = new MerklePath(authenticationPath, index);

    byte[] encode = path.encode();
    System.out.print(ByteArray.toHexString(encode));
  }

  private PedersenHash String2PedersenHash(String str) {
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    byte[] bytes1 = ByteArray.fromHexString(str);
    ZksnarkUtils.sort(bytes1);
    compressCapsule1.setContent(ByteString.copyFrom(bytes1));
    return compressCapsule1.getInstance();
  }

  private PedersenHash ByteArray2PedersenHash(byte[] bytes) {
    PedersenHashCapsule compressCapsule_in = new PedersenHashCapsule();
    compressCapsule_in.setContent(ByteString.copyFrom(bytes));
    return compressCapsule_in.getInstance();
  }

  private IncrementalMerkleVoucherContainer createComplexMerkleVoucherContainer(byte[] cm)
      throws ZksnarkException {

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

    String s1 = "556f3af94225d46b1ef652abc9005dee873b2e245eef07fd5be587e0f21023b0";
    PedersenHash a = String2PedersenHash(s1);

    String s2 = "5814b127a6c6b8f07ed03f0f6e2843ff04c9851ff824a4e5b4dad5b5f3475722";
    PedersenHash b = String2PedersenHash(s2);

    String s3 = "6c030e6d7460f91668cc842ceb78cdb54470469e78cd59cf903d3a6e1aa03e7c";
    PedersenHash c = String2PedersenHash(s3);

    PedersenHash cmHash = ByteArray2PedersenHash(cm);

    tree.append(a);
    tree.append(b);
    tree.append(cmHash);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    voucher.append(c);
    return voucher;
  }

  private IncrementalMerkleVoucherContainer createSimpleMerkleVoucherContainer(byte[] cm)
      throws ZksnarkException {

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(cm));
    PedersenHash a = compressCapsule1.getInstance();
    tree.append(a);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();

    return voucher;

  }

  private String getParamsFile(String fileName) {
    return SendCoinShieldTest.class.getClassLoader()
        .getResource("zcash-params" + File.separator + fileName).getFile();
  }

  private void librustzcashInitZksnarkParams() throws ZksnarkException {

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    Librustzcash.librustzcashInitZksnarkParams(
        new InitZksnarkParams(spendPath.getBytes(), spendPath.length(), spendHash,
            outputPath.getBytes(), outputPath.length(), outputHash));
  }


  @Test
  public void testStringRevert() throws Exception {
    byte[] bytes = ByteArray
        .fromHexString("6c030e6d7460f91668cc842ceb78cdb54470469e78cd59cf903d3a6e1aa03e7c");

    ZksnarkUtils.sort(bytes);
    System.out.println("testStringRevert------" + ByteArray.toHexString(bytes));
  }

  @Test
  public void testGenerateSpendProof() throws Exception {
    librustzcashInitZksnarkParams();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 100);
    note.r = ByteArray
        .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02");
    IncrementalMerkleVoucherContainer voucher = createComplexMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = builder.generateSpendProof(spend, ctx);

    System.out.println(ByteArray.toHexString(sdesc.getRk().toByteArray()));

  }

  @Test
  public void generateOutputProof() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void verifyOutputProof() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    ReceiveDescriptionCapsule capsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    ReceiveDescription receiveDescription = capsule.getInstance();
    ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
    if (!Librustzcash.librustzcashSaplingCheckOutput(
        new SaplingCheckOutputParams(ctx,
            receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(),
            receiveDescription.getEpk().toByteArray(),
            receiveDescription.getZkproof().toByteArray())
    )) {
      Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      throw new RuntimeException("librustzcashSaplingCheckOutput error");
    }

    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }


  private static byte charToByte(char c) {
    return (byte) "0123456789ABCDEF".indexOf(c);
  }

  public static byte[] hexString2Bytes(String hex) {

    if ((hex == null) || (hex.equals(""))) {
      return null;
    } else if (hex.length() % 2 != 0) {
      return null;
    } else {
      hex = hex.toUpperCase();
      int len = hex.length() / 2;
      byte[] b = new byte[len];
      char[] hc = hex.toCharArray();
      for (int i = 0; i < len; i++) {
        int p = 2 * i;
        b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p + 1]));
      }
      return b;
    }

  }

  /*
   * convert byte array to string
   */
  public String byteArrayFormat(byte[] bytes) {

    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%d,", b));
    }
    return StringUtils.strip(sb.toString(), ",");
  }

  @Test
  public void verifyIvkDecryptReceive() throws ZksnarkException {
    //verify c_enc
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder();

    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();

    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);

    ZenTransactionBuilder.ReceiveDescriptionInfo output = builder.getReceives().get(0);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder.generateOutputProof(output, ctx);
    Contract.ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();

    Optional<NotePlaintext> ret1 = NotePlaintext.decrypt(
        receiveDescription.getCEnc().toByteArray(),//ciphertext
        fullViewingKey.inViewingKey().getValue(),
        receiveDescription.getEpk().toByteArray(),//epk
        receiveDescription.getNoteCommitment().toByteArray() //cm
    );

    if (ret1.isPresent()) {
      NotePlaintext noteText = ret1.get();

      byte[] pk_d = new byte[32];
      if (!Librustzcash.librustzcashIvkToPkd(
          new IvkToPkdParams(incomingViewingKey.getValue(), noteText.d.getData(), pk_d))) {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        return;
      }

      Assert.assertArrayEquals(paymentAddress.getPkD(), pk_d);
      Assert.assertEquals(noteText.value, 4000);
      Assert.assertArrayEquals(noteText.memo, new byte[512]);

      GrpcAPI.Note decrypt_note = GrpcAPI.Note.newBuilder()
          .setD(ByteString.copyFrom(noteText.d.getData()))
          .setValue(noteText.value)
          .setRcm(ByteString.copyFrom(noteText.rcm))
          .setPkD(ByteString.copyFrom(pk_d))
          .build();

      System.out.println("decrypt c_enc note completely.");
    }

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void verifyOvkDecryptReceive() throws Exception {
    //decode c_out with ovk.
    librustzcashInitZksnarkParams();

    // construct payment address
    SpendingKey spendingKey2 = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    PaymentAddress paymentAddress2 = spendingKey2.defaultAddress();
    FullViewingKey fullViewingKey = spendingKey2.fullViewingKey();

    // generate output proof
    ZenTransactionBuilder builder2 = new ZenTransactionBuilder();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder2.addOutput(fullViewingKey.getOvk(), paymentAddress2, 10000, new byte[512]);
    ZenTransactionBuilder.ReceiveDescriptionInfo output = builder2.getReceives().get(0);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder2.generateOutputProof(output, ctx);
    Contract.ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();

    byte[] pkd = paymentAddress2.getPkD();
    Note note = new Note(paymentAddress2, 4000);//construct function：this.pkD = address.getPkD();

    byte[] cmu_opt = note.cm();
    Assert.assertNotNull(cmu_opt);

    NotePlaintext pt = new NotePlaintext(note, new byte[512]);
    NotePlaintextEncryptionResult enc = pt.encrypt(pkd).get();

    NoteEncryption encryptor = enc.noteEncryption;

    OutgoingPlaintext out_pt = new OutgoingPlaintext(note.pkD, encryptor.esk);

    // encrypt with ovk
    Encryption.OutCiphertext outCiphertext = out_pt.encrypt(
        fullViewingKey.getOvk(),
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        encryptor);

    // get pk_d, esk from decryption of c_out with ovk
    Optional<OutgoingPlaintext> ret2 = OutgoingPlaintext.decrypt(outCiphertext,
        fullViewingKey.getOvk(),
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        encryptor.epk
    );

    if (ret2.isPresent()) {

      OutgoingPlaintext decrypted_out_ct_unwrapped = ret2.get();

      Assert.assertArrayEquals(decrypted_out_ct_unwrapped.pk_d, out_pt.pk_d);
      Assert.assertArrayEquals(decrypted_out_ct_unwrapped.esk, out_pt.esk);

      System.out.println("decrypt c_out with ovk success");

      //decrypt c_enc with pkd、esk
      Encryption.EncCiphertext ciphertext = new Encryption.EncCiphertext();
      ciphertext.data = enc.encCiphertext;

      Optional<NotePlaintext> foo = NotePlaintext
          .decrypt(ciphertext,
              encryptor.epk,
              decrypted_out_ct_unwrapped.esk,
              decrypted_out_ct_unwrapped.pk_d,
              cmu_opt);

      if (foo.isPresent()) {

        NotePlaintext bar = foo.get();
        //verify result
        Assert.assertEquals(bar.value, 4000);
        Assert.assertArrayEquals(bar.memo, new byte[512]);

        System.out.println("decrypt c_out with pkd,esk success");

      } else {
        Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
        throw new RuntimeException("decrypt c_out with pkd,esk failed");
      }

    } else {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("decrypt c_out with failed");
    }

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void pushShieldedTransactionAndDecryptWithIvk()
      throws ContractValidateException, TooBigTransactionException, TooBigTransactionResultException,
      TaposException, TransactionExpirationException, ReceiptCheckErrException,
      DupTransactionException, VMIllegalException, ValidateSignatureException, BadItemException,
      ContractExeException, AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);

    // add here
    byte[] ivk = fullViewingKey.inViewingKey().getValue();
    Protocol.Transaction t = transactionCap.getInstance();
    for (org.tron.protos.Protocol.Transaction.Contract c : t.getRawData().getContractList()) {

      if (c.getType() != Protocol.Transaction.Contract.ContractType.ShieldedTransferContract) {
        continue;
      }

      Contract.ShieldedTransferContract stContract = c.getParameter()
          .unpack(Contract.ShieldedTransferContract.class);
      org.tron.protos.Contract.ReceiveDescription receiveDescription = stContract
          .getReceiveDescription(0);

      Optional<NotePlaintext> ret1 = NotePlaintext.decrypt(
          receiveDescription.getCEnc().toByteArray(),//ciphertext
          ivk,
          receiveDescription.getEpk().toByteArray(),//epk
          receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        NotePlaintext noteText = ret1.get();

        byte[] pk_d = new byte[32];
        if (!Librustzcash.librustzcashIvkToPkd(
            new IvkToPkdParams(incomingViewingKey.getValue(), noteText.d.getData(), pk_d))) {
          Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
          return;
        }

        Assert.assertArrayEquals(paymentAddress.getPkD(), pk_d);
        Assert.assertEquals(noteText.value, 4000 * 1000000);
        Assert.assertArrayEquals(noteText.memo, new byte[512]);

        System.out.println("verify ok.");
      }
    }
    // end here

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void pushShieldedTransactionAndDecryptWithOvk()
      throws ContractValidateException, TooBigTransactionException, TooBigTransactionResultException,
      TaposException, TransactionExpirationException, ReceiptCheckErrException,
      DupTransactionException, VMIllegalException, ValidateSignatureException, BadItemException,
      ContractExeException, AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);

    // add here
    byte[] ivk = fullViewingKey.inViewingKey().getValue();
    Protocol.Transaction t = transactionCap.getInstance();
    for (org.tron.protos.Protocol.Transaction.Contract c : t.getRawData().getContractList()) {

      if (c.getType() != Protocol.Transaction.Contract.ContractType.ShieldedTransferContract) {
        continue;
      }

      Contract.ShieldedTransferContract stContract = c.getParameter()
          .unpack(Contract.ShieldedTransferContract.class);
      org.tron.protos.Contract.ReceiveDescription receiveDescription = stContract
          .getReceiveDescription(0);

      Encryption.OutCiphertext c_out = new Encryption.OutCiphertext();
      c_out.data = receiveDescription.getCOut().toByteArray();

      Optional<OutgoingPlaintext> notePlaintext = OutgoingPlaintext.decrypt(
          c_out,//ciphertext
          fullViewingKey.getOvk(),
          receiveDescription.getValueCommitment().toByteArray(), //cv
          receiveDescription.getNoteCommitment().toByteArray(), //cmu
          receiveDescription.getEpk().toByteArray() //epk
      );

      if (notePlaintext.isPresent()) {
        OutgoingPlaintext decrypted_out_ct_unwrapped = notePlaintext.get();

        //decode c_enc with pkd、esk
        Encryption.EncCiphertext ciphertext = new Encryption.EncCiphertext();
        ciphertext.data = receiveDescription.getCEnc().toByteArray();

        Optional<NotePlaintext> foo = NotePlaintext
            .decrypt(ciphertext,
                receiveDescription.getEpk().toByteArray(),
                decrypted_out_ct_unwrapped.esk,
                decrypted_out_ct_unwrapped.pk_d,
                receiveDescription.getNoteCommitment().toByteArray());

        if (foo.isPresent()) {
          NotePlaintext bar = foo.get();
          //verify result
          Assert.assertEquals(bar.value, 4000 * 1000000);
          Assert.assertArrayEquals(bar.memo, new byte[512]);
          System.out.println("decrypt c_out with ovk success");
        }

      }
    }
    // end here

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }


  @Test
  public void testEncrypt() {

  }

  private byte[] getHash() {
    return Sha256Hash.of("this is a test".getBytes()).getBytes();
  }

  public byte[] getHash1() {
    return Sha256Hash.of("this is a test11".getBytes()).getBytes();
  }


  @Test
  public void testVerifySpendProof() throws BadItemException, ZksnarkException {
    librustzcashInitZksnarkParams();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    long value = 100;
    Note note = new Note(address, value);

    //    byte[] anchor = new byte[256];
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    //    builder.addSpend(expsk, note, anchor, voucher);
    //    SpendDescriptionInfo spend = builder.getSpends().get(0);
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer proofContext = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(spend, proofContext);
    Librustzcash.librustzcashSaplingProvingCtxFree(proofContext);

    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        new SaplingSpendSigParams(expsk.getAsk(),
            spend.alpha,
            getHash(),
            result));

    Pointer verifyContext = Librustzcash.librustzcashSaplingVerificationCtxInit();
    boolean ok = Librustzcash.librustzcashSaplingCheckSpend(
        new SaplingCheckSpendParams(verifyContext,
            spendDescriptionCapsule.getValueCommitment().toByteArray(),
            spendDescriptionCapsule.getAnchor().toByteArray(),
            spendDescriptionCapsule.getNullifier().toByteArray(),
            spendDescriptionCapsule.getRk().toByteArray(),
            spendDescriptionCapsule.getZkproof().toByteArray(),
            result,
            getHash())
    );
    Librustzcash.librustzcashSaplingVerificationCtxFree(verifyContext);
    Assert.assertEquals(ok, true);
  }

  @Test
  public void saplingBindingSig() throws BadItemException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    builder.addSpend(expsk, note, anchor, voucher);
    builder.generateSpendProof(builder.getSpends().get(0), ctx);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);

    // test create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = Librustzcash.librustzcashSaplingBindingSig(
        new SaplingBindingSigParams(ctx,
            builder.getValueBalance(),
            getHash(),
            bindingSig)
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);
  }

  @Test
  public void pushShieldedTransaction()
      throws ContractValidateException, TooBigTransactionException, TooBigTransactionResultException,
      TaposException, TransactionExpirationException, ReceiptCheckErrException,
      DupTransactionException, VMIllegalException, ValidateSignatureException, BadItemException,
      ContractExeException, AccountResourceInsufficientException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
    builder
        .addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void finalCheck() throws BadItemException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    // generate spend proof
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    builder.addSpend(expsk, note, anchor, voucher);
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(builder.getSpends().get(0), ctx);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);

    //create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = Librustzcash.librustzcashSaplingBindingSig(
        new SaplingBindingSigParams(ctx,
            builder.getValueBalance(),
            getHash(),
            bindingSig)
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);

    // check spend
    ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        new SaplingSpendSigParams(expsk.getAsk(),
            builder.getSpends().get(0).alpha,
            getHash(),
            result));

    SpendDescription spendDescription = spendDescriptionCapsule.getInstance();
    boolean ok;
    ok = Librustzcash.librustzcashSaplingCheckSpend(
        new SaplingCheckSpendParams(ctx,
            spendDescription.getValueCommitment().toByteArray(),
            spendDescription.getAnchor().toByteArray(),
            spendDescription.getNullifier().toByteArray(),
            spendDescription.getRk().toByteArray(),
            spendDescription.getZkproof().toByteArray(),
            result,
            getHash())
    );
    Assert.assertTrue(ok);

    // check output
    ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();
    ok = Librustzcash.librustzcashSaplingCheckOutput(
        new SaplingCheckOutputParams(ctx,
            receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(),
            receiveDescription.getEpk().toByteArray(),
            receiveDescription.getZkproof().toByteArray())
    );
    Assert.assertTrue(ok);

    // final check
    ok = Librustzcash.librustzcashSaplingFinalCheck(
        new SaplingFinalCheckParams(ctx,
            builder.getValueBalance(),
            bindingSig,
            getHash())
    );
    Assert.assertTrue(ok);
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  @Test
  public void testEmptyRoot() {
    byte[] bytes = IncrementalMerkleTreeContainer.emptyRoot().getContent().toByteArray();
    ZksnarkUtils.sort(bytes);
    Assert.assertEquals("3e49b5f954aa9d3545bc6c37744661eea48d7c34e3000d82b7f0010c30f4c2fb",
        ByteArray.toHexString(bytes));
  }

  @Test
  public void testEmptyRoots() throws Exception {
    JSONArray array = readFile("merkle_roots_empty_sapling.json");

    for (int i = 0; i < 32; i++) {
      String string = array.getString(i);
      EmptyMerkleRoots emptyMerkleRootsInstance = EmptyMerkleRoots.emptyMerkleRootsInstance;
      byte[] bytes = emptyMerkleRootsInstance.emptyRoot(i).getContent().toByteArray();
      Assert.assertEquals(string, ByteArray.toHexString(bytes));
    }
  }

  private JSONArray readFile(String fileName) throws Exception {
    String file1 = SendCoinShieldTest.class.getClassLoader()
        .getResource("json" + File.separator + fileName).getFile();
    List<String> readLines = Files.readLines(new File(file1),
        Charsets.UTF_8);

    JSONArray array = JSONArray
        .parseArray(readLines.stream().reduce((s, s2) -> s + s2).get());

    return array;
  }


  private String PedersenHash2String(PedersenHash hash) {
    return ByteArray.toHexString(hash.getContent().toByteArray());
  }

  @Test
  public void testComplexTreePath() throws Exception {
    IncrementalMerkleTreeContainer.DEPTH = 4;

    JSONArray root_tests = readFile("merkle_roots_sapling.json");
    JSONArray path_tests = readFile("merkle_path_sapling.json");
    JSONArray commitment_tests = readFile("merkle_commitments_sapling.json");

    int path_i = 0;

//    MerkleContainer merkleContainer = new MerkleContainer();
//    merkleContainer.getCurrentMerkle();

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeCapsule()
        .toMerkleTreeContainer();

    // The root of the tree at this point is expected to be the root of the
    // empty tree.
    Assert.assertEquals(PedersenHash2String(tree.root()),
        PedersenHash2String(IncrementalMerkleTreeContainer.emptyRoot()));

    try {
      tree.last();
      Assert.fail("The tree doesn't have a 'last' element added since it's blank.");
    } catch (Exception ex) {

    }
    // The tree is empty.
    Assert.assertEquals(0, tree.size());

    // We need to witness at every single point in the tree, so
    // that the consistency of the tree and the merkle paths can
    // be checked.
    List<IncrementalMerkleVoucherCapsule> witnesses = Lists.newArrayList();

    for (int i = 0; i < 16; i++) {
      // Witness here
      witnesses.add(tree.toVoucher().getVoucherCapsule());

      PedersenHashCapsule test_commitment = new PedersenHashCapsule();
      byte[] bytes = ByteArray.fromHexString(commitment_tests.getString(i));
      ZksnarkUtils.sort(bytes);
      test_commitment.setContent(ByteString.copyFrom(bytes));
      // Now append a commitment to the tree
      tree.append(test_commitment.getInstance());

      // Size incremented by one.
      Assert.assertEquals(i + 1, tree.size());

      // Last element added to the tree was `test_commitment`
      Assert.assertEquals(PedersenHash2String(test_commitment.getInstance()),
          PedersenHash2String(tree.last()));

      //todo:
      // Check tree root consistency
      Assert.assertEquals(root_tests.getString(i),
          PedersenHash2String(tree.root()));

      // Check serialization of tree
//      expect_ser_test_vector(ser_tests[i], tree, tree);

      boolean first = true; // The first witness can never form a path
      for (IncrementalMerkleVoucherCapsule wit : witnesses) {
        // Append the same commitment to all the witnesses
        wit.toMerkleVoucherContainer().append(test_commitment.getInstance());

        if (first) {
          try {
            wit.toMerkleVoucherContainer().path();
            Assert.fail("The first witness can never form a path");
          } catch (Exception ex) {

          }

          try {
            wit.toMerkleVoucherContainer().element();
            Assert.fail("The first witness can never form a path");
          } catch (Exception ex) {

          }
        } else {
          MerklePath path = wit.toMerkleVoucherContainer().path();
          Assert.assertEquals(path_tests.getString(path_i++), ByteArray.toHexString(path.encode()));
        }

        Assert.assertEquals(
            PedersenHash2String(wit.toMerkleVoucherContainer().root()),
            PedersenHash2String(tree.root()));

        first = false;
      }
    }
    try {
      tree.append(new PedersenHashCapsule().getInstance());
      Assert.fail("Tree should be full now");
    } catch (Exception ex) {

    }

    for (IncrementalMerkleVoucherCapsule wit : witnesses) {
      try {
        wit.toMerkleVoucherContainer().append(new PedersenHashCapsule().getInstance());
        Assert.fail("Tree should be full now");
      } catch (Exception ex) {

      }
    }
  }


  @Test
  public void testComputeCm() throws Exception {
    byte[] result = new byte[32];
    if (!Librustzcash.librustzcashSaplingComputeCm(new SaplingComputeCmParams(
        (ByteArray.fromHexString("fc6eb90855700861de6639")), (
        ByteArray
            .fromHexString("1abfbf64bc4934aaf7f29b9fea995e5a16e654e63dbe07db0ef035499d216e19")),
        9990000000L, (ByteArray
        .fromHexString("08e3a2ff1101b628147125b786c757b483f1cf7c309f8a647055bfb1ca819c02")),
        result)
    )) {
      System.out.println(" error");
    } else {
      System.out.println(" ok");
    }

  }


  @Test
  public void testNotePlaintext() throws Exception {

    byte[] ciphertext = {
        -60, -107, 77, -106, -26, 98, 119, -99, 99, -116, -36, -47, 97, -18, -33, -34, 54, 53, -33,
        81, -43, -115, 34, 36, -41, -71, -16, -56, 49, 106, 71, 113, -29, -48, 10, -53, -123, -78,
        -119, -119, -52, -107, -26, -55, 81, -51, -80, 108, 127, 118, -124, 90, 25, -7, -47, -40,
        -45,
        102, -82, 56, -49, 108, 28, 41, 110, 113, -126, -113, -82, -107, -16, -86, 40, 7, 110, -78,
        108, 85, -118, -27, -21, -104, 54, 69, 2, 92, 98, 82, -57, 69, -106, 19, -48, 59, 105, -16,
        7,
        -8, -56, -106, -83, 108, 44, -23, -69, 66, -121, 76, 121, -89, -24, 55, 18, 92, -53, 114,
        -117, 117, -58, -18, -38, 50, -98, -16, -92, -45, -88, -72, -91, 109, 109, 115, -99, -2,
        -49,
        -86, 9, 109, 123, -93, 83, 110, -10, -97, -73, 24, -81, 20, 114, 89, 99, -49, 91, -87, 82,
        -11, -49, 16, -98, -124, 97, 90, 40, 40, 21, 58, 86, 43, 52, 127, 38, -52, -10, 13, -31,
        -23,
        -123, -34, -30, -82, -83, 54, -43, 15, -13, 27, 108, 35, -39, -110, 100, 127, 20, -125,
        -103,
        -81, -82, 79, 125, 114, -97, -52, -101, 60, -33, -93, -55, -42, 37, 107, 115, -104, 75, 3,
        -26, -122, 87, -30, 99, -92, -28, -65, 75, 57, -62, 55, 23, 104, -85, -108, 113, 96, 111,
        52,
        -72, 120, -21, 71, 47, 96, -121, -71, 1, 84, 17, 51, -53, 113, 93, -85, -66, -101, 21, -21,
        -66, -115, -71, 43, 102, 39, -73, 30, -100, 22, 23, 77, 73, -41, -116, -15, 5, 5, 119, -68,
        -79, 95, 96, 111, 38, -49, -76, 51, 8, 100, 47, -86, 109, -49, 59, -28, -90, -94, 66, 49,
        -33,
        -106, -72, -123, -125, -80, 15, -121, 70, 44, 66, -81, 86, -90, -57, -8, -104, -11, -51,
        -10,
        41, -32, -24, 78, -12, 44, 35, -92, 65, 66, 91, 125, -25, -52, -122, 4, 23, -70, -128, 84,
        66,
        5, -75, 79, 15, -76, 96, -120, -77, 107, -66, -52, 87, -34, -84, -81, 14, 75, 103, 18, -86,
        17, 0, 49, 66, 10, 47, -94, -96, -110, 49, 93, 7, 52, 95, -127, -64, -53, 43, 97, 117, 88,
        -17, -81, -15, -111, -51, -62, 109, -114, 63, 99, -23, -119, 43, 32, -61, 115, -112, -12,
        -42,
        96, 70, -106, 70, 18, 27, -73, 65, -80, -76, 110, -104, 57, -86, 114, -33, 34, 125, 97, -95,
        -35, 38, 101, -52, 15, 47, 55, -21, 122, 39, 57, -65, -58, -102, -119, 75, -77, -86, -51,
        38,
        -100, -6, -89, -76, 13, -21, 59, 92, -118, 66, -47, 42, -112, -61, 96, 93, -72, 24, 102, 69,
        87, 112, -105, -122, 11, -6, 64, -31, -51, 95, 67, 93, -13, 69, 26, -70, 123, -83, 11, 87,
        -31, -127, -67, 80, 4, -46, -22, 21, -16, 29, -60, 50, -43, 117, 2, -44, -32, 7, 99, -38,
        48,
        -33, 70, -115, 11, -72, -72, -127, 113, 87, -36, 31, -73, 7, -27, 64, -4, -7, -104, -102,
        117,
        125, 81, 31, -13, -20, 50, -47, -68, 103, 93, 97, 9, -93, 22, -111, 20, 102, 106, -122, -48,
        60, -23, 78, 34, 40, 118, -72, -84, -109, 98, -94, -97, -114, -59, -67, -16, 19, 90, 99,
        -72,
        -65, -35, -80, 45, 105, -106, -56, 106, 6, -40, -23, -19, 103, -29, -68, -35, -66, 72, -89,
        4,
        35, -111, 81, 43
    };

    byte[] ivk = {
        -73, 11, 124, -48, -19, 3, -53, -33, -41, -83, -87, 80, 46, -30, 69, -79, 62, 86, -99, 84,
        -91, 113, -99, 45, -86, 15, 95, 20, 81, 71, -110, 4
    };
    byte[] epk = {
        106, -40, -51, -28, 83, 31, 126, 109, 44, -88, 85, -99, 77, -95, -10, 126, -101, -114, -79,
        69, 106, 85, -26, 109, 26, 76, 42, 79, 68, 78, -73, -86
    };
    byte[] cmu = {
        81, 122, -106, 2, -128, 93, 100, -43, 117, -44, -54, 32, -62, -91, -87, -3, 37, -71, 78, 88,
        -59, 73, -5, 85, 41, -86, -47, 107, -100, 3, 112, 9
    };

    Optional<NotePlaintext> ret = decrypt(ciphertext, ivk, epk, cmu);
    NotePlaintext result = ret.get();

    System.out.println("\nplain text rcm:");
    for (byte b : result.rcm) {
      System.out.print(b + ",");
    }
    System.out.println();

    System.out.println("plain text memo size:" + result.memo.length);
    for (byte b : result.memo) {
      System.out.print(b + ",");
    }
    System.out.println();

    System.out.println("plain text value:" + result.value);

    System.out.println("plain text d:");
    for (byte b : result.d.getData()) {
      System.out.print(b + ",");
    }
    System.out.println();
  }

  @Test
  public void testSpendingKey() throws Exception {
    SpendingKey sk = SpendingKey
        .decode("0b862f0e70048551c08518ff49a19db027d62cdeeb2fa974db91c10e6ebcdc16");
    //   SpendingKey sk = SpendingKey.random();
    System.out.println(sk.encode());
    System.out.println(
        "sk.expandedSpendingKey()" + ByteUtil.toHexString(sk.expandedSpendingKey().encode()));
    System.out.println(
        "sk.fullViewKey()" + ByteUtil.toHexString(sk.fullViewingKey().encode()));
    System.out.println(
        "sk.ivk()" + ByteUtil.toHexString(sk.fullViewingKey().inViewingKey().getValue()));
    System.out.println(
        "sk.defaultDiversifier:" + ByteUtil.toHexString(sk.defaultDiversifier().getData()));

    System.out.println(
        "sk.defaultAddress:" + ByteUtil.toHexString(sk.defaultAddress().encode()));

    System.out.println("rcm:" + ByteUtil.toHexString(Note.generateR()));
    // new sk
    System.out.println("---- random ----");

    sk = SpendingKey.random();

    DiversifierT diversifierT = new DiversifierT();
    // byte[] d = {'1', '1', '1', '1', '1', '1', '1', '1', '1', '0', '0'};
    byte[] d;
    while (true) {
      d = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (Librustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }

    diversifierT.setData(d);
    Optional<PaymentAddress> op = sk.fullViewingKey().inViewingKey().address(diversifierT);
    byte[] rcm = Note.generateR();
    System.out.println("rcm is " + ByteUtil.toHexString(rcm));

    byte[] alpha = Note.generateR();
    System.out.println("alpha is " + ByteUtil.toHexString(alpha));

  }

  @Test
  public void testTwoCMWithDiffSkInOneTx() throws Exception {
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    //prepare two cm with different sk
    SpendingKey sk1 = SpendingKey.random();
    ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
    PaymentAddress address1 = sk1.defaultAddress();
    Note note1 = new Note(address1, 110 * 1000000);

    SpendingKey sk2 = SpendingKey.random();
    ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
    PaymentAddress address2 = sk2.defaultAddress();
    Note note2 = new Note(address2, 100 * 1000000);

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
    PedersenHash a = compressCapsule1.getInstance();
    tree.append(a);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager
        .getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
    compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
    PedersenHash a2 = compressCapsule2.getInstance();
    tree.append(a2);
    IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
    byte[] anchor2 = voucher2.root().getContent().toByteArray();
    dbManager
        .getMerkleContainer()
        .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

    //add spendDesc into builder
    builder.addSpend(expsk1, note1, anchor, voucher);
    builder.addSpend(expsk2, note2, anchor2, voucher2);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);

    TransactionCapsule transactionCap = builder.build();

    //execute
    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate();
    TransactionResultCapsule resultCapsule = new TransactionResultCapsule();
    actuator.get(0).execute(resultCapsule);

  }

  private void executeTx(TransactionCapsule transactionCap) throws Exception {
    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate();
    TransactionResultCapsule resultCapsule = new TransactionResultCapsule();
    actuator.get(0).execute(resultCapsule);
  }

  @Test
  public void testValueBalance() throws Exception {

    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    //case 1， a public input, no input cm,  an output cm, no public output
    {
//      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
//      String OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
//      AccountCapsule ownerCapsule =
//          new AccountCapsule(
//              ByteString.copyFromUtf8("owner"),
//              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
//              AccountType.Normal,
//              110_000_000L);
//
//      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
//      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS),100_000_000);
//
//      // generate output proof
//      SpendingKey spendingKey = SpendingKey.random();
//      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
//      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
//      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
//      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);
//
//      TransactionCapsule transactionCap = builder.build();
//
//    // 100_000_000L + 0L !=  200_000_000L + 0L + 10_000_000L
//      try{
//        executeTx(transactionCap);
//        Assert.fail();
//      }catch (ContractValidateException e){
//        if(!e.getMessage().equals("librustzcashSaplingFinalCheck error")){
//          throw e;
//        }
//      }
    }

    //case 2， a public input, no input cm,  an output cm, a public output
    {
//      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
//
//      String OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
//      AccountCapsule ownerCapsule =
//          new AccountCapsule(
//              ByteString.copyFromUtf8("owner"),
//              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
//              AccountType.Normal,
//              110_000_000L);
//      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
//      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS),100_000_000L);
//
//      // generate output proof
//      SpendingKey spendingKey = SpendingKey.random();
//      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
//      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
//      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
//      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);
//
//      String TO_ADDRESS = Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
//      AccountCapsule toCapsule =
//          new AccountCapsule(
//              ByteString.copyFromUtf8("to"),
//              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
//              AccountType.Normal,
//              0L);
//      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
//      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS),10_000_000);
//
//      TransactionCapsule transactionCap = builder.build();
//
////   100_000_000L + 0L !=  200_000_000L + 10_000_000L + 10_000_000L
//      try{
//        executeTx(transactionCap);
//        Assert.fail();
//      }catch (ContractValidateException e){
//        if(!e.getMessage().equals("librustzcashSaplingFinalCheck error")){
//          throw e;
//        }
//      }
    }

    //case 3， no public input, an input cm,  no output cm, a public output
    {
//      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
//
//      //prepare  cm
//      SpendingKey sk1 = SpendingKey.random();
//      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
//      PaymentAddress address1 = sk1.defaultAddress();
//      Note note1 = new Note(address1, 110 * 1000000);
//
//      IncrementalMerkleTreeContainer tree =
//          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
//      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
//      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
//      PedersenHash a = compressCapsule1.getInstance();
//      tree.append(a);
//      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
//      byte[] anchor = voucher.root().getContent().toByteArray();
//      dbManager
//          .getMerkleContainer()
//          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
//
//      //add spendDesc into builder
//      builder.addSpend(expsk1, note1, anchor, voucher);
//
//      String TO_ADDRESS = Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
//      AccountCapsule toCapsule =
//          new AccountCapsule(
//              ByteString.copyFromUtf8("to"),
//              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
//              AccountType.Normal,
//              0L);
//      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
//      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS),10_000_000);
//
//      TransactionCapsule transactionCap = builder.build();
//
//      //   0L + 110_000_000L  !=  200_000_000L + 0L + 10_000_000L
//      try{
//        executeTx(transactionCap);
//        Assert.fail();
//      }catch (ContractValidateException e){
//        if(!e.getMessage().equals("librustzcashSaplingFinalCheck error")){
//          throw e;
//        }
//      }
    }

    //case 4， no public input, an input cm,  an output cm, no public output
    {
//      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
//
//      //prepare  cm
//      SpendingKey sk1 = SpendingKey.random();
//      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
//      PaymentAddress address1 = sk1.defaultAddress();
//      Note note1 = new Note(address1, 110 * 1000000);
//
//      IncrementalMerkleTreeContainer tree =
//          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
//      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
//      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
//      PedersenHash a = compressCapsule1.getInstance();
//      tree.append(a);
//      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
//      byte[] anchor = voucher.root().getContent().toByteArray();
//      dbManager
//          .getMerkleContainer()
//          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
//
//      //add spendDesc into builder
//      builder.addSpend(expsk1, note1, anchor, voucher);
//
//      // generate output proof
//      SpendingKey spendingKey = SpendingKey.random();
//      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
//      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
//      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
//      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);
//
//      TransactionCapsule transactionCap = builder.build();
//
//      //   110_000_000L + 0L!=  200_000_000L + 0L + 10_000_000L
//      try{
//        executeTx(transactionCap);
//        Assert.fail();
//      }catch (ContractValidateException e){
//        if(!e.getMessage().equals("librustzcashSaplingFinalCheck error")){
//          throw e;
//        }
//      }
    }

    //case 5， no public input, an input cm,  an output cm, a public output
    {
//      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
//
//      //prepare  cm
//      SpendingKey sk1 = SpendingKey.random();
//      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
//      PaymentAddress address1 = sk1.defaultAddress();
//      Note note1 = new Note(address1, 110 * 1000000);
//
//      IncrementalMerkleTreeContainer tree =
//          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
//      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
//      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
//      PedersenHash a = compressCapsule1.getInstance();
//      tree.append(a);
//      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
//      byte[] anchor = voucher.root().getContent().toByteArray();
//      dbManager
//          .getMerkleContainer()
//          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
//
//      //add spendDesc into builder
//      builder.addSpend(expsk1, note1, anchor, voucher);
//
//      // generate output proof
//      SpendingKey spendingKey = SpendingKey.random();
//      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
//      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
//      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
//      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);
//
//
//      String TO_ADDRESS = Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
//      AccountCapsule toCapsule =
//          new AccountCapsule(
//              ByteString.copyFromUtf8("to"),
//              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
//              AccountType.Normal,
//              0L);
//      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
//      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS),10_000_000);
//
//      TransactionCapsule transactionCap = builder.build();
//
//      //     0L + 110_000_000L !=  200_000_000L + 10_000_000L + 10_000_000L
//      try{
//        executeTx(transactionCap);
//        Assert.fail();
//      }catch (ContractValidateException e){
//        if(!e.getMessage().equals("librustzcashSaplingFinalCheck error")){
//          throw e;
//        }
//      }
    }

  }

  //  @Test
  public void TestCreateMultipleTxAtTheSameTime() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    List<TransactionCapsule> txList = Lists.newArrayList();

    //case 1， a public input, no input cm,  an output cm, no public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      String OWNER_ADDRESS =
          Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
      AccountCapsule ownerCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("owner"),
              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
              AccountType.Normal,
              220_000_000L);

      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS), 210_000_000L);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);

      TransactionCapsule transactionCap1 = builder.build();
      transactionCap1.setBlockNum(1);
      txList.add(transactionCap1);

      // 210_000_000L + 0L =  200_000_000L + 0L + 10_000_000L
    }

    //case 2， a public input, no input cm,  an output cm, a public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      String OWNER_ADDRESS =
          Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
      AccountCapsule ownerCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("owner"),
              ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
              AccountType.Normal,
              230_000_000L);
      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS), 220_000_000L);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000, new byte[512]);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("to"),
              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
              AccountType.Normal,
              0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000);

      TransactionCapsule transactionCap1 = builder.build();
      transactionCap1.setBlockNum(2);
      txList.add(transactionCap1);

//   220_000_000L + 0L =  200_000_000L + 10_000_000L + 10_000_000L

    }

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      //prepare  cm
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 20 * 1000000);

      IncrementalMerkleTreeContainer tree =
          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager
          .getMerkleContainer()
          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

      //add spendDesc into builder
      builder.addSpend(expsk1, note1, anchor, voucher);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("to"),
              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
              AccountType.Normal,
              0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000);

      TransactionCapsule transactionCap1 = builder.build();
      transactionCap1.setBlockNum(3);
      txList.add(transactionCap1);

//         0L + 20_000_000L  =  0L + 10_000_000L +  10_000_000L
    }

    System.out.println("TxList size:" + txList.size());
    txList.parallelStream().forEach(transactionCapsule -> {
      try {
        executeTx(transactionCapsule);
        System.out.println("Success execute tx,num:" + transactionCapsule.getBlockNum());
      } catch (Exception ex) {
        System.out.println(ex);
      }
    });

    System.out.println("All done");

  }

  //  @Test
  public void TestCtxGeneratesTooMuchProof() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      //prepare two cm with different sk, cm1 is used for fake spendDesc
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 110 * 1000000);

      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000);

      IncrementalMerkleTreeContainer tree =
          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher1 = tree.toVoucher();
      byte[] anchor1 = voucher1.root().getContent().toByteArray();
      dbManager
          .getMerkleContainer()
          .putMerkleTreeIntoStore(anchor1, voucher1.getVoucherCapsule().getTree());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      dbManager
          .getMerkleContainer()
          .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
            Pointer ctx) throws ZksnarkException {

          SpendDescriptionInfo fakeSpend = new SpendDescriptionInfo(expsk1, note1, anchor1,
              voucher1);
          super.generateSpendProof(fakeSpend, ctx);
          return super.generateSpendProof(spend, ctx);
        }
      };

      //add spendDesc into builder
      builder.addSpend(expsk2, note2, anchor2, voucher2);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("to"),
              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
              AccountType.Normal,
              0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000);

      TransactionCapsule transactionCap1 = builder.build();
      try {
        executeTx(transactionCap1);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
        System.out.println("Done");
      }

//         0L + 20_000_000L  =  0L + 10_000_000L +  10_000_000L
    }
  }

  //  @Test
  public void TestGeneratesProofWithDiffCtx() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    //case 3， no public input, an input cm,  no output cm, a public output
    {

      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000);

      IncrementalMerkleTreeContainer tree =
          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      dbManager
          .getMerkleContainer()
          .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
            Pointer ctx) throws ZksnarkException {
          Pointer fakeCtx = Librustzcash.librustzcashSaplingProvingCtxInit();
          return super.generateSpendProof(spend, fakeCtx);
        }
      };

      //add spendDesc into builder
      builder.addSpend(expsk2, note2, anchor2, voucher2);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("to"),
              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
              AccountType.Normal,
              0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000);

      TransactionCapsule transactionCap1 = builder.build();
      try {
        executeTx(transactionCap1);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
        System.out.println("Done");
      }

//         0L + 20_000_000L  =  0L + 10_000_000L +  10_000_000L
    }
  }


  //  @Test
  public void TestGeneratesProofWithWrongAlpha() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000);

      IncrementalMerkleTreeContainer tree =
          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();

      SpendDescriptionInfo spendDescriptionInfo = new SpendDescriptionInfo(expsk2, note2, anchor2,
          voucher2);
      byte[] bytes = ByteArray
          .fromHexString("0eadb4ea6533afa906673b0101343b00a6682093ccc81082d0970e5ed6f72cbd");
      spendDescriptionInfo.alpha = bytes;

      byte[] dataToBeSigned = ByteArray.fromHexString("aaaaaaaaa");
      byte[] result = new byte[64];
      Librustzcash.librustzcashSaplingSpendSig(
          new SaplingSpendSigParams(spendDescriptionInfo.expsk.getAsk(),
              spendDescriptionInfo.alpha,
              dataToBeSigned,
              result));
    }
  }


  //  @Test
  public void TestGeneratesProofWithWrongRcm() throws Exception {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    // generate spend proof
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000);
//    note.r =  ByteArray
//        .fromHexString("0xe7db4ea6533afa906673b0101343b00a6682093ccc81082d0970e5ed6f72cb6");

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    builder.addSpend(expsk, note, anchor, voucher);
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(builder.getSpends().get(0), ctx);

  }

  //  @Test
  public void TestWrongAsk() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000);

      IncrementalMerkleTreeContainer tree =
          new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      dbManager
          .getMerkleContainer()
          .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

      byte[] fakeAsk = ByteArray
          .fromHexString("0xe7db4ea6533afa906673b0101343b00a6682093ccc81082d0970e5ed6f72cb6");

      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        @Override
        public void createSpendAuth(byte[] dataToBeSigned) throws ZksnarkException {
          for (int i = 0; i < this.getSpends().size(); i++) {
            byte[] result = new byte[64];
            Librustzcash.librustzcashSaplingSpendSig(
                new SaplingSpendSigParams(fakeAsk,
                    this.getSpends().get(i).alpha,
                    dataToBeSigned,
                    result));
            this.getContractBuilder().getSpendDescriptionBuilder(i)
                .setSpendAuthoritySignature(ByteString.copyFrom(result));
          }
        }
      };

      //add spendDesc into builder
      builder.addSpend(expsk2, note2, anchor2, voucher2);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule =
          new AccountCapsule(
              ByteString.copyFromUtf8("to"),
              ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
              AccountType.Normal,
              0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000);

      TransactionCapsule transactionCap1 = builder.build();
      try {
        executeTx(transactionCap1);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingCheckSpend error")) {
          throw e;
        }
        System.out.println("Done");
      }

    }
  }

  private SpendDescriptionInfo generateDefaultSpend() throws BadItemException, ZksnarkException {
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 20 * 1000000);

    IncrementalMerkleTreeContainer tree =
        new IncrementalMerkleTreeContainer(new IncrementalMerkleTreeCapsule());

    PedersenHashCapsule compressCapsule = new PedersenHashCapsule();
    compressCapsule.setContent(ByteString.copyFrom(note.cm()));
    PedersenHash hash = compressCapsule.getInstance();
    tree.append(hash);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager
        .getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    return new SpendDescriptionInfo(expsk, note, anchor, voucher);
  }

  private String generateDefaultToAccount() {
    String TO_ADDRESS =
        Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
    AccountCapsule toCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("to"),
            ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)),
            AccountType.Normal,
            0L);
    dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
    return TO_ADDRESS;
  }

  private TransactionCapsule generateDefaultBuilder(ZenTransactionBuilder builder)
      throws BadItemException, ZksnarkException {
    //add spendDesc into builder
    SpendDescriptionInfo spendDescriptionInfo = generateDefaultSpend();
    builder.addSpend(spendDescriptionInfo);

    //add to transparent
    String TO_ADDRESS = generateDefaultToAccount();
    builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000);

    TransactionCapsule transactionCap = builder.build();
    return transactionCap;
  }


  //  @Test
  public void TesDefaultBuilder() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    TransactionCapsule transactionCapsule = generateDefaultBuilder(builder);
    executeTx(transactionCapsule);
  }

  //  @Test
  public void TestWrongSpendRk() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong rk
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
            Pointer ctx) throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);
          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] fakeRk = ByteArray
              .fromHexString("a167824f65f874075cf81968f9f41096c28a2d9c6396601291f76782e6bdc0a4");
          System.out.println(
              "rk:" + ByteArray.toHexString(spendDescriptionCapsule.getRk().toByteArray()));
          spendDescriptionCapsule.setRk(fakeRk);
          return spendDescriptionCapsule;
        }
      };

      TransactionCapsule transactionCapsule = generateDefaultBuilder(builder);

      try {
        executeTx(transactionCapsule);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingCheckSpend error")) {
          throw e;
        }
        System.out.println("Done");
      }
    }
  }

  //  @Test
  public void TestWrongSpendProof() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong proof
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
            Pointer ctx) throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);
          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] fakeProof = ByteArray
              .fromHexString(
                  "0ac001af7f0059cdfec9eed3900b3a4b25ace3cdeb7e962929be9432e51b222be6d7b885d5393c0d373c5b3dbc19210f94e7de831750c5d3a545bbe3732b4d87e4b4350c29519cbebdabd599db9e685f37af2440abc29b3c11cc1dc6712582f74fe06506182e9202b20467017c53fb6d744cd6e08b6428d0e0607688b67876036d2e30617fe020b1fd33ce96cda898e679f44f9715d5681ee0e42f419d7af4d438240fee7b6519e525f452d2ac56b1fb7cd12e9fb0b39caf6f84918b76fa5d4627021d");
          System.out.println("zkproof:" + ByteArray
              .toHexString(spendDescriptionCapsule.getZkproof().toByteArray()));

          spendDescriptionCapsule.setZkproof(fakeProof);
          return spendDescriptionCapsule;
        }
      };

      TransactionCapsule transactionCapsule = generateDefaultBuilder(builder);

      try {
        executeTx(transactionCapsule);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingCheckSpend error")) {
          throw e;
        }
        System.out.println("Done");
      }
    }
  }

  //  @Test
  public void TestWrongNf() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong nf
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
            Pointer ctx) throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);

          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] bytes = ByteArray
              .fromHexString(
                  "7b21b1bc8aba1bb8d5a3638ef8e3c741b84ca7c122053a1072a932c043a0a9500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");//256
          System.out.println(
              "nf:" + ByteArray.toHexString(spendDescriptionCapsule.getNullifier().toByteArray()));
          spendDescriptionCapsule.setNullifier(bytes);
          return spendDescriptionCapsule;
        }
      };

      TransactionCapsule transactionCapsule = generateDefaultBuilder(builder);

      try {
        executeTx(transactionCapsule);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingCheckSpend error")) {
          throw e;
        }
        System.out.println("Done");
        return;
      }
    }
  }

  //  @Test
  public void TestWrongAnchor() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong anchor
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend,
            Pointer ctx) throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);
          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] bytes = ByteArray
              .fromHexString(
                  "bd7e296f492ffc23248b1815277b29af3a8970fff70f8256492bbea79b9a5e3e");//256
          System.out.println(
              "bytes:" + ByteArray.toHexString(spendDescriptionCapsule.getAnchor().toByteArray()));
          spendDescriptionCapsule.setAnchor(bytes);
          return spendDescriptionCapsule;
        }
      };

      TransactionCapsule transactionCapsule = generateDefaultBuilder(builder);

      try {
        executeTx(transactionCapsule);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("Rt is invalid.")) {
          throw e;
        }
        System.out.println("Done");
        return;
      }
    }
  }


}
