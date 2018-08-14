package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.tron.common.runtime.Runtime;
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
public class InternalTransactionCallTest {
  private static Runtime runtime;
  private static Manager dbManager;
  private static AnnotationConfigApplicationContext context;
  private static DepositImpl deposit;
  private static final String dbPath = "output_InternalTransactionCallTest";
  private static final String OWNER_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
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
   * contract A {
   *   uint256 public number;
   *   address public sender;
   *   function callTest(address bAddress, uint256 _number) {
   *     bAddress.call(bytes4(sha3("setValue(uint256)")), _number); // B's storage is set, A is not modified
   *   }
   *
   *   function callcodeTest(address bAddress, uint256 _number) {
   *     bAddress.callcode(bytes4(sha3("setValue(uint256)")), _number); // A's storage is set, B is not modified
   *   }
   *
   *   function delegatecallTest(address bAddress, uint256 _number) {
   *     bAddress.delegatecall(bytes4(sha3("setValue(uint256)")), _number); // A's storage is set, B is not modified
   *   }
   * }
   *
   * contract B {
   *   uint256 public number;
   *   address public sender;
   *
   *   function setValue(uint256 _number) {
   *     number = _number;
   *     sender = msg.sender;
   *     // msg.sender is A if invoked by A's callTest. B's storage will be updated
   *     // msg.sender is A if invoked by A's callcodeTest. None of B's storage is updated
   *     // msg.sender is OWNER if invoked by A's delegatecallTest. None of B's storage is updated
   *
   *
   *     // the value of "this" is A, when invoked by either A's callcodeSetN or C.foo()
   *   }
   * }
   */



  @Test
  public void callTest()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress =deployAContractandGetItsAddress();

    /* =================================== CALL callTest to change B storage =================================== */
    String params = Hex.toHexString(new DataWord(contractBAddress).getData()) + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = TVMTestUtils.parseABI("callTest()",params);
    TVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),contractAAddress,triggerData,
        0,1000000,deposit,null);

    /* =================================== CALL number() to check A's number =================================== */
    byte[] triggerData2 = TVMTestUtils.parseABI("number()","");
    runtime = TVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),contractAAddress,triggerData2,
        0,1000000,deposit,null);
    // A should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),"0000000000000000000000000000000000000000000000000000000000000000");

    /* =================================== CALL number() to check B's number =================================== */
    byte[] triggerData3 = TVMTestUtils.parseABI("number()","");
    runtime = TVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),contractBAddress,triggerData3,
        0,1000000,deposit,null);
    // A should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),"0000000000000000000000000000000000000000000000000000000000000003");


    /* =================================== CALL number() to check B's sender =================================== */
    byte[] triggerData4 = TVMTestUtils.parseABI("sender()","");
    runtime = TVMTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),contractBAddress,triggerData4,
        0,1000000,deposit,null);
    // A should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),Hex.toHexString(new DataWord(new DataWord(contractAAddress).getLast20Bytes()).getData()));
  }

  @Test
  public void delegateCallTest(){

  }

  @Test
  public void callCodeTest(){

  }

  @Test
  public void staticCallTest(){

  }


  // Just for the AB example above
  public byte[] deployAContractandGetItsAddress()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {
    String contractName = "AContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":"
        + "\"uint256\"}],\"name\":\"delegatecallTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":"
        + "\"uint256\"}],\"name\":\"callTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[],\"name\":\"sender\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"number\",\"outputs\":[{\"name\":"
        + "\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\""
        + ":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\"_number\",\"type\":\"uint256\"}],\"name\":\"callcodeTest\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b506102d4806100206000396000f3006080604052600436106100535763ffffffff"
        + "60e060020a6000350416633da5d187811461005857806343c3a43a1461007e57806367e404ce146100a25780638381f58a146100d357806"
        + "3d7d21f5b146100fa575b600080fd5b34801561006457600080fd5b5061007c600160a060020a036004351660243561011e565b005b3480"
        + "1561008a57600080fd5b5061007c600160a060020a0360043516602435610199565b3480156100ae57600080fd5b506100b7610216565b6"
        + "0408051600160a060020a039092168252519081900360200190f35b3480156100df57600080fd5b506100e8610225565b6040805191825251"
        + "9081900360200190f35b34801561010657600080fd5b5061007c600160a060020a036004351660243561022b565b81600160a060020a03166"
        + "0405180807f73657456616c75652875696e74323536290000000000000000000000000000008152506011019050604051809103902060e060"
        + "020a9004826040518263ffffffff1660e060020a02815260040180828152602001915050600060405180830381865af4505050505050565b81"
        + "600160a060020a031660405180807f73657456616c75652875696e743235362900000000000000000000000000000081525060110190506040"
        + "51809103902060e060020a9004826040518263ffffffff1660e060020a02815260040180828152602001915050600060405180830381600087"
        + "5af1505050505050565b600154600160a060020a031681565b60005481565b81600160a060020a031660405180807f73657456616c75652875"
        + "696e74323536290000000000000000000000000000008152506011019050604051809103902060e060020a9004826040518263ffffffff1660e"
        + "060020a028152600401808281526020019150506000604051808303816000875af25050505050505600a165627a7a7230582067259fcc4aa6b9"
        + "f5995d34f7ee832cd11bf1930a9610a05643dc94243e1cfc2f0029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;
    String libraryAddressPair=null;

    byte[] contractAddress = TVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName,address,ABI,code,value,feeLimit,consumeUserResourcePercent,libraryAddressPair,
            deposit,null);
    return contractAddress;
  }

  // Just for the AB example above
  public byte[] deployBContractAndGetItsAddress()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {
    String contractName = "BContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"_number\",\"type\":\"uint256\"}],\"name\":"
        + "\"setValue\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[],\"name\":\"sender\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"number\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b5061014c806100206000396000f3006080604052600436106100565763ffffffff"
        + "7c010000000000000000000000000000000000000000000000000000000060003504166355241077811461005b57806367e404ce1461"
        + "00755780638381f58a146100b3575b600080fd5b34801561006757600080fd5b506100736004356100da565b005b34801561008157600"
        + "080fd5b5061008a6100fe565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35b348"
        + "0156100bf57600080fd5b506100c861011a565b60408051918252519081900360200190f35b6000556001805473fffffffffffffffffff"
        + "fffffffffffffffffffff191633179055565b60015473ffffffffffffffffffffffffffffffffffffffff1681565b600054815600a16562"
        + "7a7a72305820212c8bac78a209af736d0ea64104c0bb21efc4c7e6d536d07a780d4179613b820029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;
    String libraryAddressPair=null;

    byte[] contractAddress = TVMTestUtils.deployContractWholeProcessReturnContractAddress(contractName,address,ABI,code,value,feeLimit,consumeUserResourcePercent,libraryAddressPair,
        deposit,null);
    return contractAddress;
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
