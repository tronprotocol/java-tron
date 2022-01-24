package stest.tron.wallet.dailybuild.tvmnewcommand.validatemultisign;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import stest.tron.wallet.common.client.utils.TransactionUtils;

@Slf4j
public class TestValidatemultisign002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  ByteString assetAccountId1;
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey001.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey002.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  ECKey ecKey003 = new ECKey(Utils.getRandom());
  byte[] ownerAddress = ecKey003.getAddress();
  String ownerKey = ByteArray.toHexString(ecKey003.getPrivKeyBytes());
  ECKey ecKey004 = new ECKey(Utils.getRandom());
  byte[] manager4Address = ecKey004.getAddress();
  String manager4Key = ByteArray.toHexString(ecKey004.getPrivKeyBytes());
  ECKey ecKey005 = new ECKey(Utils.getRandom());
  byte[] manager5Address = ecKey005.getAddress();
  String manager5Key = ByteArray.toHexString(ecKey005.getPrivKeyBytes());
  ECKey ecKey006 = new ECKey(Utils.getRandom());
  byte[] manager6Address = ecKey006.getAddress();
  String manager6Key = ByteArray.toHexString(ecKey006.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "Deploy validatemultisign contract")
  public void test001DeployContract() {
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 100_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

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

    String filePath = "./src/test/resources/soliditycode/validatemultisign001.sol";
    String contractName = "validatemultisignTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    PublicMethed.printAddress(ownerKey);

    long needCoin = updateAccountPermissionFee * 1 + multiSignFee * 3;
    Assert.assertTrue(
        PublicMethed.sendcoin(ownerAddress, needCoin + 2048000000L, fromAddress, testKey002,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull).getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager4Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(manager5Key) + "\",\"weight\":1},"
            + "]}]}";

    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdate(accountPermissionJson, ownerAddress, ownerKey,
            blockingStubFull, ownerKeyString));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Trigger validatemultisign with signatures num")
  public void test002validatemultisign() {
    List<Object> signatures = new ArrayList<>();
    int permissionId = 2;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;

    Transaction transaction = PublicMethedForMutiSign.sendcoinWithPermissionIdNotSign(
        fromAddress, 1L, ownerAddress, permissionId, ownerKey, blockingStubFull);
    transaction = TransactionUtils.setTimestamp(transaction);
    byte[] hash = Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        transaction.getRawData().toByteArray()).getBytes();

    byte[] merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(permissionId), hash);
    byte[] tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));

    // Trigger with one signature
    List<Object> parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    String input = PublicMethed.parametersString(parameters);

    String methodStr = "testmulti(address,uint256,bytes32,bytes[])";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with five signature
    signatures.clear();
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey002.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey005.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with six signature
    signatures.clear();
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey002.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey003.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey005.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey006.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "Trigger validatemultisign with Duplicate signatures")
  public void test003validatemultisign() {
    List<Object> signatures = new ArrayList<>();
    int permissionId = 2;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;

    Transaction transaction = PublicMethedForMutiSign.sendcoinWithPermissionIdNotSign(
        fromAddress, 1L, ownerAddress, permissionId, ownerKey, blockingStubFull);
    transaction = TransactionUtils.setTimestamp(transaction);
    byte[] hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    byte[] merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(permissionId), hash);
    byte[] tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    // signatures with Duplicate signatures but weight enough
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey002.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));

    List<Object> parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    String input = PublicMethed.parametersString(parameters);

    String methodStr = "testmulti(address,uint256,bytes32,bytes[])";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with Duplicate signatures and weight not enough
    signatures.clear();
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

    // Trigger with Duplicate signatures and fix signatures
    signatures.clear();
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey005.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey005.sign(tosign).toByteArray()));

    parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    input = PublicMethed.parametersString(parameters);

    TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "Trigger validatemultisign with weight")
  public void test004validatemultisign() {
    List<Object> signatures = new ArrayList<>();
    int permissionId = 2;
    ownerKeyString[0] = ownerKey;
    ownerKeyString[1] = manager1Key;

    Transaction transaction = PublicMethedForMutiSign.sendcoinWithPermissionIdNotSign(
        fromAddress, 1L, ownerAddress, permissionId, ownerKey, blockingStubFull);
    transaction = TransactionUtils.setTimestamp(transaction);
    byte[] hash = Sha256Hash.of(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), transaction.getRawData().toByteArray()).getBytes();

    byte[] merged = ByteUtil.merge(ownerAddress, ByteArray.fromInt(permissionId), hash);
    byte[] tosign = Sha256Hash.hash(CommonParameter.getInstance()
        .isECKeyCryptoEngine(), merged);

    // signatures with weight not enough
    signatures.add(Hex.toHexString(ecKey001.sign(tosign).toByteArray()));
    signatures.add(Hex.toHexString(ecKey004.sign(tosign).toByteArray()));

    List<Object> parameters = Arrays.asList(StringUtil.encode58Check(ownerAddress),
        permissionId, "0x" + Hex.toHexString(hash), signatures);
    String input = PublicMethed.parametersString(parameters);

    String methodStr = "testmulti(address,uint256,bytes32,bytes[])";
    String TriggerTxid = PublicMethed.triggerContract(contractAddress, methodStr, input, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(TriggerTxid, blockingStubFull);
    logger.info("infoById" + infoById);

    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));

  }


}
