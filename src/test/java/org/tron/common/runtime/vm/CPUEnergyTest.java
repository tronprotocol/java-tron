package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
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
public class CPUEnergyTest {

  private Manager dbManager;
  private AnnotationConfigApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_CPUEnergyTest";
  private String OWNER_ADDRESS;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);
  }

  // solidity for callValueTest
  // pragma solidity ^0.4.0;
  //
  // contract SubContract {
  //
  //   constructor () payable {}
  //   mapping(uint256=>uint256) map;
  //
  //   function doSimple() public payable returns (uint ret) {
  //     return 42;
  //   }
  //
  //   function doComplex() public payable returns (uint ret) {
  //     for (uint i = 0; i < 10; i++) {
  //       map[i] = i;
  //     }
  //   }
  //
  // }
  //
  // contract TestForValueGasFunction {
  //
  //   SubContract subContract;
  //
  //   constructor () payable {
  //     subContract = new SubContract();
  //   }
  //
  //   function simpleCall() public { subContract.doSimple.value(10).gas(3)(); }
  //
  //   function complexCall() public { subContract.doComplex.value(10).gas(3)(); }
  //
  // }

  @Test
  public void callValueTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

    long value = 10000000L;
    long feeLimit = 20000000000000L; // sun
    long consumeUserResourcePercent = 100;
    TVMTestResult result = deployCallValueTestContract(value, feeLimit,
        consumeUserResourcePercent);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 52439);
    byte[] contractAddress = result.getContractAddress();

    /* =================================== CALL simpleCall() =================================== */
    byte[] triggerData = TVMTestUtils.parseABI("simpleCall()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 7370);

    /* =================================== CALL complexCall() =================================== */
    triggerData = TVMTestUtils.parseABI("complexCall()", null);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, feeLimit, deposit, null);

    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 9459);
    // boolean haveException = false;
    // try {
    //   result = TVMTestUtils
    //       .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
    //           contractAddress, triggerData, value, feeLimit, deposit, null);
    // } catch (Exception e) {
    //   Assert.assertTrue(e instanceof OutOfSlotTimeException);
    //   haveException = true;
    // }
    // Assert.assertTrue(haveException);
  }

  public TVMTestResult deployCallValueTestContract(long value, long feeLimit,
      long consumeUserResourcePercent)
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {
    String contractName = "TestForCallValue";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"complexCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"simpleCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405261000c61004e565b604051809103906000f080158015610028573d6000803e3d6000fd5b5060008054600160a060020a031916600160a060020a039290921691909117905561005d565b60405160d68061020b83390190565b61019f8061006c6000396000f3006080604052600436106100325763ffffffff60e060020a60003504166306ce93af811461003757806340de221c1461004e575b600080fd5b34801561004357600080fd5b5061004c610063565b005b34801561005a57600080fd5b5061004c610103565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cd95478c600a6003906040518363ffffffff1660e060020a0281526004016020604051808303818589803b1580156100d357600080fd5b5088f11580156100e7573d6000803e3d6000fd5b5050505050506040513d60208110156100ff57600080fd5b5050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663b993e5e2600a6003906040518363ffffffff1660e060020a0281526004016020604051808303818589803b1580156100d357600080fd00a165627a7a72305820cb5f172ca9f81235a8b33ee1ddef9dd1b398644cf61228569356ff051bfaf3d10029608060405260c4806100126000396000f30060806040526004361060485763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663b993e5e28114604d578063cd95478c146065575b600080fd5b6053606b565b60408051918252519081900360200190f35b60536070565b602a90565b6000805b600a81101560945760008181526020819052604090208190556001016074565b50905600a165627a7a723058205ded543feb546472be4e116e713a2d46b8dafc823ca31256e67a1be92a6752730029";
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
