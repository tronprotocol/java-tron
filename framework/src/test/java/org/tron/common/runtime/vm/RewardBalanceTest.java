package org.tron.common.runtime.vm;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.Base58;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.StoreFactory;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.invoke.ProgramInvoke;
import org.tron.core.vm.program.invoke.ProgramInvokeFactory;
import org.tron.core.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class RewardBalanceTest extends VMTestBase {

  /*
    pragma solidity ^0.5.0;

    contract ContractB{
      address user;
    }

    contract TestRewardBalance{
      address user;
      address payable owner;

      ContractB contractB = new ContractB();

      constructor() public {
        user = msg.sender;
      }

      function rewardBalanceTest(address addr) view public returns (uint256) {
        return addr.rewardbalance;
      }

      function nullAddressTest() view public returns (uint256) {
        return address(0x0).rewardbalance;
      }

      function localContractAddrTest() view public returns (uint256) {
        address payable localContract = address(uint160(address(this)));
        return localContract.rewardbalance;
      }

      function otherContractAddrTest() view public returns (uint256) {
        address payable localContract = address(uint160(address(contractB)));
        return localContract.rewardbalance;
      }

      function nonpayableAddrTest(address addr) view public returns (uint256) {
        return addr.rewardbalance;
      }

      function payableAddrTest(address payable addr) view public returns (uint256) {
        return addr.rewardbalance;
      }
    }
  */

  @Test
  public void testRewardBalance()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    StoreFactory storeFactory = StoreFactory.getInstance();
    Repository repository;
    ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    VMConfig vmConfig = VMConfig.getInstance();

    String contractName = "TestRewardBalance";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"constructor\"},{\"constant\":false,\"inputs\":[],"
        + "\"name\":\"localContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\","
        + "\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\","
        + "\"type\":\"address\"}],\"name\":\"nonpayableAddrTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[],"
        + "\"name\":\"nullAddressTest\",\"outputs\":[{\"internalType\":\"uint256\","
        + "\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[],\"name\":\"otherContractAddrTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[{\"internalType\":\"address payable\",\"name\":\"addr\","
        + "\"type\":\"address\"}],\"name\":\"payableAddrTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,"
        + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\","
        + "\"type\":\"address\"}],\"name\":\"rewardBalanceTest\","
        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
        + "\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"}]";
    String factoryCode = "60806040526040516100109061008b565b604051"
        + "809103906000f08015801561002c573d6000803e3d6000fd5b506"
        + "00280546001600160a01b0319166001600160a01b039290921691"
        + "909117905534801561005957600080fd5b50d3801561006657600"
        + "080fd5b50d2801561007357600080fd5b50600080546001600160"
        + "a01b03191633179055610097565b6072806101c083390190565b6"
        + "1011a806100a66000396000f3fe6080604052348015600f576000"
        + "80fd5b50d38015601b57600080fd5b50d28015602757600080fd5"
        + "b506004361060725760003560e01c806356b42994146077578063"
        + "627bfa45146077578063a223c65f146077578063af4a11051460a"
        + "c578063cb2d51cf1460b2578063d30a28ee1460b8575b600080fd"
        + "5b609a60048036036020811015608b57600080fd5b50356001600"
        + "160a01b031660be565b60408051918252519081900360200190f3"
        + "5b609a60cb565b609a60d1565b609a60d6565b6001600160a01b0"
        + "316d890565b6000d890565b30d890565b6002546001600160a01b"
        + "0316d89056fea26474726f6e5820717344fa5eb84d711be29808c"
        + "dff30740d75dddee7a38e76042a46157370501c64736f6c634300"
        + "050d00316080604052348015600f57600080fd5b50d38015601b5"
        + "7600080fd5b50d28015602757600080fd5b50603d806035600039"
        + "6000f3fe6080604052600080fdfea26474726f6e582090ab77a1a"
        + "2f65b0d6f77854c390b03b33fe20cd15ed8f722d497f9c3070c96"
        + "ef64736f6c634300050d0031";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, abi, factoryCode, value, feeLimit, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: rewardBalanceTest(address)
    String methodByAddr = "rewardBalanceTest(address)";
    String nonexistentAccount = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
    String hexInput = AbiUtil.parseMethod(methodByAddr,
        Collections.singletonList(nonexistentAccount));
    BlockCapsule blockCap = new BlockCapsule(Protocol.Block.newBuilder().build());
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
    byte[] result = program.getRewardBalance(new DataWord(Base58.decode(nonexistentAccount)))
        .getData();

    Assert.assertEquals(Hex.toHexString(result),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // trigger deployed contract
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
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
    result = program.getRewardBalance(new DataWord(Base58.decode(factoryAddressStr))).getData();
    Assert.assertEquals(Hex.toHexString(result),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // trigger deployed contract
    String witnessAccount = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(witnessAccount));
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
    result = program.getRewardBalance(new DataWord(Base58.decode(witnessAccount))).getData();
    Assert.assertEquals(Hex.toHexString(result),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: nullAddressTest(address)
    methodByAddr = "nullAddressTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
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
    result = program.getRewardBalance(DataWord.ZERO()).getData();
    Assert.assertEquals(Hex.toHexString(result),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
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
    result = program.getRewardBalance(new DataWord(Base58.decode(factoryAddressStr))).getData();
    Assert.assertEquals(Hex.toHexString(result),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    ConfigLoader.disable = false;
  }
}


