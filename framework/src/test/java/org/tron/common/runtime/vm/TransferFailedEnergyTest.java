package org.tron.common.runtime.vm;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import stest.tron.wallet.common.client.utils.AbiUtil;

public class TransferFailedEnergyTest extends VMTestBase {
  /*
  pragma solidity ^0.5.4;
  contract EnergyOfTransferFailedTest {
      constructor() payable public {

      }
      // InsufficientBalance
      function testTransferTrxInsufficientBalance() payable public{
          msg.sender.transfer(10);
      }

      function testSendTrxInsufficientBalance() payable public{
          msg.sender.send(10);
      }

      function testTransferTokenInsufficientBalance(trcToken tokenId) payable public{
          msg.sender.transferToken(10, tokenId);
      }

      function testCallTrxInsufficientBalance(address payable caller) public {
          caller.call.value(10)(abi.encodeWithSignature("test()"));
      }

      function testCreateTrxInsufficientBalance() payable public {
          (new Caller).value(10)();
      }

      // NonexistentTarget
      function testTransferTrxNonexistentTarget(address payable nonexistentTarget) payable public {
          require(address(this).balance >= 10);
          nonexistentTarget.transfer(10);
      }

      function testTransferTokenNonexistentTarget(address payable nonexistentTarget,
       trcToken tokenId) payable public {
          require(address(this).balance >= 10);
          nonexistentTarget.transferToken(10, tokenId);
      }

      function testCallTrxNonexistentTarget(address payable nonexistentTarget) payable public {
          require(address(this).balance >= 10);
          nonexistentTarget.call.value(10)(abi.encodeWithSignature("test()"));
      }

      function testSuicideNonexistentTarget(address payable nonexistentTarget) payable public {
           selfdestruct(nonexistentTarget);
      }

      // target is self
      function testTransferTrxSelf() payable public{
          require(address(this).balance >= 10);
          address payable self = address(uint160(address(this)));
          self.transfer(10);
      }

      function testSendTrxSelf() payable public{
          require(address(this).balance >= 10);
          address payable self = address(uint160(address(this)));
          self.send(10);
      }

      function testTransferTokenSelf(trcToken tokenId) payable public{
          require(address(this).balance >= 10);
          address payable self = address(uint160(address(this)));
          self.transferToken(10, tokenId);
      }
  }



  contract Caller {
      constructor() payable public {}
      function test() payable public {}
  }
   */

  /*
  // 0.4.25
  contract EnergyOfTransferFailedTest {

      constructor() payable public {

      }

      // InsufficientBalance
      function testTransferTrxInsufficientBalance() payable public{
          msg.sender.transfer(10);
      }

      function testSendTrxInsufficientBalance() payable public{
          msg.sender.send(10);
      }

      function testTransferTokenInsufficientBalance(trcToken tokenId) payable public{
          msg.sender.transferToken(10, tokenId);
      }

      function testCallTrxInsufficientBalance(address caller) payable public {
          caller.call.value(10)(abi.encodeWithSignature("test()"));
      }

      function testCreateTrxInsufficientBalance() payable public {
          (new Caller).value(10)();
      }

      // NonexistentTarget
      function testTransferTrxNonexistentTarget(address nonexistentTarget) payable public {
          require(address(this).balance >= 10);
          nonexistentTarget.transfer(10);
      }

      function testTransferTokenNonexistentTarget(address nonexistentTarget,
       trcToken tokenId) payable public {
          require(address(this).balance >= 10);
          nonexistentTarget.transferToken(10, tokenId);
      }

      function testCallTrxNonexistentTarget(address nonexistentTarget) public {
          require(address(this).balance >= 10);
          nonexistentTarget.call.value(10)(abi.encodeWithSignature("test()"));
      }

      function testSuicideNonexistentTarget(address nonexistentTarget) public {
           selfdestruct(nonexistentTarget);
      }

      // target is self
      function testTransferTrxSelf() payable public{
          require(address(this).balance >= 10);
          address self = address(uint160(address(this)));
          self.transfer(10);
      }

      function testSendTrxSelf() payable public{
          require(address(this).balance >= 10);
          address self = address(uint160(address(this)));
          self.send(10);
      }

      function testTransferTokenSelf(trcToken tokenId) payable public{
          require(address(this).balance >= 10);
          address self = address(uint160(address(this)));
          self.transferToken(10, tokenId);
      }
  }




  contract Caller {
      constructor() payable public {}
      function test() payable public {}
  }
  */

