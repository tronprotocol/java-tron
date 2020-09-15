package org.tron.common.runtime.vm;

import java.util.Arrays;
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

@Slf4j
public class IsContractTest extends VMTestBase {
  /*

pragma solidity >=0.4.22 <0.6.0;

contract isTestCtr {
    bool public isContrct;
    bool public senderIsContrct;
    constructor () public {
        isContrct = address(this).isContract;
    }
    function isTest(address addr) view public returns (bool) {
        return addr.isContract;
    }
    function isTestIf(address addr) view public returns (bool) {
        if (addr.isContract) {
            return true;
        } else {
            return false;
        }
    }
    function isTestEquals(address addr) view public returns (bool) {
        if (addr.isContract) {
            return true;
        } else {
            return false;
        }
    }
    function isTestView(address addr) view public returns (bool) {
        return addr.isContract;
    }
    function isTestSender() public {
        senderIsContrct = msg.sender.isContract;
    }
    function withCall(address addr) public {
        addr.call(abi.encodeWithSignature("isTestSender()"));
    }
    function withDelegatecall(address addr) public {
        addr.delegatecall(abi.encodeWithSignature("isTestSender()"));
    }
    function killme() public {
        selfdestruct(msg.sender);
    }
    function isTestTrue() pure public returns (bool) {
        return true;
    }
    function isTestFalse() pure public returns (bool) {
        return false;
    }
}

  */

  @Test
  public void testIsContract()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    ConfigLoader.disable = true;
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    VMConfig.initAllowTvmSolidity059(1);
    String contractName = "TestIsContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"senderIsContrct\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":"
        + "\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"isTestTrue\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,"
        + "\"stateMutability\":\"pure\",\"type\":\"function\"},{\"constant\":true,\"inputs\""
        + ":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTestEquals\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"killme\","
        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":"
        + "\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"addr\",\"type\""
        + ":\"address\"}],\"name\":\"isTestIf\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\""
        + ":true,\"inputs\":[],\"name\":\"isTestFalse\",\"outputs\":[{\"name\":\"\",\"type\":"
        + "\"bool\"}],\"payable\":false,\"stateMutability\":\"pure\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":"
        + "\"withCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":"
        + "\"address\"}],\"name\":\"withDelegatecall\",\"outputs\":[],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
        + "\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTest\","
        + "\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\""
        + ":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTestView\",\"outputs\":"
        + "[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\""
        + ",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"isTestSender\""
        + ",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\""
        + ":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"isContrct\",\"outputs\""
        + ":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\""
        + ",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":"
        + "\"nonpayable\",\"type\":\"constructor\"}]";

