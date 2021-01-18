package org.tron.common.runtime.vm;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.InternalTransaction;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
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
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class IsSRCandidateTest extends VMTestBase {

  /*
    pragma solidity ^0.5.0;

    contract ContractB{
      address others;
    }

    contract TestIsSRCandidate{
      address user;

      ContractB contractB = new ContractB();

      constructor() public {
        user = msg.sender;
      }

      function isSRCandidateTest(address addr) public returns (bool) {
        return address(addr).isSRCandidate;
      }

      function nullAddressTest() public returns (bool) {
        return address(0x0).isSRCandidate;
      }

      function localContractAddrTest() public returns (bool) {
        address payable localContract = address(uint160(address(this)));
        return localContract.isSRCandidate;

        return address(this).isSRCandidate;
      }

      function otherContractAddrTest() public returns (bool) {
        return address(contractB).isSRCandidate;
      }

      function nonpayableAddrTest(address addr) public returns (bool) {
        return addr.isSRCandidate;
      }

      function payableAddrTest(address payable addr) public returns (bool) {
        return addr.isSRCandidate;
      }
    }
  */

  @Test
  public void testIsSRCandidate()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    String contractName = "TestIsSRCandidate";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String abi =
        "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
            + "\"type\":\"constructor\"},{\"constant\":false,"
            + "\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\","
            + "\"type\":\"address\"}],\"name\":\"isSRCandidateTest\","
            + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[],\"name\":\"localContractAddrTest\","
            + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[{\"internalType\":\"address\","
            + "\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"nonpayableAddrTest\","
            + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[],\"name\":\"nullAddressTest\","
            + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[],\"name\":\"otherContractAddrTest\","
            + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[{\"internalType\":\"address payable\","
            + "\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"payableAddrTest\","
            + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    String factoryCode =
        "60806040526040516100109061008b565b6"
            + "04051809103906000f08015801561002c573d6000803e3d6"
            + "000fd5b50600180546001600160a01b0319166001600160a"
            + "01b039290921691909117905534801561005957600080fd5"
            + "b50d3801561006657600080fd5b50d280156100735760008"
            + "0fd5b50600080546001600160a01b0319163317905561009"
            + "7565b6072806101c283390190565b61011c806100a660003"
            + "96000f3fe6080604052348015600f57600080fd5b50d3801"
            + "5601b57600080fd5b50d28015602757600080fd5b5060043"
            + "61060725760003560e01c80632e48f1ac14607757806356b"
            + "42994146077578063627bfa45146077578063af4a1105146"
            + "0ae578063cb2d51cf1460b4578063d30a28ee1460ba575b6"
            + "00080fd5b609a60048036036020811015608b57600080fd5"
            + "b50356001600160a01b031660c0565b60408051911515825"
            + "2519081900360200190f35b609a60cd565b609a60d3565b6"
            + "09a60d8565b6001600160a01b0316d990565b6000d990565"
            + "b30d990565b6001546001600160a01b0316d99056fea2647"
            + "4726f6e5820509553fa5821ca76ddf8a0d074cd74dcb1f74"
            + "e068ca148b983f1b0bea447b99f64736f6c634300050d003"
            + "16080604052348015600f57600080fd5b50d38015601b576"
            + "00080fd5b50d28015602757600080fd5b50603d806035600"
            + "0396000f3fe6080604052600080fdfea26474726f6e58209"
            + "afab2d7a84ca331e2eb33393a62310b1a53e77c37a287407"
            + "53ae0a3a99980ba64736f6c634300050d0031";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    Repository repository;
    StoreFactory storeFactory = StoreFactory.getInstance();
    ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    VMConfig vmConfig = VMConfig.getInstance();

    // deploy contract
    Transaction trx =
        TvmTestUtils.generateDeploySmartContractAndGetTransaction(
            contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent, null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: isSRCandidateTest(address)
    String methodByAddr = "isSRCandidateTest(address)";
    String nonexistentAccount = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
    byte[] nonexistentAddr = Hex.decode("A0E6773BBF60F97D22AA3BF73D2FE235E816A1964F");
    String hexInput =
        AbiUtil.parseMethod(methodByAddr, Collections.singletonList(nonexistentAccount));

    trx =
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            Hex.decode(OWNER_ADDRESS), factoryAddress, Hex.decode(hexInput), 0, fee);
    InternalTransaction rootInternalTransaction =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    ProgramInvoke programInvoke =
        programInvokeFactory.createProgramInvoke(
            InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE,
            trx,
            0,
            0,
            null,
            repository,
            System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000,
            3_000_000L);
    Program program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    byte[] programResult = program.isSRCandidate(new DataWord(nonexistentAddr)).getData();
    Assert.assertEquals(
        Hex.toHexString(programResult),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // trigger deployed contract
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(factoryAddressStr));
    trx =
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            Hex.decode(OWNER_ADDRESS), factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke =
        programInvokeFactory.createProgramInvoke(
            InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE,
            trx,
            0,
            0,
            null,
            repository,
            System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000,
            3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.isSRCandidate(new DataWord(factoryAddress)).getData();
    Assert.assertEquals(
        Hex.toHexString(programResult),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    // trigger deployed contract
    String witnessAccount = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(witnessAccount));
    trx =
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            Hex.decode(OWNER_ADDRESS), factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke =
        programInvokeFactory.createProgramInvoke(
            InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE,
            trx,
            0,
            0,
            null,
            repository,
            System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000,
            3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.isSRCandidate(new DataWord(witnessAddr)).getData();
    Assert.assertEquals(
        Hex.toHexString(programResult),
        "0000000000000000000000000000000000000000000000000000000000000001");
    repository.commit();

    // Trigger contract method: nullAddressTest(address)
    methodByAddr = "nullAddressTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Collections.singletonList(""));
    trx =
        TvmTestUtils.generateTriggerSmartContractAndGetTransaction(
            Hex.decode(OWNER_ADDRESS), factoryAddress, Hex.decode(hexInput), 0, fee);
    rootInternalTransaction =
        new InternalTransaction(trx, InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE);
    repository = RepositoryImpl.createRoot(storeFactory);
    programInvoke =
        programInvokeFactory.createProgramInvoke(
            InternalTransaction.TrxType.TRX_CONTRACT_CALL_TYPE,
            InternalTransaction.ExecutorType.ET_PRE_TYPE,
            trx,
            0,
            0,
            null,
            repository,
            System.nanoTime() / 1000,
            System.nanoTime() / 1000 + 50000,
            3_000_000L);
    program = new Program(null, programInvoke, rootInternalTransaction, vmConfig);
    programResult = program.isSRCandidate(new DataWord()).getData();
    Assert.assertEquals(
        Hex.toHexString(programResult),
        "0000000000000000000000000000000000000000000000000000000000000000");
    repository.commit();

    ConfigLoader.disable = false;
  }
}
