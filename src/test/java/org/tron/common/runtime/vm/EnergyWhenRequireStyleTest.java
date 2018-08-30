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
import org.tron.core.exception.OutOfSlotTimeException;
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
  // If you create a contract using the new keyword but the contract creation does not finish properly (see above for the definition of “not finish properly”).
  // If you perform an external function call targeting a contract that contains no code.
  // If your contract receives Ether via a public function without payable modifier (including the constructor and the fallback function).
  // If your contract receives Ether via a public getter function.
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
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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

  @Test
  public void thisFunctionViaMessageCallTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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
  public void thatFunctionViaMessageCallTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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
  public void newContractTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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
  public void receiveTrxWithoutPayableTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

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

