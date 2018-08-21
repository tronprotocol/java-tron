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
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class RuntimeTransferComplexTest {
  private static Runtime runtime;
  private static Manager dbManager;
  private static AnnotationConfigApplicationContext context;
  private static DepositImpl deposit;
  private static final String dbPath = "output_RuntimeTransferComplexTest";
  private static final String OWNER_ADDRESS;
  private static final String TRANSFER_TO;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
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
    deposit.createAccount(Hex.decode(OWNER_ADDRESS),AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS),100000000);
    deposit.createAccount(Hex.decode(TRANSFER_TO),AccountType.Normal);
    deposit.addBalance(Hex.decode(TRANSFER_TO),10);
  }


  /**
   *
   * Test constructor Transfer
   * pragma solidity ^0.4.16;
   * contract transferWhenDeploy {
   *     constructor () payable{}
   * }
   */
  @Test
  public void TransferTrxToContractAccountWhenDeployAContract()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405260358060116000396000f3006080604052600080fd00a165627a7a72305820d3b0de5bdc00ebe85619d50b72b29d30bd00dd233e8849402671979de0e9e73b0029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;


    Transaction trx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName,address,ABI,code,value,fee,consumeUserResourcePercent,null);
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(contractAddress).getBalance(),100);
  }

  /**
   *
   * Test constructor Transfer
   * pragma solidity ^0.4.16;
   * contract transferWhenDeploy {
   *     constructor () {}
   * }
   */

  @Test
  public void TransferTrxToContractAccountFailIfNotPayable()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code = "6080604052348015600f57600080fd5b50603580601d6000396000f3006080604052600080fd00a165627a7a72305820f5dc348e1c7dc90f9996a05c69dc9d060b6d356a1ed570ce3cd89570dc4ce6440029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;


    Transaction trx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName,address,ABI,code,value,fee,consumeUserResourcePercent,null);
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
    Assert.assertNotNull(runtime.getRuntimeError().contains("REVERT"));
    Assert.assertNull(dbManager.getAccountStore().get(contractAddress));
  }


  /**
   * pragma solidity ^0.4.16;
   * contract transferWhenTriggerContract {
   *     constructor () {}
   *     function transferTo(address toAddress) public payable{
   *         toAddress.transfer(5);
   *     }
   * }
   */
  @Test
  public void TransferTrxToContractAccountWhenTriggerAContract()
      throws ContractExeException, OutOfSlotTimeException, TransactionTraceException, ContractValidateException {

    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],"
        + "\"name\":\"transferTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},"
        + "{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}]";
    String code = "608060405234801561001057600080fd5b5060ee8061001f6000396000f300608060405260043610603f576000357c01"
        + "00000000000000000000000000000000000000000000000000000000900463ffffffff168063a03fa7e3146044575b600080fd5b607"
        + "6600480360381019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506078565b005b8073"
        + "ffffffffffffffffffffffffffffffffffffffff166108fc60059081150290604051600060405180830381858888f1935050505015801560be573d"
        + "6000803e3d6000fd5b50505600a165627a7a723058209b248b5be19bae77660cdc92b0a141f279dc4746d858d9d7d270a22d014eb97a0029";
    long value = 0;
    long feeLimit = 100000000;
    long consumeUserResourcePercent = 0;

    byte[] contractAddress = TVMTestUtils.deployContractWholeProcessReturnContractAddress(contractName,address,ABI,code,value,feeLimit,consumeUserResourcePercent,null,
        deposit,null);



    String selectorStr = "transferTo(address)";
    String params = "000000000000000000000000548794500882809695a8a687866e76d4271a1abc"; //TRANSFER_TO
    byte[] triggerData = TVMTestUtils.parseABI(selectorStr,params);

    long triggerCallValue =100;

    Transaction transaction = TVMTestUtils.generateTriggerSmartContractAndGetTransaction(address,contractAddress,triggerData,triggerCallValue,feeLimit);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(transaction, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());
    Assert.assertEquals(dbManager.getAccountStore().get(contractAddress).getBalance(),100 - 5);
    Assert.assertEquals(dbManager.getAccountStore().get(Hex.decode(TRANSFER_TO)).getBalance(),10 + 5);
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
