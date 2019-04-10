package stest.tron.wallet.solidityadd;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
public class contractToMathedFeed {

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

  byte[] contractToMathedFeed = null;
  byte[] contractToMathedUseINContract = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

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
  public void test1Grammar001() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
                .sendcoin(contractExcAddress, 100000000000L,
                        testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/contractToMathedFeed.sol";
    String contractName = "ToMathedFeed";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("code:" + code);
    logger.info("abi:" + abi);


    contractToMathedFeed = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
                0L, 100, null, contractExcKey,
                contractExcAddress, blockingStubFull);
    Protocol.Account info;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(
            contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    String para = "10";
    txid = PublicMethed.triggerContract(contractToMathedFeed,
                "ToMathed(uint256)", para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    Long netFee = infoById.get().getReceipt().getNetFee();
    logger.info("energyUsed:" + energyUsed);
    logger.info("netUsed:" + netUsed);

  }

  @Test(enabled = true, description = "Support function type")
  public void test1Grammar002() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
                .sendcoin(contractToMathedFeed, 100000000000L,
                        testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/contractToMathedFeed.sol";
    String contractName = "ToMathedUseINContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("code:" + code);
    logger.info("abi:" + abi);
    contractToMathedUseINContract = PublicMethed.deployContract(
            contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    Protocol.Account info;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
            .sendcoin(contractToMathedUseINContract, 100000000000L,
                    testNetAccountAddress, testNetAccountKey, blockingStubFull));
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(
            contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    String value = "10";
    String para = "\"" + Base58.encode58Check(contractToMathedFeed)
                + "\",\"" + value + "\"";
    txid = PublicMethed.triggerContract(contractToMathedUseINContract,
                "ToMathedIUseNR(address,uint256)", para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long fee = infoById.get().getFee();

    Long netFee = infoById.get().getReceipt().getNetFee();
    String note = ByteArray
                .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("energyUsed:" + energyUsed);
    logger.info("netUsed:" + netUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
            .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(returnnumber == 0);
  }


  @Test(enabled = true, description = "Support function type")
  public void test1Grammar003() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
                .sendcoin(contractToMathedFeed, 100000000000L,
                        testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/contractToMathedFeed.sol";
    String contractName = "ToMathedUseINContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    logger.info("code:" + code);
    logger.info("abi:" + abi);


    contractToMathedUseINContract = PublicMethed.deployContract(
            contractName, abi, code, "", maxFeeLimit,
                0L, 100, null, contractExcKey,
                contractExcAddress, blockingStubFull);
    Protocol.Account info;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
            .sendcoin(contractToMathedUseINContract, 100000000000L,
                    testNetAccountAddress, testNetAccountKey, blockingStubFull));
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(
            contractExcAddress, blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    String txid = "";
    String value = "10";
    String para = "\"" + Base58.encode58Check(contractToMathedFeed)
            + "\",\"" + value + "\"";
    txid = PublicMethed.triggerContract(contractToMathedUseINContract,
                "ToMathedIUseNRE(address,uint256)", para, false,
                0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull1);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    String note = ByteArray
            .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("energyUsed:" + energyUsed);
    logger.info("netUsed:" + netUsed);
    Long returnnumber = ByteArray.toLong(ByteArray
            .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    logger.info("returnnumber:" + returnnumber);
    Assert.assertTrue(returnnumber == 1);
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
