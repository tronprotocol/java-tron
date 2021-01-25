package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MutiSignClearContractAbiTest {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  ArrayList<String> txidList = new ArrayList<String>();
  Optional<TransactionInfo> infoById = null;
  Long beforeTime;
  Long afterTime;
  Long beforeBlockNum;
  Long afterBlockNum;
  Block currentBlock;
  Long currentBlockNum;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey3.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test1MutiSignForClearContractAbi() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needcoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);

    Long maxFeeLimit = 1000000000L;
    String filePath = "src/test/resources/soliditycode/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, permissionKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, ownerAddress, blockingStubFull));
  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test2MutiSignForClearContractAbiForDefault() {
    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needcoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
        blockingStubFull, ownerKeyString);

    Long maxFeeLimit = 1000000000L;
    String filePath = "src/test/resources/soliditycode/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, permissionKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, permissionKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, ownerAddress, blockingStubFull));
  }


  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void test3MutiSignForClearContractAbiForDefault() {
    ecKey3 = new ECKey(Utils.getRandom());
    ownerAddress = ecKey3.getAddress();
    ownerKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    PublicMethed.printAddress(ownerKey);

    long needcoin = updateAccountPermissionFee + multiSignFee * 4;

    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needcoin + 100000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 0, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed
        .freezeBalanceForReceiver(fromAddress, 1000000000, 0, 1, ByteString.copyFrom(ownerAddress),
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    String[] activeDefaultKeyString = new String[1];

    activeDefaultKeyString[0] = ownerKey;
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long maxFeeLimit = 1000000000L;
    String filePath = "src/test/resources/soliditycode/TriggerConstant004.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethedForMutiSign.deployContract1(contractName, abi, code,
        "", maxFeeLimit,
        0L, 100, null, ownerKey, ownerAddress, blockingStubFull, 2, activeDefaultKeyString);
    logger.info("address:" + Base58.encode58Check(contractAddress));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    Assert.assertTrue(PublicMethedForMutiSign
        .clearContractAbi(contractAddress, ownerAddress, ownerKey,
            blockingStubFull, 2, activeDefaultKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 0, ownerAddress, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.unFreezeBalance(fromAddress, testKey002, 1, ownerAddress, blockingStubFull));
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