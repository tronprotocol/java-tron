package stest.tron.wallet.dailybuild.tvmnewcommand.tvmassetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TvmAssetIssue005 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 10000000000L;
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static String abbr = "testAsset_" + Long.toString(now);
  private static String description = "desc_" + Long.toString(now);
  private static String url = "url_" + Long.toString(now);
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private byte[] contractAddress = null;
  private long contractAddressBalance;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  private byte[] dev003Address = ecKey3.getAddress();
  private String dev003Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  private byte[] dev004Address = ecKey4.getAddress();
  private String dev004Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(dev002Key);
    PublicMethed.printAddress(dev003Key);
    PublicMethed.printAddress(dev004Key);
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 7000_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "tokenIssue and updateAsset with suicide to account")
  public void tokenIssue001AndSuicideToAccount() {
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue005.sol";
    String contractName = "tvmAssetIssue005";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 1050000000L;

    // deploy
    final String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    contractAddressBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    // tokenIssue
    name = "testAssetIssu1_" + Long.toString(now);
    abbr = "testAsse1_" + Long.toString(now);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    String txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    String assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    logger.info("getAssetV2Map(): " + PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map());
    long assetIssueValue = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    AssetIssueContract assetIssueByName = PublicMethed.getAssetIssueByName(name, blockingStubFull);
    AssetIssueContract assetIssueByAccount = PublicMethed
        .getAssetIssueByAccount(contractAddress, blockingStubFull).get().getAssetIssue(0);
    AssetIssueContract assetIssueListByName = PublicMethed
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
    long contractAddressBalance2 = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance - 1024000000L, contractAddressBalance2);

    // transferToken
    String methodTransferToken = "transferToken(address,uint256,trcToken)";
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    txid = PublicMethed.triggerContract(contractAddress, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    long assetIssueValueAfter = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev002AssetValue = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(100L, dev002AssetValue);

    // updateAsset
    String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    long returnId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnId);
    assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));

    // selfdestruct
    String methodSuicide = "SelfdestructTest(address)";
    param = "\"" + Base58.encode58Check(dev003Address) + "\"";
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodSuicide, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID().size());
    Assert.assertEquals(0,
        PublicMethed.getAssetIssueByAccount(dev003Address, blockingStubFull).get()
            .getAssetIssueCount());
    Assert.assertEquals(0,
        PublicMethed.queryAccount(dev003Address, blockingStubFull).getAssetIssuedID().size());
    long contractAssetCountDev003 = PublicMethed
        .getAssetIssueValue(dev003Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValueAfter, contractAssetCountDev003);
    assetIssueValue = PublicMethed.queryAccount(dev003Address, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(assetIssueValueAfter, assetIssueValue);
    assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    assetIssueByName = PublicMethed.getAssetIssueByName(name, blockingStubFull);
    assetIssueByAccount = PublicMethed
        .getAssetIssueByAccount(contractAddress, blockingStubFull).get().getAssetIssue(0);
    assetIssueListByName = PublicMethed
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
    dev002AssetValue = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(100L, dev002AssetValue);

    Assert.assertTrue(PublicMethed
        .sendcoin(dev002Address, 100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // transferAsset,success
    Assert.assertTrue(PublicMethed.transferAsset(dev002Address, assetIssueId.getBytes(), 100L,
        dev003Address, dev003Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long assetIssueValueDev002 = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    long assetIssueValueDev003 = PublicMethed
        .getAssetIssueValue(dev003Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(200L, assetIssueValueDev002);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueDev003);

    Assert.assertTrue(PublicMethed.transferAsset(dev004Address, assetIssueId.getBytes(), 102L,
        dev002Address, dev002Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long assetIssueValueDev002After = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    long assetIssueValueDev004 = PublicMethed
        .getAssetIssueValue(dev004Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(102L, assetIssueValueDev004);
    Assert.assertEquals(assetIssueValueDev002 - 102L, assetIssueValueDev002After);

    // updateAsset,will fail
    Assert.assertFalse(PublicMethed
        .updateAsset(dev003Address, "updateDesc1".getBytes(), "updateURL1".getBytes(), 1L, 2L,
            dev003Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertFalse(PublicMethed
        .updateAsset(contractAddress, "updateDesc2".getBytes(), "updateURL2".getBytes(), 3L, 4L,
            dev003Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "tokenIssue and updateAsset with suicide to contract")
  public void tokenIssue002AndSuicideToContract() {
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue005.sol";
    String contractName = "tvmAssetIssue005";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 1050000000L;

    // deploy
    String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    byte[] contractAddress2 = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress2, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    long contractAddressBalance2 = PublicMethed.queryAccount(contractAddress2, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance2);

    deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    smartContract = PublicMethed
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    contractAddressBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    // tokenIssue
    name = "testAssetIssu2_" + Long.toString(now);
    abbr = "testAsse2_" + Long.toString(now);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    String txid = PublicMethed.triggerContract(contractAddress2, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    String assetIssueId = PublicMethed.queryAccount(contractAddress2, blockingStubFull)
        .getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    logger.info("getAssetV2Map(): " + PublicMethed.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map());
    long assetIssueValue = PublicMethed.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress2),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    AssetIssueContract assetIssueByName = PublicMethed.getAssetIssueByName(name, blockingStubFull);
    AssetIssueContract assetIssueByAccount = PublicMethed
        .getAssetIssueByAccount(contractAddress2, blockingStubFull).get().getAssetIssue(0);
    AssetIssueContract assetIssueListByName = PublicMethed
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
    long contractAddressBalanceAfter2 = PublicMethed
        .queryAccount(contractAddress2, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance2 - 1024000000L, contractAddressBalanceAfter2);

    // transferToken
    String methodTransferToken = "transferToken(address,uint256,trcToken)";
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    txid = PublicMethed.triggerContract(contractAddress2, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long assetIssueValueAfter = PublicMethed.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev002AssetValue = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(100L, dev002AssetValue);

    param =
        "\"" + Base58.encode58Check(contractAddress) + "\"," + 50 + ",\"" + assetIssueId + "\"";
    txid = PublicMethed.triggerContract(contractAddress2, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long assetIssueValueAfter2 = PublicMethed.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long contractAssetValue = PublicMethed
        .getAssetIssueValue(contractAddress, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValueAfter - 50L, assetIssueValueAfter2);
    Assert.assertEquals(50L, contractAssetValue);

    // selfdestruct
    String methodSuicide = "SelfdestructTest(address)";
    param = "\"" + Base58.encode58Check(contractAddress) + "\"";
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress2, methodSuicide, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0,
        PublicMethed.queryAccount(contractAddress2, blockingStubFull).getAssetIssuedID().size());
    Assert.assertEquals(0,
        PublicMethed.getAssetIssueByAccount(contractAddress, blockingStubFull).get()
            .getAssetIssueCount());
    Assert.assertEquals(0,
        PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID().size());
    assetIssueValue = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(assetIssueValueAfter2 + 50L, assetIssueValue);
    assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress2),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    assetIssueByName = PublicMethed.getAssetIssueByName(name, blockingStubFull);
    assetIssueByAccount = PublicMethed
        .getAssetIssueByAccount(contractAddress2, blockingStubFull).get().getAssetIssue(0);
    assetIssueListByName = PublicMethed
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());

    // transferToken,success
    methodTransferToken = "transferToken(address,uint256,trcToken)";
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    txid = PublicMethed.triggerContract(contractAddress, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    assetIssueValueAfter = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    dev002AssetValue = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(200L, dev002AssetValue);

    Assert.assertTrue(PublicMethed.transferAsset(dev004Address, assetIssueId.getBytes(), 12L,
        dev002Address, dev002Key, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long assetIssueValueDev002After = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    long assetIssueValueDev004 = PublicMethed
        .getAssetIssueValue(dev004Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(12L, assetIssueValueDev004);
    Assert.assertEquals(dev002AssetValue - 12L, assetIssueValueDev002After);

    // updateAsset,will fail
    String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    param = "\"" + assetIssueId + "\",\"updateUrl\",\"updateDesc\"";
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnId);
    assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(0, assetIssueById.getDescription().size());
    Assert.assertEquals(0, assetIssueById.getUrl().size());
    Assert.assertEquals(Base58.encode58Check(contractAddress2),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "tokenIssue and updateAsset suicide with create2")
  public void tokenIssue003AndSuicideWithCreate2() {
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue005.sol";
    String contractName = "B";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    String methodTokenIssue = "deploy(uint256)";
    String param = "" + 6;
    logger.info("param: " + param);
    String txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());
    logger.info(
        "the value: " + PublicMethed
            .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray()));
    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());
    Long actualSalt = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
    logger.info("actualSalt: " + actualSalt);
    byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(0)),
        12, tmpAddress, 0, 20);
    String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
    String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);
    byte[] callContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    Assert.assertTrue(PublicMethed
        .sendcoin(callContractAddress, 1500_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    name = "testAssetIssu3_" + Long.toString(now);
    abbr = "testAsse3_" + Long.toString(now);
    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(callContractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    String assetIssueId = PublicMethed.queryAccount(callContractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));

    String methodSuicide = "SelfdestructTest(address)";
    param = "\"" + Base58.encode58Check(dev003Address) + "\"," + 10000000;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(callContractAddress, methodSuicide, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    methodTokenIssue = "deploy(uint256)";
    param = "" + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethed
        .sendcoin(callContractAddress, 1500_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    tokenName = PublicMethed.stringToHexString("testAssetIssue_11111");
    tokenAbbr = PublicMethed.stringToHexString("testAssetIssue_22222");
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(callContractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    String assetIssueId2 = PublicMethed.queryAccount(callContractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId2: " + assetIssueId2);
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId2));
    Assert.assertEquals(Long.parseLong(assetIssueId) + 1, Long.parseLong(assetIssueId2));
    Assert.assertEquals(2,
        PublicMethed.getAssetIssueByAccount(callContractAddress, blockingStubFull).get()
            .getAssetIssueCount());

    // updateAsset
    String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    param = "\"123\",\"updateURLURL\",\"updateDESCDESC\"";
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(callContractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnId);
    String newAssetIssueId = PublicMethed.queryAccount(callContractAddress, blockingStubFull)
        .getAssetIssuedID()
        .toStringUtf8();
    logger.info("newAssetIssueId: " + newAssetIssueId);
    AssetIssueContract newAssetIssueById = PublicMethed
        .getAssetIssueById(newAssetIssueId, blockingStubFull);
    Assert.assertEquals("testAssetIssue_11111",
        ByteArray.toStr(newAssetIssueById.getName().toByteArray()));
    Assert.assertEquals("testAssetIssue_22222",
        ByteArray.toStr(newAssetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals("updateDESCDESC",
            ByteArray.toStr(newAssetIssueById.getDescription().toByteArray()));
    Assert.assertEquals("updateURLURL", ByteArray.toStr(newAssetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, newAssetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(callContractAddress),
        Base58.encode58Check(newAssetIssueById.getOwnerAddress().toByteArray()));

    AssetIssueContract oldAssetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(oldAssetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(oldAssetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(0, oldAssetIssueById.getDescription().size());
    Assert.assertEquals(0, oldAssetIssueById.getUrl().size());
    Assert.assertEquals(6, oldAssetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(callContractAddress),
        Base58.encode58Check(oldAssetIssueById.getOwnerAddress().toByteArray()));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
