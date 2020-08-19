package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.exception.*;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

import java.util.Arrays;

@Slf4j
public class IsSRCandidateTest extends VMTestBase {

/*  pragma solidity ^0.5.0;

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
  }*/

  @Test
  public void testIsSRCandidate()
          throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
          ContractValidateException, DupTransactionException, TooBigTransactionException, AccountResourceInsufficientException, BadBlockException, NonCommonBlockException, TransactionExpirationException, UnLinkedBlockException, ZksnarkException, TaposException, TooBigTransactionResultException, ValidateSignatureException, BadNumberBlockException, ValidateScheduleException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmStake(1);
    String contractName = "TestIsSRCandidate";

//    String key = "11aba859e4477a6615c8b121e9fdbbf1bc32ca31cf06d46733e539bf94c677e0";
//    byte[] privateKey = ByteArray.fromHexString(key);
//    final ECKey ecKey = ECKey.fromPrivate(privateKey);
//    byte[] witnessAddress = ecKey.getAddress();

    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},"
        + "{\"constant\":true,\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}]"
        + ",\"name\":\"isSRCandidateTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\""
        + "}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":"
        + "[],\"name\":\"localContractAddrTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\""
        + "bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs"
        + "\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"nonpayableAddrTest\","
        + "\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability"
        + "\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"nullAddressTest\",\"outputs\":"
        + "[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\","
        + "\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"otherContractAddrTest\",\"outputs\":[{\""
        + "internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type"
        + "\":\"function\"},{\"constant\":true,\"inputs\":[{\"internalType\":\"address payable\",\"name\":\"addr\",\"type\""
        + ":\"address\"}],\"name\":\"payableAddrTest\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool"
        + "\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}]";

    String factoryCode = "60806040526040516100109061008b565b604051809103906000f08015801561002c573d6000803e"
        + "3d6000fd5b50600180546001600160a01b0319166001600160a01b03929092169190911790553480156100595760008"
        + "0fd5b50d3801561006657600080fd5b50d2801561007357600080fd5b50600080546001600160a01b03191633179055"
        + "610097565b6072806101c283390190565b61011c806100a66000396000f3fe6080604052348015600f57600080fd5b5"
        + "0d38015601b57600080fd5b50d28015602757600080fd5b506004361060725760003560e01c80632e48f1ac14607757"
        + "806356b42994146077578063627bfa45146077578063af4a11051460ae578063cb2d51cf1460b4578063d30a28ee146"
        + "0ba575b600080fd5b609a60048036036020811015608b57600080fd5b50356001600160a01b031660c0565b60408051"
        + "9115158252519081900360200190f35b609a60cd565b609a60d3565b609a60d8565b6001600160a01b0316d990565b6"
        + "000d990565b30d990565b6001546001600160a01b0316d99056fea26474726f6e5820157bf32a47535ba252072c142a"
        + "c465305387ea5890db032f2c5280a69978fb3c64736f6c634300050d00316080604052348015600f57600080fd5b50d"
        + "38015601b57600080fd5b50d28015602757600080fd5b50603d8060356000396000f3fe6080604052600080fdfea264"
        + "74726f6e5820d254d85864038ebfa30d75f1458ca4289cc5edb6406ee70315369c3d5a1e8eaa64736f6c634300050d0"
        + "031";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

//    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(witnessAddress));
//    chainManager.addWitness(ByteString.copyFrom(witnessAddress));
//    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), 1533529947843L, privateKey);
//    dbManager.pushBlock(new BlockCapsule(block));

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "", address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddressOther = WalletUtil.generateContractAddress(trx);
    String factoryAddressStrOther = StringUtil.encode58Check(factoryAddressOther);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: isSRCandidateTest(address)
    String methodByAddr = "isSRCandidateTest(address)";
    String nonexistentAccount = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(nonexistentAccount));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStr));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    String witnessAccount = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witnessAccount));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000001");

    // Trigger contract method: nullAddressTest(address)
    methodByAddr = "nullAddressTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: localContractAddrTest()
    methodByAddr = "localContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: otherContractAddrTest()
    methodByAddr = "otherContractAddrTest()";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(""));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: nonpayableAddrTest(address)
    methodByAddr = "nonpayableAddrTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(witnessAccount));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000001");

    // Trigger contract method: nonpayableAddrTest(address)
    methodByAddr = "nonpayableAddrTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(nonexistentAccount));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    // Trigger contract method: payableAddrTest(address)
    methodByAddr = "payableAddrTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(nonexistentAccount));
    result = TvmTestUtils
            .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                    factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
            "0000000000000000000000000000000000000000000000000000000000000000");

    ConfigLoader.disable = false;
  }
}


