package org.tron.common.runtime;

import static org.tron.common.runtime.TVMTestUtils.generateDeploySmartContractAndGetTransaction;
import static org.tron.common.runtime.TVMTestUtils.generateTriggerSmartContractAndGetTransaction;

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
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;


@Slf4j

public class RuntimeImplTest {

  private Manager dbManager;
  private TronApplicationContext context;
  private DepositImpl deposit;
  private String dbPath = "output_RuntimeImplTest";
  private Application AppT;
  private byte[] callerAddress;
  private long callerTotalBalance = 4_000_000_000L;
  private byte[] creatorAddress;
  private long creatorTotalBalance = 3_000_000_000L;

  /**
   * Init data.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    callerAddress = Hex
        .decode(Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc");
    creatorAddress = Hex
        .decode(Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abd");
    dbManager = context.getBean(Manager.class);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647838000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(5_000_000_000L); // unit is trx
    deposit = DepositImpl.createRoot(dbManager);
    deposit.createAccount(callerAddress, AccountType.Normal);
    deposit.addBalance(callerAddress, callerTotalBalance);
    deposit.createAccount(creatorAddress, AccountType.Normal);
    deposit.addBalance(creatorAddress, creatorTotalBalance);
    deposit.commit();
  }

  // // solidity src code
  // pragma solidity ^0.4.2;
  //
  // contract TestEnergyLimit {
  //
  //   function testNotConstant(uint256 count) {
  //     uint256 curCount = 0;
  //     while(curCount < count) {
  //       uint256 a = 1;
  //       curCount += 1;
  //     }
  //   }
  //
  //   function testConstant(uint256 count) constant {
  //     uint256 curCount = 0;
  //     while(curCount < count) {
  //       uint256 a = 1;
  //       curCount += 1;
  //     }
  //   }
  //
  // }


  @Test
  public void getCreatorEnergyLimit2Test() {

    long value = 10L;
    long feeLimit = 1_000_000_000L;
    long consumeUserResourcePercent = 0L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;

    Transaction trx = generateDeploySmartContractAndGetTransaction(contractName, creatorAddress,
        ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair);

    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    deposit = DepositImpl.createRoot(dbManager);
    AccountCapsule creatorAccount = deposit.getAccount(creatorAddress);

    long expectEnergyLimit1 = 10_000_000L;
    Assert.assertEquals(runtimeImpl.getAccountEnergyLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectEnergyLimit1);

    value = 2_500_000_000L;
    long expectEnergyLimit2 = 5_000_000L;
    Assert.assertEquals(runtimeImpl.getAccountEnergyLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectEnergyLimit2);

    value = 10L;
    feeLimit = 1_000_000L;
    long expectEnergyLimit3 = 10_000L;
    Assert.assertEquals(runtimeImpl.getAccountEnergyLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectEnergyLimit3);

    long frozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - frozenBalance;
    creatorAccount.setFrozenForEnergy(frozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    long expectEnergyLimit4 = 10_000_000L;
    Assert.assertEquals(runtimeImpl.getAccountEnergyLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectEnergyLimit4);

    feeLimit = 3_000_000_000L;
    value = 10L;
    long expectEnergyLimit5 = 20_009_999L;
    Assert.assertEquals(runtimeImpl.getAccountEnergyLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectEnergyLimit5);

    feeLimit = 3_000L;
    value = 10L;
    long expectEnergyLimit6 = 30L;
    Assert.assertEquals(runtimeImpl.getAccountEnergyLimitWithFixRatio(creatorAccount, feeLimit, value),
        expectEnergyLimit6);

  }

  @Test
  public void getCallerAndCreatorEnergyLimit2With0PercentTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {

    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 0L;
    long creatorEnergyLimit = 5_000L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;
    TVMTestResult result = TVMTestUtils
        .deployContractWithCreatorEnergyLimitAndReturnTVMTestResult(contractName, creatorAddress,
            ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, dbManager, null,
            creatorEnergyLimit);

    byte[] contractAddress = result.getContractAddress();
    byte[] triggerData = TVMTestUtils.parseABI("testNotConstant()", null);
    Transaction trx = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        triggerData, value, feeLimit);

    deposit = DepositImpl.createRoot(dbManager);
    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    AccountCapsule creatorAccount = deposit.getAccount(creatorAddress);
    AccountCapsule callerAccount = deposit.getAccount(callerAddress);
    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectEnergyLimit1 = 10_000_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit1);

    long creatorFrozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - creatorFrozenBalance;
    creatorAccount.setFrozenForEnergy(creatorFrozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectEnergyLimit2 = 10_005_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit2);

    value = 3_500_000_000L;
    long expectEnergyLimit3 = 5_005_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit3);

    value = 10L;
    feeLimit = 5_000_000_000L;
    long expectEnergyLimit4 = 40_004_999L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit4);

    long callerFrozenBalance = 1_000_000_000L;
    callerAccount.setFrozenForEnergy(callerFrozenBalance, 0L);
    callerAccount.setBalance(callerAccount.getBalance() - callerFrozenBalance);
    deposit.putAccountValue(callerAddress, callerAccount);
    deposit.commit();

    value = 10L;
    feeLimit = 5_000_000_000L;
    long expectEnergyLimit5 = 30_014_999L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit5);

  }

  @Test
  public void getCallerAndCreatorEnergyLimit2With40PercentTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {

    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 40L;
    long creatorEnergyLimit = 5_000L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;
    TVMTestResult result = TVMTestUtils
        .deployContractWithCreatorEnergyLimitAndReturnTVMTestResult(contractName, creatorAddress,
            ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, dbManager, null,
            creatorEnergyLimit);

    byte[] contractAddress = result.getContractAddress();
    byte[] triggerData = TVMTestUtils.parseABI("testNotConstant()", null);
    Transaction trx = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        triggerData, value, feeLimit);

    deposit = DepositImpl.createRoot(dbManager);
    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    AccountCapsule creatorAccount = deposit.getAccount(creatorAddress);
    AccountCapsule callerAccount = deposit.getAccount(callerAddress);
    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectEnergyLimit1 = 10_000_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit1);

    long creatorFrozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - creatorFrozenBalance;
    creatorAccount.setFrozenForEnergy(creatorFrozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectEnergyLimit2 = 10_005_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit2);

    value = 3_999_950_000L;
    long expectEnergyLimit3 = 1_250L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit3);

  }

  @Test
  public void getCallerAndCreatorEnergyLimit2With100PercentTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {

    long value = 0;
    long feeLimit = 1_000_000_000L; // sun
    long consumeUserResourcePercent = 100L;
    long creatorEnergyLimit = 5_000L;
    String contractName = "test";
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"count\",\"type\":\"uint256\"}],\"name\":\"testNotConstant\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String code = "608060405234801561001057600080fd5b50610112806100206000396000f3006080604052600436106049576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806321964a3914604e5780634c6bb6eb146078575b600080fd5b348015605957600080fd5b5060766004803603810190808035906020019092919050505060a2565b005b348015608357600080fd5b5060a06004803603810190808035906020019092919050505060c4565b005b600080600091505b8282101560bf576001905060018201915060aa565b505050565b600080600091505b8282101560e1576001905060018201915060cc565b5050505600a165627a7a72305820267cf0ebf31051a92ff62bed7490045b8063be9f1e1a22d07dce257654c8c17b0029";
    String libraryAddressPair = null;
    TVMTestResult result = TVMTestUtils
        .deployContractWithCreatorEnergyLimitAndReturnTVMTestResult(contractName, creatorAddress,
            ABI, code, value,
            feeLimit, consumeUserResourcePercent, libraryAddressPair, dbManager, null,
            creatorEnergyLimit);

    byte[] contractAddress = result.getContractAddress();
    byte[] triggerData = TVMTestUtils.parseABI("testNotConstant()", null);
    Transaction trx = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        triggerData, value, feeLimit);

    deposit = DepositImpl.createRoot(dbManager);
    RuntimeImpl runtimeImpl = new RuntimeImpl(trx, null, deposit, new ProgramInvokeFactoryImpl(),
        true);

    AccountCapsule creatorAccount = deposit.getAccount(creatorAddress);
    AccountCapsule callerAccount = deposit.getAccount(callerAddress);
    Contract.TriggerSmartContract contract = ContractCapsule.getTriggerContractFromTransaction(trx);

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectEnergyLimit1 = 10_000_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit1);

    long creatorFrozenBalance = 1_000_000_000L;
    long newBalance = creatorAccount.getBalance() - creatorFrozenBalance;
    creatorAccount.setFrozenForEnergy(creatorFrozenBalance, 0L);
    creatorAccount.setBalance(newBalance);
    deposit.putAccountValue(creatorAddress, creatorAccount);
    deposit.commit();

    feeLimit = 1_000_000_000L;
    value = 0L;
    long expectEnergyLimit2 = 10_000_000L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit2);

    value = 3_999_950_000L;
    long expectEnergyLimit3 = 500L;
    Assert.assertEquals(
        runtimeImpl.getTotalEnergyLimitWithFixRatio(creatorAccount, callerAccount, contract, feeLimit, value),
        expectEnergyLimit3);

  }

  /**
   * Release resources.
   */
  @After
  public void destroy() {
    Args.clearParam();
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }
}