    String factoryCode = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d28015610"
        + "02a57600080fd5b503073ffffffffffffffffffffffffffffffffffffffff16d46000806101000a8154816"
        + "0ff0219169083151502179055506107628061006a6000396000f3fe608060405234801561001057600080f"
        + "d5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100ce5760003560e01c8"
        + "063a56040111161008b578063a5604011146101fb578063d6aa61261461023f578063e49aa933146102835"
        + "78063e8fd28a4146102df578063ea5bbbb51461033b578063fa57c01714610345576100ce565b80630d997"
        + "5f9146100d357806312af5300146100f557806317349ae31461011757806324d97a4a14610173578063722"
        + "c15b41461017d57806382e7a219146101d9575b600080fd5b6100db610367565b604051808215151515815"
        + "260200191505060405180910390f35b6100fd61037a565b604051808215151515815260200191505060405"
        + "180910390f35b6101596004803603602081101561012d57600080fd5b81019080803573fffffffffffffff"
        + "fffffffffffffffffffffffff169060200190929190505050610383565b604051808215151515815260200"
        + "191505060405180910390f35b61017b6103bc565b005b6101bf6004803603602081101561019357600080f"
        + "d5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506103d"
        + "5565b604051808215151515815260200191505060405180910390f35b6101e1610407565b6040518082151"
        + "51515815260200191505060405180910390f35b61023d6004803603602081101561021157600080fd5b810"
        + "19080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061040f565b0"
        + "05b6102816004803603602081101561025557600080fd5b81019080803573fffffffffffffffffffffffff"
        + "fffffffffffffff169060200190929190505050610549565b005b6102c5600480360360208110156102995"
        + "7600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505"
        + "050610681565b604051808215151515815260200191505060405180910390f35b610321600480360360208"
        + "110156102f557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200"
        + "1909291905050506106a2565b604051808215151515815260200191505060405180910390f35b610343610"
        + "6c3565b005b61034d6106f6565b604051808215151515815260200191505060405180910390f35b6000600"
        + "19054906101000a900460ff1681565b60006001905090565b6000600115158273fffffffffffffffffffff"
        + "fffffffffffffffffff16d4151514156103b257600190506103b7565b600090505b919050565b3373fffff"
        + "fffffffffffffffffffffffffffffffffff16ff5b60008173fffffffffffffffffffffffffffffffffffff"
        + "fff16d4156103fd5760019050610402565b600090505b919050565b600080905090565b8073fffffffffff"
        + "fffffffffffffffffffffffffffff166040516024016040516020818303038152906040527fea5bbbb5000"
        + "000000000000000000000000000000000000000000000000000007bfffffffffffffffffffffffffffffff"
        + "fffffffffffffffffffffffff19166020820180517bfffffffffffffffffffffffffffffffffffffffffff"
        + "fffffffffffff83818316178352505050506040518082805190602001908083835b602083106104dc57805"
        + "182526020820191506020810190506020830392506104b9565b6001836020036101000a038019825116818"
        + "4511680821785525050505050509050019150506000604051808303816000865af19150503d80600081146"
        + "1053e576040519150601f19603f3d011682016040523d82523d6000602084013e610543565b606091505b5"
        + "0505050565b8073ffffffffffffffffffffffffffffffffffffffff1660405160240160405160208183030"
        + "38152906040527fea5bbbb5000000000000000000000000000000000000000000000000000000007bfffff"
        + "fffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bfffffffffffffffff"
        + "fffffffffffffffffffffffffffffffffffffff83818316178352505050506040518082805190602001908"
        + "083835b6020831061061657805182526020820191506020810190506020830392506105f3565b600183602"
        + "0036101000a038019825116818451168082178552505050505050905001915050600060405180830381855"
        + "af49150503d8060008114610676576040519150601f19603f3d011682016040523d82523d6000602084013e"
        + "61067b565b606091505b50505050565b60008173ffffffffffffffffffffffffffffffffffffffff16d4905"
        + "0919050565b60008173ffffffffffffffffffffffffffffffffffffffff16d49050919050565b3373ffffff"
        + "ffffffffffffffffffffffffffffffffff16d4600060016101000a81548160ff02191690831515021790555"
        + "0565b6000809054906101000a900460ff168156fea265627a7a72305820f19cfc778154ac6cd41bf04a45ee"
        + "be28970cd78b0d35de5d77570e139d9b760664736f6c637827302e352e392d646576656c6f702e3230313"
        + "92e372e31322b636f6d6d69742e36316637333630660057";
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

    // Trigger contract method: isTest(address)
    String methodByAddr = "isTest(address)";
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
    String existentAccount = StringUtil.encode58Check(address);
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(existentAccount));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    String precompileContractAddr =
        "0000000000000000000000000000000000000000000000000000000000010001";
    hexInput = AbiUtil.parseMethod(methodByAddr, precompileContractAddr, true);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
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
        "0000000000000000000000000000000000000000000000000000000000000001");

    // trigger deployed contract
    methodByAddr = "isContrct()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000001");

    // trigger deployed contract
    methodByAddr = "senderIsContrct()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    methodByAddr = "isTestSender()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    methodByAddr = "senderIsContrct()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    methodByAddr = "withCall(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStrOther));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    methodByAddr = "senderIsContrct()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddressOther, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000001");

    // trigger deployed contract
    methodByAddr = "withDelegatecall(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStrOther));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    methodByAddr = "senderIsContrct()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    methodByAddr = "killme()";
    hexInput = AbiUtil.parseMethod(methodByAddr, "");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddressOther, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    methodByAddr = "isTest(address)";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(factoryAddressStrOther));
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


