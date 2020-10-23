package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.Runtime;
import org.tron.common.runtime.TvmTestUtils;
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
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Protocol.AccountType;

@Slf4j
public class InternalTransactionCallTest {

  private Runtime runtime;
  private Manager dbManager;
  private TronApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_InternalTransactionCallTest";
  private String OWNER_ADDRESS;
  private Application AppT;

  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.clearParam();
    Args.setParam(new String[]{"--output-directory", dbPath, "--support-constant", "--debug"},
        Constant.TEST_CONF);

    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 100000000);
  }


  /**
   * contract A { uint256 public numberForB; address public senderForB; function callTest(address
   * bAddress, uint256 _number) { bAddress.call(bytes4(sha3("setValue(uint256)")), _number); // B's
   * storage is set, A is not modified }
   *
   * function callcodeTest(address bAddress, uint256 _number)
   * { bAddress.callcode(bytes4(sha3("setValue(uint256)")),
   * _number); // A's storage is set, B is not modified }
   *
   * function delegatecallTest(address bAddress, uint256 _number)
   * { bAddress.delegatecall(bytes4(sha3("setValue(uint256)")),
   * _number); // A's storage is set, B is not modified } }
   *
   * contract B { uint256 public numberForB; address public senderForB;
   *
   * function setValue(uint256 _number) { numberForB = _number; senderForB = msg.sender; //
   * senderForB is A if invoked by A's callTest. B's storage will be updated // senderForB is A if
   * invoked by A's callcodeTest. None of B's storage is updated // senderForB is OWNER if invoked
   * by A's delegatecallTest. None of B's storage is updated } }
   */


  /*
      A call B, anything belongs to A should not be changed, B should be changed.
      msg.sender for contractB should be A's address.
   */
  @Test
  public void callTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress = deployAContractandGetItsAddress();

    /* ============ CALL callTest() to change B storage =================================== */
    String params =
        Hex.toHexString(new DataWord(new DataWord(contractBAddress).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = TvmTestUtils.parseAbi("callTest(address,uint256)", params);
    TvmTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
        contractAAddress, triggerData, 0, 1000000000, deposit, null);

    /* =========== CALL numberForB() to check A's numberForB =================================== */
    byte[] triggerData2 = TvmTestUtils.parseAbi("numberForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData2, 0, 1000000000, deposit, null);
    // A should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* =============== CALL senderForB() to check A's senderForB ========================= */
    byte[] triggerData3 = TvmTestUtils.parseAbi("senderForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData3, 0, 1000000000, deposit, null);
    // A should be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* ========= CALL numberForB() to check B's numberForB =================================== */
    byte[] triggerData4 = TvmTestUtils.parseAbi("numberForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData4, 0, 1000000000, deposit, null);
    // B's numberForB should be changed to 3
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000003");

    /* =========== CALL senderForB() to check B's senderForB =================================== */
    byte[] triggerData5 = TvmTestUtils.parseAbi("senderForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData5, 0, 1000000000, deposit, null);
    // B 's senderForB should be A
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        Hex.toHexString(new DataWord(new DataWord(contractAAddress).getLast20Bytes()).getData()));
  }

  /*
     A delegatecall B, A should be changed, anything belongs to B should not be changed.
     msg.sender for contractB should be Caller(OWNER_ADDRESS), but this value will not be
      effected in B's senderForB since we use delegatecall.
     We store it in A's senderForB.
   */
  @Test
  public void delegateCallTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress = deployAContractandGetItsAddress();
    /* ========= CALL delegatecallTest() to change B storage =================================== */
    String params =
        Hex.toHexString(new DataWord(new DataWord(contractBAddress).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = TvmTestUtils.parseAbi("delegatecallTest(address,uint256)",
        params);
    TvmTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
        contractAAddress, triggerData, 0, 1000000000, deposit, null);

    /* ============ CALL numberForB() to check A's numberForB =================================== */
    byte[] triggerData2 = TvmTestUtils.parseAbi("numberForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData2, 0, 1000000000, deposit, null);
    // A should be changed to 3
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000003");

    /* ============= CALL senderForB() to check A's senderForB ====================== */
    byte[] triggerData3 = TvmTestUtils.parseAbi("senderForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData3, 0, 1000000000, deposit, null);
    // A's senderForB should be changed to caller's contract Address (OWNER_ADDRESS)
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        Hex.toHexString(new DataWord(new DataWord(OWNER_ADDRESS).getLast20Bytes()).getData()));

    /* =========== CALL numberForB() to check B's numberForB =================================== */
    byte[] triggerData4 = TvmTestUtils.parseAbi("numberForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData4, 0, 1000000000, deposit, null);
    // B's numberForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* ======= CALL senderForB() to check B's senderForB =================================== */
    byte[] triggerData5 = TvmTestUtils.parseAbi("senderForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData5, 0, 1000000000, deposit, null);
    // B 's senderForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

  }

  /*
     A callcode B, A should be changed, anything belongs to B should not be changed.
     msg.sender for contractB should be A,
     but this value will not be effected in B's senderForB since we use callcode.
     We store it in A's senderForB.
   */
  @Test
  public void callCodeTest()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    byte[] contractBAddress = deployBContractAndGetItsAddress();
    byte[] contractAAddress = deployAContractandGetItsAddress();
    /* ================ CALL callcodeTest() to change B storage ======================== */
    String params =
        Hex.toHexString(new DataWord(new DataWord(contractBAddress).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000003";
    byte[] triggerData = TvmTestUtils.parseAbi("callcodeTest(address,uint256)", params);
    TvmTestUtils.triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
        contractAAddress, triggerData, 0, 1000000000, deposit, null);

    /* ========== CALL numberForB() to check A's numberForB =================================== */
    byte[] triggerData2 = TvmTestUtils.parseAbi("numberForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData2, 0, 1000000000, deposit, null);
    // A should be changed to 3
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000003");

    /* =========== CALL senderForB() to check A's senderForB =================================== */
    byte[] triggerData3 = TvmTestUtils.parseAbi("senderForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData3, 0, 1000000000, deposit, null);
    // A's senderForB should be changed to A's contract Address
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        Hex.toHexString(new DataWord(new DataWord(contractAAddress).getLast20Bytes()).getData()));

    /* ======= CALL numberForB() to check B's numberForB =================================== */
    byte[] triggerData4 = TvmTestUtils.parseAbi("numberForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData4, 0, 1000000000, deposit, null);
    // B's numberForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");

    /* ========= CALL senderForB() to check B's senderForB =================================== */
    byte[] triggerData5 = TvmTestUtils.parseAbi("senderForB()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractBAddress, triggerData5, 0, 1000000000, deposit, null);
    // B 's senderForB should not be changed
    Assert.assertEquals(Hex.toHexString(runtime.getResult().getHReturn()),
        "0000000000000000000000000000000000000000000000000000000000000000");
  }

  @Test
  public void staticCallTest() {
    //TODO: need to implement this
  }


  // Just for the AB example above
  public byte[] deployAContractandGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "AContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\""
            + ":\"_number\",\"type\":\"uint256\"}],"
            + "\"name\":\"delegatecallTest\",\"outputs\":[],\"payable\":false,\"stateMutability\""
            + ":\"nonpayable\",\"type\":\"function\"},{\"constant"
            + "\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\"address\"},{\"name\":\""
            + "_number\",\"type\":\"uint256\"}],\"name\":\"callTest\","
            + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\""
            + "function\"},{\"constant\":true,\"inputs\":[],\"name\":"
            + "\"senderForB\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":"
            + "false,\"stateMutability\":\"view\",\"type\":"
            + "\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"bAddress\",\"type\":\""
            + "address\"},{\"name\":\"_number\",\"type\":\"uint256\"}],"
            + "\"name\":\"callcodeTest\",\"outputs\":[],\"payable\":false,\"stateMutability\""
            + ":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,"
            + "\"inputs\":[],\"name\":\"numberForB\",\"outputs\":[{\"name\":\"\",\"type\":\""
            + "uint256\"}],\"payable\":false,\"stateMutability\":\"view\","
            + "\"type\":\"function\"}]";
    String code =
        "608060405234801561001057600080fd5b506102d4806100206000396000f3006080604052600436106100535"
            + "763ffffffff60e060020a6000350416633da5d187811461005857806343c3a43a1461007e578063b053"
            + "ebd4146100a2578063d7d21f5b146100d3578063dd92afef146100f7575b600080fd5b3480156100645"
            + "7600080fd5b5061007c600160a060020a036004351660243561011e565b005b34801561008a57600080"
            + "fd5b5061007c600160a060020a0360043516602435610199565b3480156100ae57600080fd5b506100b"
            + "7610216565b60408051600160a060020a039092168252519081900360200190f35b3480156100df5760"
            + "0080fd5b5061007c600160a060020a0360043516602435610225565b34801561010357600080fd5b506"
            + "1010c6102a2565b60408051918252519081900360200190f35b81600160a060020a031660405180807f"
            + "73657456616c75652875696e74323536290000000000000000000000000000008152506011019050604"
            + "051809103902060e060020a9004826040518263ffffffff1660e060020a028152600401808281526020"
            + "01915050600060405180830381865af4505050505050565b81600160a060020a031660405180807f736"
            + "57456616c75652875696e74323536290000000000000000000000000000008152506011019050604051"
            + "809103902060e060020a9004826040518263ffffffff1660e060020a028152600401808281526020019"
            + "150506000604051808303816000875af1505050505050565b600154600160a060020a031681565b8160"
            + "0160a060020a031660405180807f73657456616c75652875696e7432353629000000000000000000000"
            + "0000000008152506011019050604051809103902060e060020a9004826040518263ffffffff1660e060"
            + "020a028152600401808281526020019150506000604051808303816000875af2505050505050565b600"
            + "054815600a165627a7a723058206d36ef7c6f6d387ad915f299e715c9b360f3719843a1113badb28b65"
            + "95e66c1e0029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;
    String libraryAddressPair = null;

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, deposit, null);
    return contractAddress;
  }

  // Just for the AB example above
  public byte[] deployBContractAndGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "BContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"_number\",\"type\":\"uint256\"}],"
        + "\"name\":\"setValue\","
        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\""
        + ":\"function\"},{\"constant\":true,"
        + "\"inputs\":[],\"name\":\"senderForB\",\"outputs\":[{\"name\":\"\",\"type\""
        + ":\"address\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\""
        + ":[],\"name\":\"numberForB\","
        + "\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\""
        + "stateMutability\":\"view\",\"type\":" + "\"function\"}]";
    String code =
        "608060405234801561001057600080fd5b5061014c806100206000396000f3006080604052600436106100565"
            + "763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663"
            + "55241077811461005b578063b053ebd414610075578063dd92afef146100b3575b600080fd5b3480156"
            + "1006757600080fd5b506100736004356100da565b005b34801561008157600080fd5b5061008a6100fe"
            + "565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f"
            + "35b3480156100bf57600080fd5b506100c861011a565b60408051918252519081900360200190f35b60"
            + "00556001805473ffffffffffffffffffffffffffffffffffffffff191633179055565b60015473fffff"
            + "fffffffffffffffffffffffffffffffffff1681565b600054815600a165627a7a72305820e2c513cf46"
            + "bb32018879ec48f8fe264c985b6d2c7a853a578f4f56583fe1ffb80029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;
    String libraryAddressPair = null;

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, deposit, null);
    return contractAddress;
  }


  /**
   * Release resources.
   */
  @After
  public void destroy() {
    context.destroy();
    Args.clearParam();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.warn("Release resources failure.");
    }

  }
}
