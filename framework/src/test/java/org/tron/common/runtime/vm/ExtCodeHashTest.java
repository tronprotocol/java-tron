package org.tron.common.runtime.vm;

import java.math.BigInteger;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.utils.AbiUtil;

@Slf4j
public class ExtCodeHashTest extends VMTestBase {

  @Test
  public void testExtCodeHash()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException,
      ContractValidateException {
    manager.getDynamicPropertiesStore().saveAllowTvmConstantinople(1);
    String contractName = "TestExtCodeHash";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":true,\"inputs\":[{\"name\":\"_addr\",\"type\":\"uint256\"}],"
        + "\"name\":\"getCodeHashByUint\",\"outputs\":[{\"name\":\"_hash\",\"type\":\"bytes32\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true"
        + ",\"inputs\":[{\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"getCodeHashByAddr\","
        + "\"outputs\":[{\"name\":\"_hash\",\"type\":\"bytes32\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"}]";

    String factoryCode = "608060405234801561001057600080fd5b5061010d806100206000396000f3fe608060405"
        + "2348015600f57600080fd5b506004361060325760003560e01c80637b77fd191460375780637d5e422d14607"
        + "6575b600080fd5b606060048036036020811015604b57600080fd5b810190808035906020019092919050505"
        + "060cb565b6040518082815260200191505060405180910390f35b60b560048036036020811015608a5760008"
        + "0fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505060d65"
        + "65b6040518082815260200191505060405180910390f35b6000813f9050919050565b6000813f90509190505"
        + "6fea165627a7a723058200f30933f006db4e1adeee12c030b87e720dad3cb169769159fc56ec25d9af66f00"
        + "29";
    long value = 0;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;

    // deploy contract
    Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
        contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
        null);
    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
    Assert.assertNull(runtime.getRuntimeError());

    // Trigger contract method: getCodeHashByAddr(address)
    String methodByAddr = "getCodeHashByAddr(address)";
    String nonexistentAccount = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
    String hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(nonexistentAccount));
    TVMTestResult result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    byte[] returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0000000000000000000000000000000000000000000000000000000000000000");

    // trigger deployed contract
    String existentAccount = "27WtBq2KoSy5v8VnVZBZHHJcDuWNiSgjbE3";
    hexInput = AbiUtil.parseMethod(methodByAddr, Arrays.asList(existentAccount));
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");

    // trigger deployed contract
    String methodByUint = "getCodeHashByUint(uint256)";
    byte[] fullHexAddr = new DataWord(factoryAddress).getData();
    hexInput = AbiUtil.parseMethod(methodByUint, Hex.toHexString(fullHexAddr), true);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0837cd5e284138b633cd976ea6fcb719d61d7bc33d946ec5a2d0c7da419a0bd4");

    // trigger deployed contract
    BigInteger bigIntAddr = new DataWord(factoryAddress).sValue();
    String bigIntAddrChange = BigInteger.valueOf(2).pow(160).add(bigIntAddr).toString(16);
    fullHexAddr = new DataWord(bigIntAddrChange).getData();
    hexInput = AbiUtil.parseMethod(methodByUint, Hex.toHexString(fullHexAddr), true);
    result = TvmTestUtils
        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
    Assert.assertNull(result.getRuntime().getRuntimeError());

    returnValue = result.getRuntime().getResult().getHReturn();
    // check deployed contract
    Assert.assertEquals(Hex.toHexString(returnValue),
        "0837cd5e284138b633cd976ea6fcb719d61d7bc33d946ec5a2d0c7da419a0bd4");

  }

}


