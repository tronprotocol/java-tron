package org.tron.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TVMTestUtils;
import org.tron.common.storage.Deposit;
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
import org.tron.common.runtime.Runtime;

@Slf4j

public class StorageTest {
  private Manager manager;
  private TronApplicationContext context;
  private String dbPath = "output_VMStorageTest";
  private DepositImpl deposit;
  private String OWNER_ADDRESS;
  private Runtime runtime;


  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);

    context = new TronApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    manager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(manager);
    deposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    deposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);

    deposit.commit();
  }

  @After
  public void destroy() {
    Args.clearParam();
    ApplicationFactory.create(context).shutdown();
    ApplicationFactory.create(context).shutdownServices();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
    context.destroy();
  }

  @Test
  public void writeAndCommit() {
    byte[] address = Hex.decode(OWNER_ADDRESS);
    DataWord storageKey1 = new DataWord("key1".getBytes());
    DataWord storageVal1 = new DataWord("val1".getBytes());
    DataWord nullKey = new DataWord("nullkey".getBytes());
    DataWord nullValue = new DataWord(0);

    deposit.putStorageValue(address, storageKey1, storageVal1);
    deposit.putStorageValue(address, nullKey, nullValue);

    // test cache
    Assert.assertEquals(deposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(deposit.getStorageValue(address, nullKey), nullValue);
    deposit.commit();

    // use a new deposit
    Deposit deposit1 = DepositImpl.createRoot(manager);
    Assert.assertEquals(deposit1.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertNull(deposit1.getStorageValue(address, nullKey));

    // delete key
    deposit1.putStorageValue(address, storageKey1, nullValue);
    Assert.assertNotNull(deposit1.getStorageValue(address, storageKey1));
    deposit1.commit();
    Deposit deposit2 = DepositImpl.createRoot(manager);
    Assert.assertNull(deposit2.getStorageValue(address, storageKey1));
  }

  @Test
  public void writeWithoutCommit() {
    byte[] address = Hex.decode(OWNER_ADDRESS);
    DataWord storageKey1 = new DataWord("key1".getBytes());
    DataWord storageVal1 = new DataWord("val1".getBytes());
    DataWord nullKey = new DataWord("nullkey".getBytes());
    DataWord nullValue = new DataWord(0);

    deposit.putStorageValue(address, storageKey1, storageVal1);
    deposit.putStorageValue(address, nullKey, nullValue);
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, nullKey));
    deposit.commit();
    Assert.assertEquals(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1), storageVal1);
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, nullKey));
  }

/*
  pragma solidity ^0.4.0;
  contract StorageDemo{
    mapping(uint => string) public int2str;

    function testPut(uint256 i, string s) public {
      int2str[i] = s;
    }

    function testDelete(uint256 i) public {
      delete int2str[i];
    }
  }
*/
  @Test
  public void contractWriteAndDeleteStorage()
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    String contractName = "contractWriteAndDeleteStorage";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"int2str\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"uint256\"}],\"name\":\"testDelete\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"uint256\"},{\"name\":\"s\",\"type\":\"string\"}],\"name\":\"testPut\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]\n";
    String code = "608060405234801561001057600080fd5b50610341806100206000396000f3006080604052600436106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166313d821f4811461005b57806330099fa9146100e8578063c38e31cc14610102575b600080fd5b34801561006757600080fd5b50610073600435610160565b6040805160208082528351818301528351919283929083019185019080838360005b838110156100ad578181015183820152602001610095565b50505050905090810190601f1680156100da5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156100f457600080fd5b506101006004356101fa565b005b34801561010e57600080fd5b5060408051602060046024803582810135601f81018590048502860185019096528585526101009583359536956044949193909101919081908401838280828437509497506102149650505050505050565b600060208181529181526040908190208054825160026001831615610100026000190190921691909104601f8101859004850282018501909352828152929091908301828280156101f25780601f106101c7576101008083540402835291602001916101f2565b820191906000526020600020905b8154815290600101906020018083116101d557829003601f168201915b505050505081565b600081815260208190526040812061021191610236565b50565b60008281526020818152604090912082516102319284019061027a565b505050565b50805460018160011615610100020316600290046000825580601f1061025c5750610211565b601f01602090049060005260206000209081019061021191906102f8565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106102bb57805160ff19168380011785556102e8565b828001600101855582156102e8579182015b828111156102e85782518255916020019190600101906102cd565b506102f49291506102f8565b5090565b61031291905b808211156102f457600081556001016102fe565b905600a165627a7a72305820c98643943ea978505f9cca68bdf61681462daeee9f71a6aa4414609e48dbb46b0029";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null);
    byte[] contractAddress = Wallet.generateContractAddress(trx);
    runtime = TVMTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // write storage
    // testPut(uint256,string) 1,"abc"
    // 1,"abc"
    String params1 = "0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000036162630000000000000000000000000000000000000000000000000000000000";
    String params2 = "0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000033132330000000000000000000000000000000000000000000000000000000000";
    byte[] triggerData = TVMTestUtils.parseABI("testPut(uint256,string)", params1);
    TVMTestResult result =  TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);

    Assert.assertNull(result.getRuntime().getRuntimeError());


    // overwrite storage with same value
    // testPut(uint256,string) 1,"abc"
    triggerData = TVMTestUtils.parseABI("testPut(uint256,string)", params1);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);

    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 10855);

    // overwrite storage with same value
    // testPut(uint256,string) 1,"123"


    triggerData = TVMTestUtils.parseABI("testPut(uint256,string)", params2);
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);

    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 10855);

    // delete storage
    // testDelete(uint256) 1
    triggerData = TVMTestUtils.parseABI("testDelete(uint256)", "0000000000000000000000000000000000000000000000000000000000000001");
    result = TVMTestUtils
        .triggerContractAndReturnTVMTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertNull(result.getRuntime().getResult().getException());
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 5389);
  }

}
