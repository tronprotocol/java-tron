package org.tron.core.zksnark;

import com.google.common.primitives.Bytes;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.ReceiveNote;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.common.zksnark.JLibrustzcash;
import org.tron.common.zksnark.LibrustzcashParam.BindingSigParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckOutputParams;
import org.tron.common.zksnark.LibrustzcashParam.CheckSpendParams;
import org.tron.common.zksnark.LibrustzcashParam.IvkToPkdParams;
import org.tron.common.zksnark.LibrustzcashParam.OutputProofParams;
import org.tron.common.zksnark.LibrustzcashParam.SpendSigParams;
import org.tron.core.ChainBaseManager;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorCreator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.IncrementalMerkleVoucherCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.capsule.ReceiveDescriptionCapsule;
import org.tron.core.capsule.SpendDescriptionCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.BlockGenerate;
import org.tron.core.db.Manager;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.PermissionException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.SignatureFormatException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.ZenTransactionBuilder.ReceiveDescriptionInfo;
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
import org.tron.core.zen.note.OutgoingPlaintext;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionSign;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import org.tron.protos.contract.ShieldContract.PedersenHash;
import org.tron.protos.contract.ShieldContract.ReceiveDescription;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;

@Slf4j
public class ShieldedReceiveTest extends BlockGenerate {

  private static final String dbPath = "receive_description_test";
  private static final String FROM_ADDRESS;
  private static final String ADDRESS_ONE_PRIVATE_KEY;
  private static final long OWNER_BALANCE = 100_000_000;
  private static final long FROM_AMOUNT = 110_000_000;
  private static final long tokenId = 1;
  private static final String ASSET_NAME = "trx";
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static Manager dbManager;
  private static ChainBaseManager chainBaseManager;
  private static ConsensusService consensusService;
  private static TronApplicationContext context;
  private static Wallet wallet;
  private static TransactionUtil transactionUtil;

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
    FileUtil.deleteDir(new File(dbPath));

    wallet = context.getBean(Wallet.class);
    transactionUtil = context.getBean(TransactionUtil.class);
    dbManager = context.getBean(Manager.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    setManager(dbManager);
    consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    //give a big value for pool, avoid for
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(10_000_000_000L);
    // Args.getInstance().setAllowShieldedTransaction(1);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();

    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  private static void librustzcashInitZksnarkParams() {
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createToken() {
    Args.getInstance().setZenTokenId(String.valueOf(tokenId));
    chainBaseManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    chainBaseManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);

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
    chainBaseManager
        .getAssetIssueV2Store().put(assetIssueCapsule.createDbV2Key(), assetIssueCapsule);
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
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
  }

  public IncrementalMerkleVoucherContainer createSimpleMerkleVoucherContainer(byte[] cm)
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
    long totalShieldedPoolValue =
        chainBaseManager.getDynamicPropertiesStore().getTotalShieldedPoolValue();
    totalShieldedPoolValue -= valueBalance;
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(totalShieldedPoolValue);
  }

  /*
   * test of change ShieldedTransactionFee proposal
   */
  @Test
  public void testSetShieldedTransactionFee() {
    long fee = wallet.getShieldedTransactionFee();

    chainBaseManager.getDynamicPropertiesStore().saveShieldedTransactionFee(2_000_000);
    Assert.assertEquals(2_000_000, wallet.getShieldedTransactionFee());

    chainBaseManager.getDynamicPropertiesStore().saveShieldedTransactionFee(fee);
  }

