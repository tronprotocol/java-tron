package org.tron.core.zksnark;

import static org.tron.core.capsule.TransactionCapsule.getShieldTransactionHashIgnoreTypeException;

import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.collections.Lists;
import org.tron.api.GrpcAPI;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer.EmptyMerkleRoots;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.BindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.ComputeCmParams;
import org.tron.common.zksnark.LibrustzcashParam.FinalCheckParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
import org.tron.common.zksnark.MerklePath;
import org.tron.common.zksnark.ZksnarkClient;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorCreator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
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
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.SpendDescriptionInfo;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.note.Note;
import org.tron.core.zen.note.Note.NotePlaintextEncryptionResult;
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.NoteEncryption.Encryption;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.ShieldContract.PedersenHash;
import org.tron.protos.contract.ShieldContract.ReceiveDescription;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;

public class SendCoinShieldTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static final byte[] DEFAULT_OVK;
  private static final String PUBLIC_ADDRESS_ONE;
  private static final long OWNER_BALANCE = 9999999000000L;
  private static final long tokenId = 1;
  private static final String ASSET_NAME = "trx";
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static String dbPath = "output_ShieldedTransaction_test";
  private static String dbDirectory = "db_ShieldedTransaction_test";
  private static String indexDirectory = "index_ShieldedTransaction_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static Wallet wallet;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, "config-test-mainnet.conf");
    context = new TronApplicationContext(DefaultConfig.class);
    PUBLIC_ADDRESS_ONE =
        Wallet.getAddressPreFixString() + "a7d8a35b260395c14aa456297662092ba3b76fc0";
    DEFAULT_OVK = ByteArray
        .fromHexString("030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    Args.getInstance().setZenTokenId(String.valueOf(tokenId));
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);

    AssetIssueContract assetIssueContract = AssetIssueContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE)))
        .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
        .setId(Long.toString(tokenId)).setTotalSupply(OWNER_BALANCE).setTrxNum(TRX_NUM).setNum(NUM)
        .setStartTime(START_TIME).setEndTime(END_TIME).setVoteScore(VOTE_SCORE)
        .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
        .setUrl(ByteString.copyFrom(ByteArray.fromString(URL))).build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
  }

  private void addZeroValueOutputNote(ZenTransactionBuilder builder) throws ZksnarkException {
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(DEFAULT_OVK, paymentAddress, 0, "just for decode for ovk".getBytes());
  }

  @Test
  public void testPathMock() throws ZksnarkException {
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
    ByteUtil.reverse(bytes1);
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
    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
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
    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(cm));
    PedersenHash a = compressCapsule1.getInstance();
    tree.append(a);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    return voucher;
  }

  private void librustzcashInitZksnarkParams() throws ZksnarkException {
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
  }

  @Test
  public void testStringRevert() throws Exception {
    byte[] bytes = ByteArray
        .fromHexString("6c030e6d7460f91668cc842ceb78cdb54470469e78cd59cf903d3a6e1aa03e7c");
    ByteUtil.reverse(bytes);
    System.out.println("testStringRevert------" + ByteArray.toHexString(bytes));
  }

  @Test
  public void testGenerateSpendProof() throws Exception {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();

    DiversifierT diversifierT = new DiversifierT();
    byte[] d;
    while (true) {
      d = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
      if (JLibrustzcash.librustzcashCheckDiversifier(d)) {
        break;
      }
    }
    diversifierT.setData(d);

    FullViewingKey fullViewingKey = expsk.fullViewingKey();

    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    Optional<PaymentAddress> op = incomingViewingKey.address(diversifierT);

    Note note = new Note(op.get(), 100);
    note.setRcm(ByteArray
        .fromHexString("bf4b2042e3e8c4a0b390e407a79a0b46e36eff4f7bb54b2349dbb0046ee21e02"));

    IncrementalMerkleVoucherContainer voucher = createComplexMerkleVoucherContainer(note.cm());

    byte[] anchor = voucher.root().getContent().toByteArray();
    SpendDescriptionInfo spend = new SpendDescriptionInfo(expsk, note, anchor, voucher);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule sdesc = builder.generateSpendProof(spend, ctx);
  }

  @Test
  public void generateOutputProof() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void verifyOutputProof() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000, new byte[512]);
    ReceiveDescriptionCapsule capsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    ReceiveDescription receiveDescription = capsule.getInstance();
    ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    if (!JLibrustzcash.librustzcashSaplingCheckOutput(
        new CheckOutputParams(ctx, receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(),
            receiveDescription.getEpk().toByteArray(),
            receiveDescription.getZkproof().toByteArray()))) {
      JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
      throw new RuntimeException("librustzcashSaplingCheckOutput error");
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }


  @Test
  public void testDecryptReceiveWithIvk() throws ZksnarkException {
    //verify c_enc
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder();

    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();

    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(512);
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000, memo);

    ZenTransactionBuilder.ReceiveDescriptionInfo output = builder.getReceives().get(0);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder.generateOutputProof(output, ctx);
    ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();

    Optional<Note> ret1 = Note.decrypt(receiveDescription.getCEnc().toByteArray(),//ciphertext
        fullViewingKey.inViewingKey().getValue(), receiveDescription.getEpk().toByteArray(),//epk
        receiveDescription.getNoteCommitment().toByteArray() //cm
    );

    Assert.assertTrue(ret1.isPresent());

    Note noteText = ret1.get();
    byte[] pkD = new byte[32];
    if (!JLibrustzcash.librustzcashIvkToPkd(
        new IvkToPkdParams(incomingViewingKey.getValue(), noteText.getD().getData(), pkD))) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      return;
    }

    Assert.assertArrayEquals(paymentAddress.getPkD(), pkD);
    Assert.assertEquals(noteText.getValue(), 4000);
    Assert.assertArrayEquals(noteText.getMemo(), memo);

    String paymentAddressStr = KeyIo.encodePaymentAddress(new PaymentAddress(noteText.getD(), pkD));

    GrpcAPI.Note grpcAPINote = GrpcAPI.Note.newBuilder().setPaymentAddress(paymentAddressStr)
        .setValue(noteText.getValue()).setRcm(ByteString.copyFrom(noteText.getRcm()))
        .setMemo(ByteString.copyFrom(noteText.getMemo())).build();

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  public String byte2intstring(byte[] input) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < input.length; i++) {
      sb.append(String.valueOf((int) input[i]) + ", ");
      if (i % 16 == 15) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  @Test
  public void testDecryptReceiveWithOvk() throws Exception {
    //decode c_out with ovk.
    librustzcashInitZksnarkParams();

    // construct payment address
    SpendingKey spendingKey2 = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    PaymentAddress paymentAddress2 = spendingKey2.defaultAddress();
    FullViewingKey fullViewingKey = spendingKey2.fullViewingKey();

    // generate output proof
    ZenTransactionBuilder builder2 = new ZenTransactionBuilder();
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    builder2.addOutput(fullViewingKey.getOvk(), paymentAddress2, 10000, new byte[512]);
    ZenTransactionBuilder.ReceiveDescriptionInfo output = builder2.getReceives().get(0);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder2.generateOutputProof(output, ctx);
    ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();

    byte[] pkd = paymentAddress2.getPkD();
    Note note = new Note(paymentAddress2, 4000);//construct function:this.pkD = address.getPkD();
    note.setRcm(ByteArray
        .fromHexString("83d36fd4c8eebec516c3a8ce2fe4832e01eb57bd7f9f9c9e0bd68cc69a5b0f06"));
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(512);
    note.setMemo(memo);

    byte[] cmuOpt = note.cm();
    Assert.assertNotNull(cmuOpt);

    NotePlaintextEncryptionResult enc = note.encrypt(pkd).get();
    NoteEncryption encryptor = enc.getNoteEncryption();
    OutgoingPlaintext outgoingPlaintext = new OutgoingPlaintext(note.getPkD(), encryptor.getEsk());

    // encrypt with ovk
    Encryption.OutCiphertext outCiphertext = outgoingPlaintext
        .encrypt(fullViewingKey.getOvk(), receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(), encryptor);

    // get pkD, esk from decryption of c_out with ovk
    Optional<OutgoingPlaintext> ret2 = OutgoingPlaintext
        .decrypt(outCiphertext, fullViewingKey.getOvk(),
            receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(), encryptor.getEpk());

    if (ret2.isPresent()) {
      OutgoingPlaintext decryptedOutgoingPlaintext = ret2.get();
      Assert.assertArrayEquals(decryptedOutgoingPlaintext.getPkD(), outgoingPlaintext.getPkD());
      Assert.assertArrayEquals(decryptedOutgoingPlaintext.getEsk(), outgoingPlaintext.getEsk());

      //decrypt c_enc with pkd、esk
      Encryption.EncCiphertext ciphertext = new Encryption.EncCiphertext();
      ciphertext.setData(enc.getEncCiphertext());
      Optional<Note> foo = Note
          .decrypt(ciphertext, encryptor.getEpk(), decryptedOutgoingPlaintext.getEsk(),
              decryptedOutgoingPlaintext.getPkD(), cmuOpt);

      if (foo.isPresent()) {
        Note bar = foo.get();
        //verify result
        Assert.assertEquals(4000, bar.getValue());
        Assert.assertArrayEquals(memo, bar.getMemo());
      } else {
        JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
        Assert.assertFalse(true);
      }
    } else {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      Assert.assertFalse(true);
    }

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
  }

  @Test
  public void pushShieldedTransactionAndDecryptWithIvk()
      throws ContractValidateException, TooBigTransactionException,
      TooBigTransactionResultException, TaposException, TransactionExpirationException,
      ReceiptCheckErrException, DupTransactionException, VMIllegalException,
      ValidateSignatureException, BadItemException, ContractExeException,
      AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(1000 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate spend proof
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] senderOvk = expsk.getOvk();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 1000 * 1000000L);
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
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(512);
    builder
        .addOutput(senderOvk, paymentAddress, 1000 * 1000000L - wallet.getShieldedTransactionFee(),
            memo);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);
    Assert.assertTrue(ok);

    // add here
    byte[] ivk = incomingViewingKey.getValue();
    Protocol.Transaction t = transactionCap.getInstance();

    for (org.tron.protos.Protocol.Transaction.Contract c : t.getRawData().getContractList()) {
      if (c.getType() != ContractType.ShieldedTransferContract) {
        continue;
      }
      ShieldedTransferContract stContract = c.getParameter()
          .unpack(ShieldedTransferContract.class);
      ReceiveDescription receiveDescription = stContract.getReceiveDescription(0);

      Optional<Note> ret1 = Note.decrypt(receiveDescription.getCEnc().toByteArray(),//ciphertext
          ivk, receiveDescription.getEpk().toByteArray(),//epk
          receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        Note noteText = ret1.get();
        byte[] pkD = new byte[32];
        if (!JLibrustzcash.librustzcashIvkToPkd(
            new IvkToPkdParams(incomingViewingKey.getValue(), noteText.getD().getData(), pkD))) {
          JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
          return;
        }
        Assert.assertArrayEquals(paymentAddress.getPkD(), pkD);
        Assert.assertEquals(1000 * 1000000L - wallet.getShieldedTransactionFee(),
            noteText.getValue());
        Assert.assertArrayEquals(memo, noteText.getMemo());
      } else {
        Assert.assertFalse(true);
      }
    }
    // end here
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void testDefaultAddress() throws ZksnarkException, BadItemException {
    PaymentAddress paymentAddress = SpendingKey.random().defaultAddress();
    Assert.assertNotEquals("0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(paymentAddress.getPkD()));
  }

  @Test
  public void pushShieldedTransactionAndDecryptWithOvk()
      throws ContractValidateException, TooBigTransactionException,
      TooBigTransactionResultException, TaposException, TransactionExpirationException,
      ReceiptCheckErrException, DupTransactionException, VMIllegalException,
      ValidateSignatureException, BadItemException, ContractExeException,
      AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(1000 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate spend proof
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    byte[] senderOvk = expsk.getOvk();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 1000 * 1000000L);
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
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(512);
    builder
        .addOutput(senderOvk, paymentAddress, 1000 * 1000000L - wallet.getShieldedTransactionFee(),
            memo);

    TransactionCapsule transactionCap = builder.build();
    boolean ok = dbManager.pushTransaction(transactionCap);
    Assert.assertTrue(ok);

    // add here
    Protocol.Transaction t = transactionCap.getInstance();
    for (org.tron.protos.Protocol.Transaction.Contract c : t.getRawData().getContractList()) {
      if (c.getType() != Protocol.Transaction.Contract.ContractType.ShieldedTransferContract) {
        continue;
      }
      ShieldedTransferContract stContract = c.getParameter()
          .unpack(ShieldedTransferContract.class);
      ReceiveDescription receiveDescription = stContract.getReceiveDescription(0);

      //first try to decrypt cOut with ovk, get pkd、esk
      Encryption.OutCiphertext cOut = new Encryption.OutCiphertext();
      cOut.setData(receiveDescription.getCOut().toByteArray());
      Optional<OutgoingPlaintext> notePlaintext = OutgoingPlaintext.decrypt(cOut,//ciphertext
          senderOvk, receiveDescription.getValueCommitment().toByteArray(), //cv
          receiveDescription.getNoteCommitment().toByteArray(), //cmu
          receiveDescription.getEpk().toByteArray() //epk
      );

      //then decrypt c_enc with pkd、esk, get decoded note == ciphertext
      if (notePlaintext.isPresent()) {
        OutgoingPlaintext decryptedOutgoingPlaintext = notePlaintext.get();

        Encryption.EncCiphertext ciphertext = new Encryption.EncCiphertext();
        ciphertext.setData(receiveDescription.getCEnc().toByteArray());
        Optional<Note> foo = Note.decrypt(ciphertext, receiveDescription.getEpk().toByteArray(),
            decryptedOutgoingPlaintext.getEsk(), decryptedOutgoingPlaintext.getPkD(),
            receiveDescription.getNoteCommitment().toByteArray());

        if (foo.isPresent()) {
          Note bar = foo.get();
          //verify result
          Assert.assertEquals(1000 * 1000000L - wallet.getShieldedTransactionFee(), bar.getValue());
          Assert.assertArrayEquals(memo, bar.getMemo());
        } else {
          Assert.assertFalse(true);
        }
      }
    }
    // end here

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  private byte[] getHash() {
    return Sha256Hash.of(CommonParameter
        .getInstance().isECKeyCryptoEngine(), "this is a test".getBytes()).getBytes();
  }

  public void checkZksnark() throws BadItemException, ZksnarkException {
    librustzcashInitZksnarkParams();
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(4010 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 4010 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);
    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000L, new byte[512]);
    TransactionCapsule transactionCap = builder.build();
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    boolean ret = ZksnarkClient.getInstance().checkZksnarkProof(transactionCap.getInstance(),
        getShieldTransactionHashIgnoreTypeException(transactionCap.getInstance()),
        10 * 1000000);
    Assert.assertTrue(ret);
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
    long proofContext = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(spend, proofContext);
    JLibrustzcash.librustzcashSaplingProvingCtxFree(proofContext);

    byte[] result = new byte[64];
    JLibrustzcash.librustzcashSaplingSpendSig(
        new SpendSigParams(expsk.getAsk(), spend.getAlpha(), getHash(), result));

    long verifyContext = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    boolean ok = JLibrustzcash.librustzcashSaplingCheckSpend(new CheckSpendParams(verifyContext,
        spendDescriptionCapsule.getValueCommitment().toByteArray(),
        spendDescriptionCapsule.getAnchor().toByteArray(),
        spendDescriptionCapsule.getNullifier().toByteArray(),
        spendDescriptionCapsule.getRk().toByteArray(),
        spendDescriptionCapsule.getZkproof().toByteArray(), result, getHash()));
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(verifyContext);
    Assert.assertEquals(ok, true);
  }

  @Test
  public void saplingBindingSig() throws BadItemException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 4010 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    builder.addSpend(expsk, note, anchor, voucher);
    builder.generateSpendProof(builder.getSpends().get(0), ctx);
    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000L, new byte[512]);
    builder.generateOutputProof(builder.getReceives().get(0), ctx);

    // test create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = JLibrustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx, builder.getValueBalance(), getHash(), bindingSig));
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);
  }

  @Test
  public void pushShieldedTransaction()
      throws ContractValidateException, TooBigTransactionException,
      TooBigTransactionResultException,
      TaposException, TransactionExpirationException, ReceiptCheckErrException,
      DupTransactionException, VMIllegalException, ValidateSignatureException, BadItemException,
      ContractExeException, AccountResourceInsufficientException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(4010 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 4010 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);
    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress,
        4010 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);
    TransactionCapsule transactionCap = builder.build();
    boolean ok = dbManager.pushTransaction(transactionCap);
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void finalCheck() throws BadItemException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    // generate spend proof
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 4010 * 1000000L);
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
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000L, new byte[512]);
    ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
        .generateOutputProof(builder.getReceives().get(0), ctx);

    //create binding sig
    byte[] bindingSig = new byte[64];
    boolean ret = JLibrustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx, builder.getValueBalance(), getHash(), bindingSig));
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ret);
    // check spend
    ctx = JLibrustzcash.librustzcashSaplingVerificationCtxInit();
    byte[] result = new byte[64];
    JLibrustzcash.librustzcashSaplingSpendSig(
        new SpendSigParams(expsk.getAsk(), builder.getSpends().get(0).getAlpha(), getHash(),
            result));

    SpendDescription spendDescription = spendDescriptionCapsule.getInstance();
    boolean ok;
    ok = JLibrustzcash.librustzcashSaplingCheckSpend(
        new CheckSpendParams(ctx, spendDescription.getValueCommitment().toByteArray(),
            spendDescription.getAnchor().toByteArray(),
            spendDescription.getNullifier().toByteArray(), spendDescription.getRk().toByteArray(),
            spendDescription.getZkproof().toByteArray(), result, getHash()));
    Assert.assertTrue(ok);

    // check output
    ReceiveDescription receiveDescription = receiveDescriptionCapsule.getInstance();
    ok = JLibrustzcash.librustzcashSaplingCheckOutput(
        new CheckOutputParams(ctx, receiveDescription.getValueCommitment().toByteArray(),
            receiveDescription.getNoteCommitment().toByteArray(),
            receiveDescription.getEpk().toByteArray(),
            receiveDescription.getZkproof().toByteArray()));
    Assert.assertTrue(ok);
    // final check
    ok = JLibrustzcash.librustzcashSaplingFinalCheck(
        new FinalCheckParams(ctx, builder.getValueBalance(), bindingSig, getHash()));
    Assert.assertTrue(ok);
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  @Test
  public void testEmptyRoot() {
    byte[] bytes = IncrementalMerkleTreeContainer.emptyRoot().getContent().toByteArray();
    ByteUtil.reverse(bytes);
    Assert.assertEquals("3e49b5f954aa9d3545bc6c37744661eea48d7c34e3000d82b7f0010c30f4c2fb",
        ByteArray.toHexString(bytes));
  }

  @Test
  public void testEmptyRoots() throws Exception {
    JSONArray array = readFile("merkle_roots_empty_sapling.json");
    for (int i = 0; i < 32; i++) {
      String string = array.getString(i);
      EmptyMerkleRoots emptyMerkleRootsInstance = EmptyMerkleRoots.getEmptyMerkleRootsInstance();
      byte[] bytes = emptyMerkleRootsInstance.emptyRoot(i).getContent().toByteArray();
      Assert.assertEquals(string, ByteArray.toHexString(bytes));
    }
  }

  private JSONArray readFile(String fileName) throws Exception {
    String file1 = SendCoinShieldTest.class.getClassLoader()
        .getResource("json" + File.separator + fileName).getFile();
    List<String> readLines = Files.readLines(new File(file1), Charsets.UTF_8);
    JSONArray array = JSONArray.parseArray(readLines.stream().reduce((s, s2) -> s + s2).get());
    return array;
  }


  @Test
  public void testComputeCm() throws Exception {
    byte[] result = new byte[32];
    if (!JLibrustzcash.librustzcashComputeCm(
        new ComputeCmParams((ByteArray.fromHexString("fc6eb90855700861de6639")), ByteArray
            .fromHexString("1abfbf64bc4934aaf7f29b9fea995e5a16e654e63dbe07db0ef035499d216e19"),
            9990000000L, ByteArray
            .fromHexString("08e3a2ff1101b628147125b786c757b483f1cf7c309f8a647055bfb1ca819c02"),
            result))) {
      System.out.println(" error");
    } else {
      System.out.println(" ok");
    }
  }

  @Test
  public void getSpendingKey() throws Exception {
    SpendingKey sk = SpendingKey
        .decode("0b862f0e70048551c08518ff49a19db027d62cdeeb2fa974db91c10e6ebcdc16");
    System.out.println(sk.encode());
    System.out.println(
        "sk.expandedSpendingKey()" + ByteArray.toHexString(sk.expandedSpendingKey().encode()));
    System.out.println("sk.fullViewKey()" + ByteArray.toHexString(sk.fullViewingKey().encode()));
    System.out
        .println("sk.ivk()" + ByteArray.toHexString(sk.fullViewingKey().inViewingKey().getValue()));
    System.out.println(
        "sk.defaultDiversifier:" + ByteArray.toHexString(sk.defaultDiversifier().getData()));

    System.out.println("sk.defaultAddress:" + ByteArray.toHexString(sk.defaultAddress().encode()));

    System.out.println("rcm:" + ByteArray.toHexString(Note.generateR()));

    int count = 10;
    for (int i = 0; i < count; i++) {
      // new sk
      System.out.println("---- random " + i + " ----");

      sk = SpendingKey.random();
      System.out.println("sk is: " + ByteArray.toHexString(sk.getValue()));

      DiversifierT diversifierT = new DiversifierT();
      byte[] d;
      while (true) {
        d = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
        if (JLibrustzcash.librustzcashCheckDiversifier(d)) {
          break;
        }
      }
      diversifierT.setData(d);
      System.out.println("d is: " + ByteArray.toHexString(d));

      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      System.out.println("expsk-ask is: " + ByteArray.toHexString(expsk.getAsk()));
      System.out.println("expsk-nsk is: " + ByteArray.toHexString(expsk.getNsk()));
      System.out.println("expsk-ovk is: " + ByteArray.toHexString(expsk.getOvk()));

      FullViewingKey fullViewingKey = expsk.fullViewingKey();
      System.out.println("fullviewkey-ak is: " + ByteArray.toHexString(fullViewingKey.getAk()));
      System.out.println("fullviewkey-nk is: " + ByteArray.toHexString(fullViewingKey.getNk()));
      System.out.println("fullviewkey-ovk is: " + ByteArray.toHexString(fullViewingKey.getOvk()));

      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      System.out.println("ivk is: " + ByteArray.toHexString(incomingViewingKey.getValue()));

      Optional<PaymentAddress> op = incomingViewingKey.address(diversifierT);
      System.out.println("pkD is: " + ByteArray.toHexString(op.get().getPkD()));

      byte[] rcm = Note.generateR();
      System.out.println("rcm is " + ByteArray.toHexString(rcm));

      byte[] alpha = Note.generateR();
      System.out.println("alpha is " + ByteArray.toHexString(alpha));

      String address = KeyIo.encodePaymentAddress(op.get());
      System.out.println("saplingaddress is: " + address);

      // check
      PaymentAddress paymentAddress = KeyIo.decodePaymentAddress(address);
      Assert.assertEquals(ByteArray.toHexString(paymentAddress.getD().getData()),
          ByteArray.toHexString(d));
      Assert.assertEquals(ByteArray.toHexString(paymentAddress.getPkD()),
          ByteArray.toHexString(op.get().getPkD()));

    }
  }

  @Test
  public void testTwoCMWithDiffSkInOneTx() throws Exception {
    // generate spend proof
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(110 * 1000000L);
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //prepare two cm with different sk
    SpendingKey sk1 = SpendingKey.random();
    ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
    PaymentAddress address1 = sk1.defaultAddress();
    Note note1 = new Note(address1, 1000 * 1000000L);
    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());
    PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
    compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
    PedersenHash a = compressCapsule1.getInstance();
    tree.append(a);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk1, note1, anchor, voucher);

    /*SpendingKey sk2 = SpendingKey.random();
    ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
    PaymentAddress address2 = sk2.defaultAddress();
    Note note2 = new Note(address2, 100 * 1000000);
    PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
    compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
    PedersenHash a2 = compressCapsule2.getInstance();
    tree.append(a2);
    IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
    byte[] anchor2 = voucher2.root().getContent().toByteArray();
    dbManager
        .getMerkleContainer()
        .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());
    builder.addSpend(expsk2, note2, anchor2, voucher2);*/

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress,
        1000 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);
    TransactionCapsule transactionCap = builder.build();
    //execute
    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate();
    TransactionResultCapsule resultCapsule = new TransactionResultCapsule();
    actuator.get(0).execute(resultCapsule);
  }

  private void executeTx(TransactionCapsule transactionCap) throws Exception {
    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate();
    TransactionResultCapsule resultCapsule = new TransactionResultCapsule();
    actuator.get(0).execute(resultCapsule);
  }

  @Test
  public void testValueBalance() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    //case 1， a public input, no input cm,  an output cm, no public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      String OWNER_ADDRESS =
          Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
      AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
          110_000_000L);
      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
          .putAssetV2(CommonParameter.getInstance().zenTokenId, 110_000_000L).build());

      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS), 100_000_000L);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000L, new byte[512]);

      TransactionCapsule transactionCap = builder.build();

      // 100_000_000L + 0L !=  200_000_000L + 0L + 10_000_000L
      try {
        executeTx(transactionCap);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
      }
    }

    //case 2， a public input, no input cm,  an output cm, a public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      String OWNER_ADDRESS =
          Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
      AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
          110_000_000L);

      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
          .putAssetV2(CommonParameter.getInstance().zenTokenId, 110_000_000L).build());
      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS), 100_000_000L);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000L, new byte[512]);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

      TransactionCapsule transactionCap = builder.build();

      //100_000_000L + 0L !=  200_000_000L + 10_000_000L + 10_000_000L
      try {
        executeTx(transactionCap);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
      }
    }

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      //prepare  cm
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 110 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

      //add spendDesc into builder
      builder.addSpend(expsk1, note1, anchor, voucher);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

      TransactionCapsule transactionCap = builder.build();

      //   0L + 110_000_000L  !=  200_000_000L + 0L + 10_000_000L
      try {
        executeTx(transactionCap);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
      }
    }

    //case 4， no public input, an input cm,  an output cm, no public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      //prepare  cm
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 110 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

      //add spendDesc into builder
      builder.addSpend(expsk1, note1, anchor, voucher);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000L, new byte[512]);

      TransactionCapsule transactionCap = builder.build();

      //   110_000_000L + 0L!=  200_000_000L + 0L + 10_000_000L
      try {
        executeTx(transactionCap);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
      }
    }

    //case 5， no public input, an input cm,  an output cm, a public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      //prepare  cm
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 110 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

      //add spendDesc into builder
      builder.addSpend(expsk1, note1, anchor, voucher);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000L, new byte[512]);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

      TransactionCapsule transactionCap = builder.build();

      //     0L + 110_000_000L !=  200_000_000L + 10_000_000L + 10_000_000L
      try {
        executeTx(transactionCap);
        Assert.fail();
      } catch (ContractValidateException e) {
        if (!e.getMessage().equals("librustzcashSaplingFinalCheck error")) {
          throw e;
        }
      }
    }
  }

  @Test
  public void TestCreateMultipleTxAtTheSameTime() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    List<TransactionCapsule> txList = Lists.newArrayList();
    //case 1， a public input, no input cm,  an output cm, no public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      String OWNER_ADDRESS =
          Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
      AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
          220_000_000L);

      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
          .putAssetV2(CommonParameter.getInstance().zenTokenId, 220_000_000L).build());
      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS), 210_000_000L);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000L, new byte[512]);

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
      AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
          230_000_000L);
      ownerCapsule.setInstance(ownerCapsule.getInstance().toBuilder()
          .putAssetV2(CommonParameter.getInstance().zenTokenId, 230_000_000L).build());
      dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
      builder.setTransparentInput(ByteArray.fromHexString(OWNER_ADDRESS), 220_000_000L);

      // generate output proof
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 200 * 1000000L, new byte[512]);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

      TransactionCapsule transactionCap1 = builder.build();
      transactionCap1.setBlockNum(2);
      txList.add(transactionCap1);

      //220_000_000L + 0L =  200_000_000L + 10_000_000L + 10_000_000L

    }

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

      //prepare  cm
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 20 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

      //add spendDesc into builder
      builder.addSpend(expsk1, note1, anchor, voucher);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

      TransactionCapsule transactionCap1 = builder.build();
      transactionCap1.setBlockNum(3);
      txList.add(transactionCap1);

      // 0L + 20_000_000L  =  0L + 10_000_000L +  10_000_000L
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
  }

  @Test
  public void TestCtxGeneratesTooMuchProof() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    //case 3， no public input, an input cm,  no output cm, a public output
    {
      //prepare two cm with different sk, cm1 is used for fake spendDesc
      SpendingKey sk1 = SpendingKey.random();
      ExpandedSpendingKey expsk1 = sk1.expandedSpendingKey();
      PaymentAddress address1 = sk1.defaultAddress();
      Note note1 = new Note(address1, 110 * 1000000L);

      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());
      PedersenHashCapsule compressCapsule1 = new PedersenHashCapsule();
      compressCapsule1.setContent(ByteString.copyFrom(note1.cm()));
      PedersenHash a = compressCapsule1.getInstance();
      tree.append(a);
      IncrementalMerkleVoucherContainer voucher1 = tree.toVoucher();
      byte[] anchor1 = voucher1.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor1, voucher1.getVoucherCapsule().getTree());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, long ctx)
            throws ZksnarkException {

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
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

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

      // 0L + 20_000_000L  =  0L + 10_000_000L +  10_000_000L
    }
  }

  @Test
  public void TestGeneratesProofWithDiffCtx() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    //case 3， no public input, an input cm,  no output cm, a public output
    {

      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, long ctx)
            throws ZksnarkException {
          long fakeCtx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
          return super.generateSpendProof(spend, fakeCtx);
        }
      };

      //add spendDesc into builder
      builder.addSpend(expsk2, note2, anchor2, voucher2);

      String TO_ADDRESS =
          Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

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
      // 0L + 20_000_000L  =  0L + 10_000_000L +  10_000_000L
    }
  }

  @Test
  public void TestGeneratesProofWithWrongAlpha() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    //case 3， no public input, an input cm,  no output cm, a public output
    {
      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());

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
      spendDescriptionInfo.setAlpha(bytes);

      byte[] dataToBeSigned = ByteArray
          .fromHexString("0eadb4ea6533afa906673b0101343b00a6682093ccc81082d0970e5ed6f72cbd");
      byte[] result = new byte[64];
      JLibrustzcash.librustzcashSaplingSpendSig(
          new SpendSigParams(spendDescriptionInfo.getExpsk().getAsk(),
              spendDescriptionInfo.getAlpha(), dataToBeSigned, result));
    }
  }


  @Test
  public void TestGeneratesProofWithWrongRcm() throws Exception {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    librustzcashInitZksnarkParams();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    // generate spend proof
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 4010 * 1000000L);
    //note.r =  ByteArray
    //    .fromHexString("0xe7db4ea6533afa906673b0101343b00a6682093ccc81082d0970e5ed6f72cb6");

    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    builder.addSpend(expsk, note, anchor, voucher);
    SpendDescriptionCapsule spendDescriptionCapsule = builder
        .generateSpendProof(builder.getSpends().get(0), ctx);

  }

  @Test
  public void TestWrongAsk() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    //case 3， no public input, an input cm,  no output cm, a public output
    {
      SpendingKey sk2 = SpendingKey.random();
      ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
      PaymentAddress address2 = sk2.defaultAddress();
      Note note2 = new Note(address2, 20 * 1000000L);

      IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
          new IncrementalMerkleTreeCapsule());

      PedersenHashCapsule compressCapsule2 = new PedersenHashCapsule();
      compressCapsule2.setContent(ByteString.copyFrom(note2.cm()));
      PedersenHash a2 = compressCapsule2.getInstance();
      tree.append(a2);
      IncrementalMerkleVoucherContainer voucher2 = tree.toVoucher();
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor2, voucher2.getVoucherCapsule().getTree());

      byte[] fakeAsk = ByteArray
          .fromHexString("0xe7db4ea6533afa906673b0101343b00a6682093ccc81082d0970e5ed6f72cb6");

      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        @Override
        public void createSpendAuth(byte[] dataToBeSigned) throws ZksnarkException {
          for (int i = 0; i < this.getSpends().size(); i++) {
            byte[] result = new byte[64];
            JLibrustzcash.librustzcashSaplingSpendSig(
                new SpendSigParams(fakeAsk, this.getSpends().get(i).getAlpha(), dataToBeSigned,
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
      AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
          ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
      dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);

      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS), 10_000_000L);

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
    Note note = new Note(address, 1000 * 1000000L);

    IncrementalMerkleTreeContainer tree = new IncrementalMerkleTreeContainer(
        new IncrementalMerkleTreeCapsule());

    PedersenHashCapsule compressCapsule = new PedersenHashCapsule();
    compressCapsule.setContent(ByteString.copyFrom(note.cm()));
    PedersenHash hash = compressCapsule.getInstance();
    tree.append(hash);
    IncrementalMerkleVoucherContainer voucher = tree.toVoucher();
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    return new SpendDescriptionInfo(expsk, note, anchor, voucher);
  }

  private String generateDefaultToAccount() {
    String TO_ADDRESS =
        Wallet.getAddressPreFixString() + "b48794500882809695a8a687866e76d4271a1abc";
    AccountCapsule toCapsule = new AccountCapsule(ByteString.copyFromUtf8("to"),
        ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)), AccountType.Normal, 0L);
    dbManager.getAccountStore().put(toCapsule.getAddress().toByteArray(), toCapsule);
    return TO_ADDRESS;
  }

  private TransactionCapsule generateDefaultBuilder(ZenTransactionBuilder builder)
      throws BadItemException, ZksnarkException {
    //add spendDesc into builder
    SpendDescriptionInfo spendDescriptionInfo = generateDefaultSpend();
    builder.addSpend(spendDescriptionInfo);

    //add to transparent
    addZeroValueOutputNote(builder);
    String TO_ADDRESS = generateDefaultToAccount();
    builder.setTransparentOutput(ByteArray.fromHexString(TO_ADDRESS),
        1000 * 1000000L - wallet.getShieldedTransactionFee());

    TransactionCapsule transactionCap = builder.build();
    return transactionCap;
  }

  @Test
  public void TestDefaultBuilder() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(1000 * 1000000L);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    TransactionCapsule transactionCapsule = generateDefaultBuilder(builder);
    executeTx(transactionCapsule);
  }

  @Test
  public void TestWrongSpendRk() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong rk
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, long ctx)
            throws ZksnarkException {
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

  @Test
  public void TestWrongSpendProof() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong proof
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, long ctx)
            throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);
          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] fakeProof = ByteArray.fromHexString(
              "0ac001af7f0059cdfec9eed3900b3a4b25ace3cdeb7e962929be9432e51b222be6d7b885d5393"
                  + "c0d373c5b3dbc19210f94e7de831750c5d3a545bbe3732b4d87e4b4350c29519cbebdabd599db"
                  + "9e685f37af2440abc29b3c11cc1dc6712582f74fe06506182e9202b20467017c53fb6d744cd6e"
                  + "08b6428d0e0607688b67876036d2e30617fe020b1fd33ce96cda898e679f44f9715d5681ee0e4"
                  + "2f419d7af4d438240fee7b6519e525f452d2ac56b1fb7cd12e9fb0b39caf6f84918b76fa5d46");
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

  @Test
  public void TestWrongNf() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong nf
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, long ctx)
            throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);

          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] bytes = ByteArray.fromHexString(
              "7b21b1bc8aba1bb8d5a3638ef8e3c741b84ca7c122053a1072a932c043a0a95");//256
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

  @Test
  public void TestWrongAnchor() throws Exception {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet) {
        //set wrong anchor
        @Override
        public SpendDescriptionCapsule generateSpendProof(SpendDescriptionInfo spend, long ctx)
            throws ZksnarkException {
          SpendDescriptionCapsule spendDescriptionCapsule = super.generateSpendProof(spend, ctx);
          //The format is correct, but it does not belong to this
          // note value ,fake : 200_000_000,real:20_000_000
          byte[] bytes = ByteArray.fromHexString(
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
