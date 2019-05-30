package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.zksnark.Librustzcash;
import org.tron.common.zksnark.LibrustzcashParam.InitZksnarkParams;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.IncrementalMerkleVoucherCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.PermissionException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.FullViewingKey;
import org.tron.core.zen.address.IncomingViewingKey;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.core.zen.address.SpendingKey;
import org.tron.core.zen.merkle.IncrementalMerkleTreeContainer;
import org.tron.core.zen.merkle.IncrementalMerkleVoucherContainer;
import org.tron.core.zen.note.Note;
import org.tron.protos.Contract.IncrementalMerkleVoucherInfo;
import org.tron.protos.Contract.OutputPoint;
import org.tron.protos.Contract.OutputPointInfo;
import org.tron.protos.Contract.PedersenHash;
import org.tron.protos.Contract.ShieldedTransferContract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.TransactionSign;

@Slf4j
public class ShieldedTransferActuatorTest2 {

  private static Wallet wallet;
  private static Manager dbManager;
  private static final String dbPath = "output_shieded_transfer_test_2";
  private static TronApplicationContext context;
  private static final String PUBLIC_ADDRESS_ONE;
  private static final String ADDRESS_ONE_PRIVATE_KEY;
  private static final String PUBLIC_ADDRESS_TWO;
  private static final String ADDRESS_TWO_PRIVATE_KEY;
  private static final String PUBLIC_ADDRESS_OFF_LINE;
  private static final long AMOUNT = 100000000L;
  private static final long OWNER_BALANCE = 9999999000000L;
  private static final long TO_BALANCE = 100001000000L;
  private static final String INVAILID_ADDRESS = "aaaa";

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
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() throws ZksnarkException {
    wallet = context.getBean(Wallet.class);
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

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("owner"),
            ByteString.copyFrom(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE)),
            AccountType.Normal,
            OWNER_BALANCE);
    AccountCapsule toAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8("toAccount"),
            ByteString.copyFrom(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO)),
            AccountType.Normal,
            TO_BALANCE);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().put(toAccountCapsule.getAddress().toByteArray(), toAccountCapsule);
  }

  private static String getParamsFile(String fileName) {
    return ShieldedTransferActuatorTest2.class.getClassLoader()
        .getResource("params" + File.separator + fileName).getFile();
  }

  private static void librustzcashInitZksnarkParams() throws ZksnarkException {
    String spendPath = getParamsFile("sapling-spend.params");
    String spendHash = "8270785a1a0d0bc77196f000ee6d221c9c9894f55307bd9357c3f0105d31ca63991ab91324160d8f53e2bbd3c2633a6eb8bdf5205d822e7f3f73edac51b2b70c";

    String outputPath = getParamsFile("sapling-output.params");
    String outputHash = "657e3d38dbb5cb5e7dd2970e8b03d69b4787dd907285b5a7f0790dcc8072f60bf593b32cc2d1c030e00ff5ae64bf84c5c3beb84ddc841d48264b4a171744d028";

    Librustzcash.librustzcashInitZksnarkParams(
        new InitZksnarkParams(spendPath.getBytes(), spendPath.length(), spendHash,
            outputPath.getBytes(), outputPath.length(), outputHash));
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
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
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

  /**
   * From public address to shielded Address success
   */
  @Test
  public void publicAddressToShieldedAddressSuccess() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCap = wallet.addSign(transactionSignBuild.build());

      Assert.assertTrue(dbManager.pushTransaction(transactionCap));
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * only public address to public address
   */
  @Test
  public void publicAddressToPublicAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //From amount
    builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), AMOUNT);
    //TO amount
    builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT - fee);

    try {
      TransactionCapsule transactionCap = builder.build();
      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no Description found in transaction", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * Account A send coin to shiled address, but sign transaction with Account B
   */
  @Test
  public void publicAddressToShieldedAddressInvalidSign() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_TWO_PRIVATE_KEY)));
      wallet.addSign(transactionSignBuild.build());
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      //change fee to build transaction
      dbManager.getDynamicPropertiesStore().saveShieldedTransactionFee(fee - 1000000);
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      dbManager.getDynamicPropertiesStore().saveShieldedTransactionFee(fee);

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    AccountCapsule accountCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();

      accountCapsule.setBalance(AMOUNT - 1000000);
      dbManager.getAccountStore().put(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), accountCapsule);

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferContract shieldedTransferContract = contract
          .unpack(ShieldedTransferContract.class);
      Assert.assertEquals(AMOUNT, shieldedTransferContract.getFromAmount());

      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
      accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      accountCapsule.setBalance(OWNER_BALANCE);
      dbManager.getAccountStore().put(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), accountCapsule);
    }
  }

  /**
   * from account not exist
   */
  @Test
  public void publicAddressToShieldedFromAccountNotExist() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

    AccountCapsule accountCapsule =
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
    dbManager.getAccountStore().delete(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
    Assert.assertTrue(
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE)) == null);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "Validate ShieldedTransferContract error, no OwnerAccount", e.getMessage());
    } catch (Exception e) {
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    try {
      TransactionCapsule transactionCap = getPublicToShieldedTransaction();
      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCap.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCap = wallet.addSign(transactionSignBuild.build());

      AccountCapsule accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      long balance = accountCapsule.getBalance();
      long netUsage = accountCapsule.getNetUsage();
      long freeNetUsage = accountCapsule.getFreeNetUsage();

      Assert.assertTrue(dbManager.pushTransaction(transactionCap));

      accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      Assert.assertEquals(balance - AMOUNT, accountCapsule.getBalance());
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
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
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
   * to amount equals 0 or negative number
   */
  @Test
  public void publicAddressToShieldedInvalidToAmount() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
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
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), amount);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), amount);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //From amount
    builder.setTransparentInput(ByteArray.fromHexString(INVAILID_ADDRESS), AMOUNT);
    //TO amount
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
    try {
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
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
      builder.setTransparentOutput(ByteArray.fromHexString(INVAILID_ADDRESS), AMOUNT - fee);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
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

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
   * output shield note and input shield note should be less than 10
   */
  @Test
  public void PublicToShieldAddressAndShieldToPublicAddressSuccess() {
    Args.getInstance().setAllowShieldedTransactionApi(true);
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    dbManager.getWitnessController().setActiveWitnesses(new ArrayList<>());
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));

    try {
      int[] noteNumArray = {3, 7, 10};
      for (int noteNum : noteNumArray) {
        //generate one block
        dbManager.generateBlock(witnessCapsule, System.currentTimeMillis(), privateKey,
            false, false);

        //Step 1, public address to shield address
        List<Note> listNote = new ArrayList<>();
        long ownerBalance = dbManager.getAccountStore().get(ByteArray
            .fromHexString(PUBLIC_ADDRESS_ONE)).getBalance();
        ZenTransactionBuilder builderOne = new ZenTransactionBuilder(wallet);
        //From amount
        builderOne.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE),
            (noteNum * AMOUNT + fee));
        //TO amount
        SpendingKey spendingKey = SpendingKey.random();
        FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
        IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
        PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random())
            .get();
        for (int i = 0; i < noteNum; i++) {
          Note note = new Note(paymentAddress.getD(), paymentAddress.getPkD(), AMOUNT,
              Note.generateR());
          listNote.add(note);
          builderOne.addOutput(fullViewingKey.getOvk(), note.d, note.pkD, note.value, note.r,
              new byte[512]);
        }
        TransactionCapsule transactionCapOne = builderOne.build();

        //Add public address sign
        TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
        transactionSignBuild.setTransaction(transactionCapOne.getInstance());
        transactionSignBuild.setPrivateKey(ByteString.copyFrom(
            ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
        transactionCapOne = wallet.addSign(transactionSignBuild.build());

        Assert.assertTrue(dbManager.pushTransaction(transactionCapOne));
        AccountCapsule accountCapsuleOne =
            dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
        Assert
            .assertEquals(accountCapsuleOne.getBalance(), ownerBalance - (noteNum * AMOUNT + fee));

        //package transaction to block
        dbManager.generateBlock(witnessCapsule, System.currentTimeMillis(), privateKey,
            false, false);

        //Step 2,shield address to public address
        long toBalance = dbManager.getAccountStore().get(ByteArray
            .fromHexString(PUBLIC_ADDRESS_TWO)).getBalance();
        String trxId = transactionCapOne.getTransactionId().toString();
        OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
        for (int i = 0; i < noteNum; i++) {
          OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
          outPointBuild.setHash(ByteString.copyFrom(ByteArray.fromHexString(trxId)));
          outPointBuild.setIndex(i);
          request.addOutPoints(outPointBuild.build());
        }
        IncrementalMerkleVoucherInfo merkleVoucherInfo = wallet
            .getMerkleTreeVoucherInfo(request.build());

        ZenTransactionBuilder builderTwo = new ZenTransactionBuilder(wallet);
        //From shield address
        ExpandedSpendingKey expsk = spendingKey.expandedSpendingKey();
        for (int i = 0; i < noteNum; i++) {
          Note note = listNote.get(i);
          IncrementalMerkleVoucherContainer voucher =
              new IncrementalMerkleVoucherContainer(
                  new IncrementalMerkleVoucherCapsule(merkleVoucherInfo.getVouchers(i)));
          byte[] anchor = voucher.root().getContent().toByteArray();
          builderTwo.addSpend(expsk, note, anchor, voucher);
        }
        //TO amount
        builderTwo.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO),
            (noteNum * AMOUNT - fee));
        TransactionCapsule transactionCapTwo = builderTwo.build();

        Assert.assertTrue(dbManager.pushTransaction(transactionCapTwo));
        AccountCapsule accountCapsuleTwo =
            dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO));
        Assert.assertEquals(accountCapsuleTwo.getBalance(), toBalance + (noteNum * AMOUNT - fee));
      }

    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /**
   * input shield note more than 10
   */
  @Test
  public void ShieldAddressMore10NoteToPublicAddressFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      for (int i = 0; i < 11; i++) {
        Note note = new Note(address, AMOUNT);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), 11 * AMOUNT - fee);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, number of spend notes should not be more than 10",
          e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * out shield note more than 10
   */
  @Test
  public void publicAddressToShieldMoreThan10NoteFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), 11 * AMOUNT + fee);
      //TO amount
      for (int i = 0; i < 11; i++) {
        SpendingKey spendingKey = SpendingKey.random();
        FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
        IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
        PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random())
            .get();
        builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      }
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, number of receivers should not be more than 10",
          e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * No target account
   */
  @Test
  public void shieldAddressToNoTargetAccountFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);

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
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "ShieldedTransferContract error, no receiver", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  /**
   * transaction has no from address
   */
  @Test
  public void publicAddressToShieldAddressNoFromAddressFailure() throws ZksnarkException {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    //From amount
    builder.setTransparentInput(ByteArray.fromHexString(null), AMOUNT);
    //TO amount
    SpendingKey spendingKey = SpendingKey.random();
    FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
    IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
    PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
    builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT - fee, new byte[512]);
    try {
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
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
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, 2 * AMOUNT - fee, new byte[512]);

      TransactionCapsule transactionCap = builder.build();
      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE), 2 * AMOUNT + fee);
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      builder.setTransparentOutput(ByteArray.fromHexString(null), AMOUNT);

      TransactionCapsule transactionCap = builder.build();
      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
  public void PublicToShieldAddressAndShieldToPublicAddressWithZoreValueSuccess() {
    Args.getInstance().setAllowShieldedTransactionApi(true);
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    dbManager.getWitnessController().setActiveWitnesses(new ArrayList<>());
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
    byte[] privateKey = ByteArray.fromHexString(key);
    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    dbManager.addWitness(ByteString.copyFrom(address));

    try {
      int noteNum = 5;
      //generate one block
      dbManager.generateBlock(witnessCapsule, System.currentTimeMillis(), privateKey,
          false, false);

      //Step 1, public address to shield address
      List<Note> listNote = new ArrayList<>();
      long ownerBalance = dbManager.getAccountStore().get(ByteArray
          .fromHexString(PUBLIC_ADDRESS_ONE)).getBalance();
      ZenTransactionBuilder builderOne = new ZenTransactionBuilder(wallet);
      //From amount
      builderOne.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE),
          (AMOUNT + fee));
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();

      Note note = new Note(paymentAddress.getD(), paymentAddress.getPkD(), AMOUNT,
          Note.generateR());
      listNote.add(note);
      builderOne
          .addOutput(fullViewingKey.getOvk(), note.d, note.pkD, note.value, note.r, new byte[512]);

      for (int i = 0; i < noteNum - 1; i++) {
        note = new Note(paymentAddress.getD(), paymentAddress.getPkD(), 0, Note.generateR());
        listNote.add(note);
        builderOne.addOutput(fullViewingKey.getOvk(), note.d, note.pkD, note.value, note.r,
            new byte[512]);
      }
      TransactionCapsule transactionCapOne = builderOne.build();

      //Add public address sign
      TransactionSign.Builder transactionSignBuild = TransactionSign.newBuilder();
      transactionSignBuild.setTransaction(transactionCapOne.getInstance());
      transactionSignBuild.setPrivateKey(ByteString.copyFrom(
          ByteArray.fromHexString(ADDRESS_ONE_PRIVATE_KEY)));
      transactionCapOne = wallet.addSign(transactionSignBuild.build());

      Assert.assertTrue(dbManager.pushTransaction(transactionCapOne));
      AccountCapsule accountCapsuleOne =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE));
      Assert.assertEquals(accountCapsuleOne.getBalance(), ownerBalance - AMOUNT - fee);

      //package transaction to block
      dbManager.generateBlock(witnessCapsule, System.currentTimeMillis(), privateKey,
          false, false);

      //Step 2,shield address to public address
      long toBalance = dbManager.getAccountStore().get(ByteArray
          .fromHexString(PUBLIC_ADDRESS_TWO)).getBalance();
      String trxId = transactionCapOne.getTransactionId().toString();
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
      for (int i = 0; i < noteNum; i++) {
        OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
        outPointBuild.setHash(ByteString.copyFrom(ByteArray.fromHexString(trxId)));
        outPointBuild.setIndex(i);
        request.addOutPoints(outPointBuild.build());
      }
      IncrementalMerkleVoucherInfo merkleVoucherInfo = wallet
          .getMerkleTreeVoucherInfo(request.build());

      ZenTransactionBuilder builderTwo = new ZenTransactionBuilder(wallet);
      //From shield address
      ExpandedSpendingKey expsk = spendingKey.expandedSpendingKey();
      for (int i = 0; i < noteNum; i++) {
        note = listNote.get(i);
        IncrementalMerkleVoucherContainer voucher =
            new IncrementalMerkleVoucherContainer(
                new IncrementalMerkleVoucherCapsule(merkleVoucherInfo.getVouchers(i)));
        byte[] anchor = voucher.root().getContent().toByteArray();
        builderTwo.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      builderTwo.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO),
          (AMOUNT - fee));
      TransactionCapsule transactionCapTwo = builderTwo.build();

      Assert.assertTrue(dbManager.pushTransaction(transactionCapTwo));
      AccountCapsule accountCapsuleTwo =
          dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO));
      Assert.assertEquals(accountCapsuleTwo.getBalance(), toBalance + AMOUNT - fee);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    }
  }

  /**
   * onput shield note value is 0,Does not meet the equilibrium condition
   */
  @Test
  public void ShieldToPublicAddressWithZoreValueFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      {
        Note note = new Note(address, AMOUNT);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      for (int i = 0; i < 4; i++) {
        Note note = new Note(address, 0);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
  @Test
  public void PublicAddressToShieldNoteValueWithNagativeFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
      //From amount
      builder.setTransparentInput(ByteArray.fromHexString(PUBLIC_ADDRESS_ONE),
          (AMOUNT + fee));
      //TO amount
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(new DiversifierT().random()).get();
      for (int i = 0; i < 2; i++) {
        builder.addOutput(fullViewingKey.getOvk(), paymentAddress, AMOUNT, new byte[512]);
      }
      builder.addOutput(fullViewingKey.getOvk(), paymentAddress, -AMOUNT, new byte[512]);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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
  @Test
  public void ShieldNoteValueWithNagativeToPublicFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();
    try {
      ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
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
      {
        Note note = new Note(address, -AMOUNT);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), AMOUNT - fee);

      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
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

  @Test
  public void ShieldAddressToPublicAddressBigValueFailure() {
    dbManager.getDynamicPropertiesStore().saveAllowZksnarkTransaction(1);
    long fee = dbManager.getDynamicPropertiesStore().getShieldedTransactionFee();

    AccountCapsule accountCapsuleTwo =
        dbManager.getAccountStore().get(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO));
    accountCapsuleTwo.setBalance(0);
    dbManager.getAccountStore()
        .put(accountCapsuleTwo.getAddress().toByteArray(), accountCapsuleTwo);

    ZenTransactionBuilder builder = new ZenTransactionBuilder(wallet);
    try {
      //From shield address
      SpendingKey sk = SpendingKey.random();
      ExpandedSpendingKey expsk = sk.expandedSpendingKey();
      PaymentAddress address = sk.defaultAddress();
      {
        Note note = new Note(address, Long.MAX_VALUE - fee - 1);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      {
        Note note = new Note(address, fee);
        IncrementalMerkleVoucherContainer voucher = createSimpleMerkleVoucherContainer(note.cm());
        byte[] anchor = voucher.root().getContent().toByteArray();
        dbManager.getMerkleContainer().putMerkleTreeIntoStore(anchor,
            voucher.getVoucherCapsule().getTree());
        builder.addSpend(expsk, note, anchor, voucher);
      }
      //TO amount
      builder.setTransparentOutput(ByteArray.fromHexString(PUBLIC_ADDRESS_TWO), Long.MAX_VALUE - 1);
      TransactionCapsule transactionCap = builder.build();

      Any contract =
          transactionCap.getInstance().toBuilder().getRawDataBuilder().getContract(0)
              .getParameter();
      ShieldedTransferActuator actuator = new ShieldedTransferActuator(contract, dbManager,
          transactionCap);
      TransactionResultCapsule ret = new TransactionResultCapsule();

      actuator.validate();
      actuator.execute(ret);
      Assert.assertTrue(false);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(
          "long overflow", e.getMessage());
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }
}
