package stest.tron.wallet.solidityadd;

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
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class addTrcToken001Assemble {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
          .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
            .getStringList("fullnode.ip.list").get(0);

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static String tokenName2 = "testAssetIssue2_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static ByteString assetAccountId2 = null;
  private static final long TotalSupply = 1000L;
  private String description = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.assetUrl");

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] toAddress = ecKey2.getAddress();
  String toAddressKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress2 = ecKey3.getAddress();
  String contractExcKey2 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  private static long beforeCreateAssetIssueBalance;
  private static long afterCreateAssetIssueBalance;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
                .usePlaintext(true)
                .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
                .usePlaintext(true)
                .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Support function type")
  public void test1deployandgetBalance() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
         .sendcoin(contractExcAddress, 100000000000L, testNetAccountAddress,
                 testNetAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed
            .sendcoin(contractExcAddress2, 100000000000L, testNetAccountAddress,
                    testNetAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed
            .sendcoin(toAddress, 100000000000L, testNetAccountAddress,
                    testNetAccountKey, blockingStubFull));
    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
    //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethed.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L, 100000L,
            1L, 1L, contractExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.Account getAssetIdFromThisAccount = PublicMethed
            .queryAccount(contractExcAddress, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    long start2 = System.currentTimeMillis() + 2000;
    long end2 = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethed.createAssetIssue(contractExcAddress2, tokenName2, TotalSupply, 1,
            10000, start2, end2, 1, description, url, 100000L, 100000L,
            1L, 1L, contractExcKey2, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.Account getAssetIdFromThisAccount2 = PublicMethed
            .queryAccount(contractExcAddress2, blockingStubFull);
    assetAccountId2 = getAssetIdFromThisAccount2.getAssetIssuedID();

    String filePath = "src/test/resources/soliditycode/addTrcToken001Assemble.sol";
    String contractName = "InAssemble";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("code:" + code);
    logger.info("abi:" + abi);


    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
             0L, 100, null, contractExcKey,
                contractExcAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
              .sendcoin(contractAddress, 100000000000L, testNetAccountAddress,
                      testNetAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.transferAsset(contractAddress,
            assetAccountId.toByteArray(), 100L, contractExcAddress,
            contractExcKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfo =
            PublicMethed.getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info;
    info = PublicMethed.queryAccount(toAddressKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    String txid = "";
    String tokenvalue = "";
    String para = "\"" + Base58.encode58Check(toAddress)
            + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
                "getBalance(address)", para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
  }

  @Test(enabled = true, description = "Support function type")
  public void test2getTokenBalanceConstant() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = "";
    String tokenid = assetAccountId.toStringUtf8();
    String para = "\"" + Base58.encode58Check(toAddress)
            + "\",\"" + tokenid + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
            "getTokenBalanceConstant(address,trcToken)", para, false,
            0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
  }

  @Test(enabled = true, description = "Support function type")
  public void test3getTokenBalance() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = "";
    String tokenid = assetAccountId.toStringUtf8();
    String para = "\"" + Base58.encode58Check(toAddress)
            + "\",\"" + tokenid + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
            "getTokenBalance(address,trcToken)", para, false,
            0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
            .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(returnnumber == 0);
  }

  @Test(enabled = true, description = "Support function type")
  public void test4transferTokenInAssembly() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long beforeAssetIssuetoAddress = PublicMethed
            .getAssetIssueValue(toAddress, assetAccountId, blockingStubFull);
    Long beforeAssetIssuecontractAddress = PublicMethed
            .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    String txid = "";
    String tokenid = assetAccountId.toStringUtf8();
    Long tokenvalue = 1L;
    String para = "\"" + Base58.encode58Check(toAddress)
            + "\",\"" + tokenid + "\",\"" + tokenvalue + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
            "transferTokenInAssembly(address,trcToken,uint256)", para, false,
            0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    Long afterAssetIssuetoAddress = PublicMethed
            .getAssetIssueValue(toAddress, assetAccountId, blockingStubFull);
    Long afterAssetIssuecontractAddress = PublicMethed
            .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeAssetIssuetoAddress:" + beforeAssetIssuetoAddress);
    logger.info("beforeAssetIssuecontractAddress:" + beforeAssetIssuecontractAddress);
    logger.info("afterAssetIssuetoAddress:" + afterAssetIssuetoAddress);
    logger.info("afterAssetIssuecontractAddress:" + afterAssetIssuecontractAddress);
    Assert.assertTrue(beforeAssetIssuetoAddress == afterAssetIssuetoAddress - 1L);
    Assert.assertTrue(beforeAssetIssuecontractAddress == afterAssetIssuecontractAddress + 1L);
  }

  @Test(enabled = true, description = "Support function type")
  public void test5trcTokenInMap() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = "";
    String tokenid = assetAccountId.toStringUtf8();
    Long tokenvalue = 1L;
    String para = "\"" + tokenid
            + "\",\"" + tokenvalue + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
            "trcTokenInMap(trcToken,uint256)", para, false,
            0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
            .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(returnnumber == 1);
  }

  @Test(enabled = true, description = "Support function type")
  public void test6cntTokenTokenInMap() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = "";
    String tokenid1 = assetAccountId.toStringUtf8();
    String tokenid2 = assetAccountId2.toStringUtf8();
    Long tokenvalue = 10L;
    String para = "\"" + tokenid1
            + "\",\"" + tokenid2 + "\",\"" + tokenvalue + "\"";
    txid = PublicMethed.triggerContract(contractAddress,
            "cntTokenTokenInMap(trcToken,trcToken,uint256)", para, false,
            0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
            .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    logger.info("returnnumber:" + returnnumber);
    Assert.assertTrue(returnnumber == Long.parseLong(assetAccountId2.toStringUtf8()));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
