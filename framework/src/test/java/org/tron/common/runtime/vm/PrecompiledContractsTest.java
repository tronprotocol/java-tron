package org.tron.common.runtime.vm;

import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;
import static org.tron.core.db.TransactionTrace.convertToTronAddress;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.FreezeBalanceActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.PrecompiledContracts;
import org.tron.core.vm.PrecompiledContracts.PrecompiledContract;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Key;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.core.vm.repository.Value;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Proposal.State;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.Common;

@Slf4j
public class PrecompiledContractsTest {

  // common
  private static final DataWord voteContractAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010001");
  private static final DataWord withdrawBalanceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010004");
  private static final DataWord proposalApproveAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010005");
  private static final DataWord proposalCreateAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010006");
  private static final DataWord proposalDeleteAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010007");
  private static final DataWord convertFromTronBytesAddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010008");
  private static final DataWord convertFromTronBase58AddressAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010009");

  // FreezeV2 PrecompileContracts
  private static final DataWord getChainParameterAddr = new DataWord(
      "000000000000000000000000000000000000000000000000000000000100000b");

  private static final DataWord availableUnfreezeV2SizeAddr = new DataWord(
      "000000000000000000000000000000000000000000000000000000000100000c");

  private static final DataWord unfreezableBalanceV2Addr = new DataWord(
      "000000000000000000000000000000000000000000000000000000000100000d");

  private static final DataWord expireUnfreezeBalanceV2Addr = new DataWord(
      "000000000000000000000000000000000000000000000000000000000100000e");

  private static final DataWord delegatableResourceAddr = new DataWord(
      "000000000000000000000000000000000000000000000000000000000100000f");

  private static final DataWord resourceV2Addr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000010");

  private static final DataWord checkUnDelegateResourceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000011");

  private static final DataWord resourceUsageAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000012");

  private static final DataWord totalResourceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000013");

  private static final DataWord totalDelegatedResourceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000014");

  private static final DataWord totalAcquiredResourceAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000001000015");

