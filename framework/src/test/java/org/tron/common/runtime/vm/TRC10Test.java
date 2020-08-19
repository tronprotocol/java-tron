package org.tron.common.runtime.vm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.runtime.TVMTestResult;
import org.tron.common.runtime.TvmTestUtils;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.WalletUtil;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.DelegationService;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.store.StoreFactory;
import org.tron.core.store.WitnessStore;
import org.tron.core.vm.config.ConfigLoader;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.core.vm.repository.RepositoryImpl;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.utils.AbiUtil;

import java.util.Arrays;

import static stest.tron.wallet.common.client.utils.PublicMethed.decode58Check;

@Slf4j
public class TRC10Test extends VMContractTestBase {

    /*pragma solidity ^0.5.12;
    contract HelloWorld{
        function TokenIssue(bytes32 name, bytes32 abbr, uint64
        totalSupply, uint8 precision) public returns (uint) {
            return assetissue(name, abbr, totalSupply, precision);
        }
        function UpdateAsset(trcToken tokenId, string memory desc, string
        memory url) public {
            updateasset(tokenId, bytes(desc), bytes(url));
        }
    }*/

    @Test
    public void testTRC10Validate() throws ContractExeException,
            ReceiptCheckErrException
            , VMIllegalException,
            ContractValidateException {
        ConfigLoader.disable = true;
        VMConfig.initAllowTvmTransferTrc10(1);
        VMConfig.initAllowTvmConstantinople(1);
        VMConfig.initAllowTvmSolidity059(1);
        VMConfig.initAllowTvmAssetIssue(1);
        manager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        String contractName = "AssetIssueTest";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI = "[{\"constant\":false," +
                "\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"name\"," +
                "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\"," +
                "\"name\":\"abbr\",\"type\":\"bytes32\"}," +
                "{\"internalType\":\"uint64\",\"name\":\"totalSupply\"," +
                "\"type\":\"uint64\"},{\"internalType\":\"uint8\"," +
                "\"name\":\"precision\",\"type\":\"uint8\"}]," +
                "\"name\":\"TokenIssue\"," +
                "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\"," +
                "\"type\":\"uint256\"}],\"payable\":false," +
                "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}," +
                "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken" +
                "\",\"name\":\"tokenId\",\"type\":\"trcToken\"}," +
                "{\"internalType\":\"string\",\"name\":\"desc\"," +
                "\"type\":\"string\"},{\"internalType\":\"string\"," +
                "\"name\":\"url\",\"type\":\"string\"}]," +
                "\"name\":\"UpdateAsset\",\"outputs\":[],\"payable\":false," +
                "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
        String factoryCode =
                "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506102938061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c80633615673e14610055578063f177bc7a146100c2575b600080fd5b6100ac6004803603608081101561006b57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffffff169060200190929190803560ff16906020019092919050505061021e565b6040518082815260200191505060405180910390f35b61021c600480360360608110156100d857600080fd5b8101908080359060200190929190803590602001906401000000008111156100ff57600080fd5b82018360208201111561011157600080fd5b8035906020019184600183028401116401000000008311171561013357600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561019657600080fd5b8201836020820111156101a857600080fd5b803590602001918460018302840111640100000000831117156101ca57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929050505061022f565b005b600081838587da9050949350505050565b808284db5050505056fea26474726f6e5820def53e9fef23475f8f3316cfaf66a0015e2205fe9bf0eba01f703e30ef9d732364736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
        long value = 0;
        long fee = 100000000;
        long consumeUserResourcePercent = 0;
        // deploy contract
        Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
                null);
        byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);;
        Assert.assertNull(runtime.getRuntimeError());

        // validate balance is enough
        String tokenP1 = "74657374";
        String tokenP2 = tokenP1;
        long tokenP3 = 1000;
        long tokenP4 = 2;
        String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
        String hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4));
        TVMTestResult result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        rootRepository.addBalance(factoryAddress, 10000000000L);
        rootRepository.commit();
        Assert.assertEquals(rootRepository.getBalance(factoryAddress), 10000000000L);

        // validate assetissue assetname can't be trx
        tokenP1 = "747278";
        tokenP2 = tokenP1;
        tokenP3 = 1000;
        tokenP4 = 2;
        methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
        hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        // validate assetissue precision can't more than 6
        tokenP1 = "74657374";
        tokenP2 = tokenP1;
        tokenP3 = 1000;
        tokenP4 = 7;
        methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
        hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        // trigger contract success
        tokenP1 = "74657374";
        tokenP2 = tokenP1;
        tokenP3 = 1000;
        tokenP4 = 2;
        methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
        hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 1000001);
        long expectEnergyUsageTotal = 25000;
        long expectEnergyUsageTotalMax = 30000;
        long reallyEnergyUsageTotal = result.getReceipt().getEnergyUsageTotal();
        // validate energy cost
        Assert.assertTrue(reallyEnergyUsageTotal > expectEnergyUsageTotal && reallyEnergyUsageTotal < expectEnergyUsageTotalMax);

        // validate assetissue An account can only issue one asset
        tokenP1 = "74657374";
        tokenP2 = tokenP1;
        tokenP3 = 1000;
        tokenP4 = 2;
        methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
        hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        // Trigger contract method: UpdateAsset(trcToken, string, string)
        String methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
        // 1,abc,abc,
        long updateP1 = 1000001;
        String updateP2 = "abc";
        String updateP3 = updateP2;
        hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP3));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
        Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues().get(0).getUrl().toByteArray()),
                "abc");
        Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues().get(0).getDesc().toByteArray()),
                "abc");
        // validate energy cost
        expectEnergyUsageTotal = 5000;
        expectEnergyUsageTotalMax = 10000;
        reallyEnergyUsageTotal = result.getReceipt().getEnergyUsageTotal();

        Assert.assertTrue(reallyEnergyUsageTotal > expectEnergyUsageTotal && reallyEnergyUsageTotal < expectEnergyUsageTotalMax);

        // validate desc less than 200
        updateP1 = 1000001;
        updateP2 =
                "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcababcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcababcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcababcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcab";
        updateP3 = "efg";
        hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP3));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues().get(0).getUrl().toByteArray()),
                "abc");
        Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues().get(0).getDesc().toByteArray()),
                "abc");
        Assert.assertNull(result.getRuntime().getRuntimeError());

        // validate url less than 256
        updateP1 = 1000001;
        updateP3 = "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcaabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcaabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcaabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabca";
        updateP2 = "efg";
        hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP3));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());
    }

    /*pragma solidity ^0.5.12;
    contract HelloWorld{

        function TokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public {
            assetissue(name, abbr, totalSupply, precision);
            assetissue(name, abbr, totalSupply, precision);
        }
        function UpdateAsset(trcToken tokenId, string memory desc, string memory url) public {
            updateasset(tokenId, bytes(desc), bytes(url));
            updateasset(tokenId, bytes(desc), bytes(url));
        }
    }*/

    @Test
    public void testTRC10ForMultiCall() throws ContractExeException,
            ReceiptCheckErrException
            , VMIllegalException,
            ContractValidateException {
        ConfigLoader.disable = true;
        VMConfig.initAllowTvmTransferTrc10(1);
        VMConfig.initAllowTvmConstantinople(1);
        VMConfig.initAllowTvmSolidity059(1);
        VMConfig.initAllowTvmAssetIssue(1);
        manager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        String contractName = "AssetIssueTest";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI = "[{\"constant\":false,\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"name\"," +
                "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"abbr\",\"type\":\"bytes32\"}," +
                "{\"internalType\":\"uint64\",\"name\":\"totalSupply\",\"type\":\"uint64\"}," +
                "{\"internalType\":\"uint8\",\"name\":\"precision\",\"type\":\"uint8\"}],\"name\":\"TokenIssue\"," +
                "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}," +
                "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken\",\"name\":\"tokenId\"," +
                "\"type\":\"trcToken\"},{\"internalType\":\"string\",\"name\":\"desc\",\"type\":\"string\"}," +
                "{\"internalType\":\"string\",\"name\":\"url\",\"type\":\"string\"}],\"name\":\"UpdateAsset\"," +
                "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
        String factoryCode =
                "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506102858061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c80633615673e14610055578063f177bc7a146100ae575b600080fd5b6100ac6004803603608081101561006b57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffffff169060200190929190803560ff16906020019092919050505061020a565b005b610208600480360360608110156100c457600080fd5b8101908080359060200190929190803590602001906401000000008111156100eb57600080fd5b8201836020820111156100fd57600080fd5b8035906020019184600183028401116401000000008311171561011f57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561018257600080fd5b82018360208201111561019457600080fd5b803590602001918460018302840111640100000000831117156101b657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929050505061021c565b005b80828486da5080828486da5050505050565b808284db50808284db5050505056fea26474726f6e582010b72c10cc2d93a0ddc6ec14e8a40345f73a2263120bd04e1552687570eb950c64736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
        long value = 0;
        long fee = 100000000;
        long consumeUserResourcePercent = 0;
        // deploy contract
        Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
                null);
        byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);;
        Assert. assertNull(runtime.getRuntimeError());

        // send coin
        rootRepository.addBalance(factoryAddress, 10000000000L);
        rootRepository.commit();
        Assert.assertEquals(rootRepository.getBalance(factoryAddress), 10000000000L);

        // validate updateasset  Asset is not existed in AssetIssueStore
        String methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
        // 1,abc,abc,
        long updateP1 = 1000001;
        String updateP2 = "616263";
        String updateP3 = updateP2;
        String hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP3));
        TVMTestResult result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        // multicall tokenissue
        String tokenP1 = "74657374";
        String tokenP2 = tokenP1;
        long tokenP3 = 1000;
        long tokenP4 = 2;
        String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
        hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
        Assert.assertNull(result.getRuntime().getRuntimeError());

        // multicall updateAsset
        methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
        // 1,abc,abc,
        updateP1 = 1000001;
        updateP2 = "abc";
        updateP3 = updateP2;
        hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP3));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues().get(0).getUrl().toByteArray()),
                "abc");
        Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues().get(0).getDesc().toByteArray()),
                "abc");
        Assert.assertNull(result.getRuntime().getRuntimeError());
    }

    /*pragma solidity ^0.5.12;
    contract HelloWorld{

        function TokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision, address addr) public {
            address payable newaddress = address(uint160(addr));
            newaddress.transfer(100000000);
            assetissue(name, abbr, totalSupply, precision);
            newaddress.transfer(100000000);
        }
        function UpdateAsset(trcToken tokenId, string memory desc, string memory url, address addr) public {
            address payable newaddress = address(uint160(addr));
            newaddress.transfer(100000000);
            updateasset(tokenId, bytes(desc), bytes(url));
            newaddress.transfer(100000000);
        }
    }*/

    @Test
    public void testTRC10Exception() throws ContractExeException,
            ReceiptCheckErrException
            , VMIllegalException,
            ContractValidateException {
        ConfigLoader.disable = true;
        VMConfig.initAllowTvmTransferTrc10(1);
        VMConfig.initAllowTvmConstantinople(1);
        VMConfig.initAllowTvmSolidity059(1);
        VMConfig.initAllowTvmAssetIssue(1);
        manager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
        String contractName = "AssetIssueTest";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI = "[{\"constant\":false,\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"name\"," +
                "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"abbr\",\"type\":\"bytes32\"}," +
                "{\"internalType\":\"uint64\",\"name\":\"totalSupply\",\"type\":\"uint64\"}," +
                "{\"internalType\":\"uint8\",\"name\":\"precision\",\"type\":\"uint8\"}," +
                "{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"TokenIssue\"," +
                "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}," +
                "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken\",\"name\":\"tokenId\"," +
                "\"type\":\"trcToken\"},{\"internalType\":\"string\",\"name\":\"desc\",\"type\":\"string\"}," +
                "{\"internalType\":\"string\",\"name\":\"url\",\"type\":\"string\"},{\"internalType\":\"address\"," +
                "\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"UpdateAsset\",\"outputs\":[],\"payable\":false," +
                "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
        String factoryCode =
                "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b506103f48061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c8063451ecfa214610055578063a241e431146100ce575b600080fd5b6100cc600480360360a081101561006b57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffffff169060200190929190803560ff169060200190929190803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061024a565b005b610248600480360360808110156100e457600080fd5b81019080803590602001909291908035906020019064010000000081111561010b57600080fd5b82018360208201111561011d57600080fd5b8035906020019184600183028401116401000000008311171561013f57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156101a257600080fd5b8201836020820111156101b457600080fd5b803590602001918460018302840111640100000000831117156101d657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102f3565b005b60008190508073ffffffffffffffffffffffffffffffffffffffff166108fc6305f5e1009081150290604051600060405180830381858888f19350505050158015610299573d6000803e3d6000fd5b5082848688da508073ffffffffffffffffffffffffffffffffffffffff166108fc6305f5e1009081150290604051600060405180830381858888f193505050501580156102ea573d6000803e3d6000fd5b50505050505050565b60008190508073ffffffffffffffffffffffffffffffffffffffff166108fc6305f5e1009081150290604051600060405180830381858888f19350505050158015610342573d6000803e3d6000fd5b50828486db508073ffffffffffffffffffffffffffffffffffffffff166108fc6305f5e1009081150290604051600060405180830381858888f19350505050158015610392573d6000803e3d6000fd5b50505050505056fea26474726f6e5820d0f6e502979b80da5c34f790aebc578b6eec15430790eb7e73068a6f3ca24dc864736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
        long value = 0;
        long fee = 100000000;
        long consumeUserResourcePercent = 0;
        // deploy contract
        Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
                null);
        byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);;
        Assert. assertNull(runtime.getRuntimeError());

        // sendcoin to factoryAddress 100000000000
        rootRepository.addBalance(factoryAddress, 10000000000L);
        rootRepository.commit();
        Assert.assertEquals(rootRepository.getBalance(factoryAddress), 10000000000L);

        // assetissue exception test
        String tokenP1 = "74657374";
        String tokenP2 = tokenP1;
        long tokenP3 = 1000;
        long tokenP4 = 7;
        String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8,address)";
        String recieveAddress = "TKVFQvsiztpQtX3R7CF2q6Y25AWNE7EEJu";
        String hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3, tokenP4, recieveAddress));
        TVMTestResult result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertNotNull(result.getRuntime().getRuntimeError());
        Assert.assertEquals(200000000,
                rootRepository.getBalance(decode58Check(recieveAddress)));

        /*// updateasset exception test
        String methodUpdateAsset = "UpdateAsset(trcToken,string,string,address)";
        // 1,abc,abc,
        long updateP1 = 1000001;
        String updateP2 = "616263";
        String updateP3 = updateP2;
        hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP3,recieveAddress));
        result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
                factoryAddress,
                Hex.decode(hexInput), value, fee, manager, null);
        Assert.assertNotNull(result.getRuntime().getRuntimeError());
        Assert.assertEquals(400000000, rootRepository.getBalance(decode58Check(recieveAddress)));*/
    }

    /*pragma solidity ^0.5.0;

    contract A {

        function TokenIssueA(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public {
            assetissue(name, abbr, totalSupply, precision);
        }

        function UpdateAssetA(trcToken tokenId, string memory desc, string memory url) public {
            updateasset(tokenId, bytes(desc), bytes(url));
        }

    }
    contract HelloWorld {

        A a = new A();
        function TokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision) public {
            a.TokenIssueA(name, abbr, totalSupply, precision);
        }
        function UpdateAsset(trcToken tokenId, string memory desc, string memory url) public {
            a.UpdateAssetA(tokenId, desc, url);
        }

    }*/

    @Test
    public void testTRC10ContractCall() throws ContractExeException,
            ReceiptCheckErrException
            , VMIllegalException,
            ContractValidateException {
        ConfigLoader.disable = true;
        VMConfig.initAllowTvmTransferTrc10(1);
        VMConfig.initAllowTvmConstantinople(1);
        VMConfig.initAllowTvmSolidity059(1);
        VMConfig.initAllowTvmAssetIssue(1);
        String contractName = "AssetIssueTest";
        byte[] address = Hex.decode(OWNER_ADDRESS);
        String ABI = "[{\"constant\":false,\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"name\"," +
                "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"abbr\",\"type\":\"bytes32\"}," +
                "{\"internalType\":\"uint64\",\"name\":\"totalSupply\",\"type\":\"uint64\"}," +
                "{\"internalType\":\"uint8\",\"name\":\"precision\",\"type\":\"uint8\"}],\"name\":\"TokenIssue\"," +
                "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}," +
                "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken\",\"name\":\"tokenId\"," +
                "\"type\":\"trcToken\"},{\"internalType\":\"string\",\"name\":\"desc\",\"type\":\"string\"}," +
                "{\"internalType\":\"string\",\"name\":\"url\",\"type\":\"string\"}],\"name\":\"UpdateAsset\"," +
                "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
        String factoryCode =
                "608060405260405161001090610098565b604051809103906000f08015801561002c573d6000803e3d6000fd5b506000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555034801561007857600080fd5b50d3801561008557600080fd5b50d2801561009257600080fd5b506100a5565b6102b48061054783390190565b610493806100b46000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c80633615673e14610055578063f177bc7a146100ae575b600080fd5b6100ac6004803603608081101561006b57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffffff169060200190929190803560ff16906020019092919050505061020a565b005b610208600480360360608110156100c457600080fd5b8101908080359060200190929190803590602001906401000000008111156100eb57600080fd5b8201836020820111156100fd57600080fd5b8035906020019184600183028401116401000000008311171561011f57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561018257600080fd5b82018360208201111561019457600080fd5b803590602001918460018302840111640100000000831117156101b657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506102ce565b005b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166329f00e59858585856040518563ffffffff1660e01b8152600401808581526020018481526020018367ffffffffffffffff1667ffffffffffffffff1681526020018260ff1660ff168152602001945050505050600060405180830381600087803b1580156102b057600080fd5b505af11580156102c4573d6000803e3d6000fd5b5050505050505050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cfa62e758484846040518463ffffffff1660e01b8152600401808481526020018060200180602001838103835285818151815260200191508051906020019080838360005b8381101561036757808201518184015260208101905061034c565b50505050905090810190601f1680156103945780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156103cd5780820151818401526020810190506103b2565b50505050905090810190601f1680156103fa5780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b15801561041c57600080fd5b505af1158015610430573d6000803e3d6000fd5b5050505050505056fea26474726f6e582096668d49ad0d30379832d1a56c2158d58ba09b7718b35f55df923cf09a04f99864736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5061027a8061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c806329f00e5914610055578063cfa62e75146100ae575b600080fd5b6100ac6004803603608081101561006b57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffffff169060200190929190803560ff16906020019092919050505061020a565b005b610208600480360360608110156100c457600080fd5b8101908080359060200190929190803590602001906401000000008111156100eb57600080fd5b8201836020820111156100fd57600080fd5b8035906020019184600183028401116401000000008311171561011f57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561018257600080fd5b82018360208201111561019457600080fd5b803590602001918460018302840111640100000000831117156101b657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610216565b005b80828486da5050505050565b808284db5050505056fea26474726f6e58203b97fc304f1416a7ed32a962fc3fd0005847631d91cedd6f7c799126efc1477064736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
        long value = 0;
        long fee = 100000000;
        long consumeUserResourcePercent = 0;
        // deploy contract
        Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
                contractName, address, ABI, factoryCode, value, fee, consumeUserResourcePercent,
                null);
        byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);;
        Assert. assertNull(runtime.getRuntimeError());
        runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);;
        Assert.assertNull(runtime.getRuntimeError());
    }

}



