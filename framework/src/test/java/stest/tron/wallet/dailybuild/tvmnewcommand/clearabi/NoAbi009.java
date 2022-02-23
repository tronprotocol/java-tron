package stest.tron.wallet.dailybuild.tvmnewcommand.clearabi;

import static org.hamcrest.core.StringContains.containsString;

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
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContractDataWrapper;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class NoAbi009 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress1 = ecKey2.getAddress();
  String contractExcKey1 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
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

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress1, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "clearabi contract with event")
  public void testNoAbi001() {
    String filePath = "src/test/resources/soliditycode/NoAbi001.sol";
    String contractName = "testNoABiContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
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

    String txid = PublicMethed
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    // getcontract
    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    System.out.println("smartContract:" + smartContract.toString());
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

    // getcontractinfo
    SmartContractDataWrapper contractInfo = PublicMethed
        .getContractInfo(contractAddress, blockingStubFull);
    System.out.println("contractInfo.toString():" + contractInfo.toString());
    Assert.assertTrue(contractInfo.getSmartContract().getAbi().toString().isEmpty());
    Assert.assertTrue(contractInfo.getSmartContract().getName().equalsIgnoreCase(contractName));
    Assert.assertTrue(contractInfo.getRuntimecode().size() > 0);
    Assert.assertFalse(contractInfo.getSmartContract().getBytecode().toString().isEmpty());

    // triggerconstantcontract fullnode
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "testTrigger()", "#", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("Code = " + transactionExtention.getResult().getCode());
    logger.info("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals(3,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // triggercontract
    txid = PublicMethed
        .triggerContract(contractAddress, "testTrigger()", "#", false, 0, maxFeeLimit, "0", 0,
            contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Long returnNumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(3 == returnNumber);
    List<String> retList = PublicMethed
        .getStrings(infoById.get().getLogList().get(0).getData().toByteArray());
    logger.info("retList:" + retList.toString());
    byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(1)), 12, tmpAddress, 0, 20);
    String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
    String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);
    Assert.assertEquals(WalletClient.encode58Check(contractExcAddress),addressFinal);
    Long actualNum = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
    Assert.assertEquals(returnNumber,actualNum);

    // triggerconstantcontract solidity
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtentionOnSolidity(contractAddress, "testTrigger()", "#",
            false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubSolidity);
    logger.info("Code = " + transactionExtention.getResult().getCode());
    logger.info("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals(4,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "create2 contract with event")
  public void testNoAbi002() {
    String filePath = "./src/test/resources/soliditycode/NoAbi002.sol";
    String contractName = "Factory";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, contractExcKey1,
            contractExcAddress1, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }
    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    byte[] factoryContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(factoryContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    contractName = "testNoABiContract";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String testNoABiContractCode = retMap.get("byteCode").toString();
    Long salt = 1L;
    String param = "\"" + testNoABiContractCode + "\"," + salt;
    final String triggerTxid = PublicMethed.triggerContract(factoryContractAddress,
        "deploy(bytes,uint256)", param, false, Long.valueOf(0),
        1000000000L, "0", 0, contractExcAddress1, contractExcKey1,
        blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());
    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }
    // the contract address in transaction info,
    // contract address of create2 contract is factory contract
    Assert.assertEquals(Base58.encode58Check(factoryContractAddress),
        Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));

    logger.info(
        "the value: " + PublicMethed
            .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray()));
    List<String> retList = PublicMethed
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());

    byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(0)), 12, tmpAddress, 0, 20);
    String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
    String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);

    byte[] testNoABiContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    // getcontract
    smartContract = PublicMethed.getContract(testNoABiContractAddress, blockingStubFull);
    System.out.println("smartContract:" + smartContract.toString());
    // the contract owner of contract created by create2 is the factory contract
    Assert.assertEquals(Base58.encode58Check(factoryContractAddress),
        Base58.encode58Check(smartContract.getOriginAddress().toByteArray()));
    // contract created by create2, doesn't have ABI
    Assert.assertEquals(0, smartContract.getAbi().getEntrysCount());
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    // Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());

    // getcontractinfo
    SmartContractDataWrapper contractInfo = PublicMethed
        .getContractInfo(testNoABiContractAddress, blockingStubFull);
    System.out.println("contractInfo.toString():" + contractInfo.toString());
    Assert.assertTrue(contractInfo.getSmartContract().getAbi().toString().isEmpty());
    // Assert.assertTrue(contractInfo.getSmartContract().getName().equalsIgnoreCase(contractName));
    Assert.assertTrue(contractInfo.getRuntimecode().size() > 0);
    Assert.assertFalse(contractInfo.getSmartContract().getBytecode().toString().isEmpty());

    // triggerconstantcontract fullnode
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(testNoABiContractAddress, "plusOne()", "#", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("Code = " + transactionExtention.getResult().getCode());
    logger.info("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals(1,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    // triggercontract
    String txid = PublicMethed
        .triggerContract(testNoABiContractAddress, "plusOne()", "#", false, 0, maxFeeLimit, "0", 0,
            contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Long returnNumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(1 == returnNumber);
    retList = PublicMethed
        .getStrings(infoById.get().getLogList().get(0).getData().toByteArray());
    logger.info("retList:" + retList.toString());
    tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(1)), 12, tmpAddress, 0, 20);
    addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
    addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);
    Assert.assertEquals(WalletClient.encode58Check(contractExcAddress),addressFinal);
    Long actualNum = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
    Assert.assertEquals(returnNumber,actualNum);

    // triggerconstantcontract solidity
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    transactionExtention = PublicMethed
        .triggerConstantContractForExtentionOnSolidity(testNoABiContractAddress, "plusOne()", "#",
            false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubSolidity);
    logger.info("Code = " + transactionExtention.getResult().getCode());
    logger.info("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals(2,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    PublicMethed.freedResource(contractExcAddress1, contractExcKey1, testNetAccountAddress,
        blockingStubFull);
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
