package org.tron.core.zksnark;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jna.Pointer;
import java.io.File;
import java.security.SignatureException;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam;
import org.tron.common.zksnark.LibrustzcashParam.BindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.common.zksnark.LibrustzcashParam.OutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
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
import org.tron.core.exception.*;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.ReceiveDescriptionInfo;
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
import org.tron.core.zen.note.NoteEncryption;
import org.tron.core.zen.note.Note.NotePlaintextEncryptionResult;
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Contract.SpendDescription;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionSign;

@Slf4j
public class ShieldedReceiveTest {

  private static Manager dbManager;
  private static final String dbPath = "receive_description_test";
  private static TronApplicationContext context;

  private static final String FROM_ADDRESS;
  private static final String ADDRESS_ONE_PRIVATE_KEY;
  private static final long OWNER_BALANCE = 100_000_000;
  private static final long FROM_AMOUNT = 110_000_000;
  private static Wallet wallet;
  
  private static final long  tokenId = 1;
  private static final String ASSET_NAME = "trx";
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  
  public enum TestColumn {CV, ZKPOOF, D_CM, PKD_CM, VALUE_CM, R_CM};
  public enum TestSignMissingColumn {FROM_ADDRESS, FROM_AMOUNT, SPEND_DESCRITPION,
    RECEIVE_DESCRIPTION, TO_ADDRESS, TO_AMOUNT};
  public enum TestReceiveMissingColumn {CV, CM, EPK, C_ENC, C_OUT, ZKPROOF};

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, "config-localtest.conf");
    context = new TronApplicationContext(DefaultConfig.class);
    FROM_ADDRESS = Wallet.getAddressPreFixString() + "a7d8a35b260395c14aa456297662092ba3b76fc0";
    ADDRESS_ONE_PRIVATE_KEY = "7f7f701e94d4f1dd60ee5205e7ea8ee31121427210417b608a6b2e96433549a7";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    wallet = context.getBean(Wallet.class);
    dbManager = context.getBean(Manager.class);
    //give a big value for pool, avoid for
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(10_000_000_000L);
    Args.getInstance().setAllowShieldedTransaction(true);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() throws InterruptedException {
    Args.clearParam();
    context.destroy();

    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
  /**
   * create temp Capsule test need.
   */
  @Before
  public void createToken() {
    Args.getInstance().setZenTokenId(String.valueOf(tokenId));
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);
    
    AssetIssueContract assetIssueContract =
            AssetIssueContract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(FROM_ADDRESS)))
                    .setName(ByteString.copyFrom(ByteArray.fromString(ASSET_NAME)))
                    .setId(Long.toString(tokenId))
                    .setTotalSupply(OWNER_BALANCE)
                    .setTrxNum(TRX_NUM)
                    .setNum(NUM)
                    .setStartTime(START_TIME)
                    .setEndTime(END_TIME)
                    .setVoteScore(VOTE_SCORE)
                    .setDescription(ByteString.copyFrom(ByteArray.fromString(DESCRIPTION)))
                    .setUrl(ByteString.copyFrom(ByteArray.fromString(URL)))
                    .build();
    AssetIssueCapsule assetIssueCapsule = new AssetIssueCapsule(assetIssueContract);
    dbManager.getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
  }

  /**
   * create temp transparent address for  test need.
   */
  private void createCapsule() {
    AccountCapsule ownerCapsule =
            new AccountCapsule(
                    ByteString.copyFromUtf8("owner"),
                    ByteString.copyFrom(ByteArray.fromHexString(FROM_ADDRESS)),
                    AccountType.Normal,
                    OWNER_BALANCE);
    ownerCapsule.addAssetV2(ByteArray.fromString(String.valueOf(tokenId)), OWNER_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  private String getParamsFile(String fileName) {
    return ShieldedReceiveTest.class.getClassLoader()
        .getResource("params" + File.separator + fileName).getFile();
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

  private static byte[] randomUint256() {
    return org.tron.keystore.Wallet.generateRandomBytes(32);
  }

  private static byte[] randomUint640() {
    return org.tron.keystore.Wallet.generateRandomBytes(80);
  }

  private static byte[] randomUint1536() {
    return org.tron.keystore.Wallet.generateRandomBytes(192);
  }

  private static byte[] randomUint4640() {
    return org.tron.keystore.Wallet.generateRandomBytes(580);
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

  private void updateTotalShieldedPoolValue(long valueBalance) {
    long totalShieldedPoolValue = dbManager.getDynamicPropertiesStore().getTotalShieldedPoolValue();
    totalShieldedPoolValue -= valueBalance;
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(totalShieldedPoolValue);
  }

  /**
   * test of change ShieldedTransactionFee proposal
   */
  @Test
  public void testSetShieldedTransactionFee() {
    long fee = wallet.getShieldedTransactionFee();

    dbManager.getDynamicPropertiesStore().saveShieldedTransactionFee(2_000_000);
    Assert.assertEquals(2_000_000, wallet.getShieldedTransactionFee());

    dbManager.getDynamicPropertiesStore().saveShieldedTransactionFee(fee);
  }

  /**
   * test of creating shielded transaction before turn on the switch
   */
  @Test
  public void testCreateBeforeAllowZksnark() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(0);
    createCapsule();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    //generate input
    builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE); //success

    //generate output
    SpendingKey sk = SpendingKey.random();
    FullViewingKey fullViewingKey12 = sk.fullViewingKey();
    IncomingViewingKey ivk = fullViewingKey12.inViewingKey();
    PaymentAddress paymentAddress = ivk.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey12.getOvk(), paymentAddress,
        OWNER_BALANCE - wallet.getShieldedTransactionFee(), new byte[512]); //success

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    //online
    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not support ZKSnarkTransaction, need to be opened by the committee",
          e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of broadcasting shielded transaction before allow shielded transaction
   */
  @Test
  public void testBroadcastBeforeAllowZksnark()
      throws ZksnarkException, SignatureFormatException, SignatureException, PermissionException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(0);// or 1
    createCapsule();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    //generate input
    builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE); //success

    //generate output
    SpendingKey sk = SpendingKey.random();
    FullViewingKey fullViewingKey12 = sk.fullViewingKey();
    IncomingViewingKey ivk = fullViewingKey12.inViewingKey();
    PaymentAddress paymentAddress = ivk.address(new DiversifierT()).get();
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    builder.addOutput(fullViewingKey12.getOvk(), paymentAddress,
        OWNER_BALANCE - fee, new byte[512]); //success

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    //Add public address sign
    TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
    transactionSignBuild.setTransaction(transactionCap.getInstance());
    transactionSignBuild.setPrivateKey(ByteString.copyFrom(
        ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));

    transactionCap = wallet.addSign(transactionSignBuild.build());

    try {
      dbManager.pushTransaction(transactionCap);
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Not support ZKSnarkTransaction, need to be opened by the committee",
          e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  private ZenTransactionBuilder generateBuilderWithoutColumnInDescription(
      ZenTransactionBuilder builder, Pointer ctx, TestReceiveMissingColumn column)
      throws ZksnarkException, BadItemException {
    //transparent input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : builder.getSpends()) {
      SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend, ctx);
      builder.getContractBuilder().addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : builder.getReceives()) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
          .generateOutputProof(receive, ctx);
      switch (column) {
        case CV:
          receiveDescriptionCapsule.setValueCommitment(ByteString.EMPTY);
          break;
        case CM:
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
          break;
        case EPK:
          receiveDescriptionCapsule.setEpk(ByteString.EMPTY);
          break;
        case ZKPROOF:
          receiveDescriptionCapsule.setZkproof(ByteString.EMPTY);
          break;
        case C_ENC:
          receiveDescriptionCapsule.setCEnc(ByteString.EMPTY);
          break;
        case C_OUT:
          receiveDescriptionCapsule.setCOut(ByteString.EMPTY);
          break;
        default:
          break;
      }
      builder.getContractBuilder().addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    return builder;
  }

  /**
   * test of empty CV in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCV()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx, TestReceiveMissingColumn.CV);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      TransactionExtention transactionExtention = TransactionExtention.newBuilder()
          .setTransaction(transactionCapsule.getInstance()).build();

      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of empty cm in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCM()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx, TestReceiveMissingColumn.CM);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();

      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of empty epk in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyEPK()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx, TestReceiveMissingColumn.EPK);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of empty zkproof in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyZkproof()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx,
        TestReceiveMissingColumn.ZKPROOF);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of empty c_enc in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCenc()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx,
        TestReceiveMissingColumn.C_ENC);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cout or CEnc size error", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of empty c_out in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCout()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx,
        TestReceiveMissingColumn.C_OUT);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cout or CEnc size error", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test some column in ReceiveDescription is wrong
   */
  private ReceiveDescriptionCapsule changeGenerateOutputProof(ReceiveDescriptionInfo output,
      Pointer ctx, TestColumn testColumn)
      throws ZksnarkException {
    byte[] cm = output.getNote().cm();
    if (ByteArray.isEmpty(cm)) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Output is invalid");
    }

