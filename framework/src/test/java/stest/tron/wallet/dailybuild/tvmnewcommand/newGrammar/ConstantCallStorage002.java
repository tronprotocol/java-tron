package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ConstantCallStorage002 {

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

  @BeforeClass(enabled = false)
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

  @Test(enabled = false, description = "TriggerconstantContract trigger modidy storage date with "
      + "difference date type")
  public void testConstantCallStorage001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/constantCallStorage001.sol";
    String contractName = "viewCall";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, "[]", code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
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

    // modify bool type
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeBool(bool)", "true", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getBool()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(0,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // modify NegativeInt type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeNegativeInt(int256)", "-2", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(-2,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getNegativeInt()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(-32482989,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // modify address type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeAddress(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    String ContractResult =
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    String tmpAddress =
        Base58.encode58Check(ByteArray.fromHexString("41" + ContractResult.substring(24)));
    Assert.assertEquals(WalletClient.encode58Check(contractAddress), tmpAddress);

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getAddress()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("000000000000000000000000dcad3a6d3569df655070ded06cb7a1b2ccd1d3af",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // modify byte32s type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeBytes32(bytes32)", "\"0xdCad3a6d3569DF655070DEd1\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("dcad3a6d3569df655070ded10000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getBytes32()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000dcad3a6d3569df655070ded0",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // modify bytes type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeBytes(bytes)", "\"0x05\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000001"
            + "0500000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getBytes()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));

    // modify string type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeString(string)", "\"321test\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("321test",
        ByteArray.toStr(transactionExtention
            .getConstantResult(0).substring(64, 64 + 7).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getString()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("123qwe",
        ByteArray.toStr(transactionExtention
            .getConstantResult(0).substring(64, 64 + 6).toByteArray()));

    // modify enum type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeActionChoices(uint8)", "3",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(3,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getActionChoices()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // modify Int64NegativeArray type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeInt64NegativeArray(int64[])",
            "0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            true,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        ByteArray.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getInt64NegativeArray()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd",
        ByteArray.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

    // modify Int32Array[2][] type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeInt32Array(int32[2][])",
            "0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000005"
                + "0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "0000000000000000000000000000000000000000000000000000000000000005"
                + "0000000000000000000000000000000000000000000000000000000000000006"
                + "0000000000000000000000000000000000000000000000000000000000000007"
                + "0000000000000000000000000000000000000000000000000000000000000008"
                + "0000000000000000000000000000000000000000000000000000000000000009"
                + "000000000000000000000000000000000000000000000000000000000000000a",
            true,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000005"
            + "0000000000000000000000000000000000000000000000000000000000000001"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "0000000000000000000000000000000000000000000000000000000000000004"
            + "0000000000000000000000000000000000000000000000000000000000000005"
            + "0000000000000000000000000000000000000000000000000000000000000006"
            + "0000000000000000000000000000000000000000000000000000000000000007"
            + "0000000000000000000000000000000000000000000000000000000000000008"
            + "0000000000000000000000000000000000000000000000000000000000000009"
            + "000000000000000000000000000000000000000000000000000000000000000a",
        ByteArray.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getInt32Array()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "0000000000000000000000000000000000000000000000000000000000000001"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "0000000000000000000000000000000000000000000000000000000000000004"
            + "0000000000000000000000000000000000000000000000000000000000000005"
            + "0000000000000000000000000000000000000000000000000000000000000006",
        ByteArray.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

    // modify Int256Array[2][2] type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "changeInt256Array(int256[2][2])",

            "0000000000000000000000000000000000000000000000000000000000000001"
                + "0000000000000000000000000000000000000000000000000000000000000002"
                + "0000000000000000000000000000000000000000000000000000000000000003"
                + "0000000000000000000000000000000000000000000000000000000000000004",
            true,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000001"
            + "0000000000000000000000000000000000000000000000000000000000000002"
            + "0000000000000000000000000000000000000000000000000000000000000003"
            + "0000000000000000000000000000000000000000000000000000000000000004",
        ByteArray.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "getInt256Array()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(
        "000000000000000000000000000000000000000000000000000000000000000b"
            + "0000000000000000000000000000000000000000000000000000000000000016"
            + "0000000000000000000000000000000000000000000000000000000000000021"
            + "000000000000000000000000000000000000000000000000000000000000002c",
        ByteArray.toHexString(transactionExtention
            .getConstantResult(0).toByteArray()));

    // modify mapping type
    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "setMapping(uint256)", "55",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(55,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "mapa(address)", "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\"", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(34,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

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
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
