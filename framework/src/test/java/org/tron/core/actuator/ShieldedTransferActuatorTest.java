package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer;
import org.tron.common.zksnark.IncrementalMerkleVoucherContainer;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.PermissionException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.utils.TransactionUtil;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.note.Note;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.TransactionSign;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.ShieldContract.PedersenHash;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;

@Slf4j
public class ShieldedTransferActuatorTest {

  private static final String dbPath = "output_shield_transfer_test";
  private static final String PUBLIC_ADDRESS_ONE;
  private static final String ADDRESS_ONE_PRIVATE_KEY;
  private static final String PUBLIC_ADDRESS_TWO;
  private static final String ADDRESS_TWO_PRIVATE_KEY;
  private static final String PUBLIC_ADDRESS_OFF_LINE;
  private static final long AMOUNT = 100000000L;
  private static final long OWNER_BALANCE = 9999999000000L;
  private static final long TO_BALANCE = 100001000000L;
  private static final String INVAILID_ADDRESS = "aaaa";
  private static final byte[] DEFAULT_OVK;
  private static final long tokenId = 1;
  private static final String ASSET_NAME = "trx";
  private static final int TRX_NUM = 10;
  private static final int NUM = 1;
  private static final long START_TIME = 1;
  private static final long END_TIME = 2;
  private static final int VOTE_SCORE = 2;
  private static final String DESCRIPTION = "TRX";
  private static final String URL = "https://tron.network";
  private static Wallet wallet;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static TransactionUtil transactionUtil;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    PUBLIC_ADDRESS_ONE =
        Wallet.getAddressPreFixString() + "a7d8a35b260395c14aa456297662092ba3b76fc0";
    ADDRESS_ONE_PRIVATE_KEY = "7f7f701e94d4f1dd60ee5205e7ea8ee31121427210417b608a6b2e96433549a7";
    PUBLIC_ADDRESS_TWO =
        Wallet.getAddressPreFixString() + "8ba2aaae540c642e44e3bed5522c63bbc21fff92";
    ADDRESS_TWO_PRIVATE_KEY = "e4e0edd6bff7b353dfc69a590721e902e6915c5e3e87d36dcb567a9716304720";
    PUBLIC_ADDRESS_OFF_LINE =
        Wallet.getAddressPreFixString() + "7bcb781f4743afaacf9f9528f3ea903b3782339f";
    DEFAULT_OVK = ByteArray.fromHexString(
        "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() throws ZksnarkException {
    Args.setFullNodeAllowShieldedTransaction(true);
    wallet = context.getBean(Wallet.class);
    transactionUtil = context.getBean(TransactionUtil.class);
    dbManager = context.getBean(Manager.class);
    librustzcashInitZksnarkParams();
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

  private static void librustzcashInitZksnarkParams() throws ZksnarkException {
    FullNodeHttpApiService.librustzcashInitZksnarkParams();
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    Args.getInstance().setZenTokenId(String.valueOf(tokenId));
    dbManager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
    dbManager.getDynamicPropertiesStore().saveTokenIdNum(tokenId);

    AssetIssueContract assetIssueContract =
        AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE)))
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

    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE)),
            AccountType.Normal,
            OWNER_BALANCE);
    ownerCapsule.addAssetV2(ByteArray.fromString(String.valueOf(tokenId)), OWNER_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);

    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private TransactionCapsule getPublicToShieldedTransaction() throws Exception {
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //From amount
    builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), AMOUNT);
    //TO amount
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);

    return builder.build();
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

  private long getAssertBalance(AccountCapsule accountCapsule) {
    String token = String.valueOf(tokenId);
    if (accountCapsule != null && accountCapsule.getAssetMapV2().containsKey(token)) {
      return accountCapsule.getAssetMapV2().get(token);
    } else {
      return 0;
    }
  }

  private void setAssertBalance(AccountCapsule accountCapsule, long amount) {
    String token = String.valueOf(tokenId);
    if (accountCapsule != null && amount >= 0) {
      long currentBalance = getAssertBalance(accountCapsule);
      if (currentBalance > amount) {
        accountCapsule.reduceAssetAmountV2(token.getBytes(), (currentBalance - amount),
            dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore());
      } else {
        accountCapsule.addAssetAmountV2(token.getBytes(), (amount - currentBalance),
            dbManager.getDynamicPropertiesStore(), dbManager.getAssetIssueStore());
      }
      dbManager.getAccountStore().put(accountCapsule.getAddress().toByteArray(), accountCapsule);
    }
  }

  private void addZeroValueOutputNote(ZenTransactionBuilder builder) throws Exception {
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(DEFAULT_OVK, paymentAddress, 0, "just for decode for ovk".getBytes());
  }

  /**
   * From public address to shielded Address success
   */
  @Test
  public void publicAddressToShieldedAddressSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCap = transactionUtil.addSign(transactionSignBuild.build());

      Assert.assertTrue(dbManager.pushTransaction(transactionCap));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    }
  }


  /**
   * public address to public address + zero value shieldAddress
   */
  @Test
  public void publicAddressToPublicAddressAndZereValueOutputSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), AMOUNT);
      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT - fee);
      TransactionCapsule transactionCap = builder.build();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCap = transactionUtil.addSign(transactionSignBuild.build());

      Assert.assertTrue(dbManager.pushTransaction(transactionCap));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /**
   * Account A send coin to shiled address, but sign transaction with Account B
   */
  @Test
  public void publicAddressToShieldedAddressInvalidSign() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_TWO_PRIVATE_KEY)));
      transactionUtil.addSign(transactionSignBuild.build());
      Assert.assertTrue(false);
    } catch (PermissionException e) {
      Assert.assertTrue(e instanceof PermissionException);
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * no public sign
   */
  @Test
  public void publicAddressToPublicAddressNoPublicSign() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      Assert.assertTrue(dbManager.pushTransaction(transactionCap));
    } catch (ValidateSignatureException e) {
      Assert.assertTrue(e instanceof ValidateSignatureException);
      Assert.assertEquals("miss sig or contract", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * invalid fee
   */
  @Test
  public void publicAddressToShieldedInvalidFee() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      //change fee to build transaction
      dbManager.getDynamicPropertiesStore().saveShieldedTransactionFee(fee - 1000000);
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      dbManager.getDynamicPropertiesStore().saveShieldedTransactionFee(fee);

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingFinalCheck error", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * Insufficient balance
   */
  @Test
  public void publicAddressToShieldedInsufficientBalance() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    AccountCapsule accountCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      setAssertBalance(accountCapsule, AMOUNT - 1000000);

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferContract shieldedTransferContract = contract.getParameter()
          .unpack(ShieldedTransferContract.class);
      Assert.assertEquals(AMOUNT, shieldedTransferContract.getFromAmount());

      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Validate ShieldedTransferContract error, balance is not sufficient", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    } finally {
      setAssertBalance(accountCapsule, OWNER_BALANCE);
    }
  }

  /**
   * from account not exist
   */
  @Test
  public void publicAddressToShieldedFromAccountNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    AccountCapsule accountCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
    dbManager.getAccountStore().delete(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
    Assert.assertTrue(
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE)) == null);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Validate ShieldedTransferContract error, no OwnerAccount", e.getMessage());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    } finally {
      dbManager.getAccountStore().put(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), accountCapsule);
    }
  }

  /**
   * shieldTransfer not consume bandwidth point
   */
  @Test
  public void publicAddressToShieldedAddressNotConsumeBandwidth() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCap = transactionUtil.addSign(transactionSignBuild.build());

      AccountCapsule accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      long balance = accountCapsule.getBalance();
      long netUsage = accountCapsule.getNetUsage();
      long freeNetUsage = accountCapsule.getFreeNetUsage();
      long assertBalance = getAssertBalance(accountCapsule);

      Assert.assertTrue(dbManager.pushTransaction(transactionCap));

      accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      Assert.assertEquals(assertBalance - AMOUNT, getAssertBalance(accountCapsule));
      Assert.assertEquals(balance, accountCapsule.getBalance());
      Assert.assertEquals(netUsage, accountCapsule.getNetUsage());
      Assert.assertEquals(freeNetUsage, accountCapsule.getFreeNetUsage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * from amount equals 0 or negative number
   */
  @Test
  public void publicAddressToShieldedInvalidFromAmount() throws ZksnarkException {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    try {
      long amount = 0;
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), amount);
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("from_amount must be greater than 0", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }

    try {
      long amount = -100;
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), amount);
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("from_amount should not be less than 0", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * ShieldAddress to public address must has a cm
   */
  @Test
  public void shieldAddressOnlyToPublicAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      long amount = 0;
      //From amount
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT + fee);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), amount);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("ShieldedTransferContract error, no output cm", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * to amount equals 0 or negative number
   */
  @Test
  public void publicAddressToShieldedInvalidToAmount() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      long amount = 0;
      //From amount
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);
      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), amount);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("to_amount must be greater than 0", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }

    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      long amount = -100;
      //From amount
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);
      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), amount);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("to_amount should not be less than 0", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * Invalid from Address
   */
  @Test
  public void publicAddressToShieldedInvalidFromAddress() throws ZksnarkException {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //From amount
    builder.setTransparentInput(ByteArray.fromHexString(INVAILID_ADDRESS), AMOUNT);
    //TO amount
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
    try {
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Invalid transparent_from_address", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * Invalid to Address
   */
  @Test
  public void publicAddressToShieldedInvalidToAddress() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From amount
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);
      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(INVAILID_ADDRESS), AMOUNT - fee);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Invalid transparent_to_address", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * transfer from public account and shield account
   */
  @Test
  public void transferFromPublicAndShieldAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);
      //From public address
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), AMOUNT);
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), 2 * AMOUNT - fee);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, more than 1 senders", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * input shield note more than 1
   */
  @Test
  public void shieldAddressMore10NoteToPublicAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      for (int i = 0; i < 2; i++) {
        Note note = new Note(address, AMOUNT);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), 2 * AMOUNT - fee);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, number of spend notes should not be more than 1",
          e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * out shield note more than 2
   */
  @Test
  public void publicAddressToShieldMoreThan10NoteFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), 3 * AMOUNT + fee);
      //TO amount
      for (int i = 0; i < 3; i++) {
        SpendingKey spendingKey = SpendingKey.random();
        FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
        IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
        PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random())
            .get();
        builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      }
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, number of receivers should not be more than 2",
          e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * transaction has no from address
   */
  @Test
  public void publicAddressToShieldAddressNoFromAddressFailure() throws ZksnarkException {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //From amount
    builder.setTransparentInput(ByteArray.fromHexString(null), AMOUNT);
    //TO amount
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
    try {
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, no sender", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * transaction has no from address，and has validate shield input
   */
  @Test
  public void publicAddressAndShieldAddressToShieldAddressNoFromAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(null), AMOUNT);

      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);

      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 2 * AMOUNT - fee, new byte[512]);

      TransactionCapsule transactionCap = builder.build();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "no transparent_from_address, from_amount should be 0", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * transaction hasn't to address，and has validate shield out
   */
  @Test
  public void publicAddressAToShieldAddressNoToAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), 2 * AMOUNT + fee);
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      builder.setTransparentOutput(ByteArray.fromHexString(null), AMOUNT);

      TransactionCapsule transactionCap = builder.build();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "no transparent_to_address, to_amount should be 0", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * output shield note and input shield note value is 0,Meet the balance condition
   */
  @Test
  public void publicToShieldAddressAndShieldToPublicAddressWithZoreValueSuccess() {
    Args.setFullNodeAllowShieldedTransaction(true);
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    try {
      //public address to shield address(one value is 0)
      long ownerAssertBalance = getAssertBalance(dbManager.getAccountStore().get(ByteArray
          .fromHexString(PUBLIC_ADDRESS_ONE)));
      ZenTransactionBuilder builderOne = new ZenTransactionBuilder(wallet);
      //From amount
      builderOne.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE),
          (AMOUNT + fee));
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();

      Note note = new Note(paymentAddress.getD(), paymentAddress.getPkD(), AMOUNT,
          Note.generateR());
      builderOne.addOutput(fullViewingKey.getOvk(), note.getD(), note.getPkD(), note.getValue(),
          note.getRcm(),
          new byte[512]);
      {
        note = new Note(paymentAddress.getD(), paymentAddress.getPkD(), 0, Note.generateR());
        builderOne.addOutput(fullViewingKey.getOvk(), note.getD(), note.getPkD(), note.getValue(),
            note.getRcm(),
            new byte[512]);
      }
      TransactionCapsule transactionCapOne = builderOne.build();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCapOne.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCapOne = transactionUtil.addSign(transactionSignBuild.build());

      Assert.assertTrue(dbManager.pushTransaction(transactionCapOne));
      AccountCapsule accountCapsuleOne =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      Assert.assertEquals(getAssertBalance(accountCapsuleOne),
          ownerAssertBalance - AMOUNT - fee);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /**
   * onput shield note value is 0,Does not meet the equilibrium condition
   */
  @Test
  public void shieldToPublicAddressWithZoreValueFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(AMOUNT * 2);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      {
        Note note = new Note(address, AMOUNT * 2);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingFinalCheck error", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * output shield note value is negative,meet the equilibrium condition
   */
  public void publicAddressToShieldNoteValueWithNagativeFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE),
          (fee));
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, -AMOUNT, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingFinalCheck error", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * input shield note value is negative,meet the equilibrium condition
   */
  public void shieldNoteValueWithNagativeToPublicFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(AMOUNT);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      {
        Note note = new Note(address, -1);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), -1 + fee);

      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingFinalCheck error", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * fromAmount = (Long.MAX_VALUE + shieldAmount2 + fee) & Long.MAX_VALUE
   */
  @Test
  public void publicAddressToShieldNoteValueFailure() {
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);

    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      long fromAmount = Long.MAX_VALUE + AMOUNT + fee;
      fromAmount &= Long.MAX_VALUE;
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), fromAmount);
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, Long.MAX_VALUE, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("librustzcashSaplingFinalCheck error", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * from address same to to address
   */
  @Test
  public void publicAddressAToShieldAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), 2 * AMOUNT + fee);
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(DiversifierT.random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);

      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), AMOUNT);

      TransactionCapsule transactionCap = builder.build();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Can't transfer zen to yourself", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * Note has been spent
   */
  @Test
  public void shieldAddressToPublicFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(AMOUNT);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);

      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT - fee);

      TransactionCapsule transactionCap = builder.build();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);

      TransactionResultCapsule ret = new TransactionResultCapsule();

      //set note nullifiers
      ShieldedTransferContract shieldContract = transactionCap.getInstance().getRawData()
          .getContract(0).getParameter().unpack(ShieldedTransferContract.class);
      dbManager.getNullifierStore().put(
          new BytesCapsule(shieldContract.getSpendDescription(0).getNullifier().toByteArray()));

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("note has been spend in this transaction", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * shieldedPoolValue error
   */
  @Test
  public void shieldAddressToPublicNotEnoughFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(0);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
          voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);

      //TO amount
      addZeroValueOutputNote(builder);
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT - fee);

      TransactionCapsule transactionCap = builder.build();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);

      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("shieldedPoolValue error", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * success
   */
  @Test
  public void shieldAddressToPublic() {
    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveTotalShieldedPoolValue(AMOUNT);

    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      Note note = new Note(address, AMOUNT);
      IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
      byte[] anchor = voucher.root().getContent().toByteArray();
      dbManager.getMerkleContainer()
          .putMerkleTreeIntoStore(anchor, voucher.getVoucherCapsule().getTree());
      builder.addSpend(expsk, note, anchor, voucher);

      //TO amount
      addZeroValueOutputNote(builder);

      long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionCreateAccountFee();
      String addressNotExist =
          Wallet.getAddressPreFixString() + "8ba2aaae540c642e44e3bed5522c63bbc21f0000";

      builder.setTransparentOutput(ByteArray.fromHexString(addressNotExist), AMOUNT - fee);

      TransactionCapsule transactionCap = builder.build();
      Contract contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0);
      ShieldedTransferActuator actuator = new ShieldedTransferActuator();
      actuator.setChainBaseManager(dbManager.getChainBaseManager()).setContract(contract)
          .setTx(transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }
}