//    NotePlaintext notePlaintext = new NotePlaintext(output.getNote(), output.getMemo());

    Optional<NotePlaintextEncryptionResult> res = output.getNote()
        .encrypt(output.getNote().pkD);
    if (!res.isPresent()) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Failed to encrypt note");
    }

    NotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.noteEncryption;

    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    if (!Librustzcash.librustzcashSaplingOutputProof(
        new OutputProofParams(ctx,
            encryptor.esk,
            output.getNote().d.data,
            output.getNote().pkD,
            output.getNote().rcm,
            output.getNote().value,
            cv,
            zkProof))) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Output proof failed");
    }

    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.epk);
    receiveDescriptionCapsule.setCEnc(enc.encCiphertext);
    receiveDescriptionCapsule.setZkproof(zkProof);

    OutgoingPlaintext outPlaintext =
        new OutgoingPlaintext(output.getNote().pkD, encryptor.esk);
    receiveDescriptionCapsule.setCOut(outPlaintext
        .encrypt(output.getOvk(), receiveDescriptionCapsule.getValueCommitment().toByteArray(),
            receiveDescriptionCapsule.getCm().toByteArray(),
            encryptor).data);

    Note newNote = output.getNote();
    byte[] newCm;
    switch (testColumn) {
      case CV:
        receiveDescriptionCapsule.setValueCommitment(randomUint256());
        break;
      case ZKPOOF:
        receiveDescriptionCapsule.setZkproof(randomUint1536());
        break;
      case D_CM:
        newNote.d = new DiversifierT().random();
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;
      case PKD_CM:
        newNote.pkD = randomUint256();
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;
      case VALUE_CM:
        newNote.value += 10000;
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;
      case R_CM:
        newNote.rcm = Note.generateR();
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;

      default:
        break;
    }

    return receiveDescriptionCapsule;
  }

  private TransactionCapsule changeBuildOutputProof(ZenTransactionBuilder builder, Pointer ctx,
      TestColumn testColumn)
      throws ZksnarkException {
    ShieldedTransferContract.Builder contractBuilder = builder.getContractBuilder();

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : builder.getSpends()) {
      SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend, ctx);
      contractBuilder.addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : builder.getReceives()) {
      //test case
      ReceiveDescriptionCapsule receiveDescriptionCapsule = changeGenerateOutputProof(receive, ctx,
          testColumn);
      //end of test case
      contractBuilder.addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          contractBuilder.build(), ContractType.ShieldedTransferContract);

      dataToBeSigned = transactionCapsule.hashShieldTransaction(transactionCapsule);
    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth and binding signatures
    builder.createSpendAuth(dataToBeSigned);
    byte[] bindingSig = new byte[64];
    Librustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx,
            builder.getValueBalance(),
            dataToBeSigned,
            bindingSig)
    );
    contractBuilder.setBindingSignature(ByteString.copyFrom(bindingSig));
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);

    Transaction.raw.Builder rawBuilder = transactionCapsule.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(contractBuilder.build())).build());

    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();
    return new TransactionCapsule(transaction);
  }

  private ZenTransactionBuilder generateBuilder(ZenTransactionBuilder builder, Pointer ctx)
      throws ZksnarkException, BadItemException {
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);

    // generate input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    Note note2 = new Note(address, 90 * 1000000);
    builder.addOutput(fullViewingKey1.getOvk(), note2.d, note2.pkD, note2.value, note2.rcm,
        new byte[512]);

    return builder;
  }

  /**
   * test wrong value_commitment in ReceiveDescription
   */
  @Test
  public void testReceiveDescriptionWithWrongCv()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.CV;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckOutput error",e.getMessage());
    }
  }

  /**
   * test wrong zkproof in ReceiveDescription
   */
  @Test
  public void testReceiveDescriptionWithWrongZkproof()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.ZKPOOF;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckOutput error",e.getMessage());
    }
  }

  /**
   * test note_commitment in ReceiveDescription generated by wrong d
   */
  @Test
  public void testReceiveDescriptionWithWrongD()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.D_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckOutput error",e.getMessage());
    }
  }

  /**
   * test note_commitment in ReceiveDescription generated by wrong pkd
   */
  @Test
  public void testReceiveDescriptionWithWrongPkd()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.PKD_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
  }

  /**
   * test note_commitment in ReceiveDescription generated by wrong value
   */
  @Test
  public void testReceiveDescriptionWithWrongValue()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.VALUE_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(e.getMessage().equalsIgnoreCase("librustzcashSaplingCheckOutput error"));
    }
  }

  /**
   * test note_commitment in ReceiveDescription generated by wrong r
   */
  @Test
  public void testReceiveDescriptionWithWrongRcm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.R_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckOutput error", e.getMessage());
    }
  }

  /**
   * use ask or nsk not belongs to sk.
   */
  @Test
  public void testNotMatchAskAndNsk()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // generate sk
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    //generate a note belongs to this sk
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    //test case: give a random Ask or nsk
    expsk.setAsk(randomUint256());
    //expsk.setNsk(randomUint256());
    //end of test case

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());
    try {
      TransactionCapsule transactionCap = builder.build();
      Assert.assertFalse(true);

    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
      Assert.assertEquals("Spend proof failed", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * use random ovk not related to sk.
   */
  @Test
  public void testRandomOvk()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // generate sk
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    //generate a note belongs to this sk
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    //test case: give a random Ovk
    expsk.setOvk(randomUint256());
    //end of test case

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();
    Assert.assertTrue(true);
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test add two same cm into spend
   */
  @Test
  public void testSameInputCm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    //add two same cm
    builder.addSpend(expsk, note, anchor, voucher);
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 190 * 1000000, new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("duplicate sapling nullifiers in this transaction", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test add two same cm into output
   */
  @Test
  public void testSameOutputCm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    Note note2 = new Note(address, 45 * 1000000);
    //add two same output note
    builder.addOutput(fullViewingKey1.getOvk(), note2.d, note2.pkD, note2.value, note2.rcm,
        new byte[512]);
    builder.addOutput(fullViewingKey1.getOvk(), note2.d, note2.pkD, note2.value, note2.rcm,
        new byte[512]);//same output cm

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("duplicate cm in receive_description", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test of transferring insufficient money from shield address to shield address.
   */
  @Test
  public void testShieldInputInsufficient()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    //set value that's bigger than own
    note.value += 10 * 1000000; //test case of insufficient money

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      TransactionCapsule transactionCap = builder.build();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
      Assert.assertEquals("Spend proof failed", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);

  }

  /**
   * test of transferring insufficient money from transparent address to shield address.
   */
  @Test
  public void testTransparentInputInsufficient() throws RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    createCapsule();

    //generate input
    builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), FROM_AMOUNT); // fail
    //builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE); //success

    //generate output
    SpendingKey sk = SpendingKey.random();
    FullViewingKey fullViewingKey12 = sk.fullViewingKey();
    IncomingViewingKey ivk = fullViewingKey12.inViewingKey();
    PaymentAddress paymentAddress = ivk.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey12.getOvk(), paymentAddress,
        FROM_AMOUNT - wallet.getShieldedTransactionFee(), new byte[512]); // fail
    //builder.addOutput(fullViewingKey12.getOvk(), paymentAddress,
    //        OWNER_BALANCE - wallet.getShieldedTransactionFee(), new byte[512]); //success

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    try {
      //valdiate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Validate ShieldedTransferContract error, balance is not sufficient",
          e.getMessage());
    }
  }

  /*
  test if random DiversifierT is valid.
 */
  @Test
  public void testDiversifierT() throws ZksnarkException {
    //byte[] data = org.tron.keystore.Wallet.generateRandomBytes(Constant.ZC_DIVERSIFIER_SIZE);
    byte[] data1 = ByteArray.fromHexString("93c9e5679850252b37a991");
    byte[] data2 = ByteArray.fromHexString("c50124c6a9a2e1700fc0b5");
    Assert.assertFalse(Librustzcash.librustzcashCheckDiversifier(data1));
    Assert.assertTrue(Librustzcash.librustzcashCheckDiversifier(data2));
  }

  private byte[] hashWithMissingColumn(TransactionCapsule tx, TestSignMissingColumn column)
      throws InvalidProtocolBufferException {
    Any contractParameter = tx.getInstance().getRawData().getContract(0).getParameter();
    ShieldedTransferContract shieldedTransferContract = contractParameter
        .unpack(ShieldedTransferContract.class);
    ShieldedTransferContract.Builder newContract = ShieldedTransferContract.newBuilder();

    if (column != null) {
      switch (column) {
        case FROM_ADDRESS:
          newContract.setFromAmount(shieldedTransferContract.getFromAmount());
          newContract
              .addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
          newContract.setToAmount(shieldedTransferContract.getToAmount());
          //newContract.setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
          newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
          for (SpendDescription spendDescription : shieldedTransferContract
              .getSpendDescriptionList()) {
            newContract
                .addSpendDescription(
                    spendDescription.toBuilder().clearSpendAuthoritySignature().build());
          }
          break;
        case FROM_AMOUNT:
          //newContract.setFromAmount(shieldedTransferContract.getFromAmount());
          newContract
              .addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
          newContract.setToAmount(shieldedTransferContract.getToAmount());
          newContract
              .setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
          newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
          for (SpendDescription spendDescription : shieldedTransferContract
              .getSpendDescriptionList()) {
            newContract
                .addSpendDescription(
                    spendDescription.toBuilder().clearSpendAuthoritySignature().build());
          }
          break;
        case SPEND_DESCRITPION:
          newContract.setFromAmount(shieldedTransferContract.getFromAmount());
          newContract
              .addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
          newContract.setToAmount(shieldedTransferContract.getToAmount());
          newContract
              .setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
          newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
          //for (SpendDescription spendDescription : shieldedTransferContract.getSpendDescriptionList()) {
          //  newContract
          //          .addSpendDescription(spendDescription.toBuilder().clearSpendAuthoritySignature().build());
          //}
          break;
        case RECEIVE_DESCRIPTION:
          newContract.setFromAmount(shieldedTransferContract.getFromAmount());
          //newContract.addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
          newContract.setToAmount(shieldedTransferContract.getToAmount());
          newContract
              .setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
          newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
          for (SpendDescription spendDescription : shieldedTransferContract
              .getSpendDescriptionList()) {
            newContract
                .addSpendDescription(
                    spendDescription.toBuilder().clearSpendAuthoritySignature().build());
          }
          break;
        case TO_ADDRESS:
          newContract.setFromAmount(shieldedTransferContract.getFromAmount());
          newContract
              .addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
          newContract.setToAmount(shieldedTransferContract.getToAmount());
          newContract
              .setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
          //newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
          for (SpendDescription spendDescription : shieldedTransferContract
              .getSpendDescriptionList()) {
            newContract
                .addSpendDescription(
                    spendDescription.toBuilder().clearSpendAuthoritySignature().build());
          }
          break;
        case TO_AMOUNT:
          newContract.setFromAmount(shieldedTransferContract.getFromAmount());
          newContract
              .addAllReceiveDescription(shieldedTransferContract.getReceiveDescriptionList());
          //newContract.setToAmount(shieldedTransferContract.getToAmount());
          newContract
              .setTransparentFromAddress(shieldedTransferContract.getTransparentFromAddress());
          newContract.setTransparentToAddress(shieldedTransferContract.getTransparentToAddress());
          for (SpendDescription spendDescription : shieldedTransferContract
              .getSpendDescriptionList()) {
            newContract
                .addSpendDescription(
                    spendDescription.toBuilder().clearSpendAuthoritySignature().build());
          }
          break;
        default:
          break;
      }
    }
    Transaction.raw.Builder rawBuilder = tx.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(
            Transaction.Contract.newBuilder().setType(ContractType.ShieldedTransferContract)
                .setParameter(
                    Any.pack(newContract.build())).build());

    Transaction transaction = tx.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();

    return Sha256Hash.of(transaction.getRawData().toByteArray())
        .getBytes();
  }

  private ZenTransactionBuilder generateShield2ShieldBuilder(ZenTransactionBuilder builder,
      Pointer ctx)
      throws ZksnarkException, BadItemException {
    //generate input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : builder.getSpends()) {
      SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend, ctx);
      builder.getContractBuilder().addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : builder.getReceives()) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
          .generateOutputProof(receive, ctx);
      builder.getContractBuilder().addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    return builder;
  }

  private TransactionCapsule generateTransactionCapsule(ZenTransactionBuilder builder, Pointer ctx,
      byte[] hashOfTransaction, TransactionCapsule transactionCapsule) throws ZksnarkException {
    // Create Sapling spendAuth
    for (int i = 0; i < builder.getSpends().size(); i++) {
      byte[] result = new byte[64];
      Librustzcash.librustzcashSaplingSpendSig(
          new SpendSigParams(builder.getSpends().get(i).expsk.getAsk(),
              builder.getSpends().get(i).alpha,
              hashOfTransaction,
              result));
      builder.getContractBuilder().getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }

    //create binding signatures
    byte[] bindingSig = new byte[64];
    Librustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx,
            builder.getValueBalance(),
            hashOfTransaction,
            bindingSig)
    );
    builder.getContractBuilder().setBindingSignature(ByteString.copyFrom(bindingSig));

    Transaction.raw.Builder rawBuilder = transactionCapsule.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(Transaction.Contract.newBuilder()
            .setType(ContractType.ShieldedTransferContract)
            .setParameter(Any.pack(builder.getContractBuilder().build()))
            .build());

    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();
    TransactionCapsule transactionCap = new TransactionCapsule(transaction);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    return transactionCap;
  }

  /**
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutFromAddress()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateShield2ShieldBuilder(builder, ctx);

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      //test case
      hashOfTransaction = hashWithMissingColumn(transactionCapsule,
          TestSignMissingColumn.FROM_ADDRESS);
      //end of test case

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /**
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutFromAmout()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateShield2ShieldBuilder(builder, ctx);

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      //test case
      hashOfTransaction = hashWithMissingColumn(transactionCapsule,
          TestSignMissingColumn.FROM_AMOUNT);
      //end of test case

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /**
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutSpendDescription()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateShield2ShieldBuilder(builder, ctx);

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      //test case
      hashOfTransaction = hashWithMissingColumn(transactionCapsule,
          TestSignMissingColumn.SPEND_DESCRITPION);
      //end of test case

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckSpend error", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutReceiveDescription()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateShield2ShieldBuilder(builder, ctx);

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      //test case
      hashOfTransaction = hashWithMissingColumn(transactionCapsule,
          TestSignMissingColumn.RECEIVE_DESCRIPTION);
      //end of test case

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckSpend error", e.getMessage());
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /**
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutToAddress()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateShield2ShieldBuilder(builder, ctx);

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      //test case
      hashOfTransaction = hashWithMissingColumn(transactionCapsule,
          TestSignMissingColumn.TO_ADDRESS);
      //end of test case

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /**
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutToAmount()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateShield2ShieldBuilder(builder, ctx);

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      //test case
      hashOfTransaction = hashWithMissingColumn(transactionCapsule,
          TestSignMissingColumn.TO_AMOUNT);
      //end of test case

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /**
   * test spend authorize signature with some wrong column
   */
  @Test
  public void testSpendSignatureWithWrongColumn()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    //shield transparent input
    //createCapsule();
    //builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : builder.getSpends()) {
      SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend, ctx);
      builder.getContractBuilder().addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : builder.getReceives()) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
          .generateOutputProof(receive, ctx);
      builder.getContractBuilder().addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    // Empty output script.
    byte[] hashOfTransaction;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);

      hashOfTransaction = TransactionCapsule.hashShieldTransaction(transactionCapsule);

    } catch (Exception ex) {
      Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth
    for (int i = 0; i < builder.getSpends().size(); i++) {
      byte[] result = new byte[64];
      Librustzcash.librustzcashSaplingSpendSig(
          new SpendSigParams(builder.getSpends().get(i).expsk.getAsk(),
              Note.generateR(), //builder.getSpends().get(i).alpha,
              hashOfTransaction,
              result));
      builder.getContractBuilder().getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }

    //create binding signatures
    byte[] bindingSig = new byte[64];
    Librustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx,
            builder.getValueBalance(),
            hashOfTransaction,
            bindingSig)
    );
    builder.getContractBuilder().setBindingSignature(ByteString.copyFrom(bindingSig));

    Transaction.raw.Builder rawBuilder = transactionCapsule.getInstance().toBuilder()
        .getRawDataBuilder()
        .clearContract()
        .addContract(Transaction.Contract.newBuilder()
            .setType(ContractType.ShieldedTransferContract)
            .setParameter(Any.pack(builder.getContractBuilder().build()))
            .build());

    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearRawData()
        .setRawData(rawBuilder).build();
    TransactionCapsule transactionCap = new TransactionCapsule(transaction);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      //validate
      List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
      actuator.get(0)
          .validate(); //there is getSpendAuthoritySignature in librustzcashSaplingCheckSpend
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      //signature for spend with some wrong column
      //if transparent to shield, ok
      //if shield to shield or shield to transparent, librustzcashSaplingFinalCheck error
      Assert.assertTrue(e.getMessage().equalsIgnoreCase("librustzcashSaplingCheckSpend error"));
    }
    Librustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  @AllArgsConstructor
  class TransactionHash {

    @Setter
    @Getter
    byte[] hash;
  }

  /**
   * test use isolate method to build the signature
   */
  @Test
  public void testIsolateSignature()
          throws ZksnarkException, BadItemException, ContractValidateException, ContractExeException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();
    
    // generate shield spend
    SpendingKey sk = SpendingKey
            .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    
    //build transaction without spend signature and get transactionHash
    TransactionHash transactionHash = new TransactionHash(new byte[32]);
    TransactionCapsule transactionCapsule = buildShieldedTransactionWithoutSpendAuthSig(
            sk.defaultAddress(),
            sk.fullViewingKey().getAk(),
            expsk.getNsk(),
            expsk.getOvk(),
            transactionHash,
            builder,
            ctx);
    //filled with Sapling spendAuth in builder
    List<SpendDescriptionInfo> spends = builder.getSpends();
    for (int i = 0; i < spends.size(); i++) {
      //replace with interface
      SpendAuthSigParameters spendAuthSigParameters = SpendAuthSigParameters.newBuilder()
              .setAsk(ByteString.copyFrom(expsk.getAsk())) //ask => ak
              .setAlpha(ByteString.copyFrom(spends.get(i).alpha))
              .setTxHash(ByteString.copyFrom(transactionHash.getHash()))
              .build();
      BytesMessage spendAuthSig = wallet.createSpendAuthSig(spendAuthSigParameters);
      builder.getContractBuilder().getSpendDescriptionBuilder(i)
              .setSpendAuthoritySignature(spendAuthSig.getValue());
    }
    
    Transaction.raw.Builder rawBuilder = transactionCapsule.getInstance().toBuilder()
            .getRawDataBuilder()
            .clearContract()
            .addContract(Transaction.Contract.newBuilder()
                    .setType(ContractType.ShieldedTransferContract)
                    .setParameter(Any.pack(builder.getContractBuilder().build()))
                    .build());
    Transaction transaction = transactionCapsule.getInstance().toBuilder().clearRawData()
            .setRawData(rawBuilder).build();
    
    TransactionCapsule transactionCap = new TransactionCapsule(transaction);
    //validate
    List<Actuator> actuator = ActuatorFactory.createActuator(transactionCap, dbManager);
    actuator.get(0).validate();
    //execute
    TransactionResultCapsule resultCapsule = new TransactionResultCapsule();
    boolean execute_result = actuator.get(0).execute(resultCapsule);
    Assert.assertTrue(execute_result);
  }
  
  public TransactionCapsule buildShieldedTransactionWithoutSpendAuthSig(PaymentAddress address,
          byte[] ak,
          byte[] nsk, byte[] ovk, TransactionHash dataHashToBeSigned, ZenTransactionBuilder builder,
          Pointer ctx)
          throws ZksnarkException, BadItemException, ContractValidateException {
    // generate input
    Note note = new Note(address, 100 * 1000000);
    note.rcm = ByteArray.
            fromHexString("eb1aa5dd257b9da4e4064a853dec94651be38078e29fe441a9a8075016cfa701");
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    dbManager.getMerkleContainer()
            .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    SpendDescriptionInfo skSpend = new SpendDescriptionInfo(ak,
            nsk,
            ovk,
            note,
            Note.generateR(),
            anchor,
            voucher
    );
    builder.addSpend(skSpend);
    
    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(fullViewingKey1.getOvk(), paymentAddress1, 90 * 1000000, new byte[512]);
    
    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend : builder.getSpends()) {
      SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend, ctx);
      builder.getContractBuilder().addSpendDescription(spendDescriptionCapsule.getInstance());
    }
    
    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : builder.getReceives()) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
              .generateOutputProof(receive, ctx);
      builder.getContractBuilder().addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }
    
    // Empty output script
    TransactionCapsule transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
            builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
    //replace with interface
    //System.out.println(JsonFormat.printToString(transactionCapsule.getInstance()));
    BytesMessage transactionHash = wallet
            .getShieldTransactionHash(transactionCapsule.getInstance());
    
    if (transactionHash == null) {
      throw new ZksnarkException("cal transaction hash failed");
    }
    dataHashToBeSigned.setHash(transactionHash.getValue().toByteArray());
    
    // Create binding signatures
    byte[] bindingSig = new byte[64];
    Librustzcash.librustzcashSaplingBindingSig(
            new BindingSigParams(ctx,
                    builder.getValueBalance(),
                    dataHashToBeSigned.getHash(),
                    bindingSig)
    );
    builder.getContractBuilder().setBindingSignature(ByteString.copyFrom(bindingSig));
    return transactionCapsule;
  }

  @Test
  public void testMemoTooLong()
          throws ContractValidateException, TooBigTransactionException, TooBigTransactionResultException,
          TaposException, TransactionExpirationException, ReceiptCheckErrException,
          DupTransactionException, VMIllegalException, ValidateSignatureException, BadItemException,
          ContractExeException, AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(4010 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate spend proof
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
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(1024);
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, memo);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);
    Assert.assertTrue(ok);

    // add here
    byte[] ivk = fullViewingKey.inViewingKey().getValue();
    Protocol.Transaction t = transactionCap.getInstance();

    for (org.tron.protos.Protocol.Transaction.Contract c : t.getRawData().getContractList()) {
      if (c.getType() != ContractType.ShieldedTransferContract) {
        continue;
      }
      Contract.ShieldedTransferContract stContract = c.getParameter()
              .unpack(Contract.ShieldedTransferContract.class);
      Contract.ReceiveDescription receiveDescription = stContract.getReceiveDescription(0);

      Optional<Note> ret1 = Note.decrypt(
              receiveDescription.getCEnc().toByteArray(),//ciphertext
              ivk,
              receiveDescription.getEpk().toByteArray(),//epk
              receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        Note noteText = ret1.get();
        byte[] pk_d = new byte[32];
        if (!Librustzcash.librustzcashIvkToPkd(
                new LibrustzcashParam.IvkToPkdParams(incomingViewingKey.getValue(), noteText.d.getData(), pk_d))) {
          Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
          return;
        }
        Assert.assertArrayEquals(paymentAddress.getPkD(), pk_d);
        Assert.assertEquals( 4000 * 1000000,noteText.value);

        byte[] resultMemo = new byte[512];
        System.arraycopy(memo,0,resultMemo,0,512);
        Assert.assertArrayEquals(resultMemo, noteText.memo);
      } else {
        Assert.assertFalse(true);
      }
    }
    // end here
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void testMemoNotEnough()
          throws ContractValidateException, TooBigTransactionException, TooBigTransactionResultException,
          TaposException, TransactionExpirationException, ReceiptCheckErrException,
          DupTransactionException, VMIllegalException, ValidateSignatureException, BadItemException,
          ContractExeException, AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    Pointer ctx = Librustzcash.librustzcashSaplingProvingCtxInit();

    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(4010 * 1000000l);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate spend proof
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
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(128);
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 4000 * 1000000, memo);

    TransactionCapsule transactionCap = builder.build();

    boolean ok = dbManager.pushTransaction(transactionCap);
    Assert.assertTrue(ok);

    // add here
    byte[] ivk = fullViewingKey.inViewingKey().getValue();
    Protocol.Transaction t = transactionCap.getInstance();

    for (org.tron.protos.Protocol.Transaction.Contract c : t.getRawData().getContractList()) {
      if (c.getType() != ContractType.ShieldedTransferContract) {
        continue;
      }
      Contract.ShieldedTransferContract stContract = c.getParameter()
              .unpack(Contract.ShieldedTransferContract.class);
      Contract.ReceiveDescription receiveDescription = stContract.getReceiveDescription(0);

      Optional<Note> ret1 = Note.decrypt(
              receiveDescription.getCEnc().toByteArray(),//ciphertext
              ivk,
              receiveDescription.getEpk().toByteArray(),//epk
              receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        Note noteText = ret1.get();
        byte[] pk_d = new byte[32];
        if (!Librustzcash.librustzcashIvkToPkd(
                new LibrustzcashParam.IvkToPkdParams(incomingViewingKey.getValue(), noteText.d.getData(), pk_d))) {
          Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
          return;
        }
        Assert.assertArrayEquals(paymentAddress.getPkD(), pk_d);
        Assert.assertEquals( 4000 * 1000000,noteText.value);

        byte[] resultMemo = new byte[512];
        System.arraycopy(memo,0,resultMemo,0,128);
        Assert.assertArrayEquals(resultMemo, noteText.memo);
      } else {
        Assert.assertFalse(true);
      }
    }
    // end here
    Librustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }
}
