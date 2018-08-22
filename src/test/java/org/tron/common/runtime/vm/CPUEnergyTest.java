package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
  private String dbPath = "output_CPUGasTest";
  private String OWNER_ADDRESS;


  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 100000000);
  }

  // solidity for endlessLoopTest
  // pragma solidity ^0.4.0;
  //
  // contract TestForEndlessLoop {
  //
  //   uint256 vote;
  //   constructor () public {
  //     vote = 0;
  //   }
  //
  //   function getVote() public constant returns (uint256 _vote) {
  //     _vote = vote;
  //   }
  //
  //   function setVote(uint256 _vote) public {
  //     vote = _vote;
  //     while(true)
  //     {
  //       vote += 1;
  //     }
  //   }
  // }

  @Test
  public void gasFunctionTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

    long value = 0;
    long feeLimit = 1000000000; // sun
    long consumeUserResourcePercent = 100;
    TVMTestResult result = deployGasFunctionTestContract(value, feeLimit,
        consumeUserResourcePercent);
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 52457);
    byte[] contractAddress = result.getContractAddress();
    //
    // /* =================================== CALL setVote(uint256) =================================== */
    // String params = "0000000000000000000000000000000000000000000000000000000000000003";
    // byte[] triggerData = TVMTestUtils.parseABI("setVote(uint256)", params);
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

  public TVMTestResult deployGasFunctionTestContract(long value, long feeLimit,
      long consumeUserResourcePercent)
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {
    String contractName = "TestForGasFunction";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"complexCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"simpleCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405261000c61004e565b604051809103906000f080158015610028573d6000803e3d6000fd5b5060008054600160a060020a031916600160a060020a039290921691909117905561005d565b60405160db8061020b83390190565b61019f8061006c6000396000f3006080604052600436106100325763ffffffff60e060020a60003504166306ce93af811461003757806340de221c1461004e575b600080fd5b34801561004357600080fd5b5061004c610063565b005b34801561005a57600080fd5b5061004c610103565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cd95478c600a6003906040518363ffffffff1660e060020a0281526004016020604051808303818589803b1580156100d357600080fd5b5088f11580156100e7573d6000803e3d6000fd5b5050505050506040513d60208110156100ff57600080fd5b5050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166388b2c1df600a6003906040518363ffffffff1660e060020a0281526004016020604051808303818589803b1580156100d357600080fd00a165627a7a7230582082e2b19657bf96b8ec2a95e51775c519fc54300099f3c5406adac1a9c0ba23b80029608060405234801561001057600080fd5b5060bc8061001f6000396000f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166388b2c1df8114604d578063cd95478c146065575b600080fd5b6053606b565b60408051918252519081900360200190f35b60536070565b602a90565b600080805b612710821015608b57506001810190600a026075565b5050905600a165627a7a723058209cd23f669c1016dcfc6d0f557814f6c77dfdccb9db739dbfa8001fcfaf6e03880029";
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
