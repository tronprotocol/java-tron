package org.tron.common.runtime;

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
import org.tron.common.runtime.vm.program.InternalTransaction;
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
public class InternalTransactionUniqueHashTest {

  private static Runtime runtime;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;
  private static final String dbPath = "output_InternalTransactionComplexTest";
  private static final String OWNER_ADDRESS;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug", "--support-constant"},
        Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
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
  }

  /**
   * pragma solidity ^0.4.8;
   * contract B{
   *     address public calledAddress;
   *     constructor (address d) payable{calledAddress = d;}
   *     function setB() payable returns(address,address){
   *         calledContract c1 = new calledContract();
   *         calledContract c2 = new calledContract();
   *         calledAddress.call(bytes4(keccak256("getZero()")));
   *         return (address(c1),address(c2));
   *     }
   * }
   * contract A {
   *     address public calledAddress;
   *     constructor(address d) payable{
   *         calledAddress = d;
   *     }
   *
   *     address public b1;
   *     address public b2;
   *     address public b3;
   *     address public b4;
   *     address public b5;
   *     address public b6;
   *     address public b7;
   *     address public b8;
   *
   *     function create(){
   *         B b= new B(calledAddress);
   *         B bb = new B(calledAddress);
   *         b1 = address(b);
   *         b2 = address(bb);
   *         (b3,b4)=b.setB();
   *         (b5,b6)=bb.setB();
   *         (b7,b8)=bb.setB();
   *         calledAddress.call(bytes4(keccak256("getZero()")));
   *     }
   * }
   *
   * contract calledContract {
   *     function getZero() returns(uint256){
   *         return 0;
   *     }
   * }
   */

  @Test
  public void uniqueHashTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    byte[] calledContractAddress = deployCalledContractAndGetItsAddress();
    byte[] contractAAddress = deployContractAAndGetItsAddress(calledContractAddress);
    /* =================================== CALL create() =================================== */
    byte[] triggerData1 = TVMTestUtils.parseABI("create()", "");
    runtime = TVMTestUtils
        .triggerContractWholeProcessReturnContractAddress(Hex.decode(OWNER_ADDRESS),
            contractAAddress, triggerData1,
            0, 100000000, deposit, null);
    List<InternalTransaction> internalTransactionsList = runtime.getResult().getInternalTransactions();
    // 15 internalTransactions in total
    Assert.assertEquals(internalTransactionsList.size(),15);
    List<String> hashList = new ArrayList<>();
    internalTransactionsList.forEach(internalTransaction ->hashList.add(Hex.toHexString(internalTransaction.getHash())));
    List<String> dupHash = hashList.stream().collect(Collectors.toMap(e -> e, e->1 , (a,b) -> a+b)).
        entrySet().stream().filter(entry->entry.getValue() > 1).map(entry -> entry.getKey()).collect(Collectors.toList());
    Assert.assertEquals(dupHash.size(),0);
  }

  private byte[] deployCalledContractAndGetItsAddress()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "calledContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[],\"name\":\"getZero\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],"
            + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "6080604052348015600f57600080fd5b5060988061001e6000396000f300608060405260043610603e5763ffffffff7c01"
        + "000000000000000000000000000000000000000000000000000000006000350416639f3f89dc81146043575b600080fd5b34801560"
        + "4e57600080fd5b5060556067565b60408051918252519081900360200190f35b6000905600a165627a7a72305820fa4124f68cd4c9"
        + "2df5362cb343d4831acd8ed666b72eb497974cdf511ae642a90029";
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null,
            deposit, null);
  }

  private byte[] deployContractAAndGetItsAddress(byte[] calledContractAddress) throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "calledContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":true,\"inputs\":[],\"name\":\"b2\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],"
            + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs"
            + "\":[],\"name\":\"b4\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,"
            + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
            + "\"b7\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":"
            + "\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"b8\",\"outputs\":[{\"name\":"
            + "\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{"
            + "\"constant\":true,\"inputs\":[],\"name\":\"b5\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],"
            + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],"
            + "\"name\":\"calledAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability"
            + "\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"b3\",\"outputs\":[{\"name\":\"\","
            + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,"
            + "\"inputs\":[],\"name\":\"b1\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability"
            + "\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"create\",\"outputs\":[],\"payable"
            + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"b6\","
            + "\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function"
            + "\"},{\"inputs\":[{\"name\":\"d\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":"
            + "\"constructor\"}]";
    String code =
        "608060405260405160208061092d833981016040525160008054600160a060020a03909216600160a060020a03199092169190911790556108e88061004"
            + "56000396000f3006080604052600436106100a35763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035"
            + "0416630add6ef281146100a85780633db3fb0a146100d9578063581afe62146100ee578063a3654bd814610103578063cb2182c51461011857806"
            + "3dccdfbec1461012d578063e9b6190914610142578063ee49500214610157578063efc81a8c1461016c578063f60176b214610183575b600080fd"
            + "5b3480156100b457600080fd5b506100bd610198565b60408051600160a060020a039092168252519081900360200190f35b3480156100e557600"
            + "080fd5b506100bd6101a7565b3480156100fa57600080fd5b506100bd6101b6565b34801561010f57600080fd5b506100bd6101c5565b34801561"
            + "012457600080fd5b506100bd6101d4565b34801561013957600080fd5b506100bd6101e3565b34801561014e57600080fd5b506100bd6101f2565"
            + "b34801561016357600080fd5b506100bd610201565b34801561017857600080fd5b50610181610210565b005b34801561018f57600080fd5b5061"
            + "00bd6105a1565b600254600160a060020a031681565b600454600160a060020a031681565b600754600160a060020a031681565b600854600160a"
            + "060020a031681565b600554600160a060020a031681565b600054600160a060020a031681565b600354600160a060020a031681565b6001546001"
            + "60a060020a031681565b600080548190600160a060020a03166102276105b0565b600160a060020a0390911681526040519081900360200190600"
            + "0f080158015610254573d6000803e3d6000fd5b50600054909250600160a060020a031661026c6105b0565b600160a060020a0390911681526040"
            + "5190819003602001906000f080158015610299573d6000803e3d6000fd5b5060018054600160a060020a0380861673fffffffffffffffffffffff"
            + "fffffffffffffffff1992831681179093556002805491851691909216179055604080517fd1f4ba82000000000000000000000000000000000000"
            + "0000000000000000000081528151939450919263d1f4ba829260048082019392918290030181600087803b15801561032757600080fd5b505af11"
            + "5801561033b573d6000803e3d6000fd5b505050506040513d604081101561035157600080fd5b5080516020909101516004805473ffffffffffff"
            + "ffffffffffffffffffffffffffff19908116600160a060020a039384161782556003805490911693831693909317909255604080517fd1f4ba820"
            + "0000000000000000000000000000000000000000000000000000000815281519285169363d1f4ba82938282019392909190829003018160008780"
            + "3b1580156103e757600080fd5b505af11580156103fb573d6000803e3d6000fd5b505050506040513d604081101561041157600080fd5b5080516"
            + "020909101516006805473ffffffffffffffffffffffffffffffffffffffff19908116600160a060020a0393841617909155600580549091169282"
            + "1692909217909155604080517fd1f4ba8200000000000000000000000000000000000000000000000000000000815281519284169263d1f4ba829"
            + "26004808401939192918290030181600087803b1580156104a957600080fd5b505af11580156104bd573d6000803e3d6000fd5b50505050604051"
            + "3d60408110156104d357600080fd5b50805160209091015160088054600160a060020a0392831673fffffffffffffffffffffffffffffffffffff"
            + "fff199182161790915560078054938316939091169290921790915560008054604080517f6765745a65726f282900000000000000000000000000"
            + "000000000000000000008152815190819003600901812063ffffffff7c01000000000000000000000000000000000000000000000000000000009"
            + "182900490811690910282529151929094169390926004808301939192829003018183875af1505050505050565b600654600160a060020a031681"
            + "565b6040516102fc806105c183390190560060806040526040516020806102fc833981016040525160008054600160a060020a03909216600160a"
            + "060020a03199092169190911790556102b7806100456000396000f3006080604052600436106100325763ffffffff60e060020a600035041663d1"
            + "f4ba828114610037578063dccdfbec14610072575b600080fd5b61003f6100b0565b6040805173fffffffffffffffffffffffffffffffffffffff"
            + "f938416815291909216602082015281519081900390910190f35b34801561007e57600080fd5b506100876101aa565b6040805173ffffffffffff"
            + "ffffffffffffffffffffffffffff9092168252519081900360200190f35b6000806000806100be6101c6565b604051809103906000f0801580156"
            + "100da573d6000803e3d6000fd5b5091506100e56101c6565b604051809103906000f080158015610101573d6000803e3d6000fd5b509050600080"
            + "9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1660405180807"
            + "f6765745a65726f282900000000000000000000000000000000000000000000008152506009019050604051809103902060e060020a9004604051"
            + "8163ffffffff1660e060020a0281526004016000604051808303816000875af1509396929550919350505050565b60005473fffffffffffffffff"
            + "fffffffffffffffffffffff1681565b60405160b6806101d68339019056006080604052348015600f57600080fd5b5060988061001e6000396000"
            + "f300608060405260043610603e5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416639f3f89d"
            + "c81146043575b600080fd5b348015604e57600080fd5b5060556067565b60408051918252519081900360200190f35b6000905600a165627a7a72"
            + "305820fa4124f68cd4c92df5362cb343d4831acd8ed666b72eb497974cdf511ae642a90029a165627a7a72305820c15ed08242527eded971fd722"
            + "f57396d360cb7593e830ec0a0d4e684e118556c0029a165627a7a7230582026b1b0e7b18b3f6f69efb41b5a0461c8c389aeadf1f75ddd2c15a403"
            + "6eae08b30029" + Hex.toHexString(calledContractAddress);
    long value = 0;
    long feeLimit = 1000000000;
    long consumeUserResourcePercent = 0;

    return TVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null,
            deposit, null);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

}