  /*
   * test of creating shielded transaction before turn on the switch
   */
  @Test
  public void testCreateBeforeAllowZksnark() throws ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(0);
    createCapsule();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    //generate input
    builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE); //success
    byte[] senderOvk = randomUint256();

    //generate output
    SpendingKey sk = SpendingKey.random();
    FullViewingKey fullViewingKey12 = sk.fullViewingKey();
    IncomingViewingKey ivk = fullViewingKey12.inViewingKey();
    PaymentAddress paymentAddress = ivk.address(new DiversifierT()).get();
    builder.addOutput(senderOvk, paymentAddress,
        OWNER_BALANCE - wallet.getShieldedTransactionFee(), new byte[512]); //success

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    //online
    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Not support Shielded Transaction, need to be opened by the committee",
          e.getMessage());
    }
  }

  /*
   * test of broadcasting shielded transaction before allow shielded transaction
   */
  @Test
  public void testBroadcastBeforeAllowZksnark()
      throws ZksnarkException, SignatureFormatException, SignatureException, PermissionException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(0);// or 1
    createCapsule();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    //generate input
    builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE); //success
    byte[] senderOvk = randomUint256();

    //generate output
    SpendingKey sk = SpendingKey.random();
    FullViewingKey fullViewingKey12 = sk.fullViewingKey();
    IncomingViewingKey ivk = fullViewingKey12.inViewingKey();
    PaymentAddress paymentAddress = ivk.address(new DiversifierT()).get();
    long fee = chainBaseManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    builder.addOutput(senderOvk, paymentAddress,
        OWNER_BALANCE - fee, new byte[512]); //success

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    //Add public address sign
    TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
    transactionSignBuild.setTransaction(transactionCap.getInstance());
    transactionSignBuild.setPrivateKey(ByteString.copyFrom(
        ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));

    transactionCap = transactionUtil.addSign(transactionSignBuild.build());

    try {
      dbManager.pushTransaction(transactionCap);
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Not support Shielded Transaction, need to be opened by the committee",
          e.getMessage());
    }
  }

  /*
   * generate spendproof, dataToBeSigned, outputproof example dynamicly according to the params file
   */
  public String[] generateSpendAndOutputParams() throws ZksnarkException, BadItemException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    //generate input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000 - wallet.getShieldedTransactionFee(), new byte[512]);

    // Create Sapling SpendDescriptions
    for (SpendDescriptionInfo spend2 : builder.getSpends()) {
      SpendDescriptionCapsule spendDescriptionCapsule = builder.generateSpendProof(spend2, ctx);
      builder.getContractBuilder().addSpendDescription(spendDescriptionCapsule.getInstance());
    }

    // Create Sapling OutputDescriptions
    for (ReceiveDescriptionInfo receive : builder.getReceives()) {
      ReceiveDescriptionCapsule receiveDescriptionCapsule = builder
          .generateOutputProof(receive, ctx);
      builder.getContractBuilder().addReceiveDescription(receiveDescriptionCapsule.getInstance());
    }

    // begin to generate dataToBeSigned
    TransactionCapsule transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
        builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
    byte[] dataToBeSigned = TransactionCapsule
        .getShieldTransactionHashIgnoreTypeException(transactionCapsule.getInstance());
    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    // generate checkSpendParams
    SpendDescription spendDescription = builder.getContractBuilder().getSpendDescription(0);
    CheckSpendParams checkSpendParams = new CheckSpendParams(ctx,
        spendDescription.getValueCommitment().toByteArray(),
        spendDescription.getAnchor().toByteArray(),
        spendDescription.getNullifier().toByteArray(),
        spendDescription.getRk().toByteArray(),
        spendDescription.getZkproof().toByteArray(),
        spendDescription.getSpendAuthoritySignature().toByteArray(),
        dataToBeSigned);

    boolean ok1 = JLibrustzcash.librustzcashSaplingCheckSpend(checkSpendParams);
    Assert.assertTrue(ok1);

    byte[] checkSpendParamsData = new byte[385];
    System.arraycopy(
        checkSpendParams.getCv(), 0, checkSpendParamsData, 0, 32);
    System.arraycopy(
        checkSpendParams.getAnchor(), 0, checkSpendParamsData, 32, 32);
    System.arraycopy(
        checkSpendParams.getNullifier(), 0, checkSpendParamsData, 64, 32);
    System.arraycopy(
        checkSpendParams.getRk(), 0, checkSpendParamsData, 96, 32);
    System.arraycopy(
        checkSpendParams.getZkproof(), 0, checkSpendParamsData, 128, 192);
    System.arraycopy(
        checkSpendParams.getSpendAuthSig(), 0, checkSpendParamsData, 320, 64);

    // generate CheckOutputParams
    ReceiveDescription receiveDescription =
        builder.getContractBuilder().getReceiveDescription(0);
    CheckOutputParams checkOutputParams = new CheckOutputParams(ctx,
        receiveDescription.getValueCommitment().toByteArray(),
        receiveDescription.getNoteCommitment().toByteArray(),
        receiveDescription.getEpk().toByteArray(),
        receiveDescription.getZkproof().toByteArray()
    );

    boolean ok2 = JLibrustzcash.librustzcashSaplingCheckOutput(checkOutputParams);
    Assert.assertTrue(ok2);

    return new String[]{ByteArray.toHexString(checkSpendParamsData),
        ByteArray.toHexString(dataToBeSigned),
        ByteArray.toHexString(checkOutputParams.encode())};
  }

  private long benchmarkVerifySpend(String spend, String dataToBeSigned) throws ZksnarkException {
    long startTime = System.currentTimeMillis();
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    CheckSpendParams checkSpendParams = CheckSpendParams.decode(ctx,
        ByteArray.fromHexString(spend),
        ByteArray.fromHexString(dataToBeSigned));

    boolean ok = JLibrustzcash.librustzcashSaplingCheckSpend(checkSpendParams);

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;

    System.out.println("--- time is: " + time + ", result is " + ok);
    return time;
  }

  private long benchmarkVerifyOutput(String outputParams) throws ZksnarkException {
    // expect true
    long startTime = System.currentTimeMillis();
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    CheckOutputParams checkOutputParams = CheckOutputParams.decode(ctx,
        ByteArray.fromHexString(outputParams));

    boolean ok = JLibrustzcash.librustzcashSaplingCheckOutput(checkOutputParams);

    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);

    long endTime = System.currentTimeMillis();
    long time = endTime - startTime;

    System.out.println("--- time is: " + time + ", result is " + ok);
    return time;
  }

  @Test
  public void calBenchmarkVerifySpend() throws ZksnarkException, BadItemException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 10;
    long minTime = 500;
    long maxTime = 0;
    double totalTime = 0.0;

    String[] result = generateSpendAndOutputParams();
    String spend = result[0];
    String dataToBeSigned = result[1];

    for (int i = 0; i < count; i++) {
      long time = benchmarkVerifySpend(spend, dataToBeSigned);
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
  public void calBenchmarkVerifyOutput() throws ZksnarkException, BadItemException {
    librustzcashInitZksnarkParams();
    System.out.println("--- load ok ---");

    int count = 2;
    long minTime = 500;
    long maxTime = 0;
    double totalTime = 0.0;

    String[] result = generateSpendAndOutputParams();
    String outputParams = result[2];

    for (int i = 0; i < count; i++) {
      long time = benchmarkVerifyOutput(outputParams);
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

  private ZenTransactionBuilder generateBuilderWithoutColumnInDescription(
      ZenTransactionBuilder builder, long ctx, TestReceiveMissingColumn column)
      throws ZksnarkException, BadItemException {
    //generate input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000 - wallet.getShieldedTransactionFee(), new byte[512]);

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

  /*
   * test of empty CV in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCv()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx, TestReceiveMissingColumn.CV);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      TransactionExtention transactionExtention = TransactionExtention.newBuilder()
          .setTransaction(transactionCapsule.getInstance()).build();

      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator
          .getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test of empty cm in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx, TestReceiveMissingColumn.CM);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();

      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test of empty epk in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyEpk()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx, TestReceiveMissingColumn.EPK);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test of empty zkproof in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyZkproof()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx,
        TestReceiveMissingColumn.ZKPROOF);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("param is null", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test of empty c_enc in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCenc()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx,
        TestReceiveMissingColumn.C_ENC);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cout or CEnc size error", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test of empty c_out in receive description
   */
  @Test
  public void testReceiveDescriptionWithEmptyCout()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilderWithoutColumnInDescription(builder, ctx,
        TestReceiveMissingColumn.C_OUT);

    // Empty output script.
    byte[] dataToBeSigned;//256
    TransactionCapsule transactionCapsule;
    try {
      transactionCapsule = wallet.createTransactionCapsuleWithoutValidate(
          builder.getContractBuilder().build(), ContractType.ShieldedTransferContract);
      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, dataToBeSigned,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Cout or CEnc size error", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test some column in ReceiveDescription is wrong
   */
  private ReceiveDescriptionCapsule changeGenerateOutputProof(ReceiveDescriptionInfo output,
      long ctx, TestColumn testColumn)
      throws ZksnarkException {
    byte[] cm = output.getNote().cm();
    if (ByteArray.isEmpty(cm)) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Output is invalid");
    }

    Optional<NotePlaintextEncryptionResult> res = output.getNote()
        .encrypt(output.getNote().getPkD());
    if (!res.isPresent()) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Failed to encrypt note");
    }

    NotePlaintextEncryptionResult enc = res.get();
    NoteEncryption encryptor = enc.getNoteEncryption();

    byte[] cv = new byte[32];
    byte[] zkProof = new byte[192];
    if (!JLibrustzcash.librustzcashSaplingOutputProof(
        new OutputProofParams(ctx,
            encryptor.getEsk(),
            output.getNote().getD().getData(),
            output.getNote().getPkD(),
            output.getNote().getRcm(),
            output.getNote().getValue(),
            cv,
            zkProof))) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Output proof failed");
    }

    ReceiveDescriptionCapsule receiveDescriptionCapsule = new ReceiveDescriptionCapsule();
    receiveDescriptionCapsule.setValueCommitment(cv);
    receiveDescriptionCapsule.setNoteCommitment(cm);
    receiveDescriptionCapsule.setEpk(encryptor.getEpk());
    receiveDescriptionCapsule.setCEnc(enc.getEncCiphertext());
    receiveDescriptionCapsule.setZkproof(zkProof);

    OutgoingPlaintext outPlaintext =
        new OutgoingPlaintext(output.getNote().getPkD(), encryptor.getEsk());
    receiveDescriptionCapsule.setCOut(outPlaintext
        .encrypt(output.getOvk(), receiveDescriptionCapsule.getValueCommitment().toByteArray(),
            receiveDescriptionCapsule.getCm().toByteArray(),
            encryptor).getData());

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
        newNote.setD(DiversifierT.random());
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;
      case PKD_CM:
        newNote.setPkD(randomUint256());
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;
      case VALUE_CM:
        newNote.setValue(newNote.getValue() + 10000);
        newCm = newNote.cm();
        if (newCm == null) {
          receiveDescriptionCapsule.setNoteCommitment(ByteString.EMPTY);
        } else {
          receiveDescriptionCapsule.setNoteCommitment(newCm);
        }
        break;
      case R_CM:
        newNote.setRcm(Note.generateR());
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

  private TransactionCapsule changeBuildOutputProof(ZenTransactionBuilder builder, long ctx,
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

      dataToBeSigned = TransactionCapsule.hashShieldTransaction(transactionCapsule.getInstance(),
          CommonParameter.getInstance().getZenTokenId());
    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new ZksnarkException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth and binding signatures
    builder.createSpendAuth(dataToBeSigned);
    byte[] bindingSig = new byte[64];
    JLibrustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx,
            builder.getValueBalance(),
            dataToBeSigned,
            bindingSig)
    );
    contractBuilder.setBindingSignature(ByteString.copyFrom(bindingSig));
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);

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

  private ZenTransactionBuilder generateBuilder(ZenTransactionBuilder builder, long ctx)
      throws ZksnarkException, BadItemException {
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);

    // generate input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    Note note2 = new Note(paymentAddress1, 100 * 1000000L - wallet.getShieldedTransactionFee());
    builder
        .addOutput(expsk.getOvk(), note2.getD(), note2.getPkD(), note2.getValue(), note2.getRcm(),
            new byte[512]);

    return builder;
  }

  /*
   * test wrong value_commitment in ReceiveDescription
   */
  @Test
  public void testReceiveDescriptionWithWrongCv()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.CV;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckOutput error", e.getMessage());
    }
  }

  /*
   * test wrong zkproof in ReceiveDescription
   */
  @Test
  public void testReceiveDescriptionWithWrongZkproof()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.ZKPOOF;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckOutput error", e.getMessage());
    }
  }

  /*
   * test note_commitment in ReceiveDescription generated by wrong d
   */
  @Test
  public void testReceiveDescriptionWithWrongD()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.D_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      //if generate cm successful, checkout error; else param is null because of cm is null
      Assert.assertTrue("librustzcashSaplingCheckOutput error".equalsIgnoreCase(e.getMessage())
          || "param is null".equalsIgnoreCase(e.getMessage()));
    }
  }

  /*
   * test note_commitment in ReceiveDescription generated by wrong pkd
   */
  @Test
  public void testReceiveDescriptionWithWrongPkd()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.PKD_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      //if generate cm successful, checkout error; else param is null because of cm is null
      Assert.assertTrue("librustzcashSaplingCheckOutput error".equalsIgnoreCase(e.getMessage())
          || "param is null".equalsIgnoreCase(e.getMessage()));
    }
  }

  /*
   * test note_commitment in ReceiveDescription generated by wrong value
   */
  @Test
  public void testReceiveDescriptionWithWrongValue()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.VALUE_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertTrue(e.getMessage().equalsIgnoreCase("librustzcashSaplingCheckOutput error"));
    }
  }

  /*
   * test note_commitment in ReceiveDescription generated by wrong r
   */
  @Test
  public void testReceiveDescriptionWithWrongRcm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    builder = generateBuilder(builder, ctx);

    TestColumn testColumn = TestColumn.R_CM;
    TransactionCapsule transactionCap = changeBuildOutputProof(builder, ctx, testColumn);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
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
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate sk
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    //generate a note belongs to this sk
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    chainBaseManager.getMerkleContainer()
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
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());
    try {
      TransactionCapsule transactionCap = builder.build();
      Assert.assertFalse(true);

    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
      Assert.assertEquals("Spend proof failed", e.getMessage());
    }
  }

  /**
   * use random ovk not related to sk.
   */
  @Test
  public void testRandomOvk()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate sk
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();

    //generate a note belongs to this sk
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    chainBaseManager.getMerkleContainer()
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
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();
    Assert.assertTrue(true);
  }

  /*
   * test add two same cm into spend
   */
  //@Test not used
  public void testSameInputCm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    //add two same cm
    builder.addSpend(expsk, note, anchor, voucher);
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        200 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("duplicate sapling nullifiers in this transaction", e.getMessage());
    }
  }

  /*
   * test add two same cm into output
   */
  @Test
  public void testSameOutputCm()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    //put the voucher and anchor into db
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    Note note2 = new Note(address, (100 * 1000000L - wallet.getShieldedTransactionFee()) / 2);
    //add two same output note
    builder
        .addOutput(expsk.getOvk(), note2.getD(), note2.getPkD(), note2.getValue(), note2.getRcm(),
            new byte[512]);
    builder
        .addOutput(expsk.getOvk(), note2.getD(), note2.getPkD(), note2.getValue(), note2.getRcm(),
            new byte[512]);//same output cm

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("duplicate cm in receive_description", e.getMessage());
    }
  }

  /**
   * test of transferring insufficient money from shield address to shield address.
   */
  @Test
  public void testShieldInputInsufficient()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());

    //set value that's bigger than own
    note.setValue(note.getValue() + 10 * 1000000L); //test case of insufficient money

    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

    updateTotalShieldedPoolValue(builder.getValueBalance());

    try {
      TransactionCapsule transactionCap = builder.build();
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
      Assert.assertEquals("Spend proof failed", e.getMessage());
    }
  }

  /**
   * test of transferring insufficient money from transparent address to shield address.
   */
  @Test
  public void testTransparentInputInsufficient() throws RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();
    createCapsule();

    //generate input
    builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), FROM_AMOUNT); // fail
    //builder.setTransparentInput(ByteArray.fromHexString(FROM_ADDRESS), OWNER_BALANCE); //success
    byte[] senderOvk = randomUint256();

    //generate output
    SpendingKey sk = SpendingKey.random();
    FullViewingKey fullViewingKey12 = sk.fullViewingKey();
    IncomingViewingKey ivk = fullViewingKey12.inViewingKey();
    PaymentAddress paymentAddress = ivk.address(new DiversifierT()).get();
    builder.addOutput(senderOvk, paymentAddress,
        FROM_AMOUNT - wallet.getShieldedTransactionFee(), new byte[512]); // fail
    //builder.addOutput(senderOvk, paymentAddress,
    //        OWNER_BALANCE - wallet.getShieldedTransactionFee(), new byte[512]); //success

    updateTotalShieldedPoolValue(builder.getValueBalance());
    TransactionCapsule transactionCap = builder.build();

    try {
      //valdiate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
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
    Assert.assertFalse(JLibrustzcash.librustzcashCheckDiversifier(data1));
    Assert.assertTrue(JLibrustzcash.librustzcashCheckDiversifier(data2));
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
          break;
        case RECEIVE_DESCRIPTION:
          newContract.setFromAmount(shieldedTransferContract.getFromAmount());
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

    byte[] mergedByte = Bytes.concat(
        Sha256Hash.of(
            CommonParameter
                .getInstance().isECKeyCryptoEngine(),
            CommonParameter.getInstance().getZenTokenId().getBytes()).getBytes(),
        transaction.getRawData().toByteArray());
    return Sha256Hash.of(CommonParameter
        .getInstance().isECKeyCryptoEngine(), mergedByte).getBytes();
  }

  private ZenTransactionBuilder generateShield2ShieldBuilder(ZenTransactionBuilder builder,
      long ctx)
      throws ZksnarkException, BadItemException {
    //generate input
    SpendingKey sk = SpendingKey.random();
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output
    SpendingKey sk1 = SpendingKey.random();
    FullViewingKey fullViewingKey1 = sk1.fullViewingKey();
    IncomingViewingKey ivk1 = fullViewingKey1.inViewingKey();
    PaymentAddress paymentAddress1 = ivk1.address(new DiversifierT()).get();
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

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

  public TransactionCapsule generateTransactionCapsule(ZenTransactionBuilder builder,
      long ctx, byte[] hashOfTransaction, TransactionCapsule transactionCapsule)
      throws ZksnarkException {
    // Create Sapling spendAuth
    for (int i = 0; i < builder.getSpends().size(); i++) {
      byte[] result = new byte[64];
      JLibrustzcash.librustzcashSaplingSpendSig(
          new SpendSigParams(builder.getSpends().get(i).getExpsk().getAsk(),
              builder.getSpends().get(i).getAlpha(),
              hashOfTransaction,
              result));
      builder.getContractBuilder().getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }

    //create binding signatures
    byte[] bindingSig = new byte[64];
    JLibrustzcash.librustzcashSaplingBindingSig(
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

  /*
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutFromAddress()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /*
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutFromAmout()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /*
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutSpendDescription()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckSpend error", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutReceiveDescription()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    try {
      //validate
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
      actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
      Assert.assertFalse(true);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingCheckSpend error", e.getMessage());
    }
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutToAddress()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /*
   * test signature for shield to shield transaction with some column missing
   */
  @Test
  public void testSignWithoutToAmount()
      throws BadItemException, ContractValidateException, RuntimeException,
      ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    TransactionCapsule transactionCap = generateTransactionCapsule(builder, ctx, hashOfTransaction,
        transactionCapsule);

    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate(); //there is hash(transaction) in librustzcashSaplingFinalCheck
    Assert.assertTrue(true);
  }

  /*
   * test spend authorize signature with some wrong column
   */
  @Test
  public void testSpendSignatureWithWrongColumn()
      throws BadItemException, RuntimeException, ZksnarkException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    // generate input
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
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
    builder.addOutput(expsk.getOvk(), paymentAddress1,
        100 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

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

      hashOfTransaction = TransactionCapsule
          .hashShieldTransaction(transactionCapsule.getInstance(),
              CommonParameter.getInstance().getZenTokenId());

    } catch (Exception ex) {
      JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
      throw new RuntimeException("Could not construct signature hash: " + ex.getMessage());
    }

    // Create Sapling spendAuth
    for (int i = 0; i < builder.getSpends().size(); i++) {
      byte[] result = new byte[64];
      JLibrustzcash.librustzcashSaplingSpendSig(
          new SpendSigParams(builder.getSpends().get(i).getExpsk().getAsk(),
              Note.generateR(),
              //replace builder.getSpends().get(i).alpha with random alpha, should fail
              hashOfTransaction,
              result));
      builder.getContractBuilder().getSpendDescriptionBuilder(i)
          .setSpendAuthoritySignature(ByteString.copyFrom(result));
    }

    //create binding signatures
    byte[] bindingSig = new byte[64];
    JLibrustzcash.librustzcashSaplingBindingSig(
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
      List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
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
    JLibrustzcash.librustzcashSaplingVerificationCtxFree(ctx);
  }

  /*
   * test use isolate method to build the signature
   */
  @Test
  public void testIsolateSignature()
      throws ZksnarkException, BadItemException, ContractValidateException, ContractExeException {
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

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
          .setAlpha(ByteString.copyFrom(spends.get(i).getAlpha()))
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
    List<Actuator> actuator = ActuatorCreator.getINSTANCE().createActuator(transactionCap);
    actuator.get(0).validate();
    //execute
    TransactionResultCapsule resultCapsule = new TransactionResultCapsule();
    boolean executeResult = actuator.get(0).execute(resultCapsule);
    Assert.assertTrue(executeResult);
  }

  /*
   * build ShieldedTransaction Without SpendAuthSig
   */
  public TransactionCapsule buildShieldedTransactionWithoutSpendAuthSig(PaymentAddress address,
      byte[] ak,
      byte[] nsk, byte[] ovk, TransactionHash dataHashToBeSigned, ZenTransactionBuilder builder,
      long ctx)
      throws ZksnarkException, BadItemException, ContractValidateException {
    // generate input
    Note note = new Note(address, 100 * 1000000L);
    note.setRcm(ByteArray.fromHexString(
        "eb1aa5dd257b9da4e4064a853dec94651be38078e29fe441a9a8075016cfa701"));
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
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
    builder.addOutput(ovk, paymentAddress1,
        100 * 1000000L - wallet.getShieldedTransactionFee(), new byte[512]);

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
    JLibrustzcash.librustzcashSaplingBindingSig(
        new BindingSigParams(ctx,
            builder.getValueBalance(),
            dataHashToBeSigned.getHash(),
            bindingSig)
    );
    builder.getContractBuilder().setBindingSignature(ByteString.copyFrom(bindingSig));
    return transactionCapsule;
  }

  @Test
  public void testMemoTooLong() throws ContractValidateException, TooBigTransactionException,
      TooBigTransactionResultException, TaposException, TransactionExpirationException,
      ReceiptCheckErrException, DupTransactionException, VMIllegalException,
      ValidateSignatureException, BadItemException, ContractExeException,
      AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate spend proof
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(1024);
    builder.addOutput(expsk.getOvk(), paymentAddress,
        100 * 1000000L - wallet.getShieldedTransactionFee(), memo);

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
      ShieldedTransferContract stContract = c.getParameter()
          .unpack(ShieldedTransferContract.class);
      ReceiveDescription receiveDescription = stContract.getReceiveDescription(0);

      Optional<Note> ret1 = Note.decrypt(
          receiveDescription.getCEnc().toByteArray(),//ciphertext
          ivk,
          receiveDescription.getEpk().toByteArray(),//epk
          receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        Note noteText = ret1.get();
        byte[] pkD = new byte[32];
        if (!JLibrustzcash.librustzcashIvkToPkd(
            new IvkToPkdParams(incomingViewingKey.getValue(),
                noteText.getD().getData(), pkD))) {
          JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
          return;
        }
        Assert.assertArrayEquals(paymentAddress.getPkD(), pkD);
        Assert.assertEquals(100 * 1000000L - wallet.getShieldedTransactionFee(),
            noteText.getValue());

        byte[] resultMemo = new byte[512];
        System.arraycopy(memo, 0, resultMemo, 0, 512);
        Assert.assertArrayEquals(resultMemo, noteText.getMemo());
      } else {
        Assert.assertFalse(true);
      }
    }
    // end here
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  @Test
  public void testMemoNotEnough() throws ContractValidateException, TooBigTransactionException,
      TooBigTransactionResultException, TaposException, TransactionExpirationException,
      ReceiptCheckErrException, DupTransactionException, VMIllegalException,
      ValidateSignatureException, BadItemException, ContractExeException,
      AccountResourceInsufficientException, InvalidProtocolBufferException, ZksnarkException {
    long ctx = JLibrustzcash.librustzcashSaplingProvingCtxInit();

    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(100 * 1000000L);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);

    // generate spend proof
    SpendingKey sk = SpendingKey
        .decode("ff2c06269315333a9207f817d2eca0ac555ca8f90196976324c7756504e7c9ee");
    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
    PaymentAddress address = sk.defaultAddress();
    Note note = new Note(address, 100 * 1000000L);
    IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
    byte[] anchor = voucher.root().getContent().toByteArray();
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT()).get();
    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(128);
    builder.addOutput(expsk.getOvk(), paymentAddress,
        100 * 1000000L - wallet.getShieldedTransactionFee(), memo);

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
      ShieldedTransferContract stContract = c.getParameter()
          .unpack(ShieldedTransferContract.class);
      ReceiveDescription receiveDescription = stContract.getReceiveDescription(0);

      Optional<Note> ret1 = Note.decrypt(
          receiveDescription.getCEnc().toByteArray(),//ciphertext
          ivk,
          receiveDescription.getEpk().toByteArray(),//epk
          receiveDescription.getNoteCommitment().toByteArray() //cm
      );

      if (ret1.isPresent()) {
        Note noteText = ret1.get();
        byte[] pkD = new byte[32];
        if (!JLibrustzcash.librustzcashIvkToPkd(
            new IvkToPkdParams(incomingViewingKey.getValue(),
                noteText.getD().getData(), pkD))) {
          JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
          return;
        }
        Assert.assertArrayEquals(paymentAddress.getPkD(), pkD);
        Assert.assertEquals(100 * 1000000L - wallet.getShieldedTransactionFee(),
            noteText.getValue());

        byte[] resultMemo = new byte[512];
        System.arraycopy(memo, 0, resultMemo, 0, 128);
        Assert.assertArrayEquals(resultMemo, noteText.getMemo());
      } else {
        Assert.assertFalse(true);
      }
    }
    // end here
    JLibrustzcash.librustzcashSaplingProvingCtxFree(ctx);
    Assert.assertTrue(ok);
  }

  /*
  send coin to 2 different address generated by same sk, scan note by ivk; spend this note again
   */
  @Test
  public void pushSameSkAndScanAndSpend() throws Exception {

    byte[] privateKey = ByteArray
        .fromHexString("f4df789d3210ac881cb900464dd30409453044d2777060a0c391cbdf4c6a4f57");
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] witnessAddress = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
    chainBaseManager.addWitness(ByteString.copyFrom(witnessAddress));

    //sometimes generate block failed, try several times.

    Block block = getSignedBlock(witnessCapsule.getAddress(), 0, privateKey);
    dbManager.pushBlock(new BlockCapsule(block));

    //create transactions
    librustzcashInitZksnarkParams();
    chainBaseManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    chainBaseManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(1000 * 1000000L);
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
    chainBaseManager.getMerkleContainer()
        .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
    builder.addSpend(expsk, note, anchor, voucher);

    // generate output proof
    SpendingKey sk2 = SpendingKey.random();
    FullViewingKey fullViewingKey = sk2.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();

    byte[] memo = org.tron.keystore.Wallet.generateRandomBytes(512);

    //send coin to 2 different address generated by same sk
    DiversifierT d1 = DiversifierT.random();
    PaymentAddress paymentAddress1 = incomingViewingKey.address(d1).get();
    builder.addOutput(senderOvk, paymentAddress1,
        (1000 * 1000000L - wallet.getShieldedTransactionFee()) / 2, memo);

    DiversifierT d2 = DiversifierT.random();
    PaymentAddress paymentAddress2 = incomingViewingKey.address(d2).get();
    builder.addOutput(senderOvk, paymentAddress2,
        (1000 * 1000000L - wallet.getShieldedTransactionFee()) / 2, memo);

    TransactionCapsule transactionCap = builder.build();

    byte[] trxId = transactionCap.getTransactionId().getBytes();
    boolean ok = dbManager.pushTransaction(transactionCap);
    Assert.assertTrue(ok);

    Thread.sleep(500);
    //package transaction to block
    block = getSignedBlock(witnessCapsule.getAddress(), 0, privateKey);
    dbManager.pushBlock(new BlockCapsule(block));

    BlockCapsule blockCapsule3 = new BlockCapsule(wallet.getNowBlock());
    Assert.assertEquals("blocknum != 2", 2, blockCapsule3.getNum());

    // scan note by ivk
    byte[] receiverIvk = incomingViewingKey.getValue();
    DecryptNotes notes1 = wallet.scanNoteByIvk(0, 100, receiverIvk);
    Assert.assertEquals(2, notes1.getNoteTxsCount());

    // scan note by ovk
    DecryptNotes notes2 = wallet.scanNoteByOvk(0, 100, senderOvk);
    Assert.assertEquals(2, notes2.getNoteTxsCount());

    // to spend received note above.
    ZenTransactionBuilder builder2 = new ZenTransactionBuilder(wallet);

    //query merkleinfo
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    for (int i = 0; i < notes1.getNoteTxsCount(); i++) {
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(trxId));
      outPointBuild.setIndex(i);
      request.addOutPoints(outPointBuild.build());
    }
    IncrementalMerkleVoucherInfo merkleVoucherInfo = wallet
        .getMerkleTreeVoucherInfo(request.build());

    //build spend proof. allow only one note in spend
    ExpandedSpendingKey expsk2 = sk2.expandedSpendingKey();
    for (int i = 0; i < 1; i++) {
      org.tron.api.GrpcAPI.Note grpcNote = notes1.getNoteTxs(i).getNote();
      PaymentAddress paymentAddress = KeyIo.decodePaymentAddress(grpcNote.getPaymentAddress());
      Note note2 = new Note(paymentAddress.getD(),
          paymentAddress.getPkD(),
          grpcNote.getValue(),
          grpcNote.getRcm().toByteArray()
      );

      IncrementalMerkleVoucherContainer voucher2 =
          new IncrementalMerkleVoucherContainer(
              new IncrementalMerkleVoucherCapsule(merkleVoucherInfo.getVouchers(i)));
      byte[] anchor2 = voucher2.root().getContent().toByteArray();
      builder2.addSpend(expsk2, note2, anchor2, voucher2);
    }

    //build output proof
    SpendingKey sk3 = SpendingKey.random();
    FullViewingKey fvk3 = sk3.fullViewingKey();
    IncomingViewingKey ivk3 = fvk3.inViewingKey();

    DiversifierT d3 = DiversifierT.random();
    PaymentAddress paymentAddress3 = incomingViewingKey.address(d3).get();
    byte[] memo3 = org.tron.keystore.Wallet.generateRandomBytes(512);
    builder2.addOutput(expsk2.getOvk(), paymentAddress3,
        (1000 * 1000000L - wallet.getShieldedTransactionFee()) / 2 - wallet
            .getShieldedTransactionFee(), memo3);

    TransactionCapsule transactionCap2 = builder2.build();
    boolean ok2 = dbManager.pushTransaction(transactionCap2);
    Assert.assertTrue(ok2);
  }

  @Test
  public void decodePaymentAddressIgnoreCase() {
    String addressLower =
        "ztron1975m0wyg8f30cgf2l5fgndhzqzkzgkgnxge8cwx2wr7m3q7chsuwewh2e6u24yykum0hq8ue99u";
    String addressUpper = addressLower.toUpperCase();

    PaymentAddress paymentAddress1 = KeyIo.decodePaymentAddress(addressLower);
    PaymentAddress paymentAddress2 = KeyIo.decodePaymentAddress(addressUpper);

    Assert.assertEquals(ByteArray.toHexString(paymentAddress1.getD().getData()),
        ByteArray.toHexString(paymentAddress2.getD().getData()));
    Assert.assertEquals(ByteArray.toHexString(paymentAddress1.getPkD()),
        ByteArray.toHexString(paymentAddress2.getPkD()));

  }

  @Test
  public void testCreateReceiveNoteRandom() throws ZksnarkException, BadItemException {
    ReceiveNote receiveNote1 = wallet.createReceiveNoteRandom(0);
    Assert.assertEquals(0, receiveNote1.getNote().getValue());

    ReceiveNote receiveNote2 = wallet.createReceiveNoteRandom(0);
    Assert.assertNotEquals(receiveNote1.getNote().getPaymentAddress(),
        receiveNote2.getNote().getPaymentAddress());
  }

  public enum TestColumn {
    CV,
    ZKPOOF,
    D_CM,
    PKD_CM,
    VALUE_CM,
    R_CM
  }

  public enum TestSignMissingColumn {
    FROM_ADDRESS, FROM_AMOUNT, SPEND_DESCRITPION,
    RECEIVE_DESCRIPTION, TO_ADDRESS, TO_AMOUNT
  }

  public enum TestReceiveMissingColumn {
    CV, CM, EPK, C_ENC, C_OUT, ZKPROOF
  }

  @AllArgsConstructor
  class TransactionHash {

    @Setter
    @Getter
    byte[] hash;
  }

}
