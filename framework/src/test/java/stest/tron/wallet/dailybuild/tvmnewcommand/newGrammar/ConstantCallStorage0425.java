package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
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
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ConstantCallStorage0425 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private byte[] contractAddress = null;

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 100_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Deploy contract without abi")
  public void test01DeployContract() {
    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    Protocol.Account info = PublicMethed.queryAccount(dev001Key, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = accountResource.getEnergyUsed();
    Long beforeNetUsed = accountResource.getNetUsed();
    Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    String filePath = "./src/test/resources/soliditycode/constantCallStorage0425.sol";
    String contractName = "constantCall";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, "[]", code, "",
            maxFeeLimit, 0L, 0, 1000000000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

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

    Protocol.Account infoafter = PublicMethed.queryAccount(dev001Key, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethed
        .getAccountResource(dev001Address,
            blockingStubFull);
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
  }

  @Test(enabled = false, description = "Trigger contract constant function without ABI")
  public void test02TriggerContract() {
    String triggerTxid = PublicMethed
        .triggerContract(contractAddress, "changeBool(bool)", "true", false,
            0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getLog(0).getData().toByteArray()));

    String triggerTxid2 = PublicMethed
        .triggerContract(contractAddress, "getBool()", "", false,
            0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(triggerTxid2, blockingStubFull);
    Assert.assertEquals(0, infoById1.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById1.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, ByteArray.toInt(infoById1.get().getLog(0).getData().toByteArray()));
  }

  @Test(enabled = false, description = "TriggerConstantContract bool constant function")
  public void test03TriggerConstantContract() {
    // bool
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeBool(bool)", "false", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getBool()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // int
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeInt(int256)", "30", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(30, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getInt()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(32482989,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // negative int
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeNegativeInt(int256)", "-111",
            false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(-111,
            ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getNegativeInt()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(-32482989,
            ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // uint
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeUint(uint256)", "1024", false,
            0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(1024,
            ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getUint()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert
        .assertEquals(23487823,
            ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // address
    String param = "\"" + Base58.encode58Check(dev001Address) + "\"";
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeAddress(address)", param,
            false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    byte[] tmpAddress = new byte[20];
    System
        .arraycopy(transactionExtention.getConstantResult(0).toByteArray(), 12, tmpAddress, 0, 20);
    Assert.assertEquals(Base58.encode58Check(dev001Address),
        Base58.encode58Check(ByteArray.fromHexString("41" + ByteArray.toHexString(tmpAddress))));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getAddress()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    tmpAddress = new byte[20];
    System
        .arraycopy(transactionExtention.getConstantResult(0).toByteArray(), 12, tmpAddress, 0, 20);
    Assert.assertEquals("TW63BNR5M7LuH1fjXS7Smyza3PZXfHAAs2",
        Base58.encode58Check(ByteArray.fromHexString("41" + ByteArray.toHexString(tmpAddress))));

    // bytes32
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeBytes32(bytes32)",
            "b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a",
            true, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105a",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getBytes32()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("b55a21aaee0ce8f1c8ffaa0dbd23105cb55a21aaee0ce8f1c8ffaa0dbd23105c",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // bytes
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeBytes(bytes)", "\"0x06\"",
            false,
            0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000020000000000000000000"
            + "000000000000000000000000000000000000000000000106000000000000000000000000000000000000"
            + "00000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getBytes()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(
        "0000000000000000000000000000000000000000000000000000000000000020000000000000000000"
            + "000000000000000000000000000000000000000000000900000000000000000000000000000000000000"
            + "00000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // string
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeString(string)",
            "\"1q2w\"",
            false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000002000"
            + "000000000000000000000000000000000000000000000000000000000000043171327700000000000000"
            + "000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getString()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000002000"
            + "000000000000000000000000000000000000000000000000000000000000063132337177650000000000"
            + "000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // enum
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeActionChoices(uint8)",
            "1",
            false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getActionChoices()", "", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000003",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // int64[] include negative number
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeInt64NegativeArray(int64[])",
            "0000000000000000000000000000000000000000000000000000000000000020000000000000000"
                + "00000000000000000000000000000000000000000000000040000000000000000000000000000000"
                + "00000000000000000000000000000000b00000000000000000000000000000000000000000000000"
                + "00000000000000063000000000000000000000000000000000000000000000000000000000000004"
                + "1000000000000000000000000000000000000000000000000000000000000005a",
            true, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000002000"
            + "000000000000000000000000000000000000000000000000000000000000040000000000000000000000"
            + "00000000000000000000000000000000000000000b000000000000000000000000000000000000000000"
            + "000000000000000000006300000000000000000000000000000000000000000000000000000000000000"
            + "41000000000000000000000000000000000000000000000000000000000000005a",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getInt64NegativeArray()", "", false,
            0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000002000"
            + "000000000000000000000000000000000000000000000000000000000000030000000000000000000000"
            + "00000000000000000000000000000000000000005b000000000000000000000000000000000000000000"
            + "000000000000000000000200000000000000000000000000000000000000000000000000000000000001"
            + "4d",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // int32[2][]
    String argsStr =
        "0000000000000000000000000000000000000000000000000000000000000020000000000000000"
            + "00000000000000000000000000000000000000000000000030000000000000000000000000000000"
            + "00000000000000000000000000000000d00000000000000000000000000000000000000000000000"
            + "00000000000000058fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
            + "70000000000000000000000000000000000000000000000000000000000000022000000000000000"
            + "0000000000000000000000000000000000000000000000063fffffffffffffffffffffffffffffff"
            + "fffffffffffffffffffffffffffffffc8";
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeInt32Array(int32[2][])",
            argsStr, true, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(argsStr,
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getInt32Array()", "", false,
            0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000002000"
            + "000000000000000000000000000000000000000000000000000000000000030000000000000000000000"
            + "000000000000000000000000000000000000000001000000000000000000000000000000000000000000"
            + "000000000000000000000200000000000000000000000000000000000000000000000000000000000000"
            + "030000000000000000000000000000000000000000000000000000000000000004000000000000000000"
            + "000000000000000000000000000000000000000000000500000000000000000000000000000000000000"
            + "00000000000000000000000006",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // int256[2][2]
    String argsStr1 =
        "0000000000000000000000000000000000000000000000000000000000000013000000000000000"
            + "00000000000000000000000000000000000000000000000440000000000000000000000000000000"
            + "000000000000000000000000000000037fffffffffffffffffffffffffffffffffffffffffffffff"
            + "fffffffffffffffde";
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "changeInt256Array(int256[2][2])",
            argsStr1, true, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(argsStr1,
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getInt256Array()", "", false,
            0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000000000000b00"
            + "000000000000000000000000000000000000000000000000000000000000160000000000000000000000"
            + "000000000000000000000000000000000000000021000000000000000000000000000000000000000000"
            + "000000000000000000002c",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // modify mapping type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "setMapping(uint256)", "39",
            false,
            0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(39,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "mapa(address)", "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"", false,
            0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(88,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethed.sendcoin(fromAddress, balance, dev001Address, dev001Key,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}