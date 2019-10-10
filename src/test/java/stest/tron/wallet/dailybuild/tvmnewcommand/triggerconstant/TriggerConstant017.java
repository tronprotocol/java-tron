package stest.tron.wallet.dailybuild.tvmnewcommand.triggerconstant;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
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
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TriggerConstant017 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private ManagedChannel channelRealSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubRealSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String realSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
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

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelRealSolidity = ManagedChannelBuilder.forTarget(realSoliditynode)
        .usePlaintext(true)
        .build();
    blockingStubRealSolidity = WalletSolidityGrpc.newBlockingStub(channelRealSolidity);

  }

  @Test(enabled = true, description = "TriggerConstantContract a constant function which is "
      + "deployed with ABI, but cleared ABI later")
  public void testTriggerConstantContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/ClearAbi001.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
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
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

    TransactionExtention transactionExtention1 = PublicMethed
        .triggerConstantContractForExtentionOnSolidity(contractAddress,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubSolidity);
    Transaction transaction1 = transactionExtention1.getTransaction();

    byte[] result1 = transactionExtention1.getConstantResult(0).toByteArray();
    System.out.println("message1:" + transaction1.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention1.getResult().getMessage().toByteArray()));
    System.out.println("Result1:" + Hex.toHexString(result1));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result1))));

    TransactionExtention transactionExtention2 = PublicMethed
        .triggerConstantContractForExtentionOnSolidity(contractAddress,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubRealSolidity);
    Transaction transaction2 = transactionExtention2.getTransaction();

    byte[] result2 = transactionExtention2.getConstantResult(0).toByteArray();
    System.out.println("message2:" + transaction2.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention2.getResult().getMessage().toByteArray()));
    System.out.println("Result2:" + Hex.toHexString(result2));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result2))));

    String txid = "";
    txid = PublicMethed
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

    TransactionExtention transactionExtention3 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Transaction transaction3 = transactionExtention3.getTransaction();

    byte[] result3 = transactionExtention3.getConstantResult(0).toByteArray();
    System.out.println("message3:" + transaction3.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention3.getResult().getMessage().toByteArray()));
    System.out.println("Result3:" + Hex.toHexString(result3));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result3))));

    TransactionExtention transactionExtention4 = PublicMethed
        .triggerConstantContractForExtentionOnSolidity(contractAddress,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubSolidity);
    Transaction transaction4 = transactionExtention4.getTransaction();

    byte[] result4 = transactionExtention4.getConstantResult(0).toByteArray();
    System.out.println("message4:" + transaction4.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention4.getResult().getMessage().toByteArray()));
    System.out.println("Result4:" + Hex.toHexString(result4));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result4))));

    TransactionExtention transactionExtention5 = PublicMethed
        .triggerConstantContractForExtentionOnSolidity(contractAddress,
            "testPayable()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubRealSolidity);
    Transaction transaction5 = transactionExtention5.getTransaction();

    byte[] result5 = transactionExtention5.getConstantResult(0).toByteArray();
    System.out.println("message5:" + transaction5.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention5.getResult().getMessage().toByteArray()));
    System.out.println("Result5:" + Hex.toHexString(result5));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result5))));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);

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
