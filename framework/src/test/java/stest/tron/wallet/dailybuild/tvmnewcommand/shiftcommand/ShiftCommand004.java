package stest.tron.wallet.dailybuild.tvmnewcommand.shiftcommand;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.DataWord;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ShiftCommand004 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);



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

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x0000000000000000000000000000000000000000000000000000000000000001 and Displacement number"
      + "is 0x00")
  public void test1ShiftLeft() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/ShiftCommand001.sol";
    String contractName = "TestBitwiseShift";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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
    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x00")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x0000000000000000000000000000000000000000000000000000000000000001 and Displacement number"
      + "is 0x01")
  public void test2ShiftLeft() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray
            .fromHexString("0x01"))
        .getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000002")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }


  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x0000000000000000000000000000000000000000000000000000000000000001 and Displacement number"
      + "is 0xff")
  public void test3ShiftLeft() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray
            .fromHexString("0xff"))
        .getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x8000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }


  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x0000000000000000000000000000000000000000000000000000000000000001 and Displacement number"
      + "is 0x0100")
  public void test4ShiftLeft() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x0100")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);
    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x0000000000000000000000000000000000000000000000000000000000000001 and Displacement number"
      + "is 0x0101")
  public void test5ShiftLeft() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x0101")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);
    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff and Displacement number"
      + "is 0x00")
  public void test6ShiftLeft() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x00")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);
    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff and Displacement number"
      + "is 0x01")
  public void test7ShiftLeft() {
    String filePath = "src/test/resources/soliditycode/ShiftCommand001.sol";
    String contractName = "TestBitwiseShift";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x01")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }


  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff and Displacement number"
      + "is 0xff")
  public void test8ShiftLeft() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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
    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0xff")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x8000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }


  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff and Displacement number"
      + "is 0x0100")
  public void test9ShiftLeft() {

    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x0100")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }


  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x0000000000000000000000000000000000000000000000000000000000000000 and Displacement number"
      + "is 0x01")
  public void testShiftLeft10() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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
    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x01")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff and Displacement number"
      + "is 0x01")
  public void testShiftLeft11() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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
    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0x7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x01")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(uint256,uint256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0xfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }

  @Test(enabled = true, description = "Trigger new ShiftLeft,value is "
      + "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff and Displacement number"
      + "is 0x0101")
  public void testShiftLeft12() {

    String filePath = "src/test/resources/soliditycode/TvmNewCommand043.sol";
    String contractName = "TestBitwiseShift";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
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

    byte[] originNumber = new DataWord(
        ByteArray
            .fromHexString("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))
        .getData();
    byte[] valueNumber = new DataWord(
        ByteArray.fromHexString("0x0101")).getData();
    byte[] paramBytes = new byte[originNumber.length + valueNumber.length];
    System.arraycopy(valueNumber, 0, paramBytes, 0, valueNumber.length);
    System.arraycopy(originNumber, 0, paramBytes, valueNumber.length, originNumber.length);
    String param = Hex.toHexString(paramBytes);

    txid = PublicMethed.triggerContract(contractAddress,
        "shlTest(int256,int256)", param, true,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    String returnString = (ByteArray
        .toHexString(infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnString:" + returnString);
    Assert.assertEquals(ByteArray.toLong(ByteArray
            .fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000")),
        ByteArray.toLong(ByteArray
            .fromHexString(
                ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
