package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TVMTestUtils;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.protos.Protocol.AccountType;

@Slf4j
@Ignore
public class EnergyWhenRequireStyleTest {

  private Manager dbManager;
  private AnnotationConfigApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_EnergyWhenRequireStyleTest";
  private String OWNER_ADDRESS;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    // context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);
    deposit.commit();
  }

  // A require-style exception is generated in the following situations:

  // Calling throw.
  // Calling require with an argument that evaluates to false.
  // If you call a function via a message call but it does not finish properly (i.e. it runs out of gas, has no matching function, or throws an exception itself), except when a low level operation call, send, delegatecall, callcode or staticcall is used. The low level operations never throw exceptions but indicate failures by returning false.
  // If you create a contract using the new keyword but the contract creation does not finish properly
  // If your contract receives Ether via a public function without payable modifier (including the constructor and the fallback function).
  // If a .transfer() fails.
  // If revert().

  // pragma solidity ^0.4.16;
  //
  // contract TestThrowContract {
  //
  //   function testThrow(){
  //     throw;
  //   }
  //
  // }

  @Test
  public void throwTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052348015600f57600080fd5b5060838061001e6000396000f300608060405260043610603e5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166350bff6bf81146043575b600080fd5b348015604e57600080fd5b506055603e565b0000a165627a7a72305820f51282c5910e3ff1b5f2e9509f3cf23c7035027aae1947ab46e5a9252fb061eb0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 75);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testThrow()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 124);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  // pragma solidity ^0.4.16;
  //
  // contract TestRequireContract {
  //
  //   function testRequire() {
  //     require(2==1);
  //   }
  // }

  @Test
  public void requireTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testRequire\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052348015600f57600080fd5b5060838061001e6000396000f300608060405260043610603e5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663357815c481146043575b600080fd5b348015604e57600080fd5b506055603e565b0000a165627a7a7230582054141931bcc37d4f266815f02d2fb113f5af20825cbce45d3b0f2fe90ac0145d0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 75);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testRequire()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 124);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  // pragma solidity ^0.4.16;
  //
  // contract TestThisFunctionViaMessageCallContract {
  //
  //   function testAssert(){
  //     assert(1==2);
  //   }
  //
  //   function testThisFunctionViaMessageCall() {
  //     this.testAssert.gas(5000)();
  //   }
  //
  // }

  @Test
  public void thisFunctionViaMessageCallTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testThisFunctionViaMessageCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610121806100206000396000f30060806040526004361060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632b813bc08114604d5780635df83fe7146061575b600080fd5b348015605857600080fd5b50605f6073565b005b348015606c57600080fd5b50605f6075565bfe5b3073ffffffffffffffffffffffffffffffffffffffff16632b813bc06113886040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401600060405180830381600088803b15801560db57600080fd5b5087f115801560ee573d6000803e3d6000fd5b50505050505600a165627a7a7230582087d830c44fb566498789b212e3d0374f7a7589a2efdda11b3a4c03051b57891a0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 105);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testThisFunctionViaMessageCall()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 5339);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  // pragma solidity ^0.4.16;
  //
  // contract subContract {
  //   function testAssert(){
  //     assert(1==2);
  //   }
  // }
  //
  // contract TestThatFunctionViaMessageCallContract {
  //
  //   function testThatFunctionViaMessageCall() {
  //     // msg.sender.static.testRequire(1);
  //
  //     // call, send, delegatecall, callcode or staticcall
  //     subContract sc = new subContract();
  //     sc.testAssert.gas(5000)();
  //     // address(sc).call(bytes4(keccak256("a()")));
  //     // sc.a.value(5 ether)(); // revert, actual gas, not all, balance not reduce, is a call
  //     // address(sc).send(5 ether); // no revert, success, balance not reduce, is a call
  //     // address(sc).transfer(5 ether); // revert, actual gas, not all, balance not reduce, is a call
  //   }
  // }

  @Test
  public void thatFunctionViaMessageCallTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testThatFunctionViaMessageCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b506101e6806100206000396000f3006080604052600436106100405763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416637dbc1cb88114610045575b600080fd5b34801561005157600080fd5b5061005a61005c565b005b6000610066610108565b604051809103906000f080158015610082573d6000803e3d6000fd5b5090508073ffffffffffffffffffffffffffffffffffffffff16632b813bc06113886040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401600060405180830381600088803b1580156100ec57600080fd5b5087f1158015610100573d6000803e3d6000fd5b505050505050565b60405160a3806101188339019056006080604052348015600f57600080fd5b5060858061001e6000396000f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632b813bc081146043575b600080fd5b348015604e57600080fd5b5060556057565b005bfe00a165627a7a72305820c02c76575c2a0ada80c3f6db47f885cece6c254d1e7c79eb6ddc1c1d4e70ebae0029a165627a7a72305820cf879e62f738b44636adf61bd4b2fb38c10f027d2a4484d58baf44a06dc97bd90029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 141);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testThatFunctionViaMessageCall()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 37525);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  // pragma solidity ^0.4.16;
  //
  // contract subContract {
  //   constructor (){
  //     assert(1==2);
  //   }
  // }
  //
  // contract TestNewContractContract {
  //
  //   function testNewContract() {
  //
  //     subContract sc = new subContract();
  //   }
  // }

  @Test
  public void newContractTest1()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testNewContract\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b5060d58061001f6000396000f3006080604052600436106100405763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416635d10a9e68114610045575b600080fd5b34801561005157600080fd5b5061005a61005c565b005b6000610066610087565b604051809103906000f080158015610082573d6000803e3d6000fd5b505050565b6040516013806100978339019056006080604052348015600f57600080fd5b50fe00a165627a7a72305820685ff8f74890f671deb4d3881a4b72ab0daac2ab0d36112e1ebdf98a43ac4d940029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 87);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testNewContract()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);

    // 卡住了！！
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  @Test
  public void receiveTrxWithoutPayableTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052348015600f57600080fd5b5060838061001e6000396000f300608060405260043610603e5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166350bff6bf81146043575b600080fd5b348015604e57600080fd5b506055603e565b0000a165627a7a72305820f51282c5910e3ff1b5f2e9509f3cf23c7035027aae1947ab46e5a9252fb061eb0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 75);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testThrow()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 124);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  @Test
  public void transferTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052348015600f57600080fd5b5060838061001e6000396000f300608060405260043610603e5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166350bff6bf81146043575b600080fd5b348015604e57600080fd5b506055603e565b0000a165627a7a72305820f51282c5910e3ff1b5f2e9509f3cf23c7035027aae1947ab46e5a9252fb061eb0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 75);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testThrow()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 124);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  @Test
  public void revertTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;

    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052348015600f57600080fd5b5060838061001e6000396000f300608060405260043610603e5763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166350bff6bf81146043575b600080fd5b348015604e57600080fd5b506055603e565b0000a165627a7a72305820f51282c5910e3ff1b5f2e9509f3cf23c7035027aae1947ab46e5a9252fb061eb0029";
    String libraryAddressPair = null;

    TVMTestResult result = TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 75);
    byte[] contractAddress = result.getContractAddress();

    /* ====================================================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testThrow()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 124);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), true);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() == null);

  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }
}

