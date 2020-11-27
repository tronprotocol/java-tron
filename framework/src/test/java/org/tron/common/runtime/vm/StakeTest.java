package org.tron.common.runtime.vm;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.VM;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.core.vm.program.invoke.ProgramInvokeMockImpl;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class StakeTest extends VMTestBase {

  private MaintenanceManager maintenanceManager;
  private StoreFactory storeFactory;
  private Repository repository;
  private ProgramInvokeFactory programInvokeFactory;
  private VMConfig vmConfig;

  @Before
  public void before() {
    ConsensusService consensusService = context.getBean(ConsensusService.class);
    consensusService.start();
    maintenanceManager = context.getBean(MaintenanceManager.class);

    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);

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
  public void testStake() throws Exception {
    String contractName = "TestStake";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":"
        + "\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\""
        + ":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\","
        + "\"type\":\"uint256\"}],\"name\":\"Stake\",\"outputs\":[{\"internalType\":\"bool\""
        + ",\"name\":\"result\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\""
        + "UnStake\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"result\",\"type\":\""
        + "bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}"
        + ",{\"constant\":false,\"inputs\":[{\"internalType\":\"address payable\",\"name\":\""
        + "target\",\"type\":\"address\"}],\"name\":\"selfdestructTest\",\"outputs\":[],\""
        + "payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant"
        + "\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address"
        + "\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"},{\""
        + "internalType\":\"address payable\",\"name\":\"target\",\"type\":\"address\"}],\"name"
        + "\":\"selfdestructTest2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"function\"}]";
    String factoryCode = "608060405261024a806100136000396000f3fe608060405234801561001057600080f"
        + "d5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100665760003560e01c8"
        + "063377bdd4c1461006b57806338e8221f146100af578063ebedb8b31461011d578063ecb90615146101835"
        + "75b600080fd5b6100ad6004803603602081101561008157600080fd5b81019080803573fffffffffffffff"
        + "fffffffffffffffffffffffff1690602001909291905050506101a5565b005b61011b60048036036060811"
        + "0156100c557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019"
        + "092919080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff1690602001909"
        + "291905050506101be565b005b6101696004803603604081101561013357600080fd5b81019080803573fff"
        + "fffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506101d"
        + "b565b604051808215151515815260200191505060405180910390f35b61018b6101e8565b6040518082151"
        + "51515815260200191505060405180910390f35b8073ffffffffffffffffffffffffffffffffffffffff16f"
        + "f5b8183d5508073ffffffffffffffffffffffffffffffffffffffff16ff5b60008183d5905092915050565"
        + "b6000d690509056fea26474726f6e582003e023985836e07a7f23202dfc410017c52159ee1ee3968435e99"
        + "17f83f8d5a164736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d697"
        + "42e37633236393863300057";
    long feeLimit = 100000000;

    // deploy contract
    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, 100000000, feeLimit, 0,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    String methodByAddr = "Stake(address,uint256)";
    final String witnessAddrStr = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    final byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
    BlockCapsule blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());

    // build program and trigger program.stake(witnessAddrStr, 10000000)
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witnessAddrStr, 10000000));
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
    boolean result = program.stake(new DataWord(witnessAddr), new DataWord(10000000));
    Assert.assertTrue(result);

    repository.commit();

    maintenanceManager.doMaintenance();

    AccountCapsule contract = manager.getAccountStore().get(factoryAddress);
    WitnessCapsule witness = manager.getWitnessStore().get(witnessAddr);
    Assert.assertEquals(contract.getFrozenCount(), 1);
    Assert.assertEquals(contract.getFrozenBalance(), 10000000);
    Assert.assertEquals(contract.getBalance(), 90000000);
    Assert.assertEquals(contract.getVotesList().get(0).getVoteCount(), 10);
    Assert.assertEquals(witness.getVoteCount(), 115);

    // program.stake(witnessAddrStr, 5000000)
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witnessAddrStr, 5000000));
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
    result = program.stake(new DataWord(witnessAddr), new DataWord(5000000));
    Assert.assertTrue(result);
    repository.commit();

    contract = manager.getAccountStore().get(factoryAddress);
    Assert.assertEquals(contract.getFrozenCount(), 1);
    Assert.assertEquals(contract.getFrozenBalance(), 10000000);
    Assert.assertEquals(contract.getVotesList().get(0).getVoteCount(), 5);

    // vote not exist witness
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(
        "27k66nycZATHzBasFT9782nTsYWqVtxdtAc", 20000000));
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
    result = program
        .stake(new DataWord("A0E6773BBF60F97D22AA3BF73D2FE235E816A1964F"), new DataWord(20000000));
    Assert.assertFalse(result);
    repository.commit();

    contract = manager.getAccountStore().get(factoryAddress);
    Assert.assertEquals(contract.getBalance(), 90000000);
    Assert.assertEquals(contract.getFrozenCount(), 1);
    Assert.assertEquals(contract.getFrozenBalance(), 10000000);

    // param error
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(
        "27k66nycZATHzBasFT9782nTsYWqVtxdtAc", -20000000));
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
    result = program
        .stake(new DataWord("A0E6773BBF60F97D22AA3BF73D2FE235E816A1964F"), new DataWord(-20000000));
    Assert.assertFalse(result);
    repository.commit();

    // param error
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(
        "27k66nycZATHzBasFT9782nTsYWqVtxdtAc", 2000000000));
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
    result = program.stake(new DataWord("A0E6773BBF60F97D22AA3BF73D2FE235E816A1964F"),
        new DataWord(2000000000));
    Assert.assertFalse(result);
    repository.commit();
  }
}