  private static final String dbPath = "output_PrecompiledContracts_test";
  private static final String ACCOUNT_NAME = "account";
  private static final String OWNER_ADDRESS;
  private static final String WITNESS_NAME = "witness";
  private static final String WITNESS_ADDRESS;
  private static final String WITNESS_ADDRESS_BASE = "548794500882809695a8a687866e76d4271a1abc";
  private static final String URL = "https://tron.network";
  // withdraw
  private static final long initBalance = 10_000_000_000L;
  private static final long allowance = 32_000_000L;
  private static final long latestTimestamp = 1_000_000L;
  private static TronApplicationContext context;
  private static Application appT;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    WITNESS_ADDRESS = Wallet.getAddressPreFixString() + WITNESS_ADDRESS_BASE;

  }


  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
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
    // witness: witnessCapsule
    WitnessCapsule witnessCapsule =
        new WitnessCapsule(
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            10L,
            URL);
    // witness: AccountCapsule
    AccountCapsule witnessAccountCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(WITNESS_NAME),
            StringUtil.hexString2ByteString(WITNESS_ADDRESS),
            AccountType.Normal,
            initBalance);
    // some normal account
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOUNT_NAME),
            StringUtil.hexString2ByteString(OWNER_ADDRESS),
            AccountType.Normal,
            10_000_000_000_000L);

    dbManager.getAccountStore()
        .put(witnessAccountCapsule.getAddress().toByteArray(), witnessAccountCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getWitnessStore().put(witnessCapsule.getAddress().toByteArray(), witnessCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(latestTimestamp);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
  }

  private Any getFreezeContract(String ownerAddress, long frozenBalance, long duration) {
    return Any.pack(
        FreezeBalanceContract.newBuilder()
            .setOwnerAddress(StringUtil.hexString2ByteString(ownerAddress))
            .setFrozenBalance(frozenBalance)
            .setFrozenDuration(duration)
            .build());
  }

  private PrecompiledContract createPrecompiledContract(DataWord addr, String ownerAddress) {
    PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
    contract.setCallerAddress(convertToTronAddress(Hex.decode(ownerAddress)));
    contract.setRepository(RepositoryImpl.createRoot(StoreFactory.getInstance()));
    ProgramResult programResult = new ProgramResult();
    contract.setResult(programResult);
    return contract;
  }

  //@Test
  public void voteWitnessNativeTest()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      InstantiationException, ContractValidateException, ContractExeException {
    PrecompiledContract contract = createPrecompiledContract(voteContractAddr, OWNER_ADDRESS);
    Repository deposit = RepositoryImpl.createRoot(StoreFactory.getInstance());
    byte[] witnessAddressBytes = new byte[32];
    byte[] witnessAddressBytes21 = Hex.decode(WITNESS_ADDRESS);
    System.arraycopy(witnessAddressBytes21, 0, witnessAddressBytes,
        witnessAddressBytes.length - witnessAddressBytes21.length,
        witnessAddressBytes21.length);

    DataWord voteCount = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000001");
    byte[] voteCountBytes = voteCount.getData();
    byte[] data = new byte[witnessAddressBytes.length + voteCountBytes.length];
    System.arraycopy(witnessAddressBytes, 0, data, 0, witnessAddressBytes.length);
    System.arraycopy(voteCountBytes, 0, data, witnessAddressBytes.length,
        voteCountBytes.length);

    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    Any freezeContract = getFreezeContract(OWNER_ADDRESS, frozenBalance, duration);
    Constructor<FreezeBalanceActuator> constructor =
        FreezeBalanceActuator.class
            .getDeclaredConstructor(Any.class, dbManager.getClass());
    constructor.setAccessible(true);
    FreezeBalanceActuator freezeBalanceActuator = constructor
        .newInstance(freezeContract, dbManager);

    TransactionResultCapsule ret = new TransactionResultCapsule();
    freezeBalanceActuator.validate();
    freezeBalanceActuator.execute(ret);
    contract.setRepository(deposit);
    Boolean result = contract.execute(data).getLeft();
    deposit.commit();
    Assert.assertEquals(1,
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteCount());
    Assert.assertArrayEquals(ByteArray.fromHexString(WITNESS_ADDRESS),
        dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS)).getVotesList()
            .get(0).getVoteAddress().toByteArray());
    Assert.assertEquals(true, result);
  }

  //@Test
  public void proposalTest() {

    try {
      /*
       *  create proposal Test
       */
      DataWord key = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000000");
      // 1000000 == 0xF4240
      DataWord value = new DataWord(
          "00000000000000000000000000000000000000000000000000000000000F4240");
      byte[] data4Create = new byte[64];
      System.arraycopy(key.getData(), 0, data4Create, 0, key.getData().length);
      System
          .arraycopy(value.getData(), 0, data4Create,
              key.getData().length, value.getData().length);
      PrecompiledContract createContract = createPrecompiledContract(proposalCreateAddr,
          WITNESS_ADDRESS);

      Assert.assertEquals(0, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      ProposalCapsule proposalCapsule;
      Repository deposit1 = RepositoryImpl.createRoot(StoreFactory.getInstance());
      createContract.setRepository(deposit1);
      byte[] idBytes = createContract.execute(data4Create).getRight();
      long id = ByteUtil.byteArrayToLong(idBytes);
      deposit1.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalCapsule);
      Assert.assertEquals(1, dbManager.getDynamicPropertiesStore().getLatestProposalNum());
      Assert.assertEquals(0, proposalCapsule.getApprovals().size());
      Assert.assertEquals(1000000, proposalCapsule.getCreateTime());
      Assert.assertEquals(261200000, proposalCapsule.getExpirationTime()
      ); // 2000000 + 3 * 4 * 21600000



      /*
       *  approve proposal Test
       */

      byte[] data4Approve = new byte[64];
      DataWord isApprove = new DataWord(
          "0000000000000000000000000000000000000000000000000000000000000001");
      System.arraycopy(idBytes, 0, data4Approve, 0, idBytes.length);
      System.arraycopy(isApprove.getData(), 0, data4Approve, idBytes.length,
          isApprove.getData().length);
      PrecompiledContract approveContract = createPrecompiledContract(proposalApproveAddr,
          WITNESS_ADDRESS);
      Repository deposit2 = RepositoryImpl.createRoot(StoreFactory.getInstance());
      approveContract.setRepository(deposit2);
      approveContract.execute(data4Approve);
      deposit2.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(1, proposalCapsule.getApprovals().size());
      Assert.assertEquals(ByteString.copyFrom(ByteArray.fromHexString(WITNESS_ADDRESS)),
          proposalCapsule.getApprovals().get(0));

      /*
       *  delete proposal Test
       */
      PrecompiledContract deleteContract = createPrecompiledContract(proposalDeleteAddr,
          WITNESS_ADDRESS);
      Repository deposit3 = RepositoryImpl.createRoot(StoreFactory.getInstance());
      deleteContract.setRepository(deposit3);
      deleteContract.execute(idBytes);
      deposit3.commit();
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertEquals(State.CANCELED, proposalCapsule.getState());

    } catch (ItemNotFoundException e) {
      Assert.fail();
    }
  }

  @Test
  public void tvmFreezeV2SwitchTest() {
    VMConfig.initAllowTvmFreezeV2(0L);

    PrecompiledContract getChainParameterPcc =
        PrecompiledContracts.getContractForAddress(getChainParameterAddr);
    PrecompiledContract availableUnfreezeV2SizePcc =
        PrecompiledContracts.getContractForAddress(availableUnfreezeV2SizeAddr);
    PrecompiledContract unfreezableBalanceV2Pcc =
        PrecompiledContracts.getContractForAddress(unfreezableBalanceV2Addr);
    PrecompiledContract expireUnfreezeBalanceV2Pcc =
        PrecompiledContracts.getContractForAddress(expireUnfreezeBalanceV2Addr);

    PrecompiledContract delegatableResourcePcc =
        PrecompiledContracts.getContractForAddress(delegatableResourceAddr);
    PrecompiledContract resourceV2Pcc =
        PrecompiledContracts.getContractForAddress(resourceV2Addr);
    PrecompiledContract checkUnDelegateResourcePcc =
        PrecompiledContracts.getContractForAddress(checkUnDelegateResourceAddr);

    PrecompiledContract resourceUsagePcc =
        PrecompiledContracts.getContractForAddress(resourceUsageAddr);
    PrecompiledContract totalResourcePcc =
        PrecompiledContracts.getContractForAddress(totalResourceAddr);
    PrecompiledContract totalDelegatedResourcePcc =
        PrecompiledContracts.getContractForAddress(totalDelegatedResourceAddr);
    PrecompiledContract totalAcquiredResourcePcc =
        PrecompiledContracts.getContractForAddress(totalAcquiredResourceAddr);

    Assert.assertNull(getChainParameterPcc);
    Assert.assertNull(availableUnfreezeV2SizePcc);
    Assert.assertNull(expireUnfreezeBalanceV2Pcc);
    Assert.assertNull(unfreezableBalanceV2Pcc);

    Assert.assertNull(delegatableResourcePcc);
    Assert.assertNull(resourceV2Pcc);
    Assert.assertNull(checkUnDelegateResourcePcc);

    Assert.assertNull(resourceUsagePcc);
    Assert.assertNull(totalResourcePcc);
    Assert.assertNull(totalDelegatedResourcePcc);
    Assert.assertNull(totalAcquiredResourcePcc);

    // enable TvmFreezeV2.
    VMConfig.initAllowTvmFreezeV2(1L);

    getChainParameterPcc =
        PrecompiledContracts.getContractForAddress(getChainParameterAddr);
    availableUnfreezeV2SizePcc =
        PrecompiledContracts.getContractForAddress(availableUnfreezeV2SizeAddr);
    unfreezableBalanceV2Pcc =
        PrecompiledContracts.getContractForAddress(unfreezableBalanceV2Addr);
    expireUnfreezeBalanceV2Pcc =
        PrecompiledContracts.getContractForAddress(expireUnfreezeBalanceV2Addr);

    delegatableResourcePcc =
        PrecompiledContracts.getContractForAddress(delegatableResourceAddr);
    resourceV2Pcc =
        PrecompiledContracts.getContractForAddress(resourceV2Addr);
    checkUnDelegateResourcePcc =
        PrecompiledContracts.getContractForAddress(checkUnDelegateResourceAddr);

    resourceUsagePcc =
        PrecompiledContracts.getContractForAddress(resourceUsageAddr);
    totalResourcePcc =
        PrecompiledContracts.getContractForAddress(totalResourceAddr);
    totalDelegatedResourcePcc =
        PrecompiledContracts.getContractForAddress(totalDelegatedResourceAddr);
    totalAcquiredResourcePcc =
        PrecompiledContracts.getContractForAddress(totalAcquiredResourceAddr);

    Assert.assertNotNull(getChainParameterPcc);
    Assert.assertNotNull(availableUnfreezeV2SizePcc);
    Assert.assertNotNull(expireUnfreezeBalanceV2Pcc);
    Assert.assertNotNull(unfreezableBalanceV2Pcc);

    Assert.assertNotNull(delegatableResourcePcc);
    Assert.assertNotNull(resourceV2Pcc);
    Assert.assertNotNull(checkUnDelegateResourcePcc);

    Assert.assertNotNull(resourceUsagePcc);
    Assert.assertNotNull(totalResourcePcc);
    Assert.assertNotNull(totalDelegatedResourcePcc);
    Assert.assertNotNull(totalAcquiredResourcePcc);
  }

  @Test
  public void delegatableResourceTest() {
    VMConfig.initAllowTvmFreezeV2(1L);

    PrecompiledContract delegatableResourcePcc =
        createPrecompiledContract(delegatableResourceAddr, OWNER_ADDRESS);
    Repository tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    delegatableResourcePcc.setRepository(tempRepository);

    byte[] owner = new DataWord(ByteArray.fromHexString(OWNER_ADDRESS)).getData();
    byte[] zero = DataWord.ZERO().getData();
    byte[] one = new DataWord(1).getData();
    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);

    Pair<Boolean, byte[]> res = delegatableResourcePcc.execute(null);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(res.getRight()));

    res = delegatableResourcePcc.execute(encodeMultiWord(one, zero));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(res.getRight()));

    res = delegatableResourcePcc.execute(encodeMultiWord(owner, owner));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(res.getRight()));

    AccountCapsule accountCapsule = tempRepository.getAccount(address);
    accountCapsule.setAcquiredDelegatedFrozenBalanceForEnergy(10_000_000L);
    accountCapsule.addFrozenBalanceForBandwidthV2(5_000_000L);
    accountCapsule.addFrozenBalanceForEnergyV2(10_000_000L);

    tempRepository.putAccountValue(address, accountCapsule);

    res = delegatableResourcePcc.execute(encodeMultiWord(owner, zero));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(5_000_000L, ByteArray.toLong(res.getRight()));

    res = delegatableResourcePcc.execute(encodeMultiWord(owner, one));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(10_000_000L, ByteArray.toLong(res.getRight()));

    // with usage.
    byte[] TOTAL_ENERGY_CURRENT_LIMIT = "TOTAL_ENERGY_CURRENT_LIMIT".getBytes();
    byte[] TOTAL_ENERGY_WEIGHT = "TOTAL_ENERGY_WEIGHT".getBytes();

    long energyLimit = 1_000_000_000_000L;
    tempRepository.getDynamicPropertiesStore().put(TOTAL_ENERGY_CURRENT_LIMIT,
        new BytesCapsule(ByteArray.fromLong(energyLimit)));

    long energyWeight = 1_000_000L; // unit: trx
