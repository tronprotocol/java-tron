package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import static org.hamcrest.core.StringContains.containsString;

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
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ConstantCallStorage001 {

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

  @Test(enabled = false, description = "TriggerconstantContract trigger modidy storage date")
  public void testConstantCallStorage001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/constantCallStorage001.sol";
    String contractName = "NotView";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, "[]", code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
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

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "setnum()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(138,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    logger.info("transactionExtention: " + transactionExtention);

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "num()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(123,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = false, description = "TriggerconstantContract storage date by another contract ")
  public void testConstantCallStorage002() {

    String filePath = "src/test/resources/soliditycode/constantCallStorage001.sol";
    String contractName = "UseNotView";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress002 = PublicMethed
        .deployContract(contractName, "[]", code, "", maxFeeLimit,
            0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress002, blockingStubFull);
    //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress002,
            "setnumuseproxy(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertEquals(138,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "num()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(123,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

  }


  @Test(enabled = false, description = "TriggerconstantContract storage date by another contract "
      + "view function, use 0.5.* version solidity complier")
  public void testConstantCallStorage003() {
    String filePath = "src/test/resources/soliditycode/constantCallStorage002.sol";
    String contractName = "UseNotView";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress002 = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress002, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress002,
            "setnumuseproxy(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertFalse(transactionExtention.getResult().getResult());
    Assert.assertThat(ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("Not enough energy"));
  }


  @Test(enabled = false, description = "TriggerconstantContract storage date by another contract "
      + "view function, use 0.4.* version solidity complier")
  public void testConstantCallStorage004() {
    String filePath = "src/test/resources/soliditycode/constantCallStorage002.sol";
    String contractName = "UseNotView";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress002 = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress002, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress002,
            "setnumuseproxy(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertEquals(138,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "num()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(123,
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
