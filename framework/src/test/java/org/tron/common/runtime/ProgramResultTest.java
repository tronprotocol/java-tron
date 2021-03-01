package org.tron.common.runtime;

import static org.tron.core.capsule.utils.TransactionUtil.buildTransactionInfoInstance;
import static org.tron.core.utils.TransactionUtil.generateContractAddress;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;


@Slf4j
public class ProgramResultTest {

  private static final String dbPath = "output_InternalTransactionComplexTest";
  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;
  private static Runtime runtime;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug", "--support-constant"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TRANSFER_TO = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 100000000);
    deposit.createAccount(Hex.decode(TRANSFER_TO), AccountType.Normal);
    deposit.addBalance(Hex.decode(TRANSFER_TO), 0);
    deposit.commit();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * pragma solidity ^0.4.8; contract B{ address public calledAddress; constructor (address d)
   * payable{calledAddress = d;} function setB() payable returns(address,address){ calledContract c1
   * = new calledContract(); calledContract c2 = new calledContract();
   * calledAddress.call(bytes4(keccak256("getZero()"))); return (address(c1),address(c2)); } }
   * contract A { address public calledAddress; constructor(address d) payable{ calledAddress = d;
   * }
   * <p>
   * address public b1; address public b2; address public b3; address public b4; address public b5;
   * address public b6; address public b7; address public b8;
   * <p>
   * function create(){ B b= new B(calledAddress); B bb = new B(calledAddress); b1 = address(b); b2
   * = address(bb); (b3,b4)=b.setB(); (b5,b6)=bb.setB(); (b7,b8)=bb.setB();
   * calledAddress.call(bytes4(keccak256("getZero()"))); } }
   * <p>
   * contract calledContract { function getZero() returns(uint256){ return 0; } }
   */