//    tempRepository.getDynamicPropertiesStore().put(TOTAL_ENERGY_WEIGHT,
//        new BytesCapsule(ByteArray.fromLong(energyWeight)));
    tempRepository.saveTotalEnergyWeight(energyWeight);

    // used all energy, recovered 1/2, delegatable: 1/2
    accountCapsule.setEnergyUsage(20_000_000L);

    long currentSlot = latestTimestamp / 3_000;
    accountCapsule.setLatestConsumeTimeForEnergy(0L);
    accountCapsule.setNewWindowSize(Common.ResourceCode.ENERGY, currentSlot * 2);
    tempRepository.putAccountValue(address, accountCapsule);

    res = delegatableResourcePcc.execute(encodeMultiWord(owner, one));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(5_000_000L, ByteArray.toLong(res.getRight()));

    // all recovered.
    accountCapsule.setNewWindowSize(Common.ResourceCode.ENERGY, currentSlot);
    tempRepository.putAccountValue(address, accountCapsule);
    res = delegatableResourcePcc.execute(encodeMultiWord(owner, one));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(10_000_000L, ByteArray.toLong(res.getRight()));

    // all used.
    accountCapsule.setLatestConsumeTimeForEnergy(currentSlot);
    tempRepository.putAccountValue(address, accountCapsule);
    res = delegatableResourcePcc.execute(encodeMultiWord(owner, one));
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(res.getRight()));
  }

  @Test
  public void getChainParameterTest() {
    VMConfig.initAllowTvmFreezeV2(1L);

    PrecompiledContract getChainParameterPcc =
        createPrecompiledContract(getChainParameterAddr, OWNER_ADDRESS);
    Repository tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    getChainParameterPcc.setRepository(tempRepository);

    byte[] TOTAL_ENERGY_CURRENT_LIMIT = "TOTAL_ENERGY_CURRENT_LIMIT".getBytes();
    byte[] TOTAL_ENERGY_WEIGHT = "TOTAL_ENERGY_WEIGHT".getBytes();
    byte[] UNFREEZE_DELAY_DAYS = "UNFREEZE_DELAY_DAYS".getBytes();

    DataWord totalEnergyCurrentLimitId = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000001");

    DataWord totalEnergyWeightId = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000002");

    DataWord unfreezeDelayDaysId = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000000003");

    DataWord invalidId = new DataWord(
        "0000000000000000000000000000000000000000000000000000000000FFFFFF");

    long energyLimit = 9_000_000_000_000_000L;
    tempRepository.getDynamicPropertiesStore().put(TOTAL_ENERGY_CURRENT_LIMIT,
        new BytesCapsule(ByteArray.fromLong(energyLimit)));
    Pair<Boolean, byte[]> totalEnergyCurrentLimitRes =
        getChainParameterPcc.execute(totalEnergyCurrentLimitId.getData());
    Assert.assertTrue(totalEnergyCurrentLimitRes.getLeft());
    Assert.assertEquals(ByteArray.toLong(totalEnergyCurrentLimitRes.getRight()), energyLimit);

    long energyWeight = 1_000_000_000L;
    tempRepository.getDynamicPropertiesStore().put(TOTAL_ENERGY_WEIGHT,
        new BytesCapsule(ByteArray.fromLong(energyWeight)));
    Pair<Boolean, byte[]> totalEnergyWeightRes =
        getChainParameterPcc.execute(totalEnergyWeightId.getData());
    Assert.assertTrue(totalEnergyWeightRes.getLeft());
    Assert.assertEquals(ByteArray.toLong(totalEnergyWeightRes.getRight()), energyWeight);

    long delayDays = 3L;
    tempRepository.getDynamicPropertiesStore().put(UNFREEZE_DELAY_DAYS,
        new BytesCapsule(ByteArray.fromLong(delayDays)));
    Pair<Boolean, byte[]> delayDaysRes =
        getChainParameterPcc.execute(unfreezeDelayDaysId.getData());
    Assert.assertTrue(delayDaysRes.getLeft());
    Assert.assertEquals(ByteArray.toLong(delayDaysRes.getRight()), delayDays);

    long zero = 0L;
    Pair<Boolean, byte[]> invalidParamRes = getChainParameterPcc.execute(invalidId.getData());
    Assert.assertTrue(invalidParamRes.getLeft());
    Assert.assertEquals(ByteArray.toLong(invalidParamRes.getRight()), zero);

  }

  @Test
  public void availableUnfreezeV2SizeTest() {
    VMConfig.initAllowTvmFreezeV2(1L);

    PrecompiledContract availableUnfreezeV2SizePcc =
        createPrecompiledContract(availableUnfreezeV2SizeAddr, OWNER_ADDRESS);
    Repository tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    availableUnfreezeV2SizePcc.setRepository(tempRepository);

    byte[] data = new DataWord(ByteArray.fromHexString(OWNER_ADDRESS)).getData();
    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);

    Pair<Boolean, byte[]> res = availableUnfreezeV2SizePcc.execute(data);

    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(32L, ByteArray.toLong(res.getRight()));

    AccountCapsule accountCapsule = tempRepository.getAccount(address);
    accountCapsule.addUnfrozenV2List(
        Common.ResourceCode.BANDWIDTH, 1_000_000L, latestTimestamp + 86_400_000L);
    accountCapsule.addUnfrozenV2List(
        Common.ResourceCode.ENERGY, 1_000_000L, latestTimestamp + 86_400_000L);

    tempRepository.putAccountValue(address, accountCapsule);
    res = availableUnfreezeV2SizePcc.execute(data);

    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(30L, ByteArray.toLong(res.getRight()));

    // expired unfreeze action, available size keep the same.
    accountCapsule.addUnfrozenV2List(
        Common.ResourceCode.ENERGY, 1_000_000L, latestTimestamp - 100_000L);

    tempRepository.putAccountValue(address, accountCapsule);
    res = availableUnfreezeV2SizePcc.execute(data);

    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(30L, ByteArray.toLong(res.getRight()));
  }

  @Test
  public void unfreezableBalanceV2Test() {
    VMConfig.initAllowTvmFreezeV2(1L);

    PrecompiledContract unfreezableBalanceV2Pcc =
        createPrecompiledContract(unfreezableBalanceV2Addr, OWNER_ADDRESS);
    Repository tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    unfreezableBalanceV2Pcc.setRepository(tempRepository);

    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] address32 = new DataWord(ByteArray.fromHexString(OWNER_ADDRESS)).getData();
    byte[] type = ByteUtil.longTo32Bytes(0);
    byte[] data = ByteUtil.merge(address32, type);

    Pair<Boolean, byte[]> res = unfreezableBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(0, ByteArray.toLong(res.getRight()));

    AccountCapsule accountCapsule = tempRepository.getAccount(address);
    accountCapsule.addFrozenBalanceForBandwidthV2(1_000_000L);
    tempRepository.putAccountValue(address, accountCapsule);
    res = unfreezableBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(1_000_000L, ByteArray.toLong(res.getRight()));

    accountCapsule.addFrozenBalanceForBandwidthV2(1_000_000L);
    tempRepository.putAccountValue(address, accountCapsule);
    res = unfreezableBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(2_000_000L, ByteArray.toLong(res.getRight()));

    type = ByteUtil.longTo32Bytes(1);
    data = ByteUtil.merge(address32, type);
    accountCapsule.addFrozenBalanceForEnergyV2(1_000_000L);
    tempRepository.putAccountValue(address, accountCapsule);
    res = unfreezableBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(1_000_000L, ByteArray.toLong(res.getRight()));

    type = ByteUtil.longTo32Bytes(2);
    data = ByteUtil.merge(address32, type);
    accountCapsule.addFrozenForTronPowerV2(1_000_000L);
    tempRepository.putAccountValue(address, accountCapsule);
    res = unfreezableBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(1_000_000L, ByteArray.toLong(res.getRight()));

    // new test round
    tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    unfreezableBalanceV2Pcc.setRepository(tempRepository);

    byte[] owner = new DataWord(ByteArray.fromHexString(OWNER_ADDRESS)).getData();
    byte[] zero = DataWord.ZERO().getData();
    byte[] one = new DataWord(1).getData();

    Pair<Boolean, byte[]> result = unfreezableBalanceV2Pcc.execute(null);
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(result.getRight()));

    result = unfreezableBalanceV2Pcc.execute(encodeMultiWord(one, zero));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(result.getRight()));

    result = unfreezableBalanceV2Pcc.execute(encodeMultiWord(owner, owner));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(result.getRight()));

    AccountCapsule capsule = tempRepository.getAccount(address);
    capsule.addFrozenBalanceForBandwidthV2(5_000_000L);
    capsule.addFrozenBalanceForEnergyV2(10_000_000L);

    tempRepository.putAccountValue(address, capsule);

    result = unfreezableBalanceV2Pcc.execute(encodeMultiWord(owner, zero));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(5_000_000L, ByteArray.toLong(result.getRight()));

    result = unfreezableBalanceV2Pcc.execute(encodeMultiWord(owner, one));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(10_000_000L, ByteArray.toLong(result.getRight()));
  }

  @Test
  public void expireUnfreezeBalanceV2Test() {
    VMConfig.initAllowTvmFreezeV2(1L);

    PrecompiledContract expireUnfreezeBalanceV2Pcc =
        createPrecompiledContract(expireUnfreezeBalanceV2Addr, OWNER_ADDRESS);
    Repository tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    expireUnfreezeBalanceV2Pcc.setRepository(tempRepository);
    long now = dbManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();

    byte[] address = ByteArray.fromHexString(OWNER_ADDRESS);
    byte[] address32 = new DataWord(ByteArray.fromHexString(OWNER_ADDRESS)).getData();
    byte[] time = ByteUtil.longTo32Bytes(now / 1000);
    byte[] data = ByteUtil.merge(address32, time);

    Pair<Boolean, byte[]> res = expireUnfreezeBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(0, ByteArray.toLong(res.getRight()));

    AccountCapsule accountCapsule = tempRepository.getAccount(address);
    Protocol.Account.UnFreezeV2 unFreezeV2 =
        Protocol.Account.UnFreezeV2.newBuilder()
            .setType(Common.ResourceCode.BANDWIDTH)
            .setUnfreezeExpireTime(now)
            .setUnfreezeAmount(1_000_000L)
            .build();
    accountCapsule.addUnfrozenV2(unFreezeV2);
    tempRepository.putAccountValue(address, accountCapsule);
    res = expireUnfreezeBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(1_000_000L, ByteArray.toLong(res.getRight()));

    unFreezeV2 =
        Protocol.Account.UnFreezeV2.newBuilder()
            .setType(Common.ResourceCode.ENERGY)
            .setUnfreezeExpireTime(now)
            .setUnfreezeAmount(1_000_000L)
            .build();
    accountCapsule.addUnfrozenV2(unFreezeV2);
    tempRepository.putAccountValue(address, accountCapsule);
    res = expireUnfreezeBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(2_000_000L, ByteArray.toLong(res.getRight()));

    unFreezeV2 =
        Protocol.Account.UnFreezeV2.newBuilder()
            .setType(Common.ResourceCode.TRON_POWER)
            .setUnfreezeExpireTime(now + 1_000_000L)
            .setUnfreezeAmount(1_000_000L)
            .build();
    accountCapsule.addUnfrozenV2(unFreezeV2);
    tempRepository.putAccountValue(address, accountCapsule);
    res = expireUnfreezeBalanceV2Pcc.execute(data);
    Assert.assertTrue(res.getLeft());
    Assert.assertEquals(2_000_000L, ByteArray.toLong(res.getRight()));

    // new test round
    tempRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    expireUnfreezeBalanceV2Pcc.setRepository(tempRepository);

    byte[] owner = new DataWord(ByteArray.fromHexString(OWNER_ADDRESS)).getData();
    address = ByteArray.fromHexString(OWNER_ADDRESS);

    Pair<Boolean, byte[]> result = expireUnfreezeBalanceV2Pcc.execute(null);

    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(result.getRight()));

    result = expireUnfreezeBalanceV2Pcc.execute(
        encodeMultiWord(DataWord.ZERO().getData(), DataWord.ZERO().getData()));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(result.getRight()));

    result = expireUnfreezeBalanceV2Pcc.execute(
        encodeMultiWord(owner, new DataWord(latestTimestamp / 1_000L).getData()));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(0L, ByteArray.toLong(result.getRight()));

    accountCapsule = tempRepository.getAccount(address);
    accountCapsule.addUnfrozenV2List(
        Common.ResourceCode.BANDWIDTH, 1_000_000L, latestTimestamp);
    accountCapsule.addUnfrozenV2List(
        Common.ResourceCode.ENERGY, 1_000_000L, latestTimestamp + 86_400_000L);

    tempRepository.putAccountValue(address, accountCapsule);

    result = expireUnfreezeBalanceV2Pcc.execute(
        encodeMultiWord(owner, new DataWord(latestTimestamp / 1_000L).getData()));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(1_000_000L, ByteArray.toLong(result.getRight()));

    result = expireUnfreezeBalanceV2Pcc.execute(
        encodeMultiWord(owner, new DataWord((latestTimestamp + 86_400_000L) / 1_000L).getData()));
    Assert.assertTrue(result.getLeft());
    Assert.assertEquals(2_000_000L, ByteArray.toLong(result.getRight()));
  }

  @Test
  public void convertFromTronBytesAddressNativeTest() {
  }

  //@Test
  public void convertFromTronBase58AddressNative() {
    // 27WnTihwXsqCqpiNedWvtKCZHsLjDt4Hfmf  TestNet address
    DataWord word1 = new DataWord(
        "3237576e54696877587371437170694e65645776744b435a48734c6a44743448");
    DataWord word2 = new DataWord(
        "666d660000000000000000000000000000000000000000000000000000000000");

    byte[] data = new byte[35];
    System.arraycopy(word1.getData(), 0, data, 0, word1.getData().length);
    System.arraycopy(Arrays.copyOfRange(word2.getData(), 0, 3), 0,
        data, word1.getData().length, 3);
    PrecompiledContract contract = createPrecompiledContract(convertFromTronBase58AddressAddr,
        WITNESS_ADDRESS);

    byte[] solidityAddress = contract.execute(data).getRight();
    Assert.assertArrayEquals(solidityAddress,
        new DataWord(Hex.decode(WITNESS_ADDRESS_BASE)).getData());
  }

  private static byte[] encodeMultiWord(byte[]... words) {
    if (words == null) {
      return null;
    }
    if (words.length == 1) {
      return words[0];
    }

    byte[] res = new byte[words.length * 32];

    for (int i = 0; i < words.length; i++) {
      byte[] word = stripLeadingZeroes(words[i]);

      System.arraycopy(word, 0, res, 32 * (i + 1) - word.length, word.length);
    }

    return res;
  }

}
