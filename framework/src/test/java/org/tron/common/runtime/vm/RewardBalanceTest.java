package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

import java.util.Arrays;

@Slf4j
public class RewardBalanceTest extends VMTestBase {

  @Test
  public void testRewardBalance()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    VMConfig.initAllowTvmVote(1);
    String contractName = "TestRewardBalance";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},"
        + "{\"constant\":true,\"inputs\":[],\"name\":\"localContractAddrTest\",\"outputs\":[{\"internalType\":\""
        + "uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":true,\"inputs\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\""
        + "address\"}],\"name\":\"nonpayableAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\""
        + "type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\""
        + ":true,\"inputs\":[],\"name\":\"nullAddressTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\""
        + ",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant"
        + "\":true,\"inputs\":[],\"name\":\"otherContractAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name"
        + "\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\""
        + "constant\":true,\"inputs\":[{\"internalType\":\"address payable\",\"name\":\"addr\",\"type\":\"address\"}],"
        + "\"name\":\"payableAddrTest\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\""
        + "internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"rewardBalanceTest\",\"outputs\""
        + ":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":"
        + "\"view\",\"type\":\"function\"}]";
    String factoryCode = "60806040526040516100109061008b565b604051809103906000f08015801561002c573d6000803e3d6000fd5b506"
        + "00280546001600160a01b0319166001600160a01b039290921691909117905534801561005957600080fd5b50d380156100665760008"
        + "0fd5b50d2801561007357600080fd5b50600080546001600160a01b03191633179055610097565b6072806101c083390190565b61011"
        + "a806100a66000396000f3fe6080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28015602757600080fd5b50600"
        + "4361060725760003560e01c806356b42994146077578063627bfa45146077578063a223c65f146077578063af4a11051460ac578063c"
        + "b2d51cf1460b2578063d30a28ee1460b8575b600080fd5b609a60048036036020811015608b57600080fd5b50356001600160a01b031"
        + "660be565b60408051918252519081900360200190f35b609a60cb565b609a60d1565b609a60d6565b6001600160a01b0316d890565b6"
        + "000d890565b30d890565b6002546001600160a01b0316d89056fea26474726f6e5820ebdd2d65bd20286347fb1bc137802fc49353b40"
        + "cb547e8cfa04c6c6330e5e0e764736f6c634300050d00316080604052348015600f57600080fd5b50d38015601b57600080fd5b50d28"
        + "015602757600080fd5b50603d8060356000396000f3fe6080604052600080fdfea26474726f6e58208ee7acdd9de78115ed209988338"
        + "b09808499be9fce6f39e31666a4521cf3af2b64736f6c634300050d0031";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

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

    // Trigger contract method: rewardBalanceTest(address)
    String methodByAddr = "rewardBalanceTest(address)";
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
            "0000000000000000000000000000000000000000000000000000000000000000");

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
//
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
            "0000000000000000000000000000000000000000000000000000000000000000");

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