  @Test
  public void uniqueInternalTransactionHashTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    byte[] calledContractAddress = deployCalledContractAndGetItsAddress();
    byte[] contractAAddress = deployContractAAndGetItsAddress(calledContractAddress);
    /* =================================== CALL create() =================================== */
    byte[] triggerData1 = TvmTestUtils.parseAbi("create()", "");
    runtime = TvmTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData1, 0, 100000000, deposit, null);
    List<InternalTransaction> internalTransactionsList = runtime.getResult()
        .getInternalTransactions();
    // 15 internalTransactions in total
    Assert.assertEquals(internalTransactionsList.size(), 15);
    List<String> hashList = new ArrayList<>();
    internalTransactionsList.forEach(
        internalTransaction -> hashList.add(Hex.toHexString(internalTransaction.getHash())));
    // No dup
    List<String> dupHash = hashList.stream()
        .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b)).entrySet().stream()
        .filter(entry -> entry.getValue() > 1).map(entry -> entry.getKey())
        .collect(Collectors.toList());
    Assert.assertEquals(dupHash.size(), 0);
  }

  private byte[] deployCalledContractAndGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "calledContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\"name\":\"\""
            + ",\"type\":\"uint256\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code =
        "6080604052348015600f57600080fd5b5060988061001e6000396000f300608060405260043610603e5763fff"
            + "fffff7c01000000000000000000000000000000000000000000000000000000006000350416639f3f89"
            + "dc81146043575b600080fd5b348015604e57600080fd5b5060556067565b60408051918252519081900"
            + "360200190f35b6000905600a165627a7a72305820fa4124f68cd4c92df5362cb343d4831acd8ed666b7"
            + "2eb497974cdf511ae642a90029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);
  }

  private byte[] deployContractAAndGetItsAddress(byte[] calledContractAddress)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "calledContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":true,\"inputs\":[],\"name\":\"b2\",\"outputs\":[{\"name\":\"\""
            + ",\"type\":\"address\"}],"
            + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"}"
            + ",{\"constant\":true,\"inputs"
            + "\":[],\"name\":\"b4\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}]"
            + ",\"payable\":false,"
            + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\""
            + ":true,\"inputs\":[],\"name\":"
            + "\"b7\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\""
            + ":false,\"stateMutability\":"
            + "\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[]"
            + ",\"name\":\"b8\",\"outputs\":[{\"name\":"
            + "\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\""
            + ":\"view\",\"type\":\"function\"},{"
            + "\"constant\":true,\"inputs\":[],\"name\":\"b5\",\"outputs\""
            + ":[{\"name\":\"\",\"type\":\"address\"}],"
            + "\"payable\":false,\"stateMutability\":\"view\",\"type\""
            + ":\"function\"},{\"constant\":true,\"inputs\":[],"
            + "\"name\":\"calledAddress\",\"outputs\":[{\"name\":\"\""
            + ",\"type\":\"address\"}],\"payable\":false,\"stateMutability"
            + "\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\""
            + ":[],\"name\":\"b3\",\"outputs\":[{\"name\":\"\","
            + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\""
            + ":\"view\",\"type\":\"function\"},{\"constant\":true,"
            + "\"inputs\":[],\"name\":\"b1\",\"outputs\":[{\"name\":\"\""
            + ",\"type\":\"address\"}],\"payable\":false,\"stateMutability"
            + "\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\""
            + ":[],\"name\":\"create\",\"outputs\":[],\"payable"
            + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}"
            + ",{\"constant\":true,\"inputs\":[],\"name\":\"b6\","
            + "\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false"
            + ",\"stateMutability\":\"view\",\"type\":\"function"
            + "\"},{\"inputs\":[{\"name\":\"d\",\"type\":\"address\"}],\"payable\":true"
            + ",\"stateMutability\":\"payable\",\"type\":"
            + "\"constructor\"}]";
    String code =
        "608060405260405160208061092d833981016040525160008054600160a060020a03909216600160a060020a0"
            + "3199092169190911790556108e8806100456000396000f3006080604052600436106100a35763ffffff"
            + "ff7c01000000000000000000000000000000000000000000000000000000006000350416630add6ef28"
            + "1146100a85780633db3fb0a146100d9578063581afe62146100ee578063a3654bd814610103578063cb"
            + "2182c514610118578063dccdfbec1461012d578063e9b6190914610142578063ee49500214610157578"
            + "063efc81a8c1461016c578063f60176b214610183575b600080fd5b3480156100b457600080fd5b5061"
            + "00bd610198565b60408051600160a060020a039092168252519081900360200190f35b3480156100e55"
            + "7600080fd5b506100bd6101a7565b3480156100fa57600080fd5b506100bd6101b6565b34801561010f"
            + "57600080fd5b506100bd6101c5565b34801561012457600080fd5b506100bd6101d4565b34801561013"
            + "957600080fd5b506100bd6101e3565b34801561014e57600080fd5b506100bd6101f2565b3480156101"
            + "6357600080fd5b506100bd610201565b34801561017857600080fd5b50610181610210565b005b34801"
            + "561018f57600080fd5b506100bd6105a1565b600254600160a060020a031681565b600454600160a060"
            + "020a031681565b600754600160a060020a031681565b600854600160a060020a031681565b600554600"
            + "160a060020a031681565b600054600160a060020a031681565b600354600160a060020a031681565b60"
            + "0154600160a060020a031681565b600080548190600160a060020a03166102276105b0565b600160a06"
            + "0020a03909116815260405190819003602001906000f080158015610254573d6000803e3d6000fd5b50"
            + "600054909250600160a060020a031661026c6105b0565b600160a060020a03909116815260405190819"
            + "003602001906000f080158015610299573d6000803e3d6000fd5b5060018054600160a060020a038086"
            + "1673ffffffffffffffffffffffffffffffffffffffff199283168117909355600280549185169190921"
            + "6179055604080517fd1f4ba820000000000000000000000000000000000000000000000000000000081"
            + "528151939450919263d1f4ba829260048082019392918290030181600087803b1580156103275760008"
            + "0fd5b505af115801561033b573d6000803e3d6000fd5b505050506040513d6040811015610351576000"
            + "80fd5b5080516020909101516004805473ffffffffffffffffffffffffffffffffffffffff199081166"
            + "00160a060020a039384161782556003805490911693831693909317909255604080517fd1f4ba820000"
            + "0000000000000000000000000000000000000000000000000000815281519285169363d1f4ba8293828"
            + "20193929091908290030181600087803b1580156103e757600080fd5b505af11580156103fb573d6000"
            + "803e3d6000fd5b505050506040513d604081101561041157600080fd5b5080516020909101516006805"
            + "473ffffffffffffffffffffffffffffffffffffffff19908116600160a060020a039384161790915560"
            + "05805490911692821692909217909155604080517fd1f4ba82000000000000000000000000000000000"
            + "00000000000000000000000815281519284169263d1f4ba829260048084019391929182900301816000"
            + "87803b1580156104a957600080fd5b505af11580156104bd573d6000803e3d6000fd5b5050505060405"
            + "13d60408110156104d357600080fd5b50805160209091015160088054600160a060020a0392831673ff"
            + "ffffffffffffffffffffffffffffffffffffff199182161790915560078054938316939091169290921"
            + "790915560008054604080517f6765745a65726f28290000000000000000000000000000000000000000"
            + "0000008152815190819003600901812063ffffffff7c010000000000000000000000000000000000000"
            + "00000000000000000009182900490811690910282529151929094169390926004808301939192829003"
            + "018183875af1505050505050565b600654600160a060020a031681565b6040516102fc806105c183390"
            + "190560060806040526040516020806102fc833981016040525160008054600160a060020a0390921660"
            + "0160a060020a03199092169190911790556102b7806100456000396000f300608060405260043610610"
            + "0325763ffffffff60e060020a600035041663d1f4ba828114610037578063dccdfbec14610072575b60"
            + "0080fd5b61003f6100b0565b6040805173ffffffffffffffffffffffffffffffffffffffff938416815"
            + "291909216602082015281519081900390910190f35b34801561007e57600080fd5b506100876101aa56"
            + "5b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190f35"
            + "b6000806000806100be6101c6565b604051809103906000f0801580156100da573d6000803e3d6000fd"
            + "5b5091506100e56101c6565b604051809103906000f080158015610101573d6000803e3d6000fd5b509"
            + "0506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffff"
            + "ffffffffffffffffffffffffffffff1660405180807f6765745a65726f2829000000000000000000000"
            + "00000000000000000000000008152506009019050604051809103902060e060020a90046040518163ff"
            + "ffffff1660e060020a0281526004016000604051808303816000875af15093969295509193505050505"
            + "65b60005473ffffffffffffffffffffffffffffffffffffffff1681565b60405160b6806101d6833901"
            + "9056006080604052348015600f57600080fd5b5060988061001e6000396000f30060806040526004361"
            + "0603e5763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
            + "0416639f3f89dc81146043575b600080fd5b348015604e57600080fd5b5060556067565b60408051918"
            + "252519081900360200190f35b6000905600a165627a7a72305820fa4124f68cd4c92df5362cb343d483"
            + "1acd8ed666b72eb497974cdf511ae642a90029a165627a7a72305820c15ed08242527eded971fd722f5"
            + "7396d360cb7593e830ec0a0d4e684e118556c0029a165627a7a7230582026b1b0e7b18b3f6f69efb41b"
            + "5a0461c8c389aeadf1f75ddd2c15a4036eae08b30029"
            + Hex.toHexString(calledContractAddress);
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);
  }

  /**
   * pragma solidity ^0.4.24;
   * <p>
   * contract A{ uint256 public num = 0; constructor() public payable{} function transfer(address c,
   * bool isRevert)  payable public returns(address){ B b = (new B).value(10)();//1
   * address(b).transfer(5);//2 b.payC(c, isRevert);//3 // b.payC(c,isRevert); return address(b); }
   * function getBalance() returns(uint256){ return this.balance; } } contract B{ uint256 public num
   * = 0; function f() payable returns(bool) { return true; } constructor() public payable {}
   * function payC(address c, bool isRevert) public{ c.transfer(1);//4 if (isRevert) { revert(); } }
   * function getBalance() returns(uint256){ return this.balance; } function () payable{} }
   * <p>
   * contract C{ constructor () public payable{} function () payable{} }
   */
  @Test
  public void successAndFailResultTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    byte[] cContract = deployC("C");
    byte[] aContract = deployA("A");
    //false
    String params =
        Hex.toHexString(new DataWord(new DataWord(cContract).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000000";

    // ======================================= Test Success =======================================
    byte[] triggerData1 = TvmTestUtils.parseAbi("transfer(address,bool)", params);
    Transaction trx1 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), aContract,
            triggerData1, 0, 100000000);
    TransactionTrace traceSuccess = TvmTestUtils
        .processTransactionAndReturnTrace(trx1, deposit, null);
    runtime = traceSuccess.getRuntime();
    byte[] bContract = runtime.getResult().getHReturn();
    List<InternalTransaction> internalTransactionsList = runtime.getResult()
        .getInternalTransactions();
    Assert.assertEquals(internalTransactionsList.get(0).getValue(), 10);
    Assert.assertEquals(internalTransactionsList.get(0).getSender(), aContract);
    Assert.assertEquals(
        new DataWord(internalTransactionsList.get(0).getTransferToAddress()).getLast20Bytes(),
        new DataWord(bContract).getLast20Bytes());
    Assert.assertEquals(internalTransactionsList.get(0).getNote(), "create");
    Assert.assertEquals(internalTransactionsList.get(0).isRejected(), false);
    Assert.assertEquals(internalTransactionsList.get(1).getValue(), 5);
    Assert.assertEquals(internalTransactionsList.get(1).getSender(), aContract);
    Assert.assertEquals(
        new DataWord(internalTransactionsList.get(1).getTransferToAddress()).getLast20Bytes(),
        new DataWord(bContract).getLast20Bytes());
    Assert.assertEquals(internalTransactionsList.get(1).getNote(), "call");
    Assert.assertEquals(internalTransactionsList.get(1).isRejected(), false);
    Assert.assertEquals(internalTransactionsList.get(2).getValue(), 0);
    Assert.assertEquals(internalTransactionsList.get(2).getSender(), aContract);
    Assert.assertEquals(
        new DataWord(internalTransactionsList.get(2).getTransferToAddress()).getLast20Bytes(),
        new DataWord(bContract).getLast20Bytes());
    Assert.assertEquals(internalTransactionsList.get(2).getNote(), "call");
    Assert.assertEquals(internalTransactionsList.get(2).isRejected(), false);
    Assert.assertEquals(internalTransactionsList.get(3).getValue(), 1);
    Assert.assertEquals(new DataWord(internalTransactionsList.get(3).getSender()).getLast20Bytes(),
        new DataWord(bContract).getLast20Bytes());
    Assert.assertEquals(internalTransactionsList.get(3).getTransferToAddress(), cContract);
    Assert.assertEquals(internalTransactionsList.get(3).getNote(), "call");
    Assert.assertEquals(internalTransactionsList.get(3).isRejected(), false);
    checkTransactionInfo(traceSuccess, trx1, null, internalTransactionsList);

    // ======================================= Test Fail =======================================
    // set revert == true
    params = Hex.toHexString(new DataWord(new DataWord(cContract).getLast20Bytes()).getData())
        + "0000000000000000000000000000000000000000000000000000000000000001";
    byte[] triggerData2 = TvmTestUtils.parseAbi("transfer(address,bool)", params);
    Transaction trx2 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), aContract,
            triggerData2, 0, 100000000);
    TransactionTrace traceFailed = TvmTestUtils
        .processTransactionAndReturnTrace(trx2, deposit, null);
    runtime = traceFailed.getRuntime();

    byte[] bContract2 =
        generateContractAddress(new TransactionCapsule(trx2).getTransactionId().getBytes(), 0);
    List<InternalTransaction> internalTransactionsListFail = runtime.getResult()
        .getInternalTransactions();
    Assert.assertEquals(internalTransactionsListFail.get(0).getValue(), 10);
    Assert.assertEquals(internalTransactionsListFail.get(0).getSender(), aContract);
    Assert.assertEquals(
        new DataWord(internalTransactionsListFail.get(0).getTransferToAddress()).getLast20Bytes(),
        new DataWord(bContract2).getLast20Bytes());
    Assert.assertEquals(internalTransactionsListFail.get(0).getNote(), "create");
    Assert.assertEquals(internalTransactionsListFail.get(0).isRejected(), true);
    Assert.assertEquals(internalTransactionsListFail.get(1).getValue(), 5);
    Assert.assertEquals(internalTransactionsListFail.get(1).getSender(), aContract);
    Assert.assertEquals(
        new DataWord(internalTransactionsListFail.get(1).getTransferToAddress()).getLast20Bytes(),
        new DataWord(bContract2).getLast20Bytes());
    Assert.assertEquals(internalTransactionsListFail.get(1).getNote(), "call");
    Assert.assertEquals(internalTransactionsListFail.get(1).isRejected(), true);
    Assert.assertEquals(internalTransactionsListFail.get(2).getValue(), 0);
    Assert.assertEquals(internalTransactionsListFail.get(2).getSender(), aContract);
    Assert.assertEquals(
        new DataWord(internalTransactionsListFail.get(2).getTransferToAddress()).getLast20Bytes(),
        new DataWord(bContract2).getLast20Bytes());
    Assert.assertEquals(internalTransactionsListFail.get(2).getNote(), "call");
    Assert.assertEquals(internalTransactionsListFail.get(2).isRejected(), true);
    Assert.assertEquals(internalTransactionsListFail.get(3).getValue(), 1);
    Assert.assertEquals(
        new DataWord(internalTransactionsListFail.get(3).getSender()).getLast20Bytes(),
        new DataWord(bContract2).getLast20Bytes());
    Assert.assertEquals(internalTransactionsListFail.get(3).getTransferToAddress(), cContract);
    Assert.assertEquals(internalTransactionsListFail.get(3).getNote(), "call");
    Assert.assertEquals(internalTransactionsListFail.get(3).isRejected(), true);
    checkTransactionInfo(traceFailed, trx2, null, internalTransactionsListFail);
  }

  @Test
  public void timeOutFeeTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    byte[] cContract = deployC("D");
    byte[] aContract = deployA("B");
    //false
    String params =
        Hex.toHexString(new DataWord(new DataWord(cContract).getLast20Bytes()).getData())
            + "0000000000000000000000000000000000000000000000000000000000000000";

    // ======================================= Test Success =======================================
    byte[] triggerData1 = TvmTestUtils.parseAbi("transfer(address,bool)", params);
    Transaction trx1 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), aContract,
            triggerData1, 0, 100000000);
    TransactionTrace traceSuccess = TvmTestUtils
        .processTransactionAndReturnTrace(trx1, deposit, null);

    Assert.assertEquals(traceSuccess.getReceipt().getEnergyFee(), 12705900L);

    TransactionInfoCapsule trxInfoCapsule =
        buildTransactionInfoInstance(new TransactionCapsule(trx1), null, traceSuccess);
    Assert.assertEquals(trxInfoCapsule.getFee(), 12705900L);
    Assert.assertEquals(trxInfoCapsule.getPackingFee(), 0L);

    DynamicPropertiesStore dynamicPropertiesStore = traceSuccess.getTransactionContext()
        .getStoreFactory().getChainBaseManager().getDynamicPropertiesStore();
    dynamicPropertiesStore.saveAllowTransactionFeePool(1L);

    trxInfoCapsule =
        buildTransactionInfoInstance(new TransactionCapsule(trx1), null, traceSuccess);
    Assert.assertEquals(trxInfoCapsule.getFee(), 12705900L);
    Assert.assertEquals(trxInfoCapsule.getPackingFee(), 12705900L);


    traceSuccess.getReceipt().setResult(contractResult.OUT_OF_TIME);

    trxInfoCapsule =
        buildTransactionInfoInstance(new TransactionCapsule(trx1), null, traceSuccess);
    Assert.assertEquals(trxInfoCapsule.getFee(), 12705900L);
    Assert.assertEquals(trxInfoCapsule.getPackingFee(), 0L);


  }

  private byte[] deployC(String contractName)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
            + ":\"constructor\"},{\"payable\":true,\"stateMutability\""
            + ":\"payable\",\"type\":\"fallback\"}]";
    String code = "608060405260328060116000396000f30060806040520000a165627a7a72305820193b446e66e78"
        + "aa74e45a3201095c5af56be9ee839ab815fe492202803cb71a30029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);
  }

  private byte[] deployA(String contractName)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\""
            + ":\"\",\"type\":\"uint256\"}],\"payable\""
            + ":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}"
            + ",{\"constant\":true,\"inputs\":[],\"name\":\"num\","
            + "\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\""
            + ":false,\"stateMutability\":\"view\",\"type\":\"function"
            + "\"},{\"constant\":false,\"inputs\":[{\"name\":\"c\",\"type\""
            + ":\"address\"},{\"name\":\"isRevert\",\"type\":\"bool\"}],"
            + "\"name\":\"transfer\",\"outputs\":[{\"name\":\"\",\"type\""
            + ":\"address\"}],\"payable\":true,\"stateMutability\":\"payable"
            + "\",\"type\":\"function\"},{\"inputs\":[],\"payable\""
            + ":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code =
        "6080604052600080556103c3806100176000396000f3006080604052600436106100565763ffffffff7c01000"
            + "0000000000000000000000000000000000000000000000000000060003504166312065fe0811461005b"
            + "5780634e70b1dc1461008257806377ad25dd14610097575b600080fd5b34801561006757600080fd5b5"
            + "06100706100e6565b60408051918252519081900360200190f35b34801561008e57600080fd5b506100"
            + "706100eb565b6100bd73ffffffffffffffffffffffffffffffffffffffff6004351660243515156100f"
            + "1565b6040805173ffffffffffffffffffffffffffffffffffffffff9092168252519081900360200190"
            + "f35b303190565b60005481565b600080600a6100fe6101f7565b6040518091039082f08015801561011"
            + "9573d6000803e3d6000fd5b5060405190925073ffffffffffffffffffffffffffffffffffffffff8316"
            + "915060009060059082818181858883f1935050505015801561015d573d6000803e3d6000fd5b5060408"
            + "0517f1d1537e500000000000000000000000000000000000000000000000000000000815273ffffffff"
            + "ffffffffffffffffffffffffffffffff86811660048301528515156024830152915191831691631d153"
            + "7e59160448082019260009290919082900301818387803b1580156101d757600080fd5b505af1158015"
            + "6101eb573d6000803e3d6000fd5b50929695505050505050565b6040516101908061020883390190560"
            + "0608060405260008055610179806100176000396000f3006080604052600436106100615763ffffffff"
            + "7c010000000000000000000000000000000000000000000000000000000060003504166312065fe0811"
            + "46100635780631d1537e51461008a57806326121ff0146100bd5780634e70b1dc146100d9575b005b34"
            + "801561006f57600080fd5b506100786100ee565b60408051918252519081900360200190f35b3480156"
            + "1009657600080fd5b5061006173ffffffffffffffffffffffffffffffffffffffff6004351660243515"
            + "156100f3565b6100c5610142565b604080519115158252519081900360200190f35b3480156100e5576"
            + "00080fd5b50610078610147565b303190565b60405173ffffffffffffffffffffffffffffffffffffff"
            + "ff83169060009060019082818181858883f19350505050158015610132573d6000803e3d6000fd5b508"
            + "01561013e57600080fd5b5050565b600190565b600054815600a165627a7a72305820cbf128dce414d0"
            + "a9770665be2c1a3af3cc6a642352ee077cef1427a88fa712350029a165627a7a7230582044968955c36"
            + "0128fedfc823b2c8ab2a92ab470f3085e67c816bc507926e626e90029";

    long value = 100000;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);
  }

  /**
   * pragma solidity ^0.4.24;
   * <p>
   * contract A{ constructor () payable public{} function suicide(address toAddress) public payable{
   * selfdestruct(toAddress); } function () payable public{} }s
   */
  @Test
  public void suicideResultTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    byte[] suicideContract = deploySuicide();
    Assert.assertEquals(deposit.getAccount(suicideContract).getBalance(), 1000);
    String params = Hex
        .toHexString(new DataWord(new DataWord(TRANSFER_TO).getLast20Bytes()).getData());

    // ======================================= Test Suicide =======================================
    byte[] triggerData1 = TvmTestUtils.parseAbi("suicide(address)", params);
    Transaction trx = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(Hex.decode(OWNER_ADDRESS), suicideContract,
            triggerData1, 0, 100000000);
    TransactionTrace trace = TvmTestUtils.processTransactionAndReturnTrace(trx, deposit, null);
    runtime = trace.getRuntime();
    List<InternalTransaction> internalTransactionsList = runtime.getResult()
        .getInternalTransactions();
    Assert
        .assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(), 1000);
    Assert.assertEquals(dbManager.getAccountStore().get(suicideContract), null);
    Assert.assertEquals(internalTransactionsList.get(0).getValue(), 1000);
    Assert.assertEquals(new DataWord(internalTransactionsList.get(0).getSender()).getLast20Bytes(),
        new DataWord(suicideContract).getLast20Bytes());
    Assert.assertEquals(internalTransactionsList.get(0).getTransferToAddress(),
        Hex.decode(TRANSFER_TO));
    Assert.assertEquals(internalTransactionsList.get(0).getNote(), "suicide");
    Assert.assertEquals(internalTransactionsList.get(0).isRejected(), false);
    checkTransactionInfo(trace, trx, null, internalTransactionsList);
  }

  private byte[] deploySuicide()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "suicide";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}]"
            + ",\"name\":\"suicide\","
            + "\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
            + ":\"function\"},{\"inputs"
            + "\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\""
            + ":\"constructor\"},{\"payable\":"
            + "true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    String code =
        "6080604052608a8060116000396000f300608060405260043610603e5763ffffffff7c0100000000000000000"
            + "000000000000000000000000000000000000000600035041663dbc1f22681146040575b005b603e6004"
            + "3573ffffffffffffffffffffffffffffffffffffffff1680ff00a165627a7a72305820e382f1dabb1c5"
            + "3705abe0c3e99497025ffbf78b73c079471d8984a745b3218720029";
    long value = 1000;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);
  }

  public void checkTransactionInfo(TransactionTrace trace, Transaction trx, BlockCapsule block,
      List<InternalTransaction> internalTransactionsList) {
    TransactionInfoCapsule trxInfoCapsule =
        buildTransactionInfoInstance(new TransactionCapsule(trx), null, trace);
    List<Protocol.InternalTransaction> internalTransactionListFromProtocol = trxInfoCapsule
        .getInstance().getInternalTransactionsList();
    for (int i = 0; i < internalTransactionListFromProtocol.size(); i++) {
      Assert.assertEquals(internalTransactionListFromProtocol.get(i).getRejected(),
          internalTransactionsList.get(i).isRejected());
      Assert
          .assertEquals(internalTransactionListFromProtocol.get(i).getCallerAddress().toByteArray(),
              internalTransactionsList.get(i).getSender());
      Assert.assertEquals(internalTransactionListFromProtocol.get(i).getHash().toByteArray(),
          internalTransactionsList.get(i).getHash());
      Assert.assertEquals(
          new String(internalTransactionListFromProtocol.get(i).getNote().toByteArray()),
          internalTransactionsList.get(i).getNote());
      Assert.assertEquals(
          internalTransactionListFromProtocol.get(i).getTransferToAddress().toByteArray(),
          internalTransactionsList.get(i).getTransferToAddress());
      List<Protocol.InternalTransaction.CallValueInfo> callValueInfoListFromProtocol =
          internalTransactionListFromProtocol
              .get(i).getCallValueInfoList();
      // for now only one callValue. will be change to list in future.
      Assert.assertEquals(callValueInfoListFromProtocol.get(0).getCallValue(),
          internalTransactionsList.get(i).getValue());
    }
  }

}
