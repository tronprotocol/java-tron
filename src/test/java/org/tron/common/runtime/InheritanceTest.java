package org.tron.common.runtime;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
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
public class InheritanceTest {
  private static Runtime runtime;
  private static Manager dbManager;
  private static AnnotationConfigApplicationContext context;
  private static DepositImpl deposit;
  private static final String dbPath = "output_InheritanceTest";
  private static final String OWNER_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath,"--debug"}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS),AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS),100000000);
  }



  /**
   * pragma solidity ^0.4.19;
   *
   * contract foo {
   *     uint256 public id=10;
   *     function getNumber()  returns (uint256){return 100;}
   *     function getName()  returns (string){
   *         return "foo";
   *     }
   * }
   *
   * contract bar is foo {
   *     function getName()  returns (string) { return "bar"; }
   *     function getId() returns(uint256){return id;}
   * }
   */
  @Test
  public void inheritanceTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {
    String contractName = "barContract";
    byte[] callerAddress = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[],\"name\":\"getName\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],"
        + "\"name\":\"getId\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"id\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"getNumber\","
        + "\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052600a60005534801561001557600080fd5b506101f9806100256000396000f300608060405260043610610062576000357c01"
        + "00000000000000000000000000000000000000000000000000000000900463ffffffff16806317d7de7c146100675780635d1ca631146100f757806"
        + "3af640d0f14610122578063f2c9ecd81461014d575b600080fd5b34801561007357600080fd5b5061007c610178565b6040518080602001828103825"
        + "283818151815260200191508051906020019080838360005b838110156100bc5780820151818401526020810190506100a1565b505050509050908101"
        + "90601f1680156100e95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561010357600080fd"
        + "5b5061010c6101b5565b6040518082815260200191505060405180910390f35b34801561012e57600080fd5b506101376101be565b60405180828152602"
        + "00191505060405180910390f35b34801561015957600080fd5b506101626101c4565b6040518082815260200191505060405180910390f35b6060604080"
        + "5190810160405280600381526020017f6261720000000000000000000000000000000000000000000000000000000000815250905090565b60008054905"
        + "090565b60005481565b600060649050905600a165627a7a72305820dfe79cf7f4a8a342b754cad8895b13f85de7daa11803925cf392263397653e7f0029";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    byte[] contractAddress = TVMTestUtils.deployContractWholeProcessReturnContractAddress(
        contractName,callerAddress,ABI,code,value,fee,consumeUserResourcePercent,null,deposit,null);


    /* =================================== CALL getName() return child value =================================== */
    byte[] triggerData1 = TVMTestUtils.parseABI("getName()","");
    runtime = TVMTestUtils.triggerContractWholeProcessReturnContractAddress(callerAddress,contractAddress,triggerData1,
        0,1000000,deposit,null);

    //0x20 => pointer position, 0x3 => size,  626172 => "bar"
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000003"
        + "6261720000000000000000000000000000000000000000000000000000000000");

    /* =================================== CALL getNumber() return parent value=================================== */
    byte[] triggerData2 = TVMTestUtils.parseABI("getNumber()","");
    runtime = TVMTestUtils.triggerContractWholeProcessReturnContractAddress(callerAddress,contractAddress,triggerData2,
        0,1000000,deposit,null);

    //0x64 =>100
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000064");

    /* =================================== CALL getId() call child function return parent field value=================================== */
    byte[] triggerData3 = TVMTestUtils.parseABI("getId()","");
    runtime = TVMTestUtils.triggerContractWholeProcessReturnContractAddress(callerAddress,contractAddress,triggerData3,
        0,1000000,deposit,null);

    //0x64 =>100
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "000000000000000000000000000000000000000000000000000000000000000a");
  }


  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
    context.destroy();
  }

}
