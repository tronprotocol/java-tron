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
import org.tron.common.runtime.vm.program.Program.IllegalOperationException;
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
public class CPUEnergyWhenRevertStyleTest {

  private Manager dbManager;
  private AnnotationConfigApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_CPUEnergyWhenAssertStyleTest";
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
  }

  // solidity for CPUEnergyWhenAssertStyleTest
  // pragma solidity ^0.4.0;
  //
  // An assert-style exception is generated in the following situations:
  //
  // If you access an array at a too large or negative index (i.e. x[i] where i >= x.length or i < 0).
  // If you access a fixed-length bytesN at a too large or negative index.
  // If you divide or modulo by zero (e.g. 5 / 0 or 23 % 0).
  // If you shift by a negative amount.
  // If you convert a value too big or negative into an enum type.
  // If you call a zero-initialized variable of internal function type.
  // If you call assert with an argument that evaluates to false.

  // contract TestAssertStyleContract{
  //
  //   enum fortest {one, second, third}
  //
  //   function testOutOfIndex() public {
  //     uint256[] memory a = new uint256[](10);
  //     a[10] = 10;
  //   }
  //
  //   function testbytesN() public {
  //     bytes16 a = 0x12345;
  //     uint c = 20;
  //     uint b = uint256(a[c]);
  //   }
  //
  //   function testDivZero() public {
  //     uint256 a = 0;
  //     uint256 b = 10 / a;
  //   }
  //
  //   function testShiftByNegative() public {
  //     int256 shift = -10;
  //     int256 a = 1024 >> shift;
  //   }
  //
  //   function testEnumType() public {
  //     fortest a = fortest(10);
  //
  //   }
  //
  //   function testFunctionPointer() public {
  //     function (int) internal pure returns (int) funcPtr;
  //     funcPtr(1);
  //   }
  //
  //   function testAssert(){
  //     assert(1==2);
  //   }
  //
  // }

  @Test
  public void whenAssertStyleTest()
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;
    TVMTestResult result = deployWhenAssertStyleTestContract(value, feeLimit,
        consumeUserResourcePercent);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 135);
    byte[] contractAddress = result.getContractAddress();

    /* =================================== CALL testOutOfIndex() =================================== */
    byte[] triggerData = TVMTestUtils.parseABI("testOutOfIndex()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

    /* =================================== CALL testbytesN() =================================== */
    triggerData = TVMTestUtils.parseABI("testbytesN()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

    /* =================================== CALL testDivZero() =================================== */
    triggerData = TVMTestUtils.parseABI("testDivZero()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

    /* =================================== CALL testShiftByNegative() =================================== */
    triggerData = TVMTestUtils.parseABI("testShiftByNegative()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

    /* =================================== CALL testEnumType() =================================== */
    triggerData = TVMTestUtils.parseABI("testEnumType()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

    /* =================================== CALL testFunctionPointer() =================================== */
    triggerData = TVMTestUtils.parseABI("testFunctionPointer()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

    /* =================================== CALL testAssert() =================================== */
    triggerData = TVMTestUtils.parseABI("testAssert()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 200000000000L);
    Assert.assertEquals(result.getRuntime().getResult().isRevert(), false);
    Assert.assertTrue(
        result.getRuntime().getResult().getException() instanceof IllegalOperationException);

  }

  public TVMTestResult deployWhenAssertStyleTestContract(long value, long feeLimit,
      long consumeUserResourcePercent)
      throws ContractExeException, ReceiptCheckErrException, TransactionTraceException, ContractValidateException {
    String contractName = "test";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"testbytesN\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testEnumType\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testOutOfIndex\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testDivZero\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testShiftByNegative\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testFunctionPointer\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b506101d7806100206000396000f3006080604052600436106100825763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416631e76e10781146100875780632b813bc01461009e5780635a43cddc146100b35780639a4e1fa0146100c8578063b87d948d146100dd578063e88e362a146100f2578063e9ad8ee714610107575b600080fd5b34801561009357600080fd5b5061009c61011c565b005b3480156100aa57600080fd5b5061009c610138565b3480156100bf57600080fd5b5061009c61013a565b3480156100d457600080fd5b5061009c610140565b3480156100e957600080fd5b5061009c610183565b3480156100fe57600080fd5b5061009c61018b565b34801561011357600080fd5b5061009c610196565b7201234500000000000000000000000000000000601460008282fe5bfe5b6000600afe5b60408051600a80825261016082019092526060916020820161014080388339019050509050600a81600a81518110151561017657fe5b6020908102909101015250565b60008080600afe5b600919600081610400fe5b6101386101a760018263ffffffff16565b50505600a165627a7a72305820155b43453889c7c579af81c62359ac291bb44abe0ab5c6772971f69745a4cfc20029";
    String libraryAddressPair = null;

    return TVMTestUtils
        .deployContractAndReturnTVMTestResult(contractName, address, ABI, code,
            value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair,
            deposit, null);
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

