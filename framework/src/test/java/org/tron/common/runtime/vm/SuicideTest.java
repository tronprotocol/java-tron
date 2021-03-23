package org.tron.common.runtime.vm;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.TransactionTrace;
import org.tron.core.store.AccountStore;
import org.tron.core.store.StoreFactory;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;

public class SuicideTest extends VMTestBase {

  private MaintenanceManager maintenanceManager;
  private AccountStore accountStore;
  private WitnessStore witnessStore;
  private VotesStore votesStore;
  private StoreFactory storeFactory;
  private Repository repository;
  private ProgramInvokeFactory programInvokeFactory;
  private VMConfig vmConfig;

  @Before
  public void before() {
    ConsensusService consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    maintenanceManager = context.getBean(MaintenanceManager.class);
    accountStore = manager.getAccountStore();
    witnessStore = manager.getWitnessStore();
    votesStore = manager.getVotesStore();

    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);

    CommonParameter.getInstance().setDebug(true);

    storeFactory = StoreFactory.getInstance();
    programInvokeFactory = new ProgramInvokeFactoryImpl();
    vmConfig = VMConfig.getInstance();
  }

  /*
pragma solidity ^0.5.0;
contract TestStake{

constructor() payable public{}

function selfdestructTest(address payable target) public{
selfdestruct(target);
}

function selfdestructTest2(address sr, uint256 amount, address payable target) public{
stake(sr, amount);
selfdestruct(target);
}

function Stake(address sr, uint256 amount) public returns (bool result){
return stake(sr, amount);
}
function UnStake() public returns (bool result){
return unstake();
}
}
*/
  @Test
  public void testSuicide() throws Exception {
    String contractName = "TestSuicide";
    byte[] ownerAddress = Hex.decode(OWNER_ADDRESS);
    String abi = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
        + ":\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\""
        + "name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount"
        + "\",\"type\":\"uint256\"}],\"name\":\"Stake\",\"outputs\":[{\"internalType\":\"bool\""
        + ",\"name\":\"result\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "UnStake\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"result\",\"type\":\"bool"
        + "\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\""
        + "constant\":false,\"inputs\":[{\"internalType\":\"address payable\",\"name\":\"target\""
        + ",\"type\":\"address\"}],\"name\":\"selfdestructTest\",\"outputs\":[],\"payable\":false"
        + ",\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\""
        + "inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\""
        + "internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"},{\"internalType\""
        + ":\"address payable\",\"name\":\"target\",\"type\":\"address\"}],\"name\":\""
        + "selfdestructTest2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"}]";
    String factoryCode = "608060405261024a806100136000396000f3fe608060405234801561001057600080"
        + "fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100665760003560e01"
        + "c8063377bdd4c1461006b57806338e8221f146100af578063ebedb8b31461011d578063ecb90615146101"
        + "83575b600080fd5b6100ad6004803603602081101561008157600080fd5b81019080803573fffffffffff"
        + "fffffffffffffffffffffffffffff1690602001909291905050506101a5565b005b61011b600480360360"
        + "608110156100c557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906"
        + "020019092919080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060"
        + "2001909291905050506101be565b005b6101696004803603604081101561013357600080fd5b810190808"
        + "03573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919050"
        + "50506101db565b604051808215151515815260200191505060405180910390f35b61018b6101e8565b604"
        + "051808215151515815260200191505060405180910390f35b8073ffffffffffffffffffffffffffffffff"
        + "ffffffff16ff5b8183d5508073ffffffffffffffffffffffffffffffffffffffff16ff5b60008183d5905"
        + "092915050565b6000d690509056fea26474726f6e582003e023985836e07a7f23202dfc410017c52159ee"
        + "1ee3968435e9917f83f8d5a164736f6c637828302e352e31332d646576656c6f702e323032302e382e313"
        + "32b636f6d6d69742e37633236393863300057";
    long feeLimit = 100000000;

    String witnessAddrStr = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    final byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
    final String obtainUserAddrStr = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
    final byte[] obtainUserAddr = Hex.decode("A0E6773BBF60F97D22AA3BF73D2FE235E816A1964F");
    BlockCapsule blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());

    // suicide after stake (freeze not expire)
    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    String hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, feeLimit);
    InternalTransaction rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    ProgramInvoke programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    Program program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    boolean programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    Protocol.Account.Frozen frozen1;
    frozen1 = accountStore.get(factoryAddress).getFrozenList().get(0);
    //do maintain
    maintenanceManager.doMaintenance();
    hexInput = AbiUtil.parseMethod("selfdestructTest(address)", Arrays.asList(obtainUserAddrStr));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    AccountCapsule obtainAccount = accountStore.get(obtainUserAddr);
    Assert.assertEquals(obtainAccount.getBalance(), 90000000);
    Assert.assertEquals(obtainAccount.getFrozenBalance(), 10000000);
    Assert.assertEquals(obtainAccount.getFrozenList().get(0).getExpireTime(),
        frozen1.getExpireTime());
    Assert.assertFalse(accountStore.has(factoryAddress));
    maintenanceManager.doMaintenance();
    WitnessCapsule witnessCapsule = witnessStore.get(witnessAddr);
    Assert.assertEquals(witnessCapsule.getVoteCount(), 105);
    Assert.assertEquals(obtainAccount.getVotesList().size(), 0);

    // suicide to a staked account
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    frozen1 = accountStore.get(factoryAddress).getFrozenList().get(0);
    maintenanceManager.doMaintenance();
    Protocol.Account.Frozen frozen2;
    frozen2 = accountStore.get(obtainUserAddr).getFrozenList().get(0);
    hexInput = AbiUtil.parseMethod("selfdestructTest(address)", Arrays.asList(obtainUserAddrStr));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    obtainAccount = accountStore.get(obtainUserAddr);
    Assert.assertEquals(obtainAccount.getBalance(), 180000000);
    Assert.assertEquals(obtainAccount.getFrozenBalance(), 20000000);
    Assert.assertEquals(obtainAccount.getFrozenList().get(0).getExpireTime(),
        (frozen1.getExpireTime() * frozen1.getFrozenBalance()
            + frozen2.getExpireTime() * frozen2.getFrozenBalance())
            / (frozen1.getFrozenBalance() + frozen2.getFrozenBalance()));

    //test suicide to staked contract & suicide to itself
    final long totalNetWeightStart = manager.getDynamicPropertiesStore().getTotalNetWeight();
    //deploy contract1
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    //deploy contract obtain
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "contractObtain", ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    byte[] obtainContractAddr = WalletUtil.generateContractAddress(trx);
    String obtainContractAddrStr;
    obtainContractAddrStr = StringUtil.encode58Check(obtainContractAddr);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    //factoryAddress Stake
    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    //obtainContractAddr Stake
    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        obtainContractAddr, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    frozen1 = accountStore.get(factoryAddress).getFrozenList().get(0);
    frozen2 = accountStore.get(obtainContractAddr).getFrozenList().get(0);
    maintenanceManager.doMaintenance();
    //factoryAddress suicide
    hexInput = AbiUtil.parseMethod("selfdestructTest(address)",
        Arrays.asList(obtainContractAddrStr));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    obtainAccount = accountStore.get(obtainContractAddr);
    Assert.assertEquals(obtainAccount.getBalance(), 180000000);
    Assert.assertEquals(obtainAccount.getFrozenBalance(), 20000000);
    Assert.assertEquals(obtainAccount.getFrozenList().get(0).getExpireTime(),
        (frozen1.getExpireTime() * frozen1.getFrozenBalance()
            + frozen2.getExpireTime() * frozen2.getFrozenBalance())
            / (frozen1.getFrozenBalance() + frozen2.getFrozenBalance()));
    Assert.assertEquals(manager.getDynamicPropertiesStore().getTotalNetWeight(),
        totalNetWeightStart + 20);

    //obtainContractAddr suicide to itself
    AccountCapsule blackHoleAccount;
    blackHoleAccount = accountStore.getBlackhole();
    hexInput = AbiUtil.parseMethod("selfdestructTest(address)",
        Arrays.asList(obtainContractAddrStr));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            obtainContractAddr, Hex.decode(hexInput), 0, feeLimit, manager, null);
    AccountCapsule blackHoleAccountAfter = accountStore.getBlackhole();
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(blackHoleAccountAfter.getBalance(), 200000000
        + blackHoleAccount.getBalance() + 25500); // 25500 for energy used for suicide
    Assert.assertEquals(blackHoleAccountAfter.getFrozenBalance(), 0);
    Assert.assertEquals(manager.getDynamicPropertiesStore().getTotalNetWeight(),
        totalNetWeightStart);

    //test vote
    final byte[] zeroAddr = TransactionTrace.convertToTronAddress(new byte[20]);
    //deploy contract1
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    //deploy contract obtain
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "contractObtain", ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    obtainContractAddr = WalletUtil.generateContractAddress(trx);
    obtainContractAddrStr = StringUtil.encode58Check(obtainContractAddr);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    //deploy contract2
    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "contractSuicide", ownerAddress, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    byte[] suicideContractAddr = WalletUtil.generateContractAddress(trx);
    String suicideContractAddrStr = StringUtil.encode58Check(suicideContractAddr);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    //factoryAddress Stake
    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        factoryAddress, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    //obtainContractAddr Stake
    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        obtainContractAddr, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    //suicideContractAddr Stake
    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 10000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        suicideContractAddr, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(programResult);
    repository.commit();

    maintenanceManager.doMaintenance();
    Assert.assertEquals(accountStore.get(factoryAddress).getVotesList().get(0).getVoteCount(), 10);
    Assert.assertEquals(accountStore.get(obtainContractAddr)
        .getVotesList().get(0).getVoteCount(), 10);
    Assert.assertEquals(accountStore.get(suicideContractAddr)
        .getVotesList().get(0).getVoteCount(), 10);
    Assert.assertEquals(witnessStore.get(witnessAddr).getVoteCount(), 105 + 30);
    //contract1 suicide
    hexInput = AbiUtil.parseMethod("selfdestructTest(address)",
        Arrays.asList(obtainContractAddrStr));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(accountStore.get(obtainContractAddr).getBalance(), 180000000);
    VotesCapsule zeroVotes = votesStore.get(zeroAddr);
    Assert.assertEquals(zeroVotes.getOldVotes().get(0).getVoteCount(), 10);
    Assert.assertEquals(zeroVotes.getNewVotes().size(), 0);
    //suicideContractAddr Stake
    hexInput = AbiUtil.parseMethod("Stake(address,uint256)",
        Arrays.asList(witnessAddrStr, 5000000));
    trx = TvmTestUtils.generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS),
        suicideContractAddr, Hex.decode(hexInput), 0, feeLimit);
    rootInternalTransaction = new InternalTransaction(trx,
        InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke = programInvokeFactory
        .createProgramInvoke(InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE, trx,
            0, 0, blockCap.getInstance(), repository, System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000, 3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.stake(new DataWord(witnessAddr), new DataWord(5000000));
    Assert.assertTrue(programResult);
    repository.commit();

    VotesCapsule suicideContractVotes = votesStore.get(suicideContractAddr);
    Assert.assertEquals(suicideContractVotes.getOldVotes().get(0).getVoteCount(), 10);
    Assert.assertEquals(suicideContractVotes.getNewVotes().get(0).getVoteCount(), 5);
    //suicideContractAddr suicide
    hexInput = AbiUtil.parseMethod("selfdestructTest(address)",
        Arrays.asList(obtainContractAddrStr));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            suicideContractAddr, Hex.decode(hexInput), 0, feeLimit, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(accountStore.get(obtainContractAddr).getBalance(), 270000000);
    zeroVotes = votesStore.get(zeroAddr);
    Assert.assertEquals(zeroVotes.getOldVotes().get(0).getVoteCount(), 20);
    Assert.assertEquals(zeroVotes.getNewVotes().size(), 0);
    Assert.assertFalse(votesStore.has(suicideContractAddr));
    maintenanceManager.doMaintenance();
    Assert.assertEquals(witnessStore.get(witnessAddr).getVoteCount(), 105 + 10);
  }
}
