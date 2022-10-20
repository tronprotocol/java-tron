package org.tron.common.runtime.vm;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.REVERT;
import static org.tron.protos.Protocol.Transaction.Result.contractResult.SUCCESS;
import static org.tron.protos.contract.Common.ResourceCode.ENERGY;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FastByteComparisons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DelegatedResourceStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.EnergyCost;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class FreezeTest {

  private static final String CONTRACT_CODE = "608060405261037e806100136000396000f3fe6080604052"
      + "34801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506004361061"
      + "00655760003560e01c8062f55d9d1461006a57806330e1e4e5146100ae5780637b46b80b1461011a578063e7"
      + "aa4e0b1461017c575b600080fd5b6100ac6004803603602081101561008057600080fd5b81019080803573ff"
      + "ffffffffffffffffffffffffffffffffffffff1690602001909291905050506101de565b005b610104600480"
      + "360360608110156100c457600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16"
      + "906020019092919080359060200190929190803590602001909291905050506101f7565b6040518082815260"
      + "200191505060405180910390f35b6101666004803603604081101561013057600080fd5b81019080803573ff"
      + "ffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506102f0"
      + "565b6040518082815260200191505060405180910390f35b6101c86004803603604081101561019257600080"
      + "fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001"
      + "90929190505050610327565b6040518082815260200191505060405180910390f35b8073ffffffffffffffff"
      + "ffffffffffffffffffffffff16ff5b60008373ffffffffffffffffffffffffffffffffffffffff168383d515"
      + "8015610224573d6000803e3d6000fd5b50423073ffffffffffffffffffffffffffffffffffffffff1663e7aa"
      + "4e0b86856040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffff"
      + "ff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506020604051"
      + "8083038186803b1580156102ab57600080fd5b505afa1580156102bf573d6000803e3d6000fd5b5050505060"
      + "40513d60208110156102d557600080fd5b81019080805190602001909291905050500390509392505050565b"
      + "60008273ffffffffffffffffffffffffffffffffffffffff1682d615801561031c573d6000803e3d6000fd5b"
      + "506001905092915050565b60008273ffffffffffffffffffffffffffffffffffffffff1682d7905092915050"
      + "56fea26474726f6e58200fd975eab4a8c8afe73bf3841efe4da7832d5a0d09f07115bb695c7260ea64216473"
      + "6f6c63430005100031";
  private static final String FACTORY_CODE = "6080604052610640806100136000396000f3fe60806040523"
      + "4801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060043610610"
      + "0505760003560e01c806341aa901414610055578063bb63e785146100c3575b600080fd5b610081600480360"
      + "3602081101561006b57600080fd5b8101908080359060200190929190505050610131565b604051808273fff"
      + "fffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526"
      + "0200191505060405180910390f35b6100ef600480360360208110156100d957600080fd5b810190808035906"
      + "020019092919050505061017d565b604051808273ffffffffffffffffffffffffffffffffffffffff1673fff"
      + "fffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b600080606060405"
      + "1806020016101469061026e565b6020820181038252601f19601f82011660405250905083815160208301600"
      + "0f59150813b61017357600080fd5b8192505050919050565b60008060a060f81b30846040518060200161019"
      + "79061026e565b6020820181038252601f19601f820116604052508051906020012060405160200180857efff"
      + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff19167efffffffffffffffffffffff"
      + "fffffffffffffffffffffffffffffffffffffff191681526001018473fffffffffffffffffffffffffffffff"
      + "fffffffff1673ffffffffffffffffffffffffffffffffffffffff1660601b815260140183815260200182815"
      + "26020019450505050506040516020818303038152906040528051906020012060001c9050809150509190505"
      + "65b6103918061027c8339019056fe608060405261037e806100136000396000f3fe608060405234801561001"
      + "057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100655760003"
      + "560e01c8062f55d9d1461006a57806330e1e4e5146100ae5780637b46b80b1461011a578063e7aa4e0b14610"
      + "17c575b600080fd5b6100ac6004803603602081101561008057600080fd5b81019080803573fffffffffffff"
      + "fffffffffffffffffffffffffff1690602001909291905050506101de565b005b61010460048036036060811"
      + "0156100c457600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909"
      + "2919080359060200190929190803590602001909291905050506101f7565b604051808281526020019150506"
      + "0405180910390f35b6101666004803603604081101561013057600080fd5b81019080803573fffffffffffff"
      + "fffffffffffffffffffffffffff169060200190929190803590602001909291905050506102f0565b6040518"
      + "082815260200191505060405180910390f35b6101c86004803603604081101561019257600080fd5b8101908"
      + "0803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505"
      + "050610327565b6040518082815260200191505060405180910390f35b8073fffffffffffffffffffffffffff"
      + "fffffffffffff16ff5b60008373ffffffffffffffffffffffffffffffffffffffff168383d51580156102245"
      + "73d6000803e3d6000fd5b50423073ffffffffffffffffffffffffffffffffffffffff1663e7aa4e0b8685604"
      + "0518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673fffff"
      + "fffffffffffffffffffffffffffffffffff16815260200182815260200192505050602060405180830381868"
      + "03b1580156102ab57600080fd5b505afa1580156102bf573d6000803e3d6000fd5b505050506040513d60208"
      + "110156102d557600080fd5b81019080805190602001909291905050500390509392505050565b60008273fff"
      + "fffffffffffffffffffffffffffffffffffff1682d615801561031c573d6000803e3d6000fd5b50600190509"
      + "2915050565b60008273ffffffffffffffffffffffffffffffffffffffff1682d790509291505056fea264747"
      + "26f6e58200fd975eab4a8c8afe73bf3841efe4da7832d5a0d09f07115bb695c7260ea642164736f6c6343000"
      + "5100031a26474726f6e5820403c4e856a1ab2fe0eeaf6b157c29c07fef7a9e9bdc6f0faac870d2d8873159d6"
      + "4736f6c63430005100031";

  private static final long value = 100_000_000_000_000_000L;
  private static final long fee = 1_000_000_000;
  private static final String userAStr = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
  private static final byte[] userA = Commons.decode58Check(userAStr);
  private static final String userBStr = "27jzp7nVEkH4Hf3H1PHPp4VDY7DxTy5eydL";
  private static final byte[] userB = Commons.decode58Check(userBStr);
  private static final String userCStr = "27juXSbMvL6pb8VgmKRgW6ByCfw5RqZjUuo";
  private static final byte[] userC = Commons.decode58Check(userCStr);

  private static String dbPath;
  private static TronApplicationContext context;
  private static Manager manager;
  private static byte[] owner;
  private static Repository rootRepository;

  @Before
  public void init() throws Exception {
    dbPath = "output_" + FreezeTest.class.getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    manager = context.getBean(Manager.class);
    owner = Hex.decode(Wallet.getAddressPreFixString()
        + "abd4b9367799eaa3197fecb144eb71de1e049abc");
    rootRepository = RepositoryImpl.createRoot(StoreFactory.getInstance());
    rootRepository.createAccount(owner, Protocol.AccountType.Normal);
    rootRepository.addBalance(owner, 900_000_000_000_000_000L);
    rootRepository.commit();

    ConfigLoader.disable = true;
    manager.getDynamicPropertiesStore().saveAllowTvmFreeze(1);
    VMConfig.initVmHardFork(true);
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmIstanbul(1);
    VMConfig.initAllowTvmFreeze(1);
    VMConfig.initAllowTvmFreezeV2(0);
  }

  private byte[] deployContract(String contractName, String code) throws Exception {
    return deployContract(owner, contractName, code, 0, 100_000);
  }

  private byte[] deployContract(byte[] deployer,
                                String contractName,
                                String code,
                                long consumeUserResourcePercent,
                                long originEnergyLimit) throws Exception {
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, deployer, "[]", code, value, fee, consumeUserResourcePercent,
        null, originEnergyLimit);
    byte[] contractAddr = WalletUtil.generateContractAddress(trx);
    //String contractAddrStr = StringUtil.encode58Check(contractAddr);
    TransactionCapsule trxCap = new TransactionCapsule(trx);
    TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
        new RuntimeImpl());
    trxCap.setTrxTrace(trace);
    trace.init(null);
    trace.exec();
    trace.finalization();
    Runtime runtime = trace.getRuntime();
    Assert.assertEquals(SUCCESS, runtime.getResult().getResultCode());
    Assert.assertEquals(value, manager.getAccountStore().get(contractAddr).getBalance());

    return contractAddr;
  }

  private TVMTestResult triggerContract(byte[] callerAddr,
                                        byte[] contractAddr,
                                        long feeLimit,
                                        contractResult expectedResult,
                                        Consumer<byte[]> check,
                                        String method,
                                        Object... args) throws Exception {
    String hexInput = AbiUtil.parseMethod(method, Arrays.asList(args));
    TransactionCapsule trxCap = new TransactionCapsule(
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            callerAddr, contractAddr, Hex.decode(hexInput), 0, feeLimit));
    TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
        new RuntimeImpl());
    trxCap.setTrxTrace(trace);
    trace.init(null);
    trace.exec();
    trace.finalization();
    trace.setResult();
    TVMTestResult result = new TVMTestResult(trace.getRuntime(), trace.getReceipt(), null);
    Assert.assertEquals(expectedResult, result.getReceipt().getResult());
    if (check != null) {
      check.accept(result.getRuntime().getResult().getHReturn());
    }
    return result;
  }

  private TVMTestResult triggerFreeze(byte[] callerAddr,
                                      byte[] contractAddr,
                                      byte[] receiverAddr,
                                      long frozenBalance,
                                      long res,
                                      contractResult expectedResult,
                                      Consumer<byte[]> check) throws Exception {
    return triggerContract(callerAddr, contractAddr, fee, expectedResult, check,
        "freeze(address,uint256,uint256)", StringUtil.encode58Check(receiverAddr), frozenBalance,
        res);
  }

  private TVMTestResult triggerUnfreeze(byte[] callerAddr,
                                        byte[] contractAddr,
                                        byte[] receiverAddr,
                                        long res,
                                        contractResult expectedResult,
                                        Consumer<byte[]> check) throws Exception {
    return triggerContract(callerAddr, contractAddr, fee, expectedResult, check,
        "unfreeze(address,uint256)", StringUtil.encode58Check(receiverAddr), res);
  }

  private TVMTestResult triggerSuicide(byte[] callerAddr,
                                       byte[] contractAddr,
                                       byte[] inheritorAddr,
                                       contractResult expectedResult,
                                       Consumer<byte[]> check) throws Exception {
    return triggerContract(callerAddr, contractAddr, fee, expectedResult, check,
        "destroy(address)", StringUtil.encode58Check(inheritorAddr));
  }

  private void setBalance(byte[] accountAddr,
                          long balance) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(accountAddr);
    if (accountCapsule == null) {
      accountCapsule = new AccountCapsule(ByteString.copyFrom(accountAddr),
          Protocol.AccountType.Normal);
    }
    accountCapsule.setBalance(balance);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private void setFrozenForEnergy(byte[] accountAddr, long frozenBalance) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(accountAddr);
    if (accountCapsule == null) {
      accountCapsule = new AccountCapsule(ByteString.copyFrom(accountAddr),
          Protocol.AccountType.Normal);
    }
    accountCapsule.setFrozenForEnergy(frozenBalance, 0);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private byte[] getCreate2Addr(byte[] factoryAddr,
                                long salt) throws Exception {
    TVMTestResult result = triggerContract(
        owner, factoryAddr, fee, SUCCESS, null, "getCreate2Addr(uint256)", salt);
    return TransactionTrace.convertToTronAddress(
        new DataWord(result.getRuntime().getResult().getHReturn()).getLast20Bytes());
  }

  private byte[] deployCreate2Contract(byte[] factoryAddr,
                                       long salt) throws Exception {
    TVMTestResult result = triggerContract(
        owner, factoryAddr, fee, SUCCESS, null, "deployCreate2Contract(uint256)", salt);
    return TransactionTrace.convertToTronAddress(
        new DataWord(result.getRuntime().getResult().getHReturn()).getLast20Bytes());
  }

  @Test
  public void testWithCallerEnergyChangedInTx() throws Exception {
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 10_000_000;
    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule account = new AccountCapsule(ByteString.copyFromUtf8("Yang"),
        ByteString.copyFrom(userA), Protocol.AccountType.Normal, 10_000_000);
    account.setFrozenForEnergy(10_000_000, 1);
    accountStore.put(account.createDbKey(), account);
    manager.getDynamicPropertiesStore().addTotalEnergyWeight(10);

    TVMTestResult result = freezeForOther(userA, contractAddr, userA, frozenBalance, 1);

    System.out.println(result.getReceipt().getEnergyUsageTotal());
    System.out.println(accountStore.get(userA));
    System.out.println(accountStore.get(owner));

    clearDelegatedExpireTime(contractAddr, userA);

    result = unfreezeForOther(userA, contractAddr, userA, 1);

    System.out.println(result.getReceipt().getEnergyUsageTotal());
    System.out.println(accountStore.get(userA));
    System.out.println(accountStore.get(owner));
  }

  @Test
  public void testFreezeAndUnfreeze() throws Exception {
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;

    // trigger freezeForSelf(uint256,uint256) to get bandwidth
    freezeForSelf(contract, frozenBalance, 0);

    // trigger freezeForSelf(uint256,uint256) to get energy
    freezeForSelf(contract, frozenBalance, 1);

    // tests of freezeForSelf(uint256,uint256) with invalid args
    freezeForSelfWithException(contract, frozenBalance, 2);
    freezeForSelfWithException(contract, 0, 0);
    freezeForSelfWithException(contract, -frozenBalance, 0);
    freezeForSelfWithException(contract, frozenBalance - 1, 1);
    freezeForSelfWithException(contract, value, 0);

    // not time to unfreeze
    unfreezeForSelfWithException(contract, 0);
    unfreezeForSelfWithException(contract, 1);
    // invalid args
    unfreezeForSelfWithException(contract, 2);

    clearExpireTime(contract);

    unfreezeForSelfWithException(contract, 2);
    unfreezeForSelf(contract, 0);
    unfreezeForSelf(contract, 1);
    unfreezeForSelfWithException(contract, 0);
    unfreezeForSelfWithException(contract, 1);

    long energyWithCreatingAccountA = freezeForOther(contract, userA, frozenBalance, 0)
        .getReceipt().getEnergyUsageTotal();

    long energyWithoutCreatingAccountA = freezeForOther(contract, userA, frozenBalance, 0)
        .getReceipt().getEnergyUsageTotal();
    Assert.assertEquals(energyWithCreatingAccountA - EnergyCost.getNewAcctCall(),
        energyWithoutCreatingAccountA);

    freezeForOther(contract, userA, frozenBalance, 1);

    long energyWithCreatingAccountB = freezeForOther(contract, userB, frozenBalance, 1)
        .getReceipt().getEnergyUsageTotal();

    long energyWithoutCreatingAccountB = freezeForOther(contract, userB, frozenBalance, 1)
        .getReceipt().getEnergyUsageTotal();
    Assert.assertEquals(energyWithCreatingAccountB - EnergyCost.getNewAcctCall(),
        energyWithoutCreatingAccountB);

    freezeForOther(contract, userB, frozenBalance, 0);

    freezeForOtherWithException(contract, userC, frozenBalance, 2);
    freezeForOtherWithException(contract, userC, 0, 0);
    freezeForOtherWithException(contract, userB, -frozenBalance, 0);
    freezeForOtherWithException(contract, userC, frozenBalance - 1, 1);
    freezeForOtherWithException(contract, userB, value, 0);
    freezeForOtherWithException(contract,
        deployContract("OtherContract", CONTRACT_CODE), frozenBalance, 0);

    unfreezeForOtherWithException(contract, userA, 0);
    unfreezeForOtherWithException(contract, userA, 1);
    unfreezeForOtherWithException(contract, userA, 2);
    unfreezeForOtherWithException(contract, userC, 0);
    unfreezeForOtherWithException(contract, userC, 2);

    clearDelegatedExpireTime(contract, userA);

    unfreezeForOtherWithException(contract, userA, 2);
    unfreezeForOther(contract, userA, 0);
    unfreezeForOther(contract, userA, 1);
    unfreezeForOtherWithException(contract, userA, 0);
    unfreezeForOtherWithException(contract, userA, 1);
  }

  @Test
  public void testFreezeAndUnfreezeToCreate2Contract() throws Exception {
    byte[] factoryAddr = deployContract("FactoryContract", FACTORY_CODE);
    byte[] contractAddr = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    long salt = 1;
    byte[] predictedAddr = getCreate2Addr(factoryAddr, salt);
    Assert.assertNull(manager.getAccountStore().get(predictedAddr));
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 0);
    Assert.assertNotNull(manager.getAccountStore().get(predictedAddr));
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 1);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 0);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 1);
    clearDelegatedExpireTime(contractAddr, predictedAddr);
    unfreezeForOther(contractAddr, predictedAddr, 0);
    unfreezeForOther(contractAddr, predictedAddr, 1);

    freezeForOther(contractAddr, predictedAddr, frozenBalance, 0);
    freezeForOther(contractAddr, predictedAddr, frozenBalance, 1);
    Assert.assertArrayEquals(predictedAddr, deployCreate2Contract(factoryAddr, salt));
    freezeForOtherWithException(contractAddr, predictedAddr, frozenBalance, 0);
    freezeForOtherWithException(contractAddr, predictedAddr, frozenBalance, 1);
    clearDelegatedExpireTime(contractAddr, predictedAddr);
    unfreezeForOther(contractAddr, predictedAddr, 0);
    unfreezeForOther(contractAddr, predictedAddr, 1);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 0);
    unfreezeForOtherWithException(contractAddr, predictedAddr, 1);

    setBalance(predictedAddr, 100_000_000);
    freezeForSelf(predictedAddr, frozenBalance, 0);
    freezeForSelf(predictedAddr, frozenBalance, 1);
    freezeForOther(predictedAddr, userA, frozenBalance, 0);
    freezeForOther(predictedAddr, userA, frozenBalance, 1);
    clearExpireTime(predictedAddr);
    unfreezeForSelf(predictedAddr, 0);
    unfreezeForSelf(predictedAddr, 1);
    clearDelegatedExpireTime(predictedAddr, userA);
    unfreezeForOther(predictedAddr, userA, 0);
    unfreezeForOther(predictedAddr, userA, 1);
  }

  @Test
  public void testContractSuicideToBlackHole() throws Exception {
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    freezeForOther(contract, userA, frozenBalance, 0);
    freezeForOther(contract, userA, frozenBalance, 1);
    freezeForOther(contract, userB, frozenBalance, 0);
    freezeForOther(contract, userB, frozenBalance, 1);
    suicideWithException(contract, contract);
    clearDelegatedExpireTime(contract, userA);
    unfreezeForOther(contract, userA, 0);
    unfreezeForOther(contract, userA, 1);
    suicideWithException(contract, contract);
    clearDelegatedExpireTime(contract, userB);
    unfreezeForOther(contract, userB, 0);
    unfreezeForOther(contract, userB, 1);
    suicideToAccount(contract, contract);
  }

  @Test
  public void testContractSuicideToNonExistAccount() throws Exception {
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    freezeForOther(contract, userA, frozenBalance, 0);
    freezeForOther(contract, userA, frozenBalance, 1);
    suicideWithException(contract, userB);
    clearDelegatedExpireTime(contract, userA);
    unfreezeForOther(contract, userA, 0);
    unfreezeForOther(contract, userA, 1);
    suicideToAccount(contract, userB);
  }

  @Test
  public void testContractSuicideToExistNormalAccount() throws Exception {
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    freezeForOther(contract, userA, frozenBalance, 0);
    freezeForOther(contract, userA, frozenBalance, 1);
    suicideWithException(contract, userA);
    clearDelegatedExpireTime(contract, userA);
    unfreezeForOther(contract, userA, 0);
    unfreezeForOther(contract, userA, 1);
    suicideToAccount(contract, userA);
  }

  @Test
  public void testContractSuicideToExistContractAccount() throws Exception {
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    byte[] otherContract = deployContract("OtherTestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    freezeForOther(contract, userA, frozenBalance, 0);
    freezeForOther(contract, userA, frozenBalance, 1);
    suicideWithException(contract, otherContract);
    clearDelegatedExpireTime(contract, userA);
    unfreezeForOther(contract, userA, 0);
    unfreezeForOther(contract, userA, 1);
    freezeForSelf(otherContract, frozenBalance, 0);
    freezeForSelf(otherContract, frozenBalance, 1);
    freezeForOther(otherContract, userA, frozenBalance, 0);
    freezeForOther(otherContract, userA, frozenBalance, 1);
    suicideToAccount(contract, otherContract);
    suicideWithException(otherContract, contract);
    clearDelegatedExpireTime(otherContract, userA);
    unfreezeForOther(otherContract, userA, 0);
    unfreezeForOther(otherContract, userA, 1);
    suicideToAccount(otherContract, contract);
  }

  @Test
  public void testCreate2SuicideToBlackHole() throws Exception {
    byte[] factory = deployContract("FactoryContract", FACTORY_CODE);
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    long salt = 1;
    byte[] predictedAddr = getCreate2Addr(factory, salt);
    freezeForOther(contract, predictedAddr, frozenBalance, 0);
    freezeForOther(contract, predictedAddr, frozenBalance, 1);
    Assert.assertArrayEquals(predictedAddr, deployCreate2Contract(factory, salt));
    setBalance(predictedAddr, 100_000_000);
    freezeForSelf(predictedAddr, frozenBalance, 0);
    freezeForSelf(predictedAddr, frozenBalance, 1);
    freezeForOther(predictedAddr, userA, frozenBalance, 0);
    freezeForOther(predictedAddr, userA, frozenBalance, 1);
    suicideWithException(predictedAddr, predictedAddr);
    clearDelegatedExpireTime(predictedAddr, userA);
    unfreezeForOther(predictedAddr, userA, 0);
    unfreezeForOther(predictedAddr, userA, 1);
    suicideToAccount(predictedAddr, predictedAddr);

    unfreezeForOtherWithException(contract, predictedAddr, 0);
    unfreezeForOtherWithException(contract, predictedAddr, 1);
    clearDelegatedExpireTime(contract, predictedAddr);
    unfreezeForOther(contract, predictedAddr, 0);
    unfreezeForOther(contract, predictedAddr, 1);
  }

  @Test
  public void testCreate2SuicideToAccount() throws Exception {
    byte[] factory = deployContract("FactoryContract", FACTORY_CODE);
    byte[] contract = deployContract("TestFreeze", CONTRACT_CODE);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    long salt = 2;
    byte[] predictedAddr = getCreate2Addr(factory, salt);
    freezeForOther(contract, predictedAddr, frozenBalance, 0);
    freezeForOther(contract, predictedAddr, frozenBalance, 1);
    Assert.assertArrayEquals(predictedAddr, deployCreate2Contract(factory, salt));
    setBalance(predictedAddr, 100_000_000);
    freezeForSelf(predictedAddr, frozenBalance, 0);
    freezeForSelf(predictedAddr, frozenBalance, 1);
    freezeForOther(predictedAddr, userA, frozenBalance, 0);
    freezeForOther(predictedAddr, userA, frozenBalance, 1);
    suicideWithException(predictedAddr, contract);
    clearDelegatedExpireTime(predictedAddr, userA);
    unfreezeForOther(predictedAddr, userA, 0);
    unfreezeForOther(predictedAddr, userA, 1);
    suicideToAccount(predictedAddr, contract);

    unfreezeForOtherWithException(contract, predictedAddr, 0);
    unfreezeForOtherWithException(contract, predictedAddr, 1);
    clearDelegatedExpireTime(contract, predictedAddr);
    unfreezeForOther(contract, predictedAddr, 0);
    unfreezeForOther(contract, predictedAddr, 1);
  }

  @Test
  public void testFreezeEnergyToCaller() throws Exception {
    byte[] contract = deployContract(owner, "TestFreeze", CONTRACT_CODE, 50, 10_000);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    setBalance(userA, 100_000_000);
    setFrozenForEnergy(owner, frozenBalance);
    AccountCapsule caller = manager.getAccountStore().get(userA);
    AccountCapsule deployer = manager.getAccountStore().get(owner);
    TVMTestResult result = freezeForOther(userA, contract, userA, frozenBalance, 1);
    checkReceipt(result, caller, deployer);
  }

  @Test
  public void testFreezeEnergyToDeployer() throws Exception {
    byte[] contract = deployContract(owner, "TestFreeze", CONTRACT_CODE, 50, 10_000);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    setBalance(userA, 100_000_000);
    setFrozenForEnergy(owner, frozenBalance);
    AccountCapsule caller = manager.getAccountStore().get(userA);
    AccountCapsule deployer = manager.getAccountStore().get(owner);
    TVMTestResult result = freezeForOther(userA, contract, owner, frozenBalance, 1);
    checkReceipt(result, caller, deployer);
  }

  @Test
  public void testUnfreezeEnergyToCaller() throws Exception {
    byte[] contract = deployContract(owner, "TestFreeze", CONTRACT_CODE, 50, 10_000);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    setBalance(userA, 100_000_000);
    //setFrozenForEnergy(owner, frozenBalance);
    freezeForOther(contract, userA, frozenBalance, 1);
    freezeForOther(contract, owner, frozenBalance, 1);
    clearDelegatedExpireTime(contract, userA);
    AccountCapsule caller = manager.getAccountStore().get(userA);
    AccountCapsule deployer = manager.getAccountStore().get(owner);
    TVMTestResult result = unfreezeForOther(userA, contract, userA, 1);
    checkReceipt(result, caller, deployer);
  }

  @Test
  public void testUnfreezeEnergyToDeployer() throws Exception {
    byte[] contract = deployContract(owner, "TestFreeze", CONTRACT_CODE, 50, 10_000);
    long frozenBalance = 1_000_000;
    freezeForSelf(contract, frozenBalance, 0);
    freezeForSelf(contract, frozenBalance, 1);
    setBalance(userA, 100_000_000);
    //setFrozenForEnergy(owner, frozenBalance);
    freezeForOther(contract, userA, frozenBalance, 1);
    freezeForOther(contract, owner, frozenBalance, 1);
    clearDelegatedExpireTime(contract, owner);
    AccountCapsule caller = manager.getAccountStore().get(userA);
    AccountCapsule deployer = manager.getAccountStore().get(owner);
    TVMTestResult result = unfreezeForOther(userA, contract, owner, 1);
    checkReceipt(result, caller, deployer);
  }

  private void clearExpireTime(byte[] owner) {
    AccountCapsule accountCapsule = manager.getAccountStore().get(owner);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    accountCapsule.setFrozenForBandwidth(accountCapsule.getFrozenBalance(), now);
    accountCapsule.setFrozenForEnergy(accountCapsule.getEnergyFrozenBalance(), now);
    manager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
  }

  private void clearDelegatedExpireTime(byte[] owner,
                                        byte[] receiver) {
    byte[] key = DelegatedResourceCapsule.createDbKey(owner, receiver);
    DelegatedResourceCapsule delegatedResource = manager.getDelegatedResourceStore().get(key);
    long now = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    delegatedResource.setExpireTimeForBandwidth(now);
    delegatedResource.setExpireTimeForEnergy(now);
    manager.getDelegatedResourceStore().put(key, delegatedResource);
  }

  private TVMTestResult freezeForSelf(byte[] contractAddr,
                                      long frozenBalance,
                                      long res) throws Exception {
    return freezeForSelf(owner, contractAddr, frozenBalance, res);
  }

  private TVMTestResult freezeForSelf(byte[] callerAddr,
                                      byte[] contractAddr,
                                      long frozenBalance,
                                      long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);

    TVMTestResult result = triggerFreeze(callerAddr, contractAddr, contractAddr, frozenBalance, res,
        SUCCESS,
        returnValue -> Assert.assertEquals(dynamicStore.getMinFrozenTime() * FROZEN_PERIOD,
            new DataWord(returnValue).longValue() * 1000));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() - frozenBalance, newOwner.getBalance());
    newOwner.setBalance(oldOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(1, newOwner.getFrozenCount());
      Assert.assertEquals(oldOwner.getFrozenBalance() + frozenBalance, newOwner.getFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForBandwidth(0, 0);
      newOwner.setFrozenForBandwidth(0, 0);
    } else {
      Assert.assertEquals(oldOwner.getEnergyFrozenBalance() + frozenBalance,
          newOwner.getEnergyFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForEnergy(0, 0);
      newOwner.setFrozenForEnergy(0, 0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    return result;
  }

  private TVMTestResult freezeForSelfWithException(byte[] contractAddr,
                                                   long frozenBalance,
                                                   long res) throws Exception {
    return triggerFreeze(owner, contractAddr, contractAddr, frozenBalance, res, REVERT, null);
  }

  private TVMTestResult unfreezeForSelf(byte[] contractAddr,
                                        long res) throws Exception {
    return unfreezeForSelf(owner, contractAddr, res);
  }

  private TVMTestResult unfreezeForSelf(byte[] callerAddr,
                                        byte[] contractAddr,
                                        long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long frozenBalance = res == 0 ? oldOwner.getFrozenBalance() : oldOwner.getEnergyFrozenBalance();
    Assert.assertTrue(frozenBalance > 0);

    TVMTestResult result = triggerUnfreeze(callerAddr, contractAddr, contractAddr, res, SUCCESS,
        returnValue ->
            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
                Hex.toHexString(returnValue)));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() + frozenBalance, newOwner.getBalance());
    oldOwner.setBalance(newOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(0, newOwner.getFrozenCount());
      Assert.assertEquals(0, newOwner.getFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight - frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForBandwidth(0, 0);
      newOwner.setFrozenForBandwidth(0, 0);
    } else {
      Assert.assertEquals(0, newOwner.getEnergyFrozenBalance());
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight - frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
      oldOwner.setFrozenForEnergy(0, 0);
      newOwner.setFrozenForEnergy(0, 0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    return result;
  }

  private TVMTestResult unfreezeForSelfWithException(byte[] contractAddr,
                                                     long res) throws Exception {
    return triggerUnfreeze(owner, contractAddr, contractAddr, res, REVERT, null);
  }

  private TVMTestResult freezeForOther(
      byte[] contractAddr,
      byte[] receiverAddr,
      long frozenBalance,
      long res) throws Exception {
    return freezeForOther(owner, contractAddr, receiverAddr, frozenBalance, res);
  }

  private TVMTestResult freezeForOther(byte[] callerAddr,
                                       byte[] contractAddr,
                                       byte[] receiverAddr,
                                       long frozenBalance,
                                       long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    Assert.assertNotNull(receiverAddr);
    AccountCapsule oldReceiver = accountStore.get(receiverAddr);
    long acquiredBalance = 0;
    if (oldReceiver != null) {
      acquiredBalance = res == 0 ? oldReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
          oldReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
    }

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    if (oldDelegatedResource == null) {
      oldDelegatedResource = new DelegatedResourceCapsule(
          ByteString.copyFrom(contractAddr),
          ByteString.copyFrom(receiverAddr));
    }

    TVMTestResult result = triggerFreeze(callerAddr, contractAddr, receiverAddr, frozenBalance, res,
        SUCCESS,
        returnValue -> Assert.assertEquals(dynamicStore.getMinFrozenTime() * FROZEN_PERIOD,
            new DataWord(returnValue).longValue() * 1000));

    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() - frozenBalance, newOwner.getBalance());
    newOwner.setBalance(oldOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForBandwidth() + frozenBalance,
          newOwner.getDelegatedFrozenBalanceForBandwidth());
      oldOwner.setDelegatedFrozenBalanceForBandwidth(0);
      newOwner.setDelegatedFrozenBalanceForBandwidth(0);
    } else {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForEnergy() + frozenBalance,
          newOwner.getDelegatedFrozenBalanceForEnergy());
      oldOwner.setDelegatedFrozenBalanceForEnergy(0);
      newOwner.setDelegatedFrozenBalanceForEnergy(0);
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    Assert.assertNotNull(newReceiver);
    Assert.assertEquals(acquiredBalance + frozenBalance,
        res == 0 ? newReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
            newReceiver.getAcquiredDelegatedFrozenBalanceForEnergy());
    if (oldReceiver != null) {
      newReceiver.setBalance(oldReceiver.getBalance());
      oldReceiver.setEnergyUsage(0);
      oldReceiver.setNewWindowSize(ENERGY, 28800);
      newReceiver.setEnergyUsage(0);
      newReceiver.setNewWindowSize(ENERGY,28800);
      if (res == 0) {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
      } else {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
      }
      Assert.assertArrayEquals(oldReceiver.getData(), newReceiver.getData());
    }

    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    Assert.assertNotNull(newDelegatedResource);
    if (res == 0) {
      Assert.assertEquals(frozenBalance + oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    } else {
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(frozenBalance + oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    }

    if (res == 0) {
      Assert.assertEquals(oldTotalNetWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
    } else {
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight + frozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
    }

    return result;
  }

  private TVMTestResult freezeForOtherWithException(
      byte[] contractAddr,
      byte[] receiverAddr,
      long frozenBalance,
      long res) throws Exception {
    return triggerFreeze(owner, contractAddr, receiverAddr, frozenBalance, res, REVERT, null);
  }

  private TVMTestResult unfreezeForOther(byte[] contractAddr,
                                         byte[] receiverAddr,
                                         long res) throws Exception {
    return unfreezeForOther(owner, contractAddr, receiverAddr, res);
  }

  private TVMTestResult unfreezeForOther(byte[] callerAddr,
                                         byte[] contractAddr,
                                         byte[] receiverAddr,
                                         long res) throws Exception {
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule oldOwner = accountStore.get(contractAddr);
    long delegatedBalance = res == 0 ? oldOwner.getDelegatedFrozenBalanceForBandwidth() :
        oldOwner.getDelegatedFrozenBalanceForEnergy();

    AccountCapsule oldReceiver = accountStore.get(receiverAddr);
    long acquiredBalance = 0;
    if (oldReceiver != null) {
      acquiredBalance = res == 0 ? oldReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth() :
          oldReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
    }

    DelegatedResourceStore delegatedResourceStore = manager.getDelegatedResourceStore();
    DelegatedResourceCapsule oldDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    Assert.assertNotNull(oldDelegatedResource);
    long delegatedFrozenBalance = res == 0 ? oldDelegatedResource.getFrozenBalanceForBandwidth() :
        oldDelegatedResource.getFrozenBalanceForEnergy();
    Assert.assertTrue(delegatedFrozenBalance > 0);
    Assert.assertTrue(delegatedFrozenBalance <= delegatedBalance);

    TVMTestResult result = triggerUnfreeze(callerAddr, contractAddr, receiverAddr, res, SUCCESS,
        returnValue ->
            Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
                Hex.toHexString(returnValue)));

    // check owner account
    AccountCapsule newOwner = accountStore.get(contractAddr);
    Assert.assertEquals(oldOwner.getBalance() + delegatedFrozenBalance, newOwner.getBalance());
    newOwner.setBalance(oldOwner.getBalance());
    if (res == 0) {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForBandwidth() - delegatedFrozenBalance,
          newOwner.getDelegatedFrozenBalanceForBandwidth());
      newOwner.setDelegatedFrozenBalanceForBandwidth(
          oldOwner.getDelegatedFrozenBalanceForBandwidth());
    } else {
      Assert.assertEquals(oldOwner.getDelegatedFrozenBalanceForEnergy() - delegatedFrozenBalance,
          newOwner.getDelegatedFrozenBalanceForEnergy());
      newOwner.setDelegatedFrozenBalanceForEnergy(oldOwner.getDelegatedFrozenBalanceForEnergy());
    }
    Assert.assertArrayEquals(oldOwner.getData(), newOwner.getData());

    // check receiver account
    AccountCapsule newReceiver = accountStore.get(receiverAddr);
    if (oldReceiver != null) {
      Assert.assertNotNull(newReceiver);
      long newAcquiredBalance = res == 0
          ? newReceiver.getAcquiredDelegatedFrozenBalanceForBandwidth()
          : newReceiver.getAcquiredDelegatedFrozenBalanceForEnergy();
      Assert.assertTrue(newAcquiredBalance == 0
          || acquiredBalance - newAcquiredBalance == delegatedFrozenBalance);
      newReceiver.setBalance(oldReceiver.getBalance());
      newReceiver.setEnergyUsage(0);
      newReceiver.setNewWindowSize(ENERGY,28800);
      oldReceiver.setEnergyUsage(0);
      oldReceiver.setNewWindowSize(ENERGY,28800);
      if (res == 0) {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForBandwidth(0);
      } else {
        oldReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
        newReceiver.setAcquiredDelegatedFrozenBalanceForEnergy(0);
      }
      Assert.assertArrayEquals(oldReceiver.getData(), newReceiver.getData());
    } else {
      Assert.assertNull(newReceiver);
    }

    // check delegated resource store
    DelegatedResourceCapsule newDelegatedResource = delegatedResourceStore.get(
        DelegatedResourceCapsule.createDbKey(contractAddr, receiverAddr));
    Assert.assertNotNull(newDelegatedResource);
    if (res == 0) {
      Assert.assertEquals(0, newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForEnergy(),
          newDelegatedResource.getFrozenBalanceForEnergy());
    } else {
      Assert.assertEquals(oldDelegatedResource.getFrozenBalanceForBandwidth(),
          newDelegatedResource.getFrozenBalanceForBandwidth());
      Assert.assertEquals(0, newDelegatedResource.getFrozenBalanceForEnergy());
    }

    // check total weight
    if (res == 0) {
      Assert.assertEquals(oldTotalNetWeight - delegatedFrozenBalance / TRX_PRECISION,
          dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight, dynamicStore.getTotalEnergyWeight());
    } else {
      Assert.assertEquals(oldTotalNetWeight, dynamicStore.getTotalNetWeight());
      Assert.assertEquals(oldTotalEnergyWeight - delegatedFrozenBalance / TRX_PRECISION,
          dynamicStore.getTotalEnergyWeight());
    }

    return result;
  }

  private TVMTestResult unfreezeForOtherWithException(byte[] contractAddr,
                                                      byte[] receiverAddr,
                                                      long res) throws Exception {
    return triggerUnfreeze(owner, contractAddr, receiverAddr, res, REVERT, null);
  }

  private TVMTestResult suicideWithException(byte[] contractAddr,
                                             byte[] inheritorAddr) throws Exception {
    return triggerSuicide(owner, contractAddr, inheritorAddr, REVERT, null);
  }

  private TVMTestResult suicideToAccount(byte[] contractAddr,
                                         byte[] inheritorAddr) throws Exception {
    return suicideToAccount(owner, contractAddr, inheritorAddr);
  }

  private TVMTestResult suicideToAccount(byte[] callerAddr,
                                         byte[] contractAddr,
                                         byte[] inheritorAddr) throws Exception {
    if (FastByteComparisons.isEqual(contractAddr, inheritorAddr)) {
      inheritorAddr = manager.getAccountStore().getBlackholeAddress();
    }
    DynamicPropertiesStore dynamicStore = manager.getDynamicPropertiesStore();
    long oldTotalNetWeight = dynamicStore.getTotalNetWeight();
    long oldTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();

    AccountStore accountStore = manager.getAccountStore();
    AccountCapsule contract = accountStore.get(contractAddr);
    AccountCapsule oldInheritor = accountStore.get(inheritorAddr);
    long oldBalanceOfInheritor = 0;
    if (oldInheritor != null) {
      oldBalanceOfInheritor = oldInheritor.getBalance();
    }

    TVMTestResult result = triggerSuicide(callerAddr, contractAddr, inheritorAddr, SUCCESS, null);

    Assert.assertNull(accountStore.get(contractAddr));
    AccountCapsule newInheritor = accountStore.get(inheritorAddr);
    Assert.assertNotNull(newInheritor);
    if (FastByteComparisons.isEqual(inheritorAddr,
        manager.getAccountStore().getBlackholeAddress())) {
      Assert.assertEquals(contract.getBalance() + contract.getTronPower(),
          newInheritor.getBalance() - oldBalanceOfInheritor - result.getReceipt().getEnergyFee());
    } else {
      Assert.assertEquals(contract.getBalance() + contract.getTronPower(),
          newInheritor.getBalance() - oldBalanceOfInheritor);
    }

    Assert.assertEquals(0, contract.getDelegatedFrozenBalanceForBandwidth());
    Assert.assertEquals(0, contract.getDelegatedFrozenBalanceForEnergy());

    long newTotalNetWeight = dynamicStore.getTotalNetWeight();
    long newTotalEnergyWeight = dynamicStore.getTotalEnergyWeight();
    Assert.assertEquals(contract.getFrozenBalance(),
        (oldTotalNetWeight - newTotalNetWeight) * TRX_PRECISION);
    Assert.assertEquals(contract.getEnergyFrozenBalance(),
        (oldTotalEnergyWeight - newTotalEnergyWeight) * TRX_PRECISION);

    return result;
  }

  private void checkReceipt(TVMTestResult result,
                            AccountCapsule caller,
                            AccountCapsule deployer) {
    AccountStore accountStore = manager.getAccountStore();
    long callerEnergyUsage = result.getReceipt().getEnergyUsage();
    long deployerEnergyUsage = result.getReceipt().getOriginEnergyUsage();
    long burnedTrx = result.getReceipt().getEnergyFee();
    AccountCapsule newCaller = accountStore.get(caller.createDbKey());
    Assert.assertEquals(callerEnergyUsage,
        newCaller.getEnergyUsage() - caller.getEnergyUsage());
    Assert.assertEquals(deployerEnergyUsage,
        accountStore.get(deployer.createDbKey()).getEnergyUsage() - deployer.getEnergyUsage());
    Assert.assertEquals(burnedTrx,
        caller.getBalance() - accountStore.get(caller.createDbKey()).getBalance());
  }

  @After
  public void destroy() {
    ConfigLoader.disable = false;
    VMConfig.initVmHardFork(false);
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }
}
