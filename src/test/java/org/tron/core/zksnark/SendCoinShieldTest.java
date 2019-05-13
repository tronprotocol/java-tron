package org.tron.core.zksnark;

import static org.tron.core.zen.zip32.ExtendedSpendingKey.ZIP32_HARDENED_KEY_LIMIT;

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
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.Librustzcash;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.AccountResourceInsufficientException;
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
import org.tron.core.zen.KeyStore;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.SpendDescriptionInfo;
import org.tron.core.zen.ZenTransactionBuilderFactory;
import org.tron.core.zen.ZenWallet;
import org.tron.core.zen.ZkChainParams;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer.EmptyMerkleRoots;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.capsule.IncrementalMerkleVoucherCapsule;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.merkle.MerklePath;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.zen.note.BaseNote.Note;
import org.tron.core.zen.note.BaseNotePlaintext;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.SaplingNoteEncryption;
import org.tron.core.zen.note.SaplingOutgoingPlaintext;
import org.tron.core.zen.transaction.ReceiveDescriptionCapsule;
import org.tron.core.zen.transaction.Recipient;
import org.tron.core.zen.transaction.SpendDescriptionCapsule;
import org.tron.core.zen.utils.KeyIo;
import org.tron.core.zen.zip32.ExtendedSpendingKey;
import org.tron.core.zen.zip32.HDSeed;
import org.tron.core.zen.zip32.HdChain;
import org.tron.protos.Contract;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ReceiveDescription;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol;

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

  // @Test
  public void testShieldCoinConstructor() {
    Wallet wallet = new Wallet();

    String fromAddr = wallet.getNewZenAddress();

    List<Recipient> outputs = Lists.newArrayList();
    Recipient recipient = new Recipient();
    recipient.address = wallet.getNewZenAddress();
    recipient.value = 1000_000L;
    recipient.memo = "demo";
    outputs.add(recipient);

    ZenTransactionBuilderFactory constructor = new ZenTransactionBuilderFactory();
    constructor.setFromAddress(fromAddr);
    constructor.setZOutputs(outputs);
    TransactionCapsule result = constructor.build();
  }

  //  @Test
  public void testSpendingKey() {
    SpendingKey spendingKey = SpendingKey.random();
    ExpandedSpendingKey expsk = spendingKey.expandedSpendingKey();

    Assert.assertNotNull(expsk);
    Assert.assertNotNull(expsk.fullViewingKey());
    Assert.assertNotNull(expsk.fullViewingKey().getAk());
    Assert.assertNotNull(expsk.getNsk());
  }

  private ExtendedSpendingKey createXskDefault() {
    String seedString = "ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee";
    return createXsk(seedString);
  }

  private ExtendedSpendingKey createXsk(String seedString) {
    HDSeed seed = new HDSeed(ByteArray.fromHexString(seedString));
    ExtendedSpendingKey master = ExtendedSpendingKey.Master(seed);

    int bip44CoinType = ZkChainParams.BIP44CoinType;
    ExtendedSpendingKey master32h = master.Derive(32 | ZIP32_HARDENED_KEY_LIMIT);
    ExtendedSpendingKey master32hCth = master32h.Derive(bip44CoinType | ZIP32_HARDENED_KEY_LIMIT);

    ExtendedSpendingKey xsk =
        master32hCth.Derive(HdChain.saplingAccountCounter | ZIP32_HARDENED_KEY_LIMIT);
    return xsk;
  }

  @Test
  public void testExpandedSpendingKey() {

    ExtendedSpendingKey xsk = createXskDefault();

    ExpandedSpendingKey expsk = xsk.getExpsk();
    Assert.assertNotNull(expsk);
    Assert.assertNotNull(expsk.fullViewingKey());
    Assert.assertNotNull(expsk.fullViewingKey().getAk());
    Assert.assertNotNull(expsk.fullViewingKey().inViewingKey());
    Assert.assertNotNull(expsk.getNsk());

    PaymentAddress addr = xsk.DefaultAddress();
    String paymentAddress = KeyIo.EncodePaymentAddress(addr);

    System.out.println(paymentAddress);
  }

  // @Test
  public void testShieldWallet() {
    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    ExtendedSpendingKey sk = ExtendedSpendingKey.decode(new byte[169]);
    FullViewingKey fvk = FullViewingKey.decode(new byte[96]);
    IncomingViewingKey ivk = new IncomingViewingKey(new byte[32]);

    KeyStore.addSpendingKey(fvk, sk);
    KeyStore.addFullViewingKey(ivk, fvk);
    KeyStore.addIncomingViewingKey(address, ivk);

    System.out.print(ZenWallet.getSpendingKeyForPaymentAddress(address).isPresent());
  }

  @Test
  public void testNote() {
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
  public void testPathMock() {
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

  private IncrementalMerkleVoucherContainer createComplexMerkleVoucherContainer(byte[] cm) {

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

  private IncrementalMerkleVoucherContainer createSimpleMerkleVoucherContainer(byte[] cm) {

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

  private void librustzcashInitZksnarkParams() {

    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    Librustzcash.librustzcashInitZksnarkParams(spendPath.getBytes(), spendPath.length(), spendHash,
        outputPath.getBytes(), outputPath.length(), outputHash);
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
  public void generateOutputProof() {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void verifyOutputProof() {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    ReceiveDescriptionCapsule capsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    ReceiveDescription receiveDescription = capsule.getInstance();
    ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
    if (!Librustzcash.librustzcashSaplingCheckOutput(
        ctx,
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        receiveDescription.getEpk().toByteArray(),
        receiveDescription.getZkproof().getValues().toByteArray()
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
  public void verifyIvkDecryptReceive() {
    //verify c_enc
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder();

    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();

    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    builder.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);

    ZenTransactionBuilder.ReceiveDescriptionInfo output = builder.getReceives().get(0);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder.generateOutputProof(output, ctx);
    Contract.ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();

    Optional<BaseNotePlaintext.NotePlaintext> ret1 = BaseNotePlaintext.NotePlaintext.decrypt(
        receiveDescription.getCEnc().toByteArray(),//ciphertext
        fullViewingKey.inViewingKey().getValue(),
        receiveDescription.getEpk().toByteArray(),//epk
        receiveDescription.getNoteCommitment().toByteArray() //cm
    );

    if (ret1.isPresent()) {
      BaseNotePlaintext.NotePlaintext noteText = ret1.get();

      byte[] pk_d = new byte[32];
      if (!Librustzcash
          .librustzcashIvkToPkd(incomingViewingKey.getValue(), noteText.d.getData(), pk_d)) {
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
    builder2.addSaplingOutput(fullViewingKey.getOvk(), paymentAddress2, 10000, new byte[512]);
    ZenTransactionBuilder.ReceiveDescriptionInfo output = builder2.getReceives().get(0);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder2.generateOutputProof(output, ctx);
    Contract.ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();

    byte[] pkd = paymentAddress2.getPkD();
    Note note = new Note(paymentAddress2, 4000);//construct function：this.pkD = address.getPkD();

    byte[] cmu_opt = note.cm();
    Assert.assertNotNull(cmu_opt);

    BaseNotePlaintext.NotePlaintext pt = new BaseNotePlaintext.NotePlaintext(note, new byte[512]);
    BaseNotePlaintext.SaplingNotePlaintextEncryptionResult enc = pt.encrypt(pkd).get();

    SaplingNoteEncryption encryptor = enc.noteEncryption;

    SaplingOutgoingPlaintext out_pt = new SaplingOutgoingPlaintext(note.pkD, encryptor.esk);

    // encrypt with ovk
    NoteEncryption.OutCiphertext outCiphertext = out_pt.encrypt(
        fullViewingKey.getOvk(),
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        encryptor);

    // get pk_d, esk from decryption of c_out with ovk
    Optional<SaplingOutgoingPlaintext> ret2 = SaplingOutgoingPlaintext.decrypt(outCiphertext,
        fullViewingKey.getOvk(),
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        encryptor.epk
    );

    if (ret2.isPresent()) {

      SaplingOutgoingPlaintext decrypted_out_ct_unwrapped = ret2.get();

      Assert.assertArrayEquals(decrypted_out_ct_unwrapped.pk_d, out_pt.pk_d);
      Assert.assertArrayEquals(decrypted_out_ct_unwrapped.esk, out_pt.esk);

      System.out.println("decrypt c_out with ovk success");

      //decrypt c_enc with pkd、esk
      NoteEncryption.EncCiphertext ciphertext = new NoteEncryption.EncCiphertext();
      ciphertext.data = enc.encCiphertext;

      Optional<BaseNotePlaintext.NotePlaintext> foo = BaseNotePlaintext.NotePlaintext
          .decrypt(ciphertext,
              encryptor.epk,
              decrypted_out_ct_unwrapped.esk,
              decrypted_out_ct_unwrapped.pk_d,
              cmu_opt);

      if (foo.isPresent()) {

        BaseNotePlaintext.NotePlaintext bar = foo.get();
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
      DupTransactionException, VMIllegalException, ValidateSignatureException,
      ContractExeException, AccountResourceInsufficientException, InvalidProtocolBufferException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();

    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    builder.addSaplingSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);

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

      Optional<BaseNotePlaintext.NotePlaintext> ret1 = BaseNotePlaintext.NotePlaintext.decrypt(
          receiveDescription.getCEnc().toByteArray(),//ciphertext
          ivk,
          receiveDescription.getEpk().toByteArray(),//epk
          receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        BaseNotePlaintext.NotePlaintext noteText = ret1.get();

        byte[] pk_d = new byte[32];
        if (!Librustzcash
            .librustzcashIvkToPkd(incomingViewingKey.getValue(), noteText.d.getData(), pk_d)) {
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
      DupTransactionException, VMIllegalException, ValidateSignatureException,
      ContractExeException, AccountResourceInsufficientException, InvalidProtocolBufferException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();

    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    builder.addSaplingSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);

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

      NoteEncryption.OutCiphertext c_out = new NoteEncryption.OutCiphertext();
      c_out.data = receiveDescription.getCOut().toByteArray();

      Optional<SaplingOutgoingPlaintext> notePlaintext = SaplingOutgoingPlaintext.decrypt(
          c_out,//ciphertext
          fullViewingKey.getOvk(),
          receiveDescription.getValueCommitment().toByteArray(), //cv
          receiveDescription.getNoteCommitment().toByteArray(), //cmu
          receiveDescription.getEpk().toByteArray() //epk
      );

      if (notePlaintext.isPresent()) {
        SaplingOutgoingPlaintext decrypted_out_ct_unwrapped = notePlaintext.get();

        //decode c_enc with pkd、esk
        NoteEncryption.EncCiphertext ciphertext = new NoteEncryption.EncCiphertext();
        ciphertext.data = receiveDescription.getCEnc().toByteArray();

        Optional<BaseNotePlaintext.NotePlaintext> foo = BaseNotePlaintext.NotePlaintext
            .decrypt(ciphertext,
                receiveDescription.getEpk().toByteArray(),
                decrypted_out_ct_unwrapped.esk,
                decrypted_out_ct_unwrapped.pk_d,
                receiveDescription.getNoteCommitment().toByteArray());

        if (foo.isPresent()) {
          BaseNotePlaintext.NotePlaintext bar = foo.get();
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
  public void testVerifySpendProof() {
    librustzcashInitZksnarkParams();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    ExtendedSpendingKey xsk = createXskDefault();
    //    ExpandedSpendingKey expsk = ExpandedSpendingKey.decode(new byte[96]);
    ExpandedSpendingKey expsk = xsk.getExpsk();

    //    PaymentAddress address = PaymentAddress.decode(new byte[43]);
    PaymentAddress address = xsk.DefaultAddress();
    long value = 100;
    Note note = new Note(address, value);

    //    byte[] anchor = new byte[256];
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    //    builder.addSaplingSpend(expsk, note, anchor, voucher);
    //    SpendDescriptionInfo spend = builder.getSpends().get(0);
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    Pointer proofContext = Librustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(spend, proofContext);
    Librustzcash.librustzcashSaplingProvingCtxFree(proofContext);

    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        expsk.getAsk(),
        spend.alpha,
        getHash(),
        result);

    Pointer verifyContext = Librustzcash.librustzcashSaplingVerificationCtxInit();
    boolean ok = Librustzcash.librustzcashSaplingCheckSpend(
        verifyContext,
        spendDescriptionCapsule.getValueCommitment().toByteArray(),
        spendDescriptionCapsule.getAnchor().toByteArray(),
        spendDescriptionCapsule.getNullifier().toByteArray(),
        spendDescriptionCapsule.getRk().toByteArray(),
        spendDescriptionCapsule.getZkproof().getValues().toByteArray(),
        result,
        getHash()
    );
    Librustzcash.librustzcashSaplingVerificationCtxFree(verifyContext);
    Assert.assertEquals(ok, true);
  }

  @Test
  public void saplingBindingSig() {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();

    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    builder.addSaplingSpend(expsk, note, anchor, voucher);
    builder.generateSpendProof(builder.getSpends().get(0), ctx);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);

    // test create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = Librustzcash.librustzcashSaplingBindingSig(
        ctx,
        builder.getValueBalance(),
        getHash(),
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);
  }

  @Test
  public void pushShieldedTransaction()
      throws ContractValidateException, TooBigTransactionException, TooBigTransactionResultException,
      TaposException, TransactionExpirationException, ReceiptCheckErrException,
      DupTransactionException, VMIllegalException, ValidateSignatureException,
      ContractExeException, AccountResourceInsufficientException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();

    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 4010 * 1000000);

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();

    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    builder.addSaplingSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
    builder
        .addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void finalCheck() {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    // generate spend proof
    ExtendedSpendingKey xsk = createXskDefault();
    ExpandedSpendingKey expsk = xsk.getExpsk();
    PaymentAddress address = xsk.DefaultAddress();
    Note note = new Note(address, 4010 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    builder.addSaplingSpend(expsk, note, anchor, voucher);
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(builder.getSpends().get(0), ctx);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder
        .addSaplingOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, new byte[512]);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);

    //create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = Librustzcash.librustzcashSaplingBindingSig(
        ctx,
        builder.getValueBalance(),
        getHash(),
        bindingSig
    );

    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);

    // check spend
    ctx = Librustzcash.librustzcashSaplingVerificationCtxInit();
    byte[] result = new byte[64];
    Librustzcash.librustzcashSaplingSpendSig(
        expsk.getAsk(),
        builder.getSpends().get(0).alpha,
        getHash(),
        result);

    SpendDescription spendDescription = spendDescriptionCapsule.getInstance();
    boolean ok;
    ok = Librustzcash.librustzcashSaplingCheckSpend(
        ctx,
        spendDescription.getValueCommitment().toByteArray(),
        spendDescription.getAnchor().toByteArray(),
        spendDescription.getNullifier().toByteArray(),
        spendDescription.getRk().toByteArray(),
        spendDescription.getZkproof().getValues().toByteArray(),
        result,
        getHash()
    );
    Assert.assertTrue(ok);

    // check output
    ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();
    ok = Librustzcash.librustzcashSaplingCheckOutput(
        ctx,
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        receiveDescription.getEpk().toByteArray(),
        receiveDescription.getZkproof().getValues().toByteArray()
    );
    Assert.assertTrue(ok);

    // final check
    ok = Librustzcash.librustzcashSaplingFinalCheck(
        ctx,
        builder.getValueBalance(),
        bindingSig,
        getHash()
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

}