  private static final String nonExistAddress = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";  // 21 char
  TestCase[] testCasesAfterAllowTvmConstantinop = {
      new TestCase("testTransferTrxSelf()", Collections.emptyList(), false,
          contractResult.TRANSFER_FAILED),
      new TestCase("testSendTrxSelf()", Collections.emptyList(), false,
          contractResult.TRANSFER_FAILED),
      new TestCase("testSuicideNonexistentTarget(address)",
          Collections.singletonList(nonExistAddress), false,
          contractResult.TRANSFER_FAILED),
      new TestCase("testTransferTrxNonexistentTarget(address)",
          Collections.singletonList(nonExistAddress), false,
          contractResult.TRANSFER_FAILED),
      new TestCase("testCallTrxNonexistentTarget(address)",
          Collections.singletonList(nonExistAddress), false,
          contractResult.TRANSFER_FAILED),
  };
  TestCase[] testCasesBeforeAllowTvmConstantinop = {
      new TestCase("testTransferTrxSelf()", Collections.emptyList(),
          true, contractResult.UNKNOWN),
      new TestCase("testSendTrxSelf()", Collections.emptyList(),
          true, contractResult.UNKNOWN),
      new TestCase("testSuicideNonexistentTarget(address)",
          Collections.singletonList(nonExistAddress), true, contractResult.UNKNOWN),
      new TestCase("testTransferTrxNonexistentTarget(address)",
          Collections.singletonList(nonExistAddress), true, contractResult.UNKNOWN),
      new TestCase("testCallTrxNonexistentTarget(address)",
          Collections.singletonList(nonExistAddress), true, contractResult.UNKNOWN),
  };
  TestCase[] testCasesInsufficientBalance = {
      new TestCase("testTransferTrxInsufficientBalance()", Collections.emptyList(),
          false,
          contractResult.REVERT),
      new TestCase("testSendTrxInsufficientBalance()", Collections.emptyList(),
          false,
          contractResult.SUCCESS),
      new TestCase("testCreateTrxInsufficientBalance()", Collections.emptyList(),
          false,
          contractResult.REVERT),
      new TestCase("testCallTrxInsufficientBalance()", Collections.emptyList(),
          false,
          contractResult.REVERT),
      new TestCase("testTransferTokenInsufficientBalance(trcToken)",
          Collections.singletonList(1000001), false, contractResult.REVERT),
  };

  @Test
  public void testTransferFailedAfterAllowTvmConstantinopl()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    ConfigLoader.disable = false;
    manager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    manager.getDynamicPropertiesStore().saveAllowTvmSolidity059(0);

