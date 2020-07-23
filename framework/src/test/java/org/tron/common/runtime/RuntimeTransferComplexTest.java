package org.tron.common.runtime;

import static org.tron.core.db.TransactionTrace.convertToTronAddress;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.WalletUtil;
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

  private static final String dbPath = "output_RuntimeTransferComplexTest";
  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;
  private static Runtime runtime;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;

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
   * Test constructor Transfer pragma solidity ^0.4.16; contract transferWhenDeploy { constructor ()
   * payable{} }
   */
  @Test
  public void TransferTrxToContractAccountWhenDeployAContract()
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\","
        + "\"type\":\"constructor\"}]";
    String code = "608060405260358060116000396000f3006080604052600080fd00a165627a7a72305820d3b0de5"
        + "bdc00ebe85619d50b72b29d30bd00dd233e8849402671979de0e9e73b0029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    Transaction trx = TvmTestUtils
        .generateDeploySmartContractAndGetTransaction(contractName, address, ABI, code, value, fee,
            consumeUserResourcePercent, null);
    byte[] contractAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
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
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\""
        + "nonpayable\",\"type\":\"constructor\"}]";
    String code =
        "6080604052348015600f57600080fd5b50603580601d6000396000f3006080604052600080fd00a165627a7a"
            + "72305820f5dc348e1c7dc90f9996a05c69dc9d060b6d356a1ed570ce3cd89570dc4ce6440029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    Transaction trx = TvmTestUtils
        .generateDeploySmartContractAndGetTransaction(contractName, address, ABI, code, value, fee,
            consumeUserResourcePercent, null);
    byte[] contractAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
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
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
        + "\"name\":\"transferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\""
        + ",\"type\":\"function\"},"
        + "{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":"
        + "\"constructor\"}]";
    String code =
        "608060405234801561001057600080fd5b5060ee8061001f6000396000f300608060405260043610603f57600"
            + "0357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063"
            + "a03fa7e3146044575b600080fd5b6076600480360381019080803573fffffffffffffffffffffffffff"
            + "fffffffffffff1690602001909291905050506078565b005b8073ffffffffffffffffffffffffffffff"
            + "ffffffffff166108fc60059081150290604051600060405180830381858888f1935050505015801560b"
            + "e573d6000803e3d6000fd5b50505600a165627a7a723058209b248b5be19bae77660cdc92b0a141f279"
            + "dc4746d858d9d7d270a22d014eb97a0029";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    long transferToInitBalance = dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO))
        .getBalance();

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);

    String selectorStr = "transferTo(address)";
    String params =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData = TvmTestUtils.parseAbi(selectorStr, params);

    long triggerCallValue = 100;

    Transaction transaction = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(address, contractAddress, triggerData,
            triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(contractAddress).getBalance(), 100 - 5);
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5);
    recoverDeposit();
  }

  /**
   * contract callerContract { calledContract CALLED_INSTANCE; constructor(address _addr) public
   * payable { CALLED_INSTANCE = calledContract(_addr); } // expect calledContract -5, toAddress +5
   * function testCallTransferToInCalledContract(address toAddress) {
   * CALLED_INSTANCE.transferTo(toAddress); }
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
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {
    byte[] msgSenderAddress = Hex.decode(OWNER_ADDRESS);
    byte[] calledAddress = deployCalledContract();
    byte[] callerAddress = deployCallerContract(calledAddress);
    long triggerCallValue = 0;
    long feeLimit = 100000000;

    //======================0. check initial status ===================================
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(), 1000);
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(), 1000);
    long transferToInitBalance = dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO))
        .getBalance();

    //=========================1. testCallTransferToInCalledContract=============================
    String selectorStr1 = "testCallTransferToInCalledContract(address)";
    String params1 =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData1 = TvmTestUtils.parseAbi(selectorStr1, params1);

    Transaction transaction1 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData1, triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction1, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000);  //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(),
        1000 - 5);  //Transfer 5 sun to TransferTo
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5); // get 5 sun from calledAddress
    recoverDeposit();

    //======================2. testRevertForCall =========================================
    String selectorStr2 = "testRevertForCall(address)";
    String params2 =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData2 = TvmTestUtils.parseAbi(selectorStr2, params2);

    Transaction transaction2 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData2, triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction2, deposit, null);
    Assert.assertTrue(runtime.getRuntimeError().contains("REVERT"));
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000); //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(),
        995);  //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5); //Not changed
    recoverDeposit();

    //========================3. testExceptionForCall ======================================
    String selectorStr3 = "testExceptionForCall(address)";
    String params3 =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData3 = TvmTestUtils.parseAbi(selectorStr3, params3);

    Transaction transaction3 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData3, triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction3, deposit, null);
    Assert.assertTrue(runtime.getRuntimeError().contains("Invalid operation code: opCode[fe];"));
    Assert.assertEquals(dbManager.getAccountStore().get(callerAddress).getBalance(),
        1000);  //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(calledAddress).getBalance(),
        995);   //Not changed
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),
        transferToInitBalance + 5);  //Not changed
    recoverDeposit();

    //========================4. testTransferToInCreatedContract ==================================
    String selectorStr4 = "testTransferToInCreatedContract(address)";
    String params4 =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData4 = TvmTestUtils.parseAbi(selectorStr4, params4);

    Transaction transaction4 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData4, triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction4, deposit, null);
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

    //====================5. testRevertForCreate =================================
    String selectorStr5 = "testRevertForCreate(address)";
    String params5 =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData5 = TvmTestUtils.parseAbi(selectorStr5, params5);

    Transaction transaction5 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData5, triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction5, deposit, null);
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

    //=======================5. testExceptionForCreate =====================================
    String selectorStr6 = "testExceptionForCreate(address)";
    String params6 =
        "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData6 = TvmTestUtils.parseAbi(selectorStr6, params6);

    Transaction transaction6 = TvmTestUtils
        .generateTriggerSmartContractAndGetTransaction(msgSenderAddress, callerAddress,
            triggerData6, triggerCallValue, feeLimit);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(transaction6, deposit, null);
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
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {
    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
        + "\"name\":\"callcodeTransferTo\","
        + "\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":"
        + "[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\"transferTo\","
        + "\"outputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":"
        + "\"toAddress\",\"type\":\"address\"}],\"name\":"
        + "\"delegateTransferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\""
        + ":\"payable\",\"type\":\"function\"},{\"inputs\":[],"
        + "\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code =
        "6080604052610105806100136000396000f30060806040526004361060485763ffffffff7c010000000000000"
            + "000000000000000000000000000000000000000000060003504166312065fe08114604d578063a03fa7"
            + "e3146071575b600080fd5b348015605857600080fd5b50605f6092565b6040805191825251908190036"
            + "0200190f35b609073ffffffffffffffffffffffffffffffffffffffff600435166097565b005b303190"
            + "565b60405173ffffffffffffffffffffffffffffffffffffffff8216906000906005908281818185888"
            + "3f1935050505015801560d5573d6000803e3d6000fd5b50505600a165627a7a72305820b6a"
            + "1478817a43ce2d6d3e26b8c8a89b35b4b49abe5b33849faedc32a602ed0850029";
    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, address, ABI, code, value,
            feeLimit, consumeUserResourcePercent, null, deposit, null);
    return contractAddress;
  }

  private byte[] deployCallerContract(byte[] calledAddress)
      throws ContractExeException, ReceiptCheckErrException,
      ContractValidateException, VMIllegalException {
    String contractName = "callerContract";
    byte[] callerAddress = Hex.decode(OWNER_ADDRESS);
    String callerABI =
        "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":"
            + "\"testExceptionForCreate\""
            + ",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":true,\"stateMutabi"
            + "lity\":\"payable\",\"type\":\"function\"},{"
            + "\"constant\":true,\"inputs\":[],\"name\":\"getBalance\",\"outputs\":[{\"name\":\"\""
            + ",\"type\":\"uint256\"}],\"payable\":false,"
            + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\""
            + ":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
            + "\"name\":\"testRevertForCreate\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}"
            + "],\"payable\":true,\"stateMutability\":\"payable\","
            + "\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\""
            + "type\":\"address\"}],\"name\":"
            + "\"testCallTransferToInCalledContract\",\"outputs\":[],\"payable\":false,\"stateMut"
            + "ability\":\"nonpayable\",\"type\":\"function\"},"
            + "{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"n"
            + "ame\":\"testRevertForCall\",\"outputs\":[],\"payable"
            + "\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":f"
            + "alse,\"inputs\":[{\"name\":\"toAddress\",\"type\":"
            + "\"address\"}],\"name\":\"testTransferToInCreatedContract\",\"outputs\":[{\"name\":"
            + "\"\",\"type\":\"address\"}],\"payable\":true,"
            + "\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":false,\"input"
            + "s\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
            + "\"name\":\"testExceptionForCall\",\"outputs\":[],\"payable\":false,\"stateMutabili"
            + "ty\":\"nonpayable\",\"type\":\"function\"},{\"inputs"
            + "\":[{\"name\":\"_addr\",\"type\":\"address\"}],\"payable\":true,\"stateMutability\""
            + ":\"payable\",\"type\":\"constructor\"}]";
    String callerCode =
        "6080604052604051602080610631833981016040525160008054600160a060020a03909216600160a060020a0"
            + "3199092169190911790556105ec806100456000396000f3006080604052600436106100695763ffffff"
            + "ff60e060020a6000350416630f384973811461006e57806312065fe01461009e5780632a36308914610"
            + "0c55780632fdb75bc146100d9578063330715c1146100fc5780635fba6a6e1461011d57806373e6fd78"
            + "14610131575b600080fd5b610082600160a060020a0360043516610152565b60408051600160a060020"
            + "a039092168252519081900360200190f35b3480156100aa57600080fd5b506100b36101f4565b604080"
            + "51918252519081900360200190f35b610082600160a060020a03600435166101f9565b3480156100e55"
            + "7600080fd5b506100fa600160a060020a036004351661029e565b005b34801561010857600080fd5b50"
            + "6100fa600160a060020a036004351661031d565b610082600160a060020a0360043516610381565b348"
            + "01561013d57600080fd5b506100fa600160a060020a0360043516610428565b600080606461015f6104"
            + "8c565b6040518091039082f08015801561017a573d6000803e3d6000fd5b509050905080600160a0600"
            + "20a031663a03fa7e3846040518263ffffffff1660e060020a0281526004018082600160a060020a0316"
            + "600160a060020a03168152602001915050600060405180830381600087803b1580156101da57600080f"
            + "d5b505af11580156101ee573d6000803e3d6000fd5b50505050fe5b303190565b600080606461020661"
            + "048c565b6040518091039082f080158015610221573d6000803e3d6000fd5b509050905080600160a06"
            + "0020a031663a03fa7e3846040518263ffffffff1660e060020a0281526004018082600160a060020a03"
            + "16600160a060020a03168152602001915050600060405180830381600087803b1580156102815760008"
            + "0fd5b505af1158015610295573d6000803e3d6000fd5b50505050600080fd5b60008054604080517fa0"
            + "3fa7e3000000000000000000000000000000000000000000000000000000008152600160a060020a038"
            + "5811660048301529151919092169263a03fa7e3926024808201939182900301818387803b1580156103"
            + "0257600080fd5b505af1158015610316573d6000803e3d6000fd5b5050505050565b600080546040805"
            + "17fa03fa7e3000000000000000000000000000000000000000000000000000000008152600160a06002"
            + "0a0385811660048301529151919092169263a03fa7e3926024808201939182900301818387803b15801"
            + "561028157600080fd5b600080606461038e61048c565b6040518091039082f0801580156103a9573d60"
            + "00803e3d6000fd5b509050905080600160a060020a031663a03fa7e3846040518263ffffffff1660e06"
            + "0020a0281526004018082600160a060020a0316600160a060020a031681526020019150506000604051"
            + "80830381600087803b15801561040957600080fd5b505af115801561041d573d6000803e3d6000fd5b5"
            + "09295945050505050565b60008054604080517fa03fa7e3000000000000000000000000000000000000"
            + "000000000000000000008152600160a060020a0385811660048301529151919092169263a03fa7e3926"
            + "024808201939182900301818387803b1580156101da57600080fd5b6040516101248061049d83390190"
            + "56006080604052610111806100136000396000f30060806040526004361060485763ffffffff7c01000"
            + "0000000000000000000000000000000000000000000000000000060003504166312065fe08114604d57"
            + "8063a03fa7e3146071575b600080fd5b348015605857600080fd5b50605f609e565b604080519182525"
            + "19081900360200190f35b348015607c57600080fd5b50609c73ffffffffffffffffffffffffffffffff"
            + "ffffffff6004351660a3565b005b303190565b60405173fffffffffffffffffffffffffffffffffffff"
            + "fff82169060009060059082818181858883f1935050505015801560e1573d6000803e3d6000fd5b5050"
            + "5600a165627a7a72305820be71319c8486b22c77db76e8bf3c188eee99c4d5cef7c7c9a7fcf82660cd9"
            + "ed10029a165627a7a723058209824f9789a24b669fe58f76f188fd083dce266b61ab11d43e69215492a"
            + "1bffa50029" + Hex.toHexString((new DataWord(calledAddress)).getData());
    long value = 1000;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;
    byte[] contractAddress = TvmTestUtils
        .deployContractWholeProcessReturnContractAddress(contractName, callerAddress, callerABI,
            callerCode, value, feeLimit, consumeUserResourcePercent, null, deposit, null);
    return contractAddress;
  }

  private void recoverDeposit() {
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
  }


}
