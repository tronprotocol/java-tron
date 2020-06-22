package stest.tron.wallet.dailybuild.tvmnewcommand.batchValidateSignContract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Hash;
import org.tron.common.utils.Utils;
import org.tron.common.utils.WalletUtil;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class batchValidateSignContract007 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext(true).build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Constructor test multivalidatesign")
  public void test01Constructor() {
    String txid = PublicMethed
        .sendcoinGetTransactionId(contractExcAddress, 2000000000L, testNetAccountAddress,
            testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull);
    Protocol.Account info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "src/test/resources/soliditycode/batchvalidatesign007.sol";
    String contractName = "Demo";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = Hash.sha3(txid.getBytes());
    for (int i = 0; i < 16; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletUtil.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String data = PublicMethed.parametersString(parameters);
    String constructorStr = "constructor(bytes32,bytes[],address[])";
    txid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Long fee1 = infoById.get().getFee();
    Long netUsed1 = infoById.get().getReceipt().getNetUsage();
    Long energyUsed1 = infoById.get().getReceipt().getEnergyUsage();
    Long netFee1 = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal1 = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee1:" + fee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    logger.info("energyUsageTotal1:" + energyUsageTotal1);
    contractAddress = infoById.get().getContractAddress().toByteArray();

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testConstructorPure()", "", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Assert.assertEquals("11111111111111110000000000000000",
        PublicMethed.bytes32ToString(transactionExtention.getConstantResult(0).toByteArray()));
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());

    txid = PublicMethed
        .triggerContract(contractAddress, "testConstructor()", "", false, 0, maxFeeLimit,
            contractExcAddress, contractExcKey, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    if (infoById2.get().getResultValue() == 0) {
      Assert.assertEquals("11111111111111110000000000000000",
          PublicMethed.bytes32ToString(infoById2.get().getContractResult(0).toByteArray()));
    } else {
      Assert.assertTrue("CPU timeout for 'PUSH1' operation executing"
          .equals(infoById2.get().getResMessage().toStringUtf8()) || "Already Time Out"
          .equals(infoById2.get().getResMessage().toStringUtf8()));
      PublicMethed.waitProduceNextBlock(blockingStubFull);
    }
    Long fee2 = infoById2.get().getFee();
    Long netUsed2 = infoById2.get().getReceipt().getNetUsage();
    Long energyUsed2 = infoById2.get().getReceipt().getEnergyUsage();
    Long netFee2 = infoById2.get().getReceipt().getNetFee();
    long energyUsageTotal2 = infoById2.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee2:" + fee2);
    logger.info("netUsed2:" + netUsed2);
    logger.info("energyUsed2:" + energyUsed2);
    logger.info("netFee2:" + netFee2);
    logger.info("energyUsageTotal2:" + energyUsageTotal2);

    Protocol.Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(contractExcAddress, blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee1 + fee2 == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed1 + energyUsed2 >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed1 + netUsed2 >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed1 + netUsed2 >= afterNetUsed);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(contractExcKey, blockingStubFull).getBalance();
    PublicMethed.sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
