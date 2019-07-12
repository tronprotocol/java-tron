package org.tron.common.runtime.vm;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.runtime.config.VMConfig;
import org.tron.core.Wallet;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
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
        if (addr.isContract == true) {
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
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    VMConfig.initAllowTvmTransferTrc10(1);
    VMConfig.initAllowTvmConstantinople(1);
    String contractName = "TestIsContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[],\"name\":\"senderIsContrct\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"isTestTrue\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"pure\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTestEquals\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTestIf\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"isTestFalse\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"pure\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"withCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"withDelegatecall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTest\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"isTestView\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"isTestSender\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"isContrct\",\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String factoryCode = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b503073ffffffffffffffffffffffffffffffffffffffff16d46000806101000a81548160ff0219169083151502179055506107348061006a6000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100c35760003560e01c8063a56040111161008b578063a5604011146101e6578063d6aa61261461022a578063e49aa9331461026e578063e8fd28a4146102ca578063ea5bbbb514610326578063fa57c01714610330576100c3565b80630d9975f9146100c857806312af5300146100ea57806317349ae31461010c578063722c15b41461016857806382e7a219146101c4575b600080fd5b6100d0610352565b604051808215151515815260200191505060405180910390f35b6100f2610365565b604051808215151515815260200191505060405180910390f35b61014e6004803603602081101561012257600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061036e565b604051808215151515815260200191505060405180910390f35b6101aa6004803603602081101561017e57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506103a7565b604051808215151515815260200191505060405180910390f35b6101cc6103d9565b604051808215151515815260200191505060405180910390f35b610228600480360360208110156101fc57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506103e1565b005b61026c6004803603602081101561024057600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061051b565b005b6102b06004803603602081101561028457600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610653565b604051808215151515815260200191505060405180910390f35b61030c600480360360208110156102e057600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610674565b604051808215151515815260200191505060405180910390f35b61032e610695565b005b6103386106c8565b604051808215151515815260200191505060405180910390f35b600060019054906101000a900460ff1681565b60006001905090565b6000600115158273ffffffffffffffffffffffffffffffffffffffff16d41515141561039d57600190506103a2565b600090505b919050565b60008173ffffffffffffffffffffffffffffffffffffffff16d4156103cf57600190506103d4565b600090505b919050565b600080905090565b8073ffffffffffffffffffffffffffffffffffffffff166040516024016040516020818303038152906040527fea5bbbb5000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff83818316178352505050506040518082805190602001908083835b602083106104ae578051825260208201915060208101905060208303925061048b565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d8060008114610510576040519150601f19603f3d011682016040523d82523d6000602084013e610515565b606091505b50505050565b8073ffffffffffffffffffffffffffffffffffffffff166040516024016040516020818303038152906040527fea5bbbb5000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff83818316178352505050506040518082805190602001908083835b602083106105e857805182526020820191506020810190506020830392506105c5565b6001836020036101000a038019825116818451168082178552505050505050905001915050600060405180830381855af49150503d8060008114610648576040519150601f19603f3d011682016040523d82523d6000602084013e61064d565b606091505b50505050565b60008173ffffffffffffffffffffffffffffffffffffffff16d49050919050565b60008173ffffffffffffffffffffffffffffffffffffffff16d49050919050565b3373ffffffffffffffffffffffffffffffffffffffff16d4600060016101000a81548160ff021916908315150217905550565b6000809054906101000a900460ff168156fea265627a7a72305820dc6e6f3ee7019031599fd5e56e8387d10fdfd271785d233e6eb21ca2995e675364736f6c637827302e352e392d646576656c6f702e323031392e372e31322b636f6d6d69742e36316637333630660057";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent, null);
    byte[] factoryAddress = Wallet.generateContractAddress(trx);
    String factoryAddressStr = Wallet.encode58Check(factoryAddress);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        "", address, ABI, factoryCode, value, fee, consumeUserResourcePercent, null);
    byte[] factoryAddressOther = Wallet.generateContractAddress(trx);
    String factoryAddressStrOther = Wallet.encode58Check(factoryAddressOther);
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
    String existentAccount = Wallet.encode58Check(address);
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

  }

}


