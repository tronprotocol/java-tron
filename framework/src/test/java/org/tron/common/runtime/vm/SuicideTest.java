package org.tron.common.runtime.vm;

import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;

import javax.print.CancelablePrintJob;
import java.util.ArrayList;
import java.util.Arrays;

public class SuicideTest extends VMTestBase{

    private MaintenanceManager maintenanceManager;

    @Before
    public void before(){
        ConsensusService consensusService = context.getBean(ConsensusService.class);
        consensusService.start();
        maintenanceManager = context.getBean(MaintenanceManager.class);

        ConfigLoader.disable = true;
        VMConfig.initAllowTvmTransferTrc10(1);
        VMConfig.initAllowTvmConstantinople(1);
        VMConfig.initAllowTvmSolidity059(1);
        VMConfig.initAllowTvmStake(1);
        manager.getDynamicPropertiesStore().saveChangeDelegation(1);
    }
    /*
pragma solidity ^0.5.0;
contract TestStake{

constructor() payable public{}

function selfdestructTest(address payable target) public{
  selfdestruct(target);
}

function selfdestructTest2(address sr, uint256 amount, address payable target) public{
stake(sr, amount);
selfdestruct(target);
}

function Stake(address sr, uint256 amount) public returns (bool result){
return stake(sr, amount);
}
function UnStake() public returns (bool result){
return unstake();
}
}
 */
    @Test
    public void testSuicide() throws Exception{
        String contractName = "TestSuicide";
        byte[] ownerAddress = Hex.decode(OWNER_ADDRESS);
        String ownerAddressStr = StringUtil.encode58Check(ownerAddress);
        String ABI = "[{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"}],\"name\":\"Stake\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"result\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"UnStake\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"result\",\"type\":\"bool\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address payable\",\"name\":\"target\",\"type\":\"address\"}],\"name\":\"selfdestructTest\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"internalType\":\"address\",\"name\":\"sr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"},{\"internalType\":\"address payable\",\"name\":\"target\",\"type\":\"address\"}],\"name\":\"selfdestructTest2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
        String factoryCode = "608060405261024a806100136000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100665760003560e01c8063377bdd4c1461006b57806338e8221f146100af578063ebedb8b31461011d578063ecb9061514610183575b600080fd5b6100ad6004803603602081101561008157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506101a5565b005b61011b600480360360608110156100c557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506101be565b005b6101696004803603604081101561013357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506101db565b604051808215151515815260200191505060405180910390f35b61018b6101e8565b604051808215151515815260200191505060405180910390f35b8073ffffffffffffffffffffffffffffffffffffffff16ff5b8183d5508073ffffffffffffffffffffffffffffffffffffffff16ff5b60008183d5905092915050565b6000d690509056fea26474726f6e582003e023985836e07a7f23202dfc410017c52159ee1ee3968435e9917f83f8d5a164736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
        long feeLimit = 100000000;

        String witnessAddrStr = "27Ssb1WE8FArwJVRRb8Dwy3ssVGuLY8L3S1";
        byte[] witnessAddr = Hex.decode("a0299f3db80a24b20a254b89ce639d59132f157f13");
        String obtainUserAddrStr = "27k66nycZATHzBasFT9782nTsYWqVtxdtAc";
        byte[] obtainUserAddr = Hex.decode("A0E6773BBF60F97D22AA3BF73D2FE235E816A1964F");

        // suicide after stake (freeze not expire)
        // deploy contract
        Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, ownerAddress, ABI, factoryCode, 100000000, feeLimit, 0,
                null);
        byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
        String factoryAddressStr = StringUtil.encode58Check(factoryAddress);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
        Assert.assertNull(runtime.getRuntimeError());
        String hexInput = AbiUtil.parseMethod("Stake(address,uint256)", Arrays.asList(witnessAddrStr, 10000000));
        TVMTestResult result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        byte[] returnValue = result.getRuntime().getResult().getHReturn();
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0000000000000000000000000000000000000000000000000000000000000001");
        Protocol.Account.Frozen frozen1 = manager.getAccountStore().get(factoryAddress).getFrozenList().get(0);
        //do maintain
        maintenanceManager.doMaintenance();
        hexInput = AbiUtil.parseMethod("selfdestructTest(address)", Arrays.asList(obtainUserAddrStr));
        result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        AccountCapsule obtainAccount = manager.getAccountStore().get(obtainUserAddr);
        Assert.assertEquals(obtainAccount.getBalance(), 90000000);
        Assert.assertEquals(obtainAccount.getFrozenBalance(), 10000000);
        Assert.assertEquals(obtainAccount.getFrozenList().get(0).getExpireTime(), frozen1.getExpireTime());

        // suicide to a staked account
        trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, ownerAddress, ABI, factoryCode, 100000000, feeLimit, 0,
                null);
        factoryAddress = WalletUtil.generateContractAddress(trx);
        factoryAddressStr = StringUtil.encode58Check(factoryAddress);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
        Assert.assertNull(runtime.getRuntimeError());
        hexInput = AbiUtil.parseMethod("Stake(address,uint256)", Arrays.asList(witnessAddrStr, 10000000));
        result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        returnValue = result.getRuntime().getResult().getHReturn();
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0000000000000000000000000000000000000000000000000000000000000001");
        frozen1 = manager.getAccountStore().get(factoryAddress).getFrozenList().get(0);
        maintenanceManager.doMaintenance();
        Protocol.Account.Frozen frozen2 = manager.getAccountStore().get(obtainUserAddr).getFrozenList().get(0);
        hexInput = AbiUtil.parseMethod("selfdestructTest(address)", Arrays.asList(obtainUserAddrStr));
        result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        obtainAccount = manager.getAccountStore().get(obtainUserAddr);
        Assert.assertEquals(obtainAccount.getBalance(), 180000000);
        Assert.assertEquals(obtainAccount.getFrozenBalance(), 20000000);
        Assert.assertEquals(obtainAccount.getFrozenList().get(0).getExpireTime(),
                (frozen1.getExpireTime()*frozen1.getFrozenBalance()
                        +frozen2.getExpireTime()*frozen2.getFrozenBalance())
                        /(frozen1.getFrozenBalance()+frozen2.getFrozenBalance()));

        //suicide to staked contract
        //deploy contract1
        trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, ownerAddress, ABI, factoryCode, 100000000, feeLimit, 0,
                null);
        factoryAddress = WalletUtil.generateContractAddress(trx);
        factoryAddressStr = StringUtil.encode58Check(factoryAddress);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
        Assert.assertNull(runtime.getRuntimeError());
        //deploy contract obtain
        trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                "contractObtain", ownerAddress, ABI, factoryCode, 100000000, feeLimit, 0,
                null);
        byte[] obtainContractAddr = WalletUtil.generateContractAddress(trx);
        String obtainContractAddrStr = StringUtil.encode58Check(obtainContractAddr);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, rootDeposit, null);
        Assert.assertNull(runtime.getRuntimeError());
        //factoryAddress Stake
        hexInput = AbiUtil.parseMethod("Stake(address,uint256)", Arrays.asList(witnessAddrStr, 10000000));
        result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        returnValue = result.getRuntime().getResult().getHReturn();
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0000000000000000000000000000000000000000000000000000000000000001");
        //obtainContractAddr Stake
        hexInput = AbiUtil.parseMethod("Stake(address,uint256)", Arrays.asList(witnessAddrStr, 10000000));
        result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        obtainContractAddr, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        returnValue = result.getRuntime().getResult().getHReturn();
        Assert.assertEquals(Hex.toHexString(returnValue),
                "0000000000000000000000000000000000000000000000000000000000000001");
        frozen1 = manager.getAccountStore().get(factoryAddress).getFrozenList().get(0);
        frozen2 = manager.getAccountStore().get(obtainContractAddr).getFrozenList().get(0);
        maintenanceManager.doMaintenance();
        hexInput = AbiUtil.parseMethod("selfdestructTest(address)", Arrays.asList(obtainContractAddrStr));
        result = TvmTestUtils
                .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                        factoryAddress, Hex.decode(hexInput), 0, feeLimit, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        obtainAccount = manager.getAccountStore().get(obtainContractAddr);
        Assert.assertEquals(obtainAccount.getBalance(), 180000000);
        Assert.assertEquals(obtainAccount.getFrozenBalance(), 20000000);
        Assert.assertEquals(obtainAccount.getFrozenList().get(0).getExpireTime(),
                (frozen1.getExpireTime()*frozen1.getFrozenBalance()
                        +frozen2.getExpireTime()*frozen2.getFrozenBalance())
                        /(frozen1.getFrozenBalance()+frozen2.getFrozenBalance()));

    }
}
