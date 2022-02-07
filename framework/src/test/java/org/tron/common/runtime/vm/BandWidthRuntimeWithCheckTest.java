/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.runtime.vm;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ReceiptCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.TooBigTransactionResultException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.StoreFactory;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

/**
 * pragma solidity ^0.4.2;
 *
 * contract Fibonacci {
 *
 * event Notify(uint input, uint result);
 *
 * function fibonacci(uint number) constant returns(uint result) { if (number == 0) { return 0; }
 * else if (number == 1) { return 1; } else { uint256 first = 0; uint256 second = 1; uint256 ret =
 * 0; for(uint256 i = 2; i <= number; i++) { ret = first + second; first = second; second = ret; }
 * return ret; } }
 *
 * function fibonacciNotify(uint number) returns(uint result) { result = fibonacci(number);
 * Notify(number, result); } }
 */
public class BandWidthRuntimeWithCheckTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static String dbPath = "output_BandWidthRuntimeTest_test";
  private static String dbDirectory = "db_BandWidthRuntimeTest_test";
  private static String indexDirectory = "index_BandWidthRuntimeTest_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;
  private static ChainBaseManager chainBaseManager;

  private static String OwnerAddress = "TCWHANtDDdkZCTo2T2peyEq3Eg9c2XB7ut";
  private static String TriggerOwnerAddress = "TCSgeWapPJhCqgWRxXCKb6jJ5AgNWSGjPA";
  private static String TriggerOwnerTwoAddress = "TPMBUANrTwwQAPwShn7ZZjTJz1f3F8jknj";

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w"
        },
        "config-test-mainnet.conf"
    );
    context = new TronApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);

    //init energy
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(10_000_000L);

    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Commons.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);

    accountCapsule.setFrozenForEnergy(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Commons.decodeFromBase58Check(OwnerAddress), accountCapsule);

    AccountCapsule accountCapsule2 = new AccountCapsule(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Commons.decodeFromBase58Check(TriggerOwnerAddress)), AccountType.Normal,
        totalBalance);

    accountCapsule2.setFrozenForEnergy(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Commons.decodeFromBase58Check(TriggerOwnerAddress), accountCapsule2);
    AccountCapsule accountCapsule3 = new AccountCapsule(
        ByteString.copyFrom("triggerOwnerAddress".getBytes()),
        ByteString.copyFrom(Commons.decodeFromBase58Check(TriggerOwnerTwoAddress)),
        AccountType.Normal,
        totalBalance);
    accountCapsule3.setNetUsage(5000L);
    accountCapsule3.setLatestConsumeFreeTime(chainBaseManager.getHeadSlot());
    accountCapsule3.setFrozenForEnergy(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Commons.decodeFromBase58Check(TriggerOwnerTwoAddress), accountCapsule3);

  }

  /**
   * destroy clear data of testing.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testSuccess() {
    try {
      byte[] contractAddress = createContract();
      AccountCapsule triggerOwner = dbManager.getAccountStore()
          .get(Commons.decodeFromBase58Check(TriggerOwnerAddress));
      long energy = triggerOwner.getEnergyUsage();
      long balance = triggerOwner.getBalance();
      TriggerSmartContract triggerContract = TvmTestUtils.createTriggerContract(contractAddress,
          "fibonacciNotify(uint256)", "7000", false,
          0, Commons.decodeFromBase58Check(TriggerOwnerAddress));
      Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
          Contract.newBuilder().setParameter(Any.pack(triggerContract))
              .setType(ContractType.TriggerSmartContract)).setFeeLimit(1000000000)).build();
      TransactionCapsule trxCap = new TransactionCapsule(transaction);
      TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
          new RuntimeImpl());
      dbManager.consumeBandwidth(trxCap, trace);
      BlockCapsule blockCapsule = null;
      DepositImpl deposit = DepositImpl.createRoot(dbManager);
      trace.init(blockCapsule);
      trace.exec();
      trace.finalization();

      triggerOwner = dbManager.getAccountStore()
          .get(Commons.decodeFromBase58Check(TriggerOwnerAddress));
      energy = triggerOwner.getEnergyUsage() - energy;
      balance = balance - triggerOwner.getBalance();
      Assert.assertEquals(624668, trace.getReceipt().getEnergyUsageTotal());
      Assert.assertEquals(50000, energy);
      Assert.assertEquals(57466800, balance);
      Assert.assertEquals(624668 * Constant.SUN_PER_ENERGY,
          balance + energy * Constant.SUN_PER_ENERGY);
    } catch (TronException e) {
      Assert.assertNotNull(e);
    } catch (ReceiptCheckErrException e) {
      Assert.assertNotNull(e);
    }

  }

  @Test
  public void testSuccessNoBandWidth() {
    try {
      byte[] contractAddress = createContract();
      TriggerSmartContract triggerContract = TvmTestUtils.createTriggerContract(contractAddress,
          "fibonacciNotify(uint256)", "50", false,
          0, Commons.decodeFromBase58Check(TriggerOwnerTwoAddress));
      Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
          Contract.newBuilder().setParameter(Any.pack(triggerContract))
              .setType(ContractType.TriggerSmartContract)).setFeeLimit(1000000000)).build();
      TransactionCapsule trxCap = new TransactionCapsule(transaction);
      trxCap.setResultCode(contractResult.SUCCESS);
      TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
          new RuntimeImpl());
      dbManager.consumeBandwidth(trxCap, trace);
      long bandWidth = trxCap.getSerializedSize() + Constant.MAX_RESULT_SIZE_IN_TX;
      BlockCapsule blockCapsule = null;
      DepositImpl deposit = DepositImpl.createRoot(dbManager);
      trace.init(blockCapsule);
      trace.exec();
      trace.finalization();
      trace.check();
      AccountCapsule triggerOwnerTwo = dbManager.getAccountStore()
          .get(Commons.decodeFromBase58Check(TriggerOwnerTwoAddress));
      long balance = triggerOwnerTwo.getBalance();
      ReceiptCapsule receipt = trace.getReceipt();
      Assert.assertNull(trace.getRuntimeError());
      Assert.assertEquals(bandWidth, receipt.getNetUsage());
      Assert.assertEquals(6118, receipt.getEnergyUsageTotal());
      Assert.assertEquals(6118, receipt.getEnergyUsage());
      Assert.assertEquals(0, receipt.getEnergyFee());
      Assert.assertEquals(totalBalance,
          balance);
    } catch (TronException e) {
      Assert.assertNotNull(e);
    } catch (ReceiptCheckErrException e) {
      Assert.assertNotNull(e);
    }
  }

  private byte[] createContract()
      throws ContractValidateException, AccountResourceInsufficientException,
      TooBigTransactionResultException, ContractExeException, ReceiptCheckErrException,
      VMIllegalException {
    AccountCapsule owner = dbManager.getAccountStore()
        .get(Commons.decodeFromBase58Check(OwnerAddress));
    long energy = owner.getEnergyUsage();
    long balance = owner.getBalance();

    String contractName = "Fibonacci";
    String code = "608060405234801561001057600080fd5b506101ba806100206000396000f3006080604052600436"
        + "1061004c576000357c0100000000000000000000000000000000000000000000000000000000900463fffff"
        + "fff1680633c7fdc701461005157806361047ff414610092575b600080fd5b34801561005d57600080fd5b506"
        + "1007c600480360381019080803590602001909291905050506100d3565b60405180828152602001915050604"
        + "05180910390f35b34801561009e57600080fd5b506100bd60048036038101908080359060200190929190505"
        + "050610124565b6040518082815260200191505060405180910390f35b60006100de82610124565b90507f71e"
        + "71a8458267085d5ab16980fd5f114d2d37f232479c245d523ce8d23ca40ed82826040518083815260200182"
        + "81526020019250505060405180910390a1919050565b60008060008060008086141561013d5760009450610"
        + "185565b600186141561014f5760019450610185565b600093506001925060009150600290505b858111151"
        + "56101815782840191508293508192508080600101915050610160565b8194505b505050509190505600a16"
        + "5627a7a7230582071f3cf655137ce9dc32d3307fb879e65f3960769282e6e452a5f0023ea046ed20029";

    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],"
        + "\"name\":\"fibonacciNotify\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":true,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],"
        + "\"name\":\"fibonacci\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"anonymous\""
        + ":false,\"inputs\":[{\"indexed\":false,\"name\":\"input\",\"type\":\"uint256\"},"
        + "{\"indexed\":false,\"name\":\"result\",\"type\":\"uint256\"}],\"name\":\"Notify\","
        + "\"type\":\"event\"}]";

    CreateSmartContract smartContract = TvmTestUtils.createSmartContract(
        Commons.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0,
        100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(smartContract))
            .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)).build();
    TransactionCapsule trxCap = new TransactionCapsule(transaction);
    trxCap.setResultCode(contractResult.SUCCESS);
    TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
        new RuntimeImpl());
    dbManager.consumeBandwidth(trxCap, trace);
    BlockCapsule blockCapsule = null;
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    trace.init(blockCapsule);
    trace.exec();
    trace.finalization();
    trace.check();

    owner = dbManager.getAccountStore()
        .get(Commons.decodeFromBase58Check(OwnerAddress));
    energy = owner.getEnergyUsage() - energy;
    balance = balance - owner.getBalance();
    Assert.assertNull(trace.getRuntimeError());
    Assert.assertEquals(88529, trace.getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(50000, energy);
    Assert.assertEquals(3852900, balance);
    Assert
        .assertEquals(88529 * Constant.SUN_PER_ENERGY,
            balance + energy * Constant.SUN_PER_ENERGY);
    if (trace.getRuntimeError() != null) {
      return trace.getRuntimeResult().getContractAddress();
    }
    return trace.getRuntimeResult().getContractAddress();

  }
}