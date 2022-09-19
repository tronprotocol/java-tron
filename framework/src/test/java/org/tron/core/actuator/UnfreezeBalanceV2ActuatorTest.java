package org.tron.core.actuator;

import static junit.framework.TestCase.fail;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

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
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.Common.ResourceCode;

@Slf4j
public class UnfreezeBalanceV2ActuatorTest {

  private static final String dbPath = "output_unfreeze_balance_test";
  private static final String OWNER_ADDRESS;
  private static final String RECEIVER_ADDRESS;
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ACCOUNT_INVALID;
  private static final long initBalance = 10_000_000_000L;
  private static final long frozenBalance = 1_000_000_000L;
  private static final long smallTatalResource = 100L;
  private static Manager dbManager;
  private static TronApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    RECEIVER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049150";
    OWNER_ACCOUNT_INVALID =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a3456";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
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
  public void createAccountCapsule() {
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);

    AccountCapsule receiverCapsule = new AccountCapsule(ByteString.copyFromUtf8("receiver"),
        ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)), AccountType.Normal,
        initBalance);
    dbManager.getAccountStore().put(receiverCapsule.getAddress().toByteArray(), receiverCapsule);
  }

  private Any getContractForBandwidthV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
                    .setOwnerAddress(
                            ByteString.copyFrom(ByteArray.fromHexString(ownerAddress))
                    )
                    .setUnfreezeBalance(unfreezeBalance)
                    .setResource(ResourceCode.BANDWIDTH)
                    .build()
    );
  }

  private Any getContractForCpuV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.ENERGY).build());
  }

  private Any getContractForTronPowerV2(String ownerAddress, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.TRON_POWER).build());
  }

  private Any getDelegatedContractForBandwidth(
          String ownerAddress, long unfreezeBalance
  ) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.BANDWIDTH).build());
  }

  private Any getDelegatedContractForCpu(
          String ownerAddress, String receiverAddress, long unfreezeBalance
  ) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(ResourceCode.ENERGY).build());
  }

  private Any getContract(String ownerAddress, ResourceCode resourceCode, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
        .setUnfreezeBalance(unfreezeBalance)
        .setResource(resourceCode).build());
  }


  private Any getContractForTronPowerV2_001(String ownerAddress, long unfreezeBalance) {
    return Any.pack(BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .setUnfreezeBalance(unfreezeBalance).build());
  }

  @Test
  public void testUnfreezeBalanceForBandwidth() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    long unfreezeBalance = frozenBalance - 100;

    Assert.assertEquals(accountCapsule.getFrozenBalanceV2(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long totalNetWeightBefore = dbManager.getDynamicPropertiesStore().getTotalNetWeight();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getFrozenBalanceV2(), 100);
      Assert.assertEquals(owner.getTronPower(), 100L);

      long totalNetWeightAfter = dbManager.getDynamicPropertiesStore().getTotalNetWeight();
      Assert.assertEquals(totalNetWeightBefore,
              totalNetWeightAfter + (frozenBalance - 100) / 1000_000L);

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForEnergy() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForEnergy(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long totalEnergyWeightBefore = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
              .get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(owner.getBalance(), initBalance + frozenBalance);
      Assert.assertEquals(owner.getAllFrozenBalanceForEnergy(), 100);
      Assert.assertEquals(owner.getTronPower(), 100);
      long totalEnergyWeightAfter = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
      Assert.assertEquals(totalEnergyWeightBefore,
          totalEnergyWeightAfter + (frozenBalance - 100) / 1000_000L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForBandwidth() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForBandwidthV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(DelegatedResourceCapsule.createDbAddressKeyV2(
                          ByteArray.fromHexString(OWNER_ADDRESS)),
                  delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(DelegatedResourceCapsule.createDbAddressKeyV2(
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceAccountIndex);
    }

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      AccountCapsule receiverResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(RECEIVER_ADDRESS));

      //Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(100L, ownerResult.getTronPower());
      Assert.assertEquals(100L, ownerResult.getDelegatedFrozenBalanceForBandwidth());
      Assert.assertEquals(100L, receiverResult.getAllFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                        ByteArray.fromHexString(OWNER_ADDRESS))
              );
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(1,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(RECEIVER_ADDRESS)
                      )
              );
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert.assertEquals(1,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());

    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForBandwidthWithDeletedReceiver() {

    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForBandwidthV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(
                  DelegatedResourceCapsule.createDbAddressKeyV2(
                          ByteArray.fromHexString(OWNER_ADDRESS)), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(DelegatedResourceCapsule.createDbAddressKeyV2(
                  ByteArray.fromHexString(RECEIVER_ADDRESS)),
                  delegatedResourceAccountIndex);
    }

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "Receiver Account[a0abd4b9367799eaa3197fecb144eb71de1e049150] does not exist");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(OWNER_ADDRESS)));
      Assert
          .assertEquals(0,
              delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(RECEIVER_ADDRESS)));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());

    } catch (ContractValidateException e) {
      logger.error("", e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void testUnfreezeDelegatedBalanceForBandwidthWithRecreatedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    long unfreezeBalance = frozenBalance;

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForBandwidthV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(
                  DelegatedResourceCapsule.createDbAddressKeyV2(
                          ByteArray.fromHexString(OWNER_ADDRESS)), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(DelegatedResourceCapsule
                  .createDbAddressKeyV2(ByteArray.fromHexString(RECEIVER_ADDRESS)),
                  delegatedResourceAccountIndex);
    }

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getAccountStore().delete(receiver.createDbKey());
    receiver = new AccountCapsule(receiver.getAddress(), ByteString.EMPTY, AccountType.Normal);
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(10L);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);
    receiver = dbManager.getAccountStore().get(receiver.createDbKey());
    Assert.assertEquals(10, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(0);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "AcquiredDelegatedFrozenBalanceForBandwidth[10] < delegatedBandwidth[1000000000]");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForBandwidth());

      //check DelegatedResourceAccountIndex
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleOwner = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(OWNER_ADDRESS)));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getFromAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleOwner.getToAccountsList().size());

      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndexCapsuleReceiver = dbManager
          .getDelegatedResourceAccountIndexStore().get(
                      DelegatedResourceCapsule.createDbAddressKeyV2(
                              ByteArray.fromHexString(RECEIVER_ADDRESS)));
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getToAccountsList().size());
      Assert.assertEquals(0,
          delegatedResourceAccountIndexCapsuleReceiver.getFromAccountsList().size());

      receiver = dbManager.getAccountStore().get(receiver.createDbKey());
      Assert.assertEquals(0, receiver.getAcquiredDelegatedFrozenBalanceForBandwidth());

    } catch (ContractValidateException e) {
      logger.error("", e);
      Assert.fail();
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  /**
   * when SameTokenName close,delegate balance frozen, unfreoze show error
   */
  @Test
  public void testUnfreezeDelegatedBalanceForBandwidthSameTokenNameClose() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(0);
    long unfreezeBalance = frozenBalance;

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForBandwidthV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.setAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    //init DelegatedResourceCapsule
    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForBandwidth(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    //init DelegatedResourceAccountIndex
    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              owner.getAddress());
      delegatedResourceAccountIndex
          .addToAccount(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(
                  DelegatedResourceCapsule.createDbAddressKeyV2(
                          ByteArray.fromHexString(OWNER_ADDRESS)), delegatedResourceAccountIndex);
    }

    {
      DelegatedResourceAccountIndexCapsule delegatedResourceAccountIndex =
          new DelegatedResourceAccountIndexCapsule(
              receiver.getAddress());
      delegatedResourceAccountIndex
          .addFromAccount(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)));
      dbManager.getDelegatedResourceAccountIndexStore()
          .put(DelegatedResourceCapsule
                  .createDbAddressKeyV2(ByteArray.fromHexString(RECEIVER_ADDRESS)),
                  delegatedResourceAccountIndex);
    }

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForBandwidth(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance(BANDWIDTH)", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpu() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForEnergyV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForEnergy(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      AccountCapsule receiverResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(RECEIVER_ADDRESS));

      //Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForEnergy());
      Assert.assertEquals(0L, receiverResult.getAllFrozenBalanceForEnergy());
    } catch (ContractValidateException e) {
      logger.error("", e);
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpuWithDeletedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule owner = dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForEnergyV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(), receiver.getAddress());
    delegatedResourceCapsule.setFrozenBalanceForEnergy(frozenBalance, now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "Receiver Account[a0abd4b9367799eaa3197fecb144eb71de1e049150] does not exist");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getDelegatedFrozenBalanceForEnergy());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testUnfreezeDelegatedBalanceForCpuWithRecreatedReceiver() {
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    long unfreezeBalance = frozenBalance;

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    //owner.addDelegatedFrozenBalanceForEnergyV2(frozenBalance);
    Assert.assertEquals(frozenBalance, owner.getTronPower());

    AccountCapsule receiver = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(RECEIVER_ADDRESS));
    receiver.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    Assert.assertEquals(0L, receiver.getTronPower());

    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    DelegatedResourceCapsule delegatedResourceCapsule = new DelegatedResourceCapsule(
        owner.getAddress(),
        receiver.getAddress()
    );
    delegatedResourceCapsule.setFrozenBalanceForEnergy(
        frozenBalance,
        now - 100L);
    dbManager.getDelegatedResourceStore().put(DelegatedResourceCapsule
        .createDbKeyV2(ByteArray.fromHexString(OWNER_ADDRESS),
            ByteArray.fromHexString(RECEIVER_ADDRESS)), delegatedResourceCapsule);

    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getDelegatedContractForCpu(OWNER_ADDRESS, RECEIVER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(0);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(0);
    dbManager.getAccountStore().delete(receiver.createDbKey());
    receiver = new AccountCapsule(receiver.getAddress(), ByteString.EMPTY, AccountType.Normal);
    receiver.setAcquiredDelegatedFrozenBalanceForEnergy(10L);
    dbManager.getAccountStore().put(receiver.createDbKey(), receiver);
    receiver = dbManager.getAccountStore().get(receiver.createDbKey());
    Assert.assertEquals(10, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertEquals(e.getMessage(),
          "AcquiredDelegatedFrozenBalanceForEnergy[10] < delegatedEnergy[1000000000]");
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    dbManager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    dbManager.getDynamicPropertiesStore().saveAllowTvmSolidity059(1);

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule ownerResult =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));

      //Assert.assertEquals(initBalance + frozenBalance, ownerResult.getBalance());
      Assert.assertEquals(0L, ownerResult.getTronPower());
      Assert.assertEquals(0L, ownerResult.getAllFrozenBalanceForBandwidth());
      receiver = dbManager.getAccountStore().get(receiver.createDbKey());
      Assert.assertEquals(0, receiver.getAcquiredDelegatedFrozenBalanceForEnergy());
    } catch (ContractValidateException e) {
      Assert.fail();
    } catch (ContractExeException e) {
      Assert.fail();
    }
  }

  @Test
  public void invalidOwnerAddress() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS_INVALID, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);

      Assert.assertEquals("Invalid address", e.getMessage());

    } catch (ContractExeException e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }

  }

  @Test
  public void invalidOwnerAccount() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.setFrozen(1_000_000_000L, now);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ACCOUNT_INVALID, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ACCOUNT_INVALID + "] does not exist",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void noFrozenBalance() {
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    long unfreezeBalance = frozenBalance;
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("cannot run here.");

    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("no frozenBalance(BANDWIDTH)", e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  @Test
  public void testClearVotes() {
    byte[] ownerAddressBytes = ByteArray.fromHexString(OWNER_ADDRESS);
    ByteString ownerAddress = ByteString.copyFrom(ownerAddressBytes);
    long unfreezeBalance = frozenBalance;
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore().get(ownerAddressBytes);
    accountCapsule.addFrozenBalanceForBandwidthV2(1_000_000_000L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    dbManager.getVotesStore().reset();
    Assert.assertNull(dbManager.getVotesStore().get(ownerAddressBytes));
    try {
      actuator.validate();
      actuator.execute(ret);
      VotesCapsule votesCapsule = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesCapsule);
      Assert.assertEquals(0, votesCapsule.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

    // if had votes
    List<Vote> oldVotes = new ArrayList<Vote>();
    VotesCapsule votesCapsule = new VotesCapsule(
        ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), oldVotes);
    votesCapsule.addNewVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100);
    dbManager.getVotesStore().put(ByteArray.fromHexString(OWNER_ADDRESS), votesCapsule);
    accountCapsule.addFrozenBalanceForBandwidthV2(1_000_000_000L);
    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    try {
      actuator.validate();
      actuator.execute(ret);
      votesCapsule = dbManager.getVotesStore().get(ownerAddressBytes);
      Assert.assertNotNull(votesCapsule);
      Assert.assertEquals(0, votesCapsule.getNewVotes().size());
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  @Test
  public void commonErrorCheck() {
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();
    long unfreezeBalance = frozenBalance;

    Any invalidContractTypes = Any.pack(
            AssetIssueContractOuterClass.AssetIssueContract.newBuilder().build()
      );
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error, expected type [UnfreezeBalanceContract], real type[");
    actuatorTest.invalidContractType();

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForBandwidth(), frozenBalance);
    Assert.assertEquals(accountCapsule.getTronPower(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);

    actuatorTest.setContract(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();
  }


  @Test
  public void testUnfreezeBalanceForEnergyWithOldTronPowerAfterNewResourceModel() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.setOldTronPower(frozenBalance);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);
    Assert.assertEquals(accountCapsule.getAllFrozenBalanceForEnergy(), frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getVotesList().size(), 0L);
      Assert.assertEquals(owner.getInstance().getOldTronPower(), -1L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForEnergyWithoutOldTronPowerAfterNewResourceModel() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.setOldTronPower(-1L);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForCpuV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getVotesList().size(), 1L);
      Assert.assertEquals(owner.getInstance().getOldTronPower(), -1L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForTronPowerWithOldTronPowerAfterNewResourceModel() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.addFrozenForTronPowerV2(frozenBalance);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForTronPowerV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner = dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS));

      Assert.assertEquals(owner.getVotesList().size(), 0L);
      Assert.assertEquals(owner.getInstance().getOldTronPower(), -1L);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void testUnfreezeBalanceForTronPowerWithOldTronPowerAfterNewResourceModelError() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowNewResourceModel(1L);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.addFrozenForTronPowerV2(frozenBalance);
    accountCapsule.addVotes(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)), 100L);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContractForTronPowerV2(OWNER_ADDRESS, unfreezeBalance));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    try {
      actuator.validate();
      //Assert.fail();
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    }
  }


  @Test
  public void testUnfreezeBalanceCheckExistFreezedBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 100;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);
    accountCapsule.addFrozenBalanceForEnergyV2(frozenBalance);
    accountCapsule.addFrozenForTronPowerV2(frozenBalance);
    //accountCapsule.addDelegatedFrozenBalanceForBandwidthV2(frozenBalance);
    //accountCapsule.addDelegatedFrozenBalanceForEnergyV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    boolean bret1 = actuator.checkExistFreezedBalance(
            accountCapsule, ResourceCode.BANDWIDTH);
    Assert.assertTrue(true == bret1);
    boolean bret2 = actuator.checkExistFreezedBalance(
            accountCapsule, ResourceCode.ENERGY);
    Assert.assertTrue(true == bret2);
    boolean bret3 = actuator.checkExistFreezedBalance(
            accountCapsule, ResourceCode.TRON_POWER);
    Assert.assertTrue(true == bret3);

  }


  @Test
  public void testUnfreezeBalanceCheckUnfreezeBalance() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.BANDWIDTH)
            .build();
    boolean bret1 = actuator.checkUnfreezeBalance(
            accountCapsule, unfreezeBalanceV2Contract, ResourceCode.BANDWIDTH
    );
    Assert.assertTrue(true == bret1);
  }


  @Test
  public void testUnfreezeBalanceGetFreezeType() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .build();

    ResourceCode freezeType = unfreezeBalanceV2Contract.getResource();

    Assert.assertTrue(ResourceCode.TRON_POWER.equals(freezeType));
  }

  @Test
  public void testUnfreezeBalanceCalcUnfreezeExpireTime() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .build();

    long ret = actuator.calcUnfreezeExpireTime(now);

    Assert.assertTrue(true);
  }

  @Test
  public void testUnfreezeBalanceUpdateAccountFrozenInfo() {
    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);
    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));
    accountCapsule.addFrozenBalanceForBandwidthV2(frozenBalance);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setUnfreezeBalance(unfreezeBalance)
            .setResource(ResourceCode.TRON_POWER)
            //.setReceiverAddress(ByteString.copyFrom(ByteArray.fromHexString(RECEIVER_ADDRESS)))
            .build();

    actuator.updateAccountFrozenInfo(
            ResourceCode.BANDWIDTH, accountCapsule, unfreezeBalance
    );

    Assert.assertTrue(accountCapsule.getAllFrozenBalanceForBandwidth() == 1);
  }


  @Test
  public void testUnfreezeBalanceUnfreezeExpire() {

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long balance = accountCapsule.getBalance();
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            1,
            now + 19 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            10,
            now + 31 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.ENERGY,
            20,
            now + 32 * FROZEN_PERIOD
    );

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now + 31 * FROZEN_PERIOD);
    actuator.unfreezeExpire(accountCapsule,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp());

    Assert.assertEquals(accountCapsule.getBalance(), balance + 11);
    Assert.assertEquals(accountCapsule.getUnfrozenV2List().size(), 1);
    Assert.assertEquals(accountCapsule.getUnfrozenV2List().get(0).getUnfreezeAmount(), 20);
  }


  @Test
  public void testAddTotalResourceWeight() {

    long now = System.currentTimeMillis();
    long total = frozenBalance;
    dbManager.getDynamicPropertiesStore().saveTotalTronPowerWeight(total);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    long unfreezeBalance = frozenBalance;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long balance = accountCapsule.getBalance();

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));
    BalanceContract.UnfreezeBalanceV2Contract unfreezeBalanceV2Contract =
            BalanceContract.UnfreezeBalanceV2Contract.newBuilder()
                    .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
                    .setUnfreezeBalance(unfreezeBalance)
                    .setResource(ResourceCode.TRON_POWER)
                    .build();

    actuator.updateTotalResourceWeight(unfreezeBalanceV2Contract, unfreezeBalance);

    Assert.assertEquals(total - unfreezeBalance / TRX_PRECISION,
            dbManager.getDynamicPropertiesStore().getTotalTronPowerWeight());

  }

  @Test
  public void testUnfreezeBalanceUnfreezeCount() {

    long now = System.currentTimeMillis();
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(now);
    dbManager.getDynamicPropertiesStore().saveUnfreezeDelayDays(30);
    dbManager.getDynamicPropertiesStore().saveAllowDelegateResource(1);

    long unfreezeBalance = frozenBalance - 1;

    AccountCapsule accountCapsule = dbManager.getAccountStore()
            .get(ByteArray.fromHexString(OWNER_ADDRESS));

    long balance = accountCapsule.getBalance();
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            1,
            now + 19 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.BANDWIDTH,
            10,
            now + 31 * FROZEN_PERIOD
    );
    accountCapsule.addUnfrozenV2List(
            ResourceCode.ENERGY,
            20,
            now + 32 * FROZEN_PERIOD
    );

    int count = accountCapsule.getUnfreezingV2Count(now);
    Assert.assertEquals(3, count);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    UnfreezeBalanceV2Actuator actuator = new UnfreezeBalanceV2Actuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
            .setAny(getContractForBandwidthV2(OWNER_ADDRESS, unfreezeBalance));

    dbManager.getDynamicPropertiesStore()
            .saveLatestBlockHeaderTimestamp(now + 32 * FROZEN_PERIOD);
    actuator.unfreezeExpire(accountCapsule,
            dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp());


    int after_count = accountCapsule.getUnfreezingV2Count(now);
    Assert.assertEquals(0, after_count);

  }


}

