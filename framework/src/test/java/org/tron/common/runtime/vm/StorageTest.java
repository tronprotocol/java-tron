package org.tron.common.runtime.vm;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.WalletUtil;
import org.tron.core.config.Parameter.ForkBlockVersionConsts;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol.Transaction;

@Slf4j
public class StorageTest extends VMTestBase {


  @Test
  public void writeAndCommit() {
    byte[] address = Hex.decode(OWNER_ADDRESS);
    DataWord storageKey1 = new DataWord("key1".getBytes());
    DataWord storageVal1 = new DataWord("val1".getBytes());
    DataWord nullKey = new DataWord("nullkey".getBytes());
    DataWord nullValue = new DataWord(0);

    rootDeposit.putStorageValue(address, storageKey1, storageVal1);
    rootDeposit.putStorageValue(address, nullKey, nullValue);

    // test cache
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(rootDeposit.getStorageValue(address, nullKey), nullValue);
    rootDeposit.commit();

    // use a new rootDeposit
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

    rootDeposit.putStorageValue(address, storageKey1, storageVal1);
    rootDeposit.putStorageValue(address, nullKey, nullValue);
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, nullKey));
    rootDeposit.commit();
    Assert.assertEquals(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1),
        storageVal1);
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
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException,
      VMIllegalException {
    String contractName = "contractWriteAndDeleteStorage";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":"
        + "\"int2str\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\""
        + ":[{\"name\":\"i\",\"type\":\"uint256\"}],\"name\":\"testDelete\",\"outputs\":[],"
        + "\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"uint256\"},{\"name\":\"s\","
        + "\"type\":\"string\"}],\"name\":\"testPut\",\"outputs\":[],\"payable\":false,"
        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]\n";

    String code = "608060405234801561001057600080fd5b50610341806100206000396000f3006080604052600436"
        + "106100565763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504"
        + "166313d821f4811461005b57806330099fa9146100e8578063c38e31cc14610102575b600080fd5b34801561"
        + "006757600080fd5b50610073600435610160565b604080516020808252835181830152835191928392908301"
        + "9185019080838360005b838110156100ad578181015183820152602001610095565b50505050905090810190"
        + "601f1680156100da5780820380516001836020036101000a031916815260200191505b509250505060405180"
        + "910390f35b3480156100f457600080fd5b506101006004356101fa565b005b34801561010e57600080fd5b50"
        + "60408051602060046024803582810135601f8101859004850286018501909652858552610100958335953695"
        + "6044949193909101919081908401838280828437509497506102149650505050505050565b60006020818152"
        + "9181526040908190208054825160026001831615610100026000190190921691909104601f81018590048502"
        + "82018501909352828152929091908301828280156101f25780601f106101c757610100808354040283529160"
        + "2001916101f2565b820191906000526020600020905b8154815290600101906020018083116101d557829003"
        + "601f168201915b505050505081565b600081815260208190526040812061021191610236565b50565b600082"
        + "81526020818152604090912082516102319284019061027a565b505050565b50805460018160011615610100"
        + "020316600290046000825580601f1061025c5750610211565b601f0160209004906000526020600020908101"
        + "9061021191906102f8565b828054600181600116156101000203166002900490600052602060002090601f01"
        + "6020900481019282601f106102bb57805160ff19168380011785556102e8565b828001600101855582156102"
        + "e8579182015b828111156102e85782518255916020019190600101906102cd565b506102f49291506102f856"
        + "5b5090565b61031291905b808211156102f457600081556001016102fe565b905600a165627a7a72305820c9"
        + "8643943ea978505f9cca68bdf61681462daeee9f71a6aa4414609e48dbb46b0029";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null);
    byte[] contractAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // write storage
    // testPut(uint256,string) 1,"abc"
    // 1,"abc"
    String params1 = "00000000000000000000000000000000000000000000000000000000000000010000000000000"
        + "0000000000000000000000000000000000000000000000000400000000000000000000000000000000000000"
        + "0000000000000000000000000036162630000000000000000000000000000000000000000000000000000000"
        + "000";
    String params2 = "00000000000000000000000000000000000000000000000000000000000000010000000000000"
        + "0000000000000000000000000000000000000000000000000400000000000000000000000000000000000000"
        + "0000000000000000000000000033132330000000000000000000000000000000000000000000000000000000"
        + "000";
    byte[] triggerData = TvmTestUtils.parseAbi("testPut(uint256,string)", params1);
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);

    Assert.assertNull(result.getRuntime().getRuntimeError());

    // overwrite storage with same value
    // testPut(uint256,string) 1,"abc"
    triggerData = TvmTestUtils.parseAbi("testPut(uint256,string)", params1);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);

    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 10855);

    // overwrite storage with same value
    // testPut(uint256,string) 1,"123"

    triggerData = TvmTestUtils.parseAbi("testPut(uint256,string)", params2);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);

    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 10855);

    // delete storage
    // testDelete(uint256) 1
    triggerData = TvmTestUtils.parseAbi("testDelete(uint256)",
        "0000000000000000000000000000000000000000000000000000000000000001");
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            contractAddress, triggerData, 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());
    Assert.assertNull(result.getRuntime().getResult().getException());
    Assert.assertEquals(result.getReceipt().getEnergyUsageTotal(), 5389);
  }

  @Test
  @Ignore
  public void testParentChild() {
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 1);
    this.manager.getDynamicPropertiesStore()
        .statsByVersion(ForkBlockVersionConsts.ENERGY_LIMIT, stats);
    VMConfig.initVmHardFork(true);
    byte[] address = Hex.decode(OWNER_ADDRESS);
    DataWord storageKey1 = new DataWord("key1".getBytes());
    DataWord storageVal1 = new DataWord("val1".getBytes());
    DataWord zeroKey = new DataWord("zero_key".getBytes());
    DataWord zeroValue = new DataWord(0);
    DataWord parentChangedVal = new DataWord("parent_changed_val".getBytes());

    DataWord storageParentKey1 = new DataWord("parent_key1".getBytes());
    DataWord storageParentVal1 = new DataWord("parent_val1".getBytes());
    DataWord storageParentZeroKey = new DataWord("parent_zero_key1".getBytes());

    Deposit chlidDeposit = rootDeposit.newDepositChild();

    // write to root cache
    rootDeposit.putStorageValue(address, storageParentKey1, storageParentVal1);
    rootDeposit.putStorageValue(address, storageParentZeroKey, zeroValue);

    // write to child cache
    chlidDeposit.putStorageValue(address, storageKey1, storageVal1);
    chlidDeposit.putStorageValue(address, zeroKey, zeroValue);

    // check child cache
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, zeroKey), zeroValue);
    Assert
        .assertEquals(chlidDeposit.getStorageValue(address, storageParentKey1), storageParentVal1);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentZeroKey), zeroValue);

    chlidDeposit.putStorageValue(address, storageParentKey1, parentChangedVal);

    // check root cache
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentKey1), storageParentVal1);

    Assert.assertNull(rootDeposit.getStorageValue(address, storageKey1));
    Assert.assertNull(rootDeposit.getStorageValue(address, zeroKey));
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentZeroKey), zeroValue);

    // check db
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, zeroKey));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentKey1));
    Assert
        .assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentZeroKey));

    // commit child cache
    chlidDeposit.commit();

    // check root cache
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(rootDeposit.getStorageValue(address, zeroKey), zeroValue);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentZeroKey), zeroValue);

    // check db
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, zeroKey));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentKey1));
    Assert
        .assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentZeroKey));

    rootDeposit.commit();
    Assert.assertEquals(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1),
        storageVal1);
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, zeroKey));
    Assert.assertEquals(DepositImpl.createRoot(manager).getStorageValue(address, storageParentKey1),
        parentChangedVal);
    Assert
        .assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentZeroKey));
    CommonParameter.setENERGY_LIMIT_HARD_FORK(false);
  }

  @Test
  public void testParentChildOldVersion() {
    byte[] stats = new byte[27];
    Arrays.fill(stats, (byte) 0);
    this.manager.getDynamicPropertiesStore()
        .statsByVersion(ForkBlockVersionConsts.ENERGY_LIMIT, stats);
    byte[] address = Hex.decode(OWNER_ADDRESS);
    DataWord storageKey1 = new DataWord("key1".getBytes());
    DataWord storageVal1 = new DataWord("val1".getBytes());
    DataWord zeroKey = new DataWord("zero_key".getBytes());
    DataWord zeroValue = new DataWord(0);
    DataWord parentChangedVal = new DataWord("parent_changed_val".getBytes());

    DataWord storageParentKey1 = new DataWord("parent_key1".getBytes());
    DataWord storageParentVal1 = new DataWord("parent_val1".getBytes());
    DataWord storageParentZeroKey = new DataWord("parent_zero_key1".getBytes());

    Deposit chlidDeposit = rootDeposit.newDepositChild();

    // write to root cache
    rootDeposit.putStorageValue(address, storageParentKey1, storageParentVal1);
    rootDeposit.putStorageValue(address, storageParentZeroKey, zeroValue);

    // write to child cache
    chlidDeposit.putStorageValue(address, storageKey1, storageVal1);
    chlidDeposit.putStorageValue(address, zeroKey, zeroValue);

    // check child cache
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, zeroKey), zeroValue);
    Assert
        .assertEquals(chlidDeposit.getStorageValue(address, storageParentKey1), storageParentVal1);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentZeroKey), zeroValue);

    chlidDeposit.putStorageValue(address, storageParentKey1, parentChangedVal);

    // check root cache
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);

    Assert.assertEquals(rootDeposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(rootDeposit.getStorageValue(address, zeroKey), zeroValue);
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentZeroKey), zeroValue);

    // check parent deposit == child deposit
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageKey1),
        chlidDeposit.getStorageValue(address, storageKey1));
    Assert.assertEquals(rootDeposit.getStorageValue(address, zeroKey),
        chlidDeposit.getStorageValue(address, zeroKey));
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentKey1),
        chlidDeposit.getStorageValue(address, storageParentKey1));
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentZeroKey),
        chlidDeposit.getStorageValue(address, storageParentZeroKey));

    // check db
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, zeroKey));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentKey1));
    Assert
        .assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentZeroKey));

    // didn't commit child cache
    //    chlidDeposit.commit();

    // check root cache
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageKey1), storageVal1);
    Assert.assertEquals(rootDeposit.getStorageValue(address, zeroKey), zeroValue);
    Assert.assertEquals(rootDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);

    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentKey1), parentChangedVal);
    Assert.assertEquals(chlidDeposit.getStorageValue(address, storageParentZeroKey), zeroValue);

    // check db
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, zeroKey));
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentKey1));
    Assert
        .assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentZeroKey));

    rootDeposit.commit();
    Assert.assertEquals(DepositImpl.createRoot(manager).getStorageValue(address, storageKey1),
        storageVal1);
    Assert.assertNull(DepositImpl.createRoot(manager).getStorageValue(address, zeroKey));
    Assert.assertEquals(DepositImpl.createRoot(manager).getStorageValue(address, storageParentKey1),
        parentChangedVal);
    Assert
        .assertNull(DepositImpl.createRoot(manager).getStorageValue(address, storageParentZeroKey));
    CommonParameter.setENERGY_LIMIT_HARD_FORK(false);
  }
}
