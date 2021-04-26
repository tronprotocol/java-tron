//package org.tron.common.runtime.vm;
//
//import static stest.tron.wallet.common.client.utils.PublicMethed.decode58Check;
//
//import java.util.Arrays;
//import java.util.Collections;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.Test;
//import org.spongycastle.util.encoders.Hex;
//import org.testng.Assert;
//import org.tron.common.runtime.TVMTestResult;
//import org.tron.common.runtime.TvmTestUtils;
//import org.tron.common.utils.ByteArray;
//import org.tron.common.utils.WalletUtil;
//import org.tron.core.exception.ContractExeException;
//import org.tron.core.exception.ContractValidateException;
//import org.tron.core.exception.ReceiptCheckErrException;
//import org.tron.core.exception.VMIllegalException;
//import org.tron.core.vm.config.ConfigLoader;
//import org.tron.core.vm.config.VMConfig;
//import org.tron.protos.Protocol;
//import stest.tron.wallet.common.client.utils.AbiUtil;
//
//@Slf4j
//public class Trc10Test extends VMContractTestBase {
//
//  /*pragma solidity ^0.5.12;
//   contract HelloWorld{
//       function TokenIssue(bytes32 name, bytes32 abbr, uint64
//       totalSupply, uint8 precision) public returns (uint) {
//           return assetissue(name, abbr, totalSupply, precision);
//       }
//       function UpdateAsset(trcToken tokenId, string memory url, string
//       memory desc) public {
//           updateasset(tokenId, bytes(url), bytes(desc));
//       }
//   }*/
//  @Test
//  public void testTrc10Validate() throws ContractExeException,
//      ReceiptCheckErrException, VMIllegalException,
//      ContractValidateException {
//    ConfigLoader.disable = true;
//    VMConfig.initAllowTvmTransferTrc10(1);
//    VMConfig.initAllowTvmConstantinople(1);
//    VMConfig.initAllowTvmSolidity059(1);
//    VMConfig.initAllowTvmAssetIssue(1);
//    manager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
//    String contractName = "AssetIssueTest";
//    byte[] address = Hex.decode(OWNER_ADDRESS);
//    String abi = "[{\"constant\":false,"
//        + "\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"name\","
//        + "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\","
//        + "\"name\":\"abbr\",\"type\":\"bytes32\"},"
//        + "{\"internalType\":\"uint64\",\"name\":\"totalSupply\","
//        + "\"type\":\"uint64\"},{\"internalType\":\"uint8\","
//        + "\"name\":\"precision\",\"type\":\"uint8\"}],"
//        + "\"name\":\"TokenIssue\","
//        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\","
//        + "\"type\":\"uint256\"}],\"payable\":false,"
//        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
//        + "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken"
//        + "\",\"name\":\"tokenId\",\"type\":\"trcToken\"},"
//        + "{\"internalType\":\"string\",\"name\":\"desc\","
//        + "\"type\":\"string\"},{\"internalType\":\"string\","
//        + "\"name\":\"url\",\"type\":\"string\"}],"
//        + "\"name\":\"UpdateAsset\",\"outputs\":[],\"payable\":false,"
//        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
//    String factoryCode =
//        "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a576000"
//            + "80fd5b506102938061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561"
//            + "001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c80633615"
//            + "673e14610055578063f177bc7a146100c2575b600080fd5b6100ac6004803603608081101561006b"
//            + "57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffff"
//            + "ff169060200190929190803560ff16906020019092919050505061021e565b604051808281526020"
//            + "0191505060405180910390f35b61021c600480360360608110156100d857600080fd5b8101908080"
//            + "359060200190929190803590602001906401000000008111156100ff57600080fd5b820183602082"
//            + "01111561011157600080fd5b80359060200191846001830284011164010000000083111715610133"
//            + "57600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260"
//            + "2001838380828437600081840152601f19601f820116905080830192505050505050509192919290"
//            + "8035906020019064010000000081111561019657600080fd5b8201836020820111156101a8576000"
//            + "80fd5b803590602001918460018302840111640100000000831117156101ca57600080fd5b919080"
//            + "80601f01602080910402602001604051908101604052809392919081815260200183838082843760"
//            + "0081840152601f19601f82011690508083019250505050505050919291929050505061022f565b00"
//            + "5b600081838587da9050949350505050565b808284db5050505056fea26474726f6e5820def53e9f"
//            + "ef23475f8f3316cfaf66a0015e2205fe9bf0eba01f703e30ef9d732364736f6c637828302e352e31"
//            + "332d646576656c6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
//    long value = 0;
//    long fee = 100000000;
//    long consumeUserResourcePercent = 0;
//    // deploy contract
//    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
//        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
//        null);
//    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
//    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
//    Assert.assertNull(runtime.getRuntimeError());
//
//    // validate balance is enough
//    String tokenP1 = "74657374";
//    String tokenP2 = tokenP1;
//    long tokenP3 = 1000;
//    long tokenP4 = 2;
//    String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    String hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2,
//        tokenP3, tokenP4));
//    TVMTestResult result =
//        TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//            factoryAddress,
//            Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    rootRepository.addBalance(factoryAddress, 10000000000L);
//    rootRepository.commit();
//    Assert.assertEquals(rootRepository.getBalance(factoryAddress), 10000000000L);
//
//    // validate assetissue assetname can't be trx
//    tokenP1 = "747278";
//    tokenP2 = tokenP1;
//    tokenP3 = 1000;
//    tokenP4 = 2;
//    methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3,
//        tokenP4));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // validate assetissue precision can't more than 6
//    tokenP1 = "74657374";
//    tokenP2 = tokenP1;
//    tokenP3 = 1000;
//    tokenP4 = 7;
//    methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3,
//        tokenP4));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // trigger contract success
//    tokenP1 = "74657374";
//    tokenP2 = tokenP1;
//    tokenP3 = 1000;
//    tokenP4 = 2;
//    methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3,
//        tokenP4));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 1000001);
//    long expectEnergyUsageTotal = 25000;
//    long expectEnergyUsageTotalMax = 30000;
//    long reallyEnergyUsageTotal = result.getReceipt().getEnergyUsageTotal();
//    // validate energy cost
//    Assert.assertTrue(reallyEnergyUsageTotal > expectEnergyUsageTotal
//        && reallyEnergyUsageTotal < expectEnergyUsageTotalMax);
//
//    // validate assetissue An account can only issue one asset
//    tokenP1 = "74657374";
//    tokenP2 = tokenP1;
//    tokenP3 = 1000;
//    tokenP4 = 2;
//    methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP2, tokenP3,
//        tokenP4));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // Trigger contract method: UpdateAsset(trcToken, string, string)
//    String methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
//    // 1,abc,abc,
//    long updateP1 = 1000001;
//    String updateP2 = "abc";
//    String updateP3 = updateP2;
//    hexInput = AbiUtil.parseMethod(methodUpdateAsset,
//    Arrays.asList(updateP1, updateP2, updateP3));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//        .get(0).getUrl().toByteArray()), "abc");
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//        .get(0).getDesc().toByteArray()), "abc");
//    // validate energy cost
//    expectEnergyUsageTotal = 5000;
//    expectEnergyUsageTotalMax = 10000;
//    reallyEnergyUsageTotal = result.getReceipt().getEnergyUsageTotal();
//
//    Assert.assertTrue(reallyEnergyUsageTotal > expectEnergyUsageTotal
//        && reallyEnergyUsageTotal < expectEnergyUsageTotalMax);
//
//    // validate desc less than 200
//    updateP1 = 1000001;
//    updateP2 =
//        "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcab"
//            + "cabcabcabcabcabcabcababcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcab"
//            + "cabcabcabcabcabcabcabcabcabcabcabcabcabcababcabcabcabcabcabcabcabcabcabcabcabcab"
//            + "cabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcababcabcabcabcabcab"
//            + "cabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabca"
//            + "bcab";
//    updateP3 = "efg";
//    hexInput = AbiUtil.parseMethod(methodUpdateAsset,
//    Arrays.asList(updateP1, updateP2, updateP3));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//        .get(0).getUrl().toByteArray()), "abc");
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//        .get(0).getDesc().toByteArray()), "abc");
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // validate url less than 256
//    updateP1 = 1000001;
//    updateP3 =
//        "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcab"
//            + "cabcabcabcabcabcabcaabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc"
//            + "abcabcabcabcabcabcabcabcabcabcabcabcabcaabcabcabcabcabcabcabcabcabcabcabcabcabca"
//            + "bcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcaabcabcabcabcabcabcab"
//            + "cabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabca";
//    updateP2 = "efg";
//    hexInput = AbiUtil.parseMethod(methodUpdateAsset,
//    Arrays.asList(updateP1, updateP2, updateP3));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//  }
//
//  /*pragma solidity ^0.5.12;
//   contract HelloWorld{
//       function TokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision)
//       public {
//           assetissue(name, abbr, totalSupply, precision);
//           assetissue(name, abbr, totalSupply, precision);
//       }
//       function UpdateAsset(trcToken tokenId, string memory url, string memory desc) public {
//           updateasset(tokenId, bytes(url), bytes(desc));
//           updateasset(tokenId, bytes(url), bytes(desc));
//       }
//   }*/
//
//  @Test
//  public void testTrc10ForMultiCall() throws ContractExeException,
//      ReceiptCheckErrException, VMIllegalException,
//      ContractValidateException {
//    ConfigLoader.disable = true;
//    VMConfig.initAllowTvmTransferTrc10(1);
//    VMConfig.initAllowTvmConstantinople(1);
//    VMConfig.initAllowTvmSolidity059(1);
//    VMConfig.initAllowTvmAssetIssue(1);
//    manager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
//    String contractName = "AssetIssueTest";
//    byte[] address = Hex.decode(OWNER_ADDRESS);
//    String abi = "[{\"constant\":false,\"inputs\":[{\"internalType\":\"bytes32\","
//        + "\"name\":\"name\","
//        + "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"abbr\","
//        + "\"type\":\"bytes32\"},"
//        + "{\"internalType\":\"uint64\",\"name\":\"totalSupply\",\"type\":\"uint64\"},"
//        + "{\"internalType\":\"uint8\",\"name\":\"precision\",\"type\":\"uint8\"}],"
//        + "\"name\":\"TokenIssue\","
//        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
//        + "\"type\":\"function\"},"
//        + "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken\",\"name\":\"tokenId\","
//        + "\"type\":\"trcToken\"},{\"internalType\":\"string\",\"name\":\"desc\","
//        + "\"type\":\"string\"},"
//        + "{\"internalType\":\"string\",\"name\":\"url\",\"type\":\"string\"}],"
//        + "\"name\":\"UpdateAsset\","
//        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
//        + "\"type\":\"function\"}]";
//    String factoryCode =
//        "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a576000"
//            + "80fd5b506102858061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561"
//            + "001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c80633615"
//            + "673e14610055578063f177bc7a146100ae575b600080fd5b6100ac6004803603608081101561006b"
//            + "57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffff"
//            + "ff169060200190929190803560ff16906020019092919050505061020a565b005b61020860048036"
//            + "0360608110156100c457600080fd5b81019080803590602001909291908035906020019064010000"
//            + "00008111156100eb57600080fd5b8201836020820111156100fd57600080fd5b8035906020019184"
//            + "600183028401116401000000008311171561011f57600080fd5b91908080601f0160208091040260"
//            + "20016040519081016040528093929190818152602001838380828437600081840152601f19601f82"
//            + "01169050808301925050505050505091929192908035906020019064010000000081111561018257"
//            + "600080fd5b82018360208201111561019457600080fd5b8035906020019184600183028401116401"
//            + "00000000831117156101b657600080fd5b91908080601f0160208091040260200160405190810160"
//            + "40528093929190818152602001838380828437600081840152601f19601f82011690508083019250"
//            + "505050505050919291929050505061021c565b005b80828486da5080828486da5050505050565b80"
//            + "8284db50808284db5050505056fea26474726f6e582010b72c10cc2d93a0ddc6ec14e8a40345f73a"
//            + "2263120bd04e1552687570eb950c64736f6c637828302e352e31332d646576656c6f702e32303230"
//            + "2e382e31332b636f6d6d69742e37633236393863300057";
//    long value = 0;
//    long fee = 100000000;
//    long consumeUserResourcePercent = 0;
//    // deploy contract
//    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
//        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
//        null);
//    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
//    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
//    Assert.assertNull(runtime.getRuntimeError());
//
//    // send coin
//    rootRepository.addBalance(factoryAddress, 10000000000L);
//    rootRepository.commit();
//    Assert.assertEquals(rootRepository.getBalance(factoryAddress), 10000000000L);
//
//    // validate updateasset  Asset is not existed in AssetIssueStore
//    String methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
//    // 1,abc,abc,
//    long updateP1 = 1000001;
//    String updateP2 = "616263";
//    String updateP3 = updateP2;
//    String hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2,
//        updateP3));
//    TVMTestResult result =
//        TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//            factoryAddress,
//            Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // multicall tokenissue
//    String tokenP1 = "74657374";
//    long tokenP3 = 1000;
//    long tokenP4 = 2;
//    String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP1, tokenP3,
//        tokenP4));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // multicall updateAsset
//    methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
//    // 1,abc,abc,
//    updateP1 = 1000001;
//    updateP2 = "abc";
//    updateP3 = updateP2;
//    hexInput = AbiUtil.parseMethod(methodUpdateAsset,
//    Arrays.asList(updateP1, updateP2, updateP3));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//            .get(0).getUrl().toByteArray()),
//        "abc");
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//            .get(0).getDesc().toByteArray()),
//        "abc");
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//  }
//
//  /*pragma solidity ^0.5.12;
//   contract HelloWorld{
//       function TokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision,
//       address addr) public {
//           address payable newaddress = address(uint160(addr));
//           newaddress.transfer(100000000);
//           assetissue(name, abbr, totalSupply, precision);
//           newaddress.transfer(100000000);
//       }
//       function UpdateAsset(trcToken tokenId, string memory url, string memory desc, address
//       addr) public {
//           address payable newaddress = address(uint160(addr));
//           newaddress.transfer(100000000);
//           updateasset(tokenId, bytes(url), bytes(desc));
//           newaddress.transfer(100000000);
//       }
//   }*/
//
//  @Test
//  public void testTrc10Exception() throws ContractExeException,
//      ReceiptCheckErrException, VMIllegalException,
//      ContractValidateException {
//    ConfigLoader.disable = true;
//    VMConfig.initAllowTvmTransferTrc10(1);
//    VMConfig.initAllowTvmConstantinople(1);
//    VMConfig.initAllowTvmSolidity059(1);
//    VMConfig.initAllowTvmAssetIssue(1);
//    String contractName = "AssetIssueTest";
//    byte[] address = Hex.decode(OWNER_ADDRESS);
//    String abi = "[{\"constant\":false,\"inputs\":[{\"internalType\":\"bytes32\","
//        + "\"name\":\"name\","
//        + "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"abbr\","
//        + "\"type\":\"bytes32\"},"
//        + "{\"internalType\":\"uint64\",\"name\":\"totalSupply\",\"type\":\"uint64\"},"
//        + "{\"internalType\":\"uint8\",\"name\":\"precision\",\"type\":\"uint8\"},"
//        + "{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],"
//        + "\"name\":\"TokenIssue\","
//        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
//        + "\"type\":\"function\"},"
//        + "{\"constant\":false,\"inputs\":[{\"internalType\":\"trcToken\",\"name\":\"tokenId\","
//        + "\"type\":\"trcToken\"},{\"internalType\":\"string\",\"name\":\"desc\","
//        + "\"type\":\"string\"},"
//        + "{\"internalType\":\"string\",\"name\":\"url\",\"type\":\"string\"},"
//        + "{\"internalType\":\"address\","
//        + "\"name\":\"addr\",\"type\":\"address\"}],\"name\":\"UpdateAsset\",\"outputs\":[],"
//        + "\"payable\":false,"
//        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
//    String factoryCode =
//        "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a576000"
//            + "80fd5b506103f48061003a6000396000f3fe608060405234801561001057600080fd5b50d3801561"
//            + "001d57600080fd5b50d2801561002a57600080fd5b50600436106100505760003560e01c8063451e"
//            + "cfa214610055578063a241e431146100ce575b600080fd5b6100cc600480360360a081101561006b"
//            + "57600080fd5b810190808035906020019092919080359060200190929190803567ffffffffffffff"
//            + "ff169060200190929190803560ff169060200190929190803573ffffffffffffffffffffffffffff"
//            + "ffffffffffff16906020019092919050505061024a565b005b610248600480360360808110156100"
//            + "e457600080fd5b81019080803590602001909291908035906020019064010000000081111561010b"
//            + "57600080fd5b82018360208201111561011d57600080fd5b80359060200191846001830284011164"
//            + "01000000008311171561013f57600080fd5b91908080601f01602080910402602001604051908101"
//            + "6040528093929190818152602001838380828437600081840152601f19601f820116905080830192"
//            + "505050505050509192919290803590602001906401000000008111156101a257600080fd5b820183"
//            + "6020820111156101b457600080fd5b80359060200191846001830284011164010000000083111715"
//            + "6101d657600080fd5b91908080601f01602080910402602001604051908101604052809392919081"
//            + "8152602001838380828437600081840152601f19601f820116905080830192505050505050509192"
//            + "919290803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102"
//            + "f3565b005b60008190508073ffffffffffffffffffffffffffffffffffffffff166108fc6305f5e1"
//            + "009081150290604051600060405180830381858888f19350505050158015610299573d6000803e3d"
//            + "6000fd5b5082848688da508073ffffffffffffffffffffffffffffffffffffffff166108fc6305f5"
//            + "e1009081150290604051600060405180830381858888f193505050501580156102ea573d6000803e"
//            + "3d6000fd5b50505050505050565b60008190508073ffffffffffffffffffffffffffffffffffffff"
//            + "ff166108fc6305f5e1009081150290604051600060405180830381858888f1935050505015801561"
//            + "0342573d6000803e3d6000fd5b50828486db508073ffffffffffffffffffffffffffffffffffffff"
//            + "ff166108fc6305f5e1009081150290604051600060405180830381858888f1935050505015801561"
//            + "0392573d6000803e3d6000fd5b50505050505056fea26474726f6e5820e6e64fe3d21e1866d8b2da"
//            + "9e9bba20e7b5fee47cf6122aae388eb63b4d3df07264736f6c637828302e352e31332d646576656c"
//            + "6f702e323032302e382e31332b636f6d6d69742e37633236393863300057";
//    long value = 0;
//    long fee = 100000000;
//    long consumeUserResourcePercent = 0;
//    // deploy contract
//    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
//        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
//        null);
//    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
//    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
//    Assert.assertNull(runtime.getRuntimeError());
//
//    // sendcoin to factoryAddress 100000000000
//    rootRepository.addBalance(factoryAddress, 10000000000L);
//    rootRepository.commit();
//    Assert.assertEquals(rootRepository.getBalance(factoryAddress), 10000000000L);
//
//    // assetissue exception test
//    String tokenP1 = "74657374";
//    long tokenP3 = 1000;
//    long tokenP4 = 7;
//    String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8,address)";
//    String receiveAddress = "27VZHn9PFZwNh7o2EporxmLkpe157iWZVkh";
//    String hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP1,
//        tokenP3, tokenP4, receiveAddress));
//    TVMTestResult result =
//        TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//            factoryAddress,
//            Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()), 0);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//    Assert.assertEquals(200000000,
//        rootRepository.getBalance(decode58Check(receiveAddress)));
//
//    // updateasset exception test
//    String methodUpdateAsset = "UpdateAsset(trcToken,string,string,address)";
//    // 1,abc,abc,
//    long updateP1 = 1000001;
//    String updateP2 = "616263";
//    hexInput = AbiUtil.parseMethod(methodUpdateAsset, Arrays.asList(updateP1, updateP2, updateP2,
//        receiveAddress));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//    Assert.assertEquals(200000000, rootRepository.getBalance(decode58Check(receiveAddress)));
//  }
//
//  /*pragma solidity ^0.5.0;
//   contract A {
//       function TokenIssueA(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision)
//       public returns (uint){
//           return assetissue(name, abbr, totalSupply, precision);
//       }
//       function UpdateAssetA(trcToken tokenId, string memory desc, string memory url) public {
//           updateasset(tokenId, bytes(desc), bytes(url));
//       }
//   }
//   contract HelloWorld {
//       A a = new A();
//       function TokenIssue(bytes32 name, bytes32 abbr, uint64 totalSupply, uint8 precision)
//       public returns (uint) {
//           return a.TokenIssueA(name, abbr, totalSupply, precision);
//       }
//       function UpdateAsset(trcToken tokenId, string memory url, string memory desc) public {
//           a.UpdateAssetA(tokenId, url, desc);
//       }
//       function getContractAddress() public returns (address) {
//           return address(a);
//       }
//   }*/
//
//  @Test
//  public void testTrc10ContractCall() throws ContractExeException,
//      ReceiptCheckErrException, VMIllegalException,
//      ContractValidateException {
//    ConfigLoader.disable = true;
//    VMConfig.initAllowTvmTransferTrc10(1);
//    VMConfig.initAllowTvmConstantinople(1);
//    VMConfig.initAllowTvmSolidity059(1);
//    VMConfig.initAllowTvmAssetIssue(1);
//    manager.getDynamicPropertiesStore().saveAllowSameTokenName(1);
//    String contractName = "AssetIssueTest";
//    byte[] address = Hex.decode(OWNER_ADDRESS);
//    String abi = "[{\"constant\":false,\"inputs\":[{\"internalType\":\"bytes32\","
//        + "\"name\":\"name\","
//        + "\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"abbr\","
//        + "\"type\":\"bytes32\"},"
//        + "{\"internalType\":\"uint64\",\"name\":\"totalSupply\",\"type\":\"uint64\"},"
//        + "{\"internalType\":\"uint8\",\"name\":\"precision\",\"type\":\"uint8\"}],"
//        + "\"name\":\"TokenIssue\","
//        + "\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],"
//        + "\"payable\":false,"
//        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
//        + "\"inputs\":[{\"internalType\":\"trcToken\",\"name\":
//        \"tokenId\",\"type\":\"trcToken\"},"
//        + "{\"internalType\":\"string\",\"name\":\"desc\",\"type\":\"string\"},"
//        + "{\"internalType\":\"string\","
//        + "\"name\":\"url\",\"type\":\"string\"}],\"name\":\"UpdateAsset\",\"outputs\":[],"
//        + "\"payable\":false,"
//        + "\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,"
//        + "\"inputs\":[],"
//        + "\"name\":\"getContractAddress\",\"outputs\":[{\"internalType\":\"address\","
//        + "\"name\":\"\","
//        + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\","
//        + "\"type\":\"function\"}]";
//    String factoryCode =
//        "608060405260405161001090610098565b604051809103906000f08015801561002c573d6000803e"
//            + "3d6000fd5b506000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916"
//            + "908373ffffffffffffffffffffffffffffffffffffffff16021790555034801561007857600080fd"
//            + "5b50d3801561008557600080fd5b50d2801561009257600080fd5b506100a5565b6102cd80610602"
//            + "83390190565b61054e806100b46000396000f3fe608060405234801561001057600080fd5b50d380"
//            + "1561001d57600080fd5b50d2801561002a57600080fd5b506004361061005b5760003560e01c8063"
//            + "32a2c5d0146100605780633615673e146100aa578063f177bc7a14610117575b600080fd5b610068"
//            + "610273565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffff"
//            + "ffffffffffffffffffffffffff16815260200191505060405180910390f35b610101600480360360"
//            + "808110156100c057600080fd5b810190808035906020019092919080359060200190929190803567"
//            + "ffffffffffffffff169060200190929190803560ff16906020019092919050505061029c565b6040"
//            + "518082815260200191505060405180910390f35b6102716004803603606081101561012d57600080"
//            + "fd5b81019080803590602001909291908035906020019064010000000081111561015457600080fd"
//            + "5b82018360208201111561016657600080fd5b803590602001918460018302840111640100000000"
//            + "8311171561018857600080fd5b91908080601f016020809104026020016040519081016040528093"
//            + "929190818152602001838380828437600081840152601f19601f8201169050808301925050505050"
//            + "50509192919290803590602001906401000000008111156101eb57600080fd5b8201836020820111"
//            + "156101fd57600080fd5b8035906020019184600183028401116401000000008311171561021f5760"
//            + "0080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001"
//            + "838380828437600081840152601f19601f8201169050808301925050505050505091929192905050"
//            + "50610389565b005b60008060009054906101000a900473ffffffffffffffffffffffffffffffffff"
//            + "ffffff16905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffff"
//            + "ffffffff1673ffffffffffffffffffffffffffffffffffffffff166329f00e598686868660405185"
//            + "63ffffffff1660e01b8152600401808581526020018481526020018367ffffffffffffffff1667ff"
//            + "ffffffffffffff1681526020018260ff1660ff168152602001945050505050602060405180830381"
//            + "600087803b15801561034457600080fd5b505af1158015610358573d6000803e3d6000fd5b505050"
//            + "506040513d602081101561036e57600080fd5b810190808051906020019092919050505090509493"
//            + "50505050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff16"
//            + "73ffffffffffffffffffffffffffffffffffffffff1663cfa62e758484846040518463ffffffff16"
//            + "60e01b81526004018084815260200180602001806020018381038352858181518152602001915080"
//            + "51906020019080838360005b83811015610422578082015181840152602081019050610407565b50"
//            + "505050905090810190601f16801561044f5780820380516001836020036101000a03191681526020"
//            + "0191505b50838103825284818151815260200191508051906020019080838360005b838110156104"
//            + "8857808201518184015260208101905061046d565b50505050905090810190601f1680156104b557"
//            + "80820380516001836020036101000a031916815260200191505b5095505050505050600060405180"
//            + "830381600087803b1580156104d757600080fd5b505af11580156104eb573d6000803e3d6000fd5b"
//            + "5050505050505056fea26474726f6e582068c6e59329c5b9e63f34ea5762d58db8649b294831356a"
//            + "dfe323a04f3c96c25f64736f6c637828302e352e31332d646576656c6f702e323032302e382e3133"
//            + "2b636f6d6d69742e37633236393863300057608060405234801561001057600080fd5b50d3801561"
//            + "001d57600080fd5b50d2801561002a57600080fd5b506102938061003a6000396000f3fe60806040"
//            + "5234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b50"
//            + "600436106100505760003560e01c806329f00e5914610055578063cfa62e75146100c2575b600080"
//            + "fd5b6100ac6004803603608081101561006b57600080fd5b81019080803590602001909291908035"
//            + "9060200190929190803567ffffffffffffffff169060200190929190803560ff1690602001909291"
//            + "9050505061021e565b6040518082815260200191505060405180910390f35b61021c600480360360"
//            + "608110156100d857600080fd5b810190808035906020019092919080359060200190640100000000"
//            + "8111156100ff57600080fd5b82018360208201111561011157600080fd5b80359060200191846001"
//            + "83028401116401000000008311171561013357600080fd5b91908080601f01602080910402602001"
//            + "6040519081016040528093929190818152602001838380828437600081840152601f19601f820116"
//            + "90508083019250505050505050919291929080359060200190640100000000811115610196576000"
//            + "80fd5b8201836020820111156101a857600080fd5b80359060200191846001830284011164010000"
//            + "0000831117156101ca57600080fd5b91908080601f01602080910402602001604051908101604052"
//            + "8093929190818152602001838380828437600081840152601f19601f820116905080830192505050"
//            + "50505050919291929050505061022f565b005b600081838587da9050949350505050565b808284db"
//            + "5050505056fea26474726f6e582071af66a376cfd560797ad3c889d08b9037d8ec121805b96fd252"
//            + "3415b77f4df264736f6c637828302e352e31332d646576656c6f702e323032302e382e31332b636f"
//            + "6d6d69742e37633236393863300057";
//    long value = 0;
//    long fee = 100000000;
//    long consumeUserResourcePercent = 0;
//    // deploy contract
//    Protocol.Transaction trx = TvmTestUtils.generateDeploySmartContractAndGetTransaction(
//        contractName, address, abi, factoryCode, value, fee, consumeUserResourcePercent,
//        null);
//    byte[] factoryAddress = WalletUtil.generateContractAddress(trx);
//    runtime = TvmTestUtils.processTransactionAndReturnRuntime(trx, manager, null);
//    Assert.assertNull(runtime.getRuntimeError());
//
//    // Trigger contract method: getContractAddress()
//    String methodByAddr = "getContractAddress()";
//    String hexInput = AbiUtil.parseMethod(methodByAddr, Collections.emptyList());
//    TVMTestResult result = TvmTestUtils
//        .triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//            factoryAddress, Hex.decode(hexInput), 0, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//    byte[] returnValue = result.getRuntime().getResult().getHReturn();
//
//    // Contract A Address
//    String tmpAddress = "a0" + Hex.toHexString(returnValue).substring(24);
//
//    // sendcoin to A address 100000000000
//    rootRepository.addBalance(ByteArray.fromHexString(tmpAddress), 10000000000L);
//    rootRepository.commit();
//    Assert.assertEquals(rootRepository.getBalance(ByteArray.fromHexString(tmpAddress)),
//        10000000000L);
//
//    // assetissue test
//    String tokenP1 = "74657374";
//    long tokenP3 = 1000;
//    long tokenP4 = 2;
//    String methodTokenIssue = "TokenIssue(bytes32,bytes32,uint64,uint8)";
//    hexInput = AbiUtil.parseMethod(methodTokenIssue, Arrays.asList(tokenP1, tokenP1, tokenP3,
//        tokenP4));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertEquals(ByteArray.toInt(result.getRuntime().getResult().getHReturn()),
//    1000001);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//
//    // updateasset test
//    String methodUpdateAsset = "UpdateAsset(trcToken,string,string)";
//    // 1,abc,abc,
//    long updateP1 = 1000001;
//    String updateP2 = "abc";
//    hexInput = AbiUtil.parseMethod(methodUpdateAsset,
//    Arrays.asList(updateP1, updateP2, updateP2));
//    result = TvmTestUtils.triggerContractAndReturnTvmTestResult(Hex.decode(OWNER_ADDRESS),
//        factoryAddress,
//        Hex.decode(hexInput), value, fee, manager, null);
//    Assert.assertNull(result.getRuntime().getRuntimeError());
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//            .get(0).getUrl().toByteArray()),
//        "abc");
//    Assert.assertEquals(ByteArray.toStr(manager.getAssetIssueV2Store().getAllAssetIssues()
//            .get(0).getDesc().toByteArray()),
//        "abc");
//  }
//}
//
//
//
