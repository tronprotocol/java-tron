package stest.tron.wallet.dailybuild.tvmnewcommand.tvmassetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TvmAssetIssue002 {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 10000000000L;
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static String abbr = "testAsset_" + Long.toString(now);
  private static String assetIssueId = null;
  long contractAddressBalance;
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

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());



  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(dev002Key);
  }

  @Test(enabled = false, description = "tokenIssue illegal parameter verification")
  public void tokenIssue001IllegalParameterVerification() {
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue001.sol";
    String contractName = "tvmAssetIssue001";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 2050000000L;

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
    long contractAddressBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    /*String param = "0000000000000000000000000000000000007465737441737365744973737565"
        + "0000000000000000000074657374417373657431353938333439363637393631"
        + "0000000000000000000000000000000000000000000000000000000000989680"
        + "0000000000000000000000000000000000000000000000000000000000000001";*/
    // assetName is trx
    String tokenName = PublicMethed.stringToHexString("trx");
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    Map<String, Long> assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // assetName.length > 32 compile fail
    /*tokenName = PublicMethed.stringToHexString("testAssetIssue_testAssetIssue_tes");
    tokenAbbr = PublicMethed.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());*/

    // assetName is ""
    tokenName = PublicMethed.stringToHexString("");
    tokenAbbr = PublicMethed.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // assetName is chinese
    tokenName = PublicMethed.stringToHexString("名字");
    tokenAbbr = PublicMethed.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // assetAbbr is null
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString("");
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // assetAbbr is chinese
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString("简称");
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // totalSupply is Long.MAX_VALUE+1
    param = "a8547918"
        + "74657374417373657449737375655f3136303034333636393333333600000000"
        + "7472780000000000000000000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000008000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000006";
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // totalSupply is -1
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString("trx");
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + -1 + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    logger.info("totalSupply is -1");
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("SUCCESS", infoById.get().getReceipt().getResult().toString());
    Assert.assertTrue(infoById.get().getFee() < 1000000000);
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // totalSupply is 0
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString("trx");
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + 0 + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    logger.info("totalSupply is 0");
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("SUCCESS", infoById.get().getReceipt().getResult().toString());
    Assert.assertTrue(infoById.get().getFee() < 1000000000);
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // precision is 7
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 7;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // precision is -1
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + -1;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());

    // assetAbbr is trx will success
    tokenName = PublicMethed.stringToHexString(name);
    tokenAbbr = PublicMethed.stringToHexString("trx");
    param = "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    AssetIssueContract assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals("trx", ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(1, assetV2Map.size());

    // created multiple times will fail
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(1, assetV2Map.size());
    String assetIssueId1 = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID()
        .toStringUtf8();
    Assert.assertEquals(assetIssueId, assetIssueId1);
  }

  @Test(enabled = false, description = "tokenIssue trx balance insufficient")
  public void tokenIssue002TrxBalanceInsufficient() {
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue001.sol";
    String contractName = "tvmAssetIssue001";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 1023999999L;

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
    long contractAddressBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    // trx balance insufficient
    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param = "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    Map<String, Long> assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());
  }

  @Test(enabled = false, description = "tokenIssue called multiple times in one contract")
  public void tokenIssue003CalledMultipleTimesInOneContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue002.sol";
    String contractName = "tvmAssetIssue002";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 1024000000L;

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
    long contractAddressBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param = "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 5;
    logger.info("param: " + param);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);

    Map<String, Long> assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(1, assetV2Map.size());
    assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long assetIssueValue = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(5, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "tokenIssue revert")
  public void tokenIssue004Revert() {
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue003.sol";
    String contractName = "tvmAssetIssue003";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 2500000000L;

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
    long contractAddressBalance = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param = "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 4;
    logger.info("param: " + param);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    String txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    Map<String, Long> assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(1, assetV2Map.size());
    assetIssueId = PublicMethed.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    long assetIssueValue = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(4, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));

    String tokenName1 = PublicMethed.stringToHexString(name + "_rev");
    String tokenAbbr1 = PublicMethed.stringToHexString(abbr + "_rev");
    param =
        "\"" + tokenName1 + "\",\"" + tokenAbbr1 + "\",\"" + 1000000 + "\",\"" + 3 + "\",\""
            + Base58.encode58Check(dev002Address) + "\"";
    logger.info("param: " + param);
    String methodTokenIssueRevert = "tokenIssueAndTransfer(bytes32,bytes32,uint64,uint8,address)";
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssueRevert, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(1, assetV2Map.size());
    String assetIssueId1 = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId1: " + assetIssueId1);
    Assert.assertEquals(assetIssueId, assetIssueId1);
    assetIssueValue = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(4, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));

    long balance = PublicMethed.queryAccount(dev002Address, blockingStubFull).getBalance();
    Assert.assertEquals(200000000L, balance);
  }

  @Test(enabled = false, description = "tokenIssue call another contract in one contract")
  public void tokenIssue005CallAnotherInOneContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue004.sol";
    String contractName = "tvmAssetIssue004";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    long callvalue = 1030000000L;
    String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    callvalue = 1024000000L;
    String txid = PublicMethed.triggerContract(contractAddress, "getContractAddress()", "#", false,
        callvalue, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    String addressHex =
        "41" + ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("address_hex: " + addressHex);
    byte[] contractAddressA = ByteArray.fromHexString(addressHex);
    logger.info("contractAddressA: " + Base58.encode58Check(contractAddressA));
    contractAddressBalance = PublicMethed.queryAccount(contractAddressA, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(dev001Address, blockingStubFull);
    Account info = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String tokenName = PublicMethed.stringToHexString(name);
    String tokenAbbr = PublicMethed.stringToHexString(abbr);
    String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 2;
    logger.info("param: " + param);
    String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    txid = PublicMethed.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    assetIssueId = PublicMethed.queryAccount(contractAddressA, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    Map<String, Long> assetV2Map = PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());
    long assetIssueValue = PublicMethed.queryAccount(contractAddressA, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethed
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(2, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddressA),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));

    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(dev001Address, blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    long contractAddressBalance2 = PublicMethed.queryAccount(contractAddressA, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance - 1024000000L, contractAddressBalance2);

    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    String methodTransferToken = "transferToken(address,uint256,trcToken)";
    txid = PublicMethed.triggerContract(contractAddressA, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    long assetIssueValueAfter = PublicMethed.queryAccount(contractAddressA, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev002AssetValue = PublicMethed
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(100L, dev002AssetValue);
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