    String contractName = "EnergyOfTransferFailedTest";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[]";
    String code = "608060405261060c806100136000396000f3fe6080604052600436106100c4576000357c010000"
        + "00000000000000000000000000000000000000000000000000009004806344e51c131161008157806344e5"
        + "1c131461017c57806362098f73146101a257806387a9d735146101aa5780639d24d299146101c75780639d"
        + "a1bf8a146101f3578063f10d5077146101fb576100c4565b806307ed3f71146100c957806308e4ab991461"
        + "00d35780630d0def85146100db5780630de991bb1461012857806317b6ad5b1461013057806322970e1814"
        + "610156575b600080fd5b6100d1610218565b005b6100d1610244565b3480156100e757600080fd5b50d380"
        + "156100f457600080fd5b50d2801561010157600080fd5b506100d160048036036020811015610118576000"
        + "80fd5b5035600160a060020a0316610272565b6100d161036f565b6100d160048036036020811015610146"
        + "57600080fd5b5035600160a060020a03166103ad565b6100d16004803603602081101561016c57600080fd"
        + "5b5035600160a060020a03166103b9565b6100d16004803603602081101561019257600080fd5b50356001"
        + "60a060020a03166103fa565b6100d1610409565b6100d1600480360360208110156101c057600080fd5b50"
        + "35610423565b6100d1600480360360408110156101dd57600080fd5b50600160a060020a03813516906020"
        + "0135610496565b6100d16104c1565b6100d16004803603602081101561021157600080fd5b50356104ed56"
        + "5b6040513390600090600a9082818181858883f19350505050158015610241573d6000803e3d6000fd5b50"
        + "565b600a60405161025290610550565b6040518091039082f08015801561026d573d6000803e3d6000fd5b"
        + "505050565b60408051600481526024810182526020810180517bffffffffffffffffffffffffffffffffff"
        + "ffffffffffffffffffffff167ff8a8fd6d0000000000000000000000000000000000000000000000000000"
        + "000017815291518151600160a060020a03851693600a9392918291908083835b6020831061030257805182"
        + "52601f1990920191602091820191016102e3565b6001836020036101000a03801982511681845116808217"
        + "855250505050505090500191505060006040518083038185875af1925050503d8060008114610364576040"
        + "519150601f19603f3d011682016040523d82523d6000602084013e610369565b606091505b50505050565b"
        + "600a3031101561037e57600080fd5b60405130908190600090600a9082818181858883f193505050501580"
        + "156103a9573d6000803e3d6000fd5b5050565b80600160a060020a0316ff5b600a303110156103c8576000"
        + "80fd5b604051600160a060020a03821690600090600a9082818181858883f193505050501580156103a957"
        + "3d6000803e3d6000fd5b600a3031101561027257600080fd5b6040513390600090600a9082818181858883"
        + "f15050505050565b600a3031101561043257600080fd5b30806000600a848015801561044657600080fd5b"
        + "50806780000000000000001115801561045e57600080fd5b5080620f42401015801561047157600080fd5b"
        + "50604051600081818185878a8ad094505050505015801561026d573d6000803e3d6000fd5b600a30311015"
        + "6104a557600080fd5b600160a060020a0382166000600a838015801561044657600080fd5b600a30311015"
        + "6104d057600080fd5b60405130908190600090600a9082818181858883f1505050505050565b336000600a"
        + "838015801561050057600080fd5b50806780000000000000001115801561051857600080fd5b5080620f42"
        + "401015801561052b57600080fd5b50604051600081818185878a8ad09450505050501580156103a9573d60"
        + "00803e3d6000fd5b60848061055d8339019056fe608060405260738060116000396000f3fe608060405260"
        + "0436106038577c01000000000000000000000000000000000000000000000000000000006000350463f8a8"
        + "fd6d8114603d575b600080fd5b60436045565b005b56fea165627a7a72305820ae73d633cf81f32e8c6917"
        + "d5faea925dd9c04abcecf002617d6ec1440f1349c90029a165627a7a723058201f0cab76b7df6e1900e752"
        + "4fd40e9257a915692888b74c2e2e68df9d9a4a5a910029";
    long value = 100000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent,
        null);
    byte[] addressWithSufficientBalance = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesAfterAllowTvmConstantinop) {
      checkResult(testCase, addressWithSufficientBalance);
    }

    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, 0, fee, consumeUserResourcePercent,
        null);
    byte[] addressWithoutBalance = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesInsufficientBalance) {
      checkResult(testCase, addressWithoutBalance);
    }
  }

  @Test
  public void testTransferFailedBeforeAllowTvmConstantinopl()
      throws ContractExeException, ReceiptCheckErrException,
      VMIllegalException, ContractValidateException {
    ConfigLoader.disable = false;
    manager.getDynamicPropertiesStore().saveAllowTvmTransferTrc10(1);
    manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(0);
    manager.getDynamicPropertiesStore().saveAllowTvmSolidity059(0);

    String contractName = "EnergyOfTransferFailedTest";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[]";
    String code = "6080604052610537806100136000396000f3006080604052600436106100b95763ffffffff7c010"
        + "000000000000000000000000000000000000000000000000000000060003504166307ed3f7181146100be57"
        + "806308e4ab99146100c85780630d0def85146100d05780630de991bb146100e457806317b6ad5b146100ec5"
        + "7806322970e181461012757806344e51c131461013b57806362098f731461017657806387a9d7351461017e"
        + "5780639d24d299146101895780639da1bf8a146101a0578063f10d5077146101a8575b600080fd5b6100c661"
        + "01b3565b005b6100c66101df565b6100c6600160a060020a0360043516610209565b6100c66102d9565b3480"
        + "156100f857600080fd5b50d3801561010557600080fd5b50d2801561011257600080fd5b506100c6600160a0"
        + "60020a036004351661031a565b6100c6600160a060020a0360043516610326565b34801561014757600080fd"
        + "5b50d3801561015457600080fd5b50d2801561016157600080fd5b506100c6600160a060020a036004351661"
        + "0367565b6100c6610376565b6100c6600435610390565b6100c6600160a060020a03600435166024356103d1"
        + "565b6100c6610416565b6100c6600435610445565b6040513390600090600a9082818181858883f193505050"
        + "501580156101dc573d6000803e3d6000fd5b50565b600a6101e9610472565b6040518091039082f080158015"
        + "610204573d6000803e3d6000fd5b505050565b60408051600481526024810182526020810180517bffffffff"
        + "ffffffffffffffffffffffffffffffffffffffffffffffff167ff8a8fd6d0000000000000000000000000000"
        + "000000000000000000000000000017815291518151600160a060020a03851693600a93929182919080838360"
        + "005b8381101561029457818101518382015260200161027c565b50505050905090810190601f1680156102c1"
        + "5780820380516001836020036101000a031916815260200191505b5091505060006040518083038185875af1"
        + "5050505050565b6000600a303110156102ea57600080fd5b5060405130908190600090600a90828181818588"
        + "83f19350505050158015610316573d6000803e3d6000fd5b5050565b80600160a060020a0316ff5b600a3031"
        + "101561033557600080fd5b604051600160a060020a03821690600090600a9082818181858883f19350505050"
        + "158015610316573d6000803e3d6000fd5b600a3031101561020957600080fd5b6040513390600090600a9082"
        + "818181858883f15050505050565b6000600a303110156103a157600080fd5b5060405130908190600090600"
        + "a9085908381818185878a84d0945050505050158015610204573d6000803e3d6000fd5b600a303110156103"
        + "e057600080fd5b604051600160a060020a03831690600090600a9084908381818185878a84d094505050505"
        + "0158015610204573d6000803e3d6000fd5b6000600a3031101561042757600080fd5b506040513090819060"
        + "0090600a9082818181858883f1505050505050565b6040513390600090600a9084908381818185878a84d09"
        + "45050505050158015610316573d6000803e3d6000fd5b604051608a80610482833901905600608060405260"
        + "798060116000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000"
        + "00000000000000000000000000000600035041663f8a8fd6d81146043575b600080fd5b6049604b565b005b"
        + "5600a165627a7a7230582066a52bc3564dc2aaabcf2d0f05931fdc34035835d12182e3140d5f3f7e5d73720"
        + "029a165627a7a723058207a028d240821b50b5b91c64afed0a997bd46f353f5e8f374ee4823c19d0ed0130"
        + "029";
    long value = 100000;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent,
        null);
    byte[] addressWithSufficientBalance = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesBeforeAllowTvmConstantinop) {
      checkResult(testCase, addressWithSufficientBalance);
    }

    trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, 0, fee, consumeUserResourcePercent,
        null);
    byte[] addressWithoutBalance = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    for (TestCase testCase : testCasesInsufficientBalance) {
      checkResult(testCase, addressWithoutBalance);
    }
  }

  private void checkResult(TestCase testCase, byte[] factoryAddress)
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    String hexInput = AbiUtil.parseMethod(testCase.getMethod(), testCase.getParams());
    long fee = 100000000;
    long allEnergy = 1000000;
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    ProgramResult programResult = result.getRuntime().getResult();
    ReceiptCapsule receiptCapsule = result.getReceipt();
    Assert.assertEquals(receiptCapsule.getResult(), testCase.getReceiptResult(),
        testCase.getMethod());
    if (testCase.allEnergy) {
      Assert.assertEquals(programResult.getEnergyUsed(), 1000000, testCase.getMethod());
    } else {
      Assert.assertTrue(programResult.getEnergyUsed() < allEnergy, testCase.getMethod());
    }
  }

  @Data
  @AllArgsConstructor
  @ToString
  static class TestCase {

    String method;
    List<Object> params;
    boolean allEnergy;
    contractResult receiptResult;
  }
}
