package org.tron.common.runtime;

import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
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
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.DataWord;

@Slf4j
public class RuntimeTransferComplexTest {

  private static Runtime runtime;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;
  private static final String dbPath = "output_RuntimeTransferComplexTest";
  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
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
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 1000000000);
    deposit.createAccount(Hex.decode(TRANSFER_TO), AccountType.Normal);
    deposit.addBalance(Hex.decode(TRANSFER_TO), 10);
    deposit.commit();
  }


  /**
   * Test constructor Transfer pragma solidity ^0.4.16; contract transferWhenDeploy { constructor ()
   * payable{} }
   */
  @Test
  public void TransferTrxToContractAccountWhenDeployAContract()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405260358060116000396000f3006080604052600080fd00a165627a7a72305820d3b0de5bdc00ebe85619d50b72b29d30bd00dd233e8849402671979de0e9e73b0029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    Transaction trx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null);
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(contractAddress).getBalance(), 100);
    recoverDeposit();
  }

  /**
   * Test constructor Transfer pragma solidity ^0.4.16; contract transferWhenDeploy { constructor ()
   * {} }
   */

  @Test
  public void TransferTrxToContractAccountFailIfNotPayable()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code =
        "6080604052348015600f57600080fd5b50603580601d6000396000f3006080604052600080fd00a165627a7a72305820f"
            + "5dc348e1c7dc90f9996a05c69dc9d060b6d356a1ed570ce3cd89570dc4ce6440029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    Transaction trx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null);
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
    Assert.assertNotNull(runtime.getRuntimeError().contains("REVERT"));
    Assert.assertNull(dbManager.getAccountStore().get(contractAddress));
    recoverDeposit();
  }


  /**
   * pragma solidity ^0.4.16; contract transferWhenTriggerContract { constructor () {} function
   * transferTo(address toAddress) public payable{ toAddress.transfer(5); } }
   */
  @Test
  public void TransferTrxToContractAccountWhenTriggerAContract()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
        + "\"name\":\"transferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},"
        + "{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code =
        "608060405234801561001057600080fd5b5060ee8061001f6000396000f300608060405260043610603f576000357c01"
            + "00000000000000000000000000000000000000000000000000000000900463ffffffff168063a03fa7e3146044575b600080fd5b607"
            + "6600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506078565b005b8073"
            + "ffffffffffffffffffffffffffffffffffffffff166108fc60059081150290604051600060405180830381858888f1935050505015801560be573d"
            + "6000803e3d6000fd5b50505600a165627a7a723058209b248b5be19bae77660cdc92b0a141f279dc4746d858d9d7d270a22d014eb97a0029";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    long transferToInitBalance = dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO))
        .getBalance();

    byte[] contractAddress = TVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null,
            deposit, null);

    String selectorStr = "transferTo(address)";
    String params = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData = TVMTestUtils.parseABI(selectorStr, params);

    long triggerCallValue = 100;

    Transaction transaction = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(address, contractAddress, triggerData,
            triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(contractAddress).getBalance(), 100 - 5);
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5);
    recoverDeposit();
  }

  /**
   * contract callerContract { calledContract CALLED_INSTANCE; constructor(address _addr) public
   * payable { CALLED_INSTANCE = calledContract(_addr); } // expect calledContract -5, toAddress +5
   * function testCallTransferToInCalledContract(address toAddress){ CALLED_INSTANCE.transferTo(toAddress);
   * }
   *
   * // expect calledContract -0, toAddress +0 function testRevertForCall(address toAddress){
   * CALLED_INSTANCE.transferTo(toAddress); revert(); } function testExceptionForCall(address
   * toAddress){ CALLED_INSTANCE.transferTo(toAddress); assert(1==2); } // expect c +100 -5,
   * toAddress +0 function testTransferToInCreatedContract(address toAddress) payable
   * returns(address){ createdContract c = (new createdContract).value(100)();
   * c.transferTo(toAddress); return address(c); }
   *
   * // expect c +100 -5, toAddress not exist function testRevertForCreate(address toAddress)
   * payable returns(address){ createdContract c = (new createdContract).value(100)();
   * c.transferTo(toAddress); revert(); return address(c); }
   *
   * // expect c +100 -5, toAddress not exist function testExceptionForCreate(address toAddress)
   * payable returns(address){ createdContract c = (new createdContract).value(100)();
   * c.transferTo(toAddress); assert(1==2); return address(c); }
   *
   * function getBalance() public view returns(uint256){ return this.balance; } }
   *
   * contract calledContract { constructor() payable {} function transferTo(address toAddress)
   * payable{ toAddress.transfer(5); }
   *
   * function getBalance() public view returns(uint256){ return this.balance; }
   *
   * }
   *
   * contract createdContract { constructor() payable {} function transferTo(address toAddress){
   * toAddress.transfer(5); }
   *
   * function getBalance() public view returns(uint256){ return this.balance; } }
   */

  @Test
  public void TransferCallValueTestWhenUsingCallAndCreate()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    byte[] msgSenderAddress = Hex.decode(OWNER_ADDRESS);
    byte[] calledAddress = deployCalledContract();
    byte[] callerAddress = deployCallerContract(calledAddress);
    long triggerCallValue = 0;
    long feeLimit = 100000000;

    //==================================0. check initial status ================================================
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(), 1000);
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(), 1000);
    long transferToInitBalance = dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO))
        .getBalance();

    //==================================1. testCallTransferToInCalledContract====================================
    String selectorStr1 = "testCallTransferToInCalledContract(address)";
    String params1 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData1 = TVMTestUtils.parseABI(selectorStr1, params1);

    Transaction transaction1 = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData1, triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction1, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000);  //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(),
        1000 - 5);  //Transfer 5 sun to TransferTo
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5); // get 5 sun from calledAddress
    recoverDeposit();

    //==================================2. testRevertForCall =================================================
    String selectorStr2 = "testRevertForCall(address)";
    String params2 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData2 = TVMTestUtils.parseABI(selectorStr2, params2);

    Transaction transaction2 = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData2, triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction2, deposit, null);
    Assert.assertTrue(runtime.getRuntimeError().contains("REVERT"));
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000); //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(),
        995);  //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5); //Not changed
    recoverDeposit();

    //==================================3. testExceptionForCall =================================================
    String selectorStr3 = "testExceptionForCall(address)";
    String params3 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData3 = TVMTestUtils.parseABI(selectorStr3, params3);

    Transaction transaction3 = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData3, triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction3, deposit, null);
    Assert.assertTrue(runtime.getRuntimeError().contains("Invalid operation code: opCode[fe];"));
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000);  //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(),
        995);   //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5);  //Not changed
    recoverDeposit();

    //==================================4. testTransferToInCreatedContract =================================================
    String selectorStr4 = "testTransferToInCreatedContract(address)";
    String params4 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData4 = TVMTestUtils.parseABI(selectorStr4, params4);

    Transaction transaction4 = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData4, triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction4, deposit, null);
    byte[] createdAddress = convertToTronAddress(
        new DataWord(runtime.getResult().getHReturn()).getLast20Bytes());
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000 - 100);  // Transfer to createdAddress
    Assert.assertEquals(dbManager.getAccountStore().get(createdAddress).getBalance(),
        100 - 5);  // Transfer to transfer_to
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5 + 5); // get 5 from created Address
    recoverDeposit();

    //==================================5. testRevertForCreate =================================================
    String selectorStr5 = "testRevertForCreate(address)";
    String params5 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData5 = TVMTestUtils.parseABI(selectorStr5, params5);

    Transaction transaction5 = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData5, triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction5, deposit, null);
    byte[] createdAddress2 = convertToTronAddress(
        new DataWord(runtime.getResult().getHReturn()).getLast20Bytes());
    Assert.assertTrue(Hex.toHexString(new DataWord(createdAddress2).getLast20Bytes())
        .equalsIgnoreCase("0000000000000000000000000000000000000000"));
    Assert.assertTrue(runtime.getRuntimeError().contains("REVERT"));
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000 - 100);  // Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(createdAddress).getBalance(),
        100 - 5);  // Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5 + 5); // Not changed
    recoverDeposit();

    //==================================5. testExceptionForCreate =================================================
    String selectorStr6 = "testExceptionForCreate(address)";
    String params6 = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData6 = TVMTestUtils.parseABI(selectorStr6, params6);

    Transaction transaction6 = TVMTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData6, triggerCallValue, feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction6, deposit, null);
    byte[] createdAddress3 = convertToTronAddress(
        new DataWord(runtime.getResult().getHReturn()).getLast20Bytes());
    Assert.assertTrue(Hex.toHexString(new DataWord(createdAddress2).getLast20Bytes())
        .equalsIgnoreCase("0000000000000000000000000000000000000000"));
    Assert.assertTrue(runtime.getRuntimeError().contains("Invalid operation code: opCode[fe];"));
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000 - 100);  // Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(createdAddress).getBalance(),
        100 - 5);  // Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5 + 5); // Not changed
    recoverDeposit();
  }


  private byte[] deployCalledContract()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"callcodeTransferTo\","
            + "\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":"
            + "[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":"
            + "\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":"
            + "\"delegateTransferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[],"
            + "\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code =
        "6080604052610105806100136000396000f30060806040526004361060485763ffffffff7c0100000000000000000000000000000000000000000"
            + "00000000000000060003504166312065fe08114604d578063a03fa7e3146071575b600080fd5b348015605857600080fd5b50605f6092565b6040805191825"
            + "2519081900360200190f35b609073ffffffffffffffffffffffffffffffffffffffff600435166097565b005b303190565b60405173fffffffffffffffffff"
            + "fffffffffffffffffffff82169060009060059082818181858883f1935050505015801560d5573d6000803e3d6000fd5b50505600a165627a7a72305820b6a"
            + "1478817a43ce2d6d3e26b8c8a89b35b4b49abe5b33849faedc32a602ed0850029";
    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    byte[] contractAddress = TVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null,
            deposit, null);
    return contractAddress;
  }

  private byte[] deployCallerContract(byte[] calledAddress)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "callerContract";
    byte[] callerAddress = Hex.decode(OWNER_ADDRESS);
    String callerABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"testExceptionForCreate\""
            + ",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{"
            + "\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,"
            + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
            + "\"name\":\"testRevertForCreate\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\":\"payable\","
            + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":"
            + "\"testCallTransferToInCalledContract\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"testRevertForCall\",\"outputs\":[],\"payable"
            + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":"
            + "\"address\"}],\"name\":\"testTransferToInCreatedContract\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":true,"
            + "\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
            + "\"name\":\"testExceptionForCall\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs"
            + "\":[{\"name\":\"_addr\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String callerCode =
        "6080604052604051602080610631833981016040525160008054600160a060020a03909216600160a060020a03199092169190911790556105ec8061"
            + "00456000396000f3006080604052600436106100695763ffffffff60e060020a6000350416630f384973811461006e57806312065fe01461009e5780632a3630891461"
            + "00c55780632fdb75bc146100d9578063330715c1146100fc5780635fba6a6e1461011d57806373e6fd7814610131575b600080fd5b610082600160a060020a03600435"
            + "16610152565b60408051600160a060020a039092168252519081900360200190f35b3480156100aa57600080fd5b506100b36101f4565b604080519182525190819003"
            + "60200190f35b610082600160a060020a03600435166101f9565b3480156100e557600080fd5b506100fa600160a060020a036004351661029e565b005b348015610108"
            + "57600080fd5b506100fa600160a060020a036004351661031d565b610082600160a060020a0360043516610381565b34801561013d57600080fd5b506100fa600160a0"
            + "60020a0360043516610428565b600080606461015f61048c565b6040518091039082f08015801561017a573d6000803e3d6000fd5b509050905080600160a060020a03"
            + "1663a03fa7e3846040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051808303816000"
            + "87803b1580156101da57600080fd5b505af11580156101ee573d6000803e3d6000fd5b50505050fe5b303190565b600080606461020661048c565b6040518091039082"
            + "f080158015610221573d6000803e3d6000fd5b509050905080600160a060020a031663a03fa7e3846040518263ffffffff1660e060020a0281526004018082600160a0"
            + "60020a0316600160a060020a03168152602001915050600060405180830381600087803b15801561028157600080fd5b505af1158015610295573d6000803e3d6000fd"
            + "5b50505050600080fd5b60008054604080517fa03fa7e3000000000000000000000000000000000000000000000000000000008152600160a060020a03858116600483"
            + "01529151919092169263a03fa7e3926024808201939182900301818387803b15801561030257600080fd5b505af1158015610316573d6000803e3d6000fd5b50505050"
            + "50565b60008054604080517fa03fa7e3000000000000000000000000000000000000000000000000000000008152600160a060020a0385811660048301529151919092"
            + "169263a03fa7e3926024808201939182900301818387803b15801561028157600080fd5b600080606461038e61048c565b6040518091039082f0801580156103a9573d"
            + "6000803e3d6000fd5b509050905080600160a060020a031663a03fa7e3846040518263ffffffff1660e060020a0281526004018082600160a060020a0316600160a060"
            + "020a03168152602001915050600060405180830381600087803b15801561040957600080fd5b505af115801561041d573d6000803e3d6000fd5b509295945050505050"
            + "565b60008054604080517fa03fa7e3000000000000000000000000000000000000000000000000000000008152600160a060020a038581166004830152915191909216"
            + "9263a03fa7e3926024808201939182900301818387803b1580156101da57600080fd5b6040516101248061049d83390190560060806040526101118061001360003960"
            + "00f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166312065fe08114604d578063"
            + "a03fa7e3146071575b600080fd5b348015605857600080fd5b50605f609e565b60408051918252519081900360200190f35b348015607c57600080fd5b50609c73ffff"
            + "ffffffffffffffffffffffffffffffffffff6004351660a3565b005b303190565b60405173ffffffffffffffffffffffffffffffffffffffff82169060009060059082"
            + "818181858883f1935050505015801560e1573d6000803e3d6000fd5b50505600a165627a7a72305820be71319c8486b22c77db76e8bf3c188eee99c4d5cef7c7c9a7fc"
            + "f82660cd9ed10029a165627a7a723058209824f9789a24b669fe58f76f188fd083dce266b61ab11d43e69215492a1bffa50029"
            + Hex.toHexString((new DataWord(calledAddress)).getData());
    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    byte[] contractAddress = TVMTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, callerAddress, callerABI,
            callerCode, value, feeLimit, consumeUserResourcePercent, null,
            deposit, null);
    return contractAddress;
  }

  private void recoverDeposit() {
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
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
