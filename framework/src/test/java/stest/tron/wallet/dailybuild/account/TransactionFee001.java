package stest.tron.wallet.dailybuild.account;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Sha256Hash;



@Slf4j

public class TransactionFee001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
          .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String witnessKey01 = Configuration.getByPath("testng.conf")
          .getString("witness.key1");
  private final byte[] witnessAddress01 = PublicMethed.getFinalAddress(witnessKey01);
  private final String witnessKey02 = Configuration.getByPath("testng.conf")
          .getString("witness.key2");
  private final byte[] witnessAddress02 = PublicMethed.getFinalAddress(witnessKey02);
  private long multiSignFee = Configuration.getByPath("testng.conf")
          .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
          .getLong("defaultParameter.updateAccountPermissionFee");
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
          .getLong("defaultParameter.maxFeeLimit");
  private final String blackHoleAdd = Configuration.getByPath("testng.conf")
          .getString("defaultParameter.blackHoleAddress");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] deployAddress = ecKey1.getAddress();
  final String deployKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private String fullnode = Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(0);

  Long startNum = 0L;
  Long endNum = 0L;
  Long witness01Allowance1 = 0L;
  Long witness02Allowance1 = 0L;
  Long blackHoleBalance1 = 0L;
  Long witness01Allowance2 = 0L;
  Long witness02Allowance2 = 0L;
  Long blackHoleBalance2 = 0L;
  Long witness01Increase = 0L;
  Long witness02Increase = 0L;
  Long blackHoleIncrease = 0L;
  String txid = null;


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
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Test deploy contract with energy fee to sr")
  public void test01DeployContractEnergyFeeToSr() {

    Assert.assertTrue(PublicMethed.sendcoin(deployAddress, 20000000000L, fromAddress,
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode//contractLinkage003.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = null;
    String code = null;
    String abi = null;
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();

    startNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
       .getBlockHeader().getRawData().getNumber();
    witness01Allowance1 = PublicMethed.queryAccount(witnessAddress01, blockingStubFull)
       .getAllowance();
    witness02Allowance1 = PublicMethed.queryAccount(witnessAddress02, blockingStubFull)
       .getAllowance();
    blackHoleBalance1 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();

    txid = PublicMethed
            .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
                    0L, 0, null,
                    deployKey, deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    endNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
       .getBlockHeader().getRawData().getNumber();
    witness01Allowance2 = PublicMethed.queryAccount(witnessAddress01, blockingStubFull)
       .getAllowance();
    witness02Allowance2 = PublicMethed.queryAccount(witnessAddress02, blockingStubFull)
       .getAllowance();
    blackHoleBalance2 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info("-----black hole 1: " + blackHoleBalance1 + "  black hole 2: " + blackHoleBalance2
            + "   increase :" + blackHoleIncrease);
    logger.info("====== witness02Allowance1 :" + witness02Allowance1 + "   witness02Allowance2 :"
            + witness02Allowance2 + "increase :" + witness02Increase);
    logger.info("====== witness01Allowance1 :" + witness01Allowance1 + "  witness01Allowance2 :"
            + witness01Allowance2 + "  increase :" + witness01Increase);

    Map<String, Long> witnessAllowance = PublicMethed.getAllowance2(startNum, endNum,
            blockingStubFull);

    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress01))
            - witness01Increase)) <= 2);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress02))
            - witness02Increase)) <= 2);
    Assert.assertEquals(blackHoleBalance1, blackHoleBalance2);

  }

  @Test(enabled = true, description = "Test update account permission fee to black hole,"
          + "trans with multi sign and fee to sr")
  public void test02UpdateAccountPermissionAndMultiSiginTrans() {

    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
    byte[] tmpAddr02 = tmpEcKey02.getAddress();
    final String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2 + multiSignFee;

    Assert.assertTrue(PublicMethed.sendcoin(ownerAddress, needCoin + 1_000_000, fromAddress,
            testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethed.queryAccount(ownerAddress, blockingStubFull)
            .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();
    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(witnessKey01);
    activePermissionKeys.add(tmpKey02);

    logger.info("** update owner and active permission to two address");
    startNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance1 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance1 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance1 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();
    String accountPermissionJson =
            "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\","
                    + "\"threshold\":1,\"keys\":["
                    + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
                    + "\",\"weight\":1}]},"
                    + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\""
                    + ",\"threshold\":2,"
                    + "\"operations\""
                    + ":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
                    + "\"keys\":["
                    + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey01)
                    + "\",\"weight\":1},"
                    + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
                    + "\",\"weight\":1}"
                    + "]}]}";

    txid = PublicMethedForMutiSign.accountPermissionUpdateForTransactionId(accountPermissionJson,
            ownerAddress, ownerKey, blockingStubFull,
            ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    endNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
       .getBlockHeader().getRawData().getNumber();
    witness01Allowance2 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance2 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance2 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
       blockingStubFull).getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info("-----black hole 1: " + blackHoleBalance1 + "  black hole 2: " + blackHoleBalance2
            + "   increase :" + blackHoleIncrease);
    logger.info("====== witness02Allowance1 :" + witness02Allowance1 + "   witness02Allowance2 :"
            + witness02Allowance2 + "increase :" + witness02Increase);
    logger.info("====== witness01Allowance1 :" + witness01Allowance1 + "  witness01Allowance2 :"
            + witness01Allowance2 + "  increase :" + witness01Increase);

    Map<String, Long> witnessAllowance =
            PublicMethed.getAllowance2(startNum, endNum, blockingStubFull);

    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress01))
            - witness01Increase)) <= 2);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress02))
            - witness02Increase)) <= 2);
    Assert.assertEquals(100000000L, blackHoleIncrease.longValue());

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
            PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList());

    logger.info(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    startNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance1 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance1 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance1 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();

    Protocol.Transaction transaction = PublicMethedForMutiSign
            .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);
    txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    logger.info("-----transaction: " + txid);

    Protocol.Transaction transaction1 = PublicMethedForMutiSign.addTransactionSignWithPermissionId(
            transaction, tmpKey02, 2, blockingStubFull);
    txid = ByteArray.toHexString(Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction1.getRawData().toByteArray()));
    logger.info("-----transaction1: " + txid);

    Protocol.Transaction transaction2 = PublicMethedForMutiSign.addTransactionSignWithPermissionId(
            transaction1, witnessKey01, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));

    GrpcAPI.TransactionSignWeight txWeight = PublicMethedForMutiSign
            .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    endNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance2 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance2 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance2 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info("-----black hole 1: " + blackHoleBalance1 + "  black hole 2: " + blackHoleBalance2
            + "   increase :" + blackHoleIncrease);
    logger.info("====== witness02Allowance1 :" + witness02Allowance1 + "   witness02Allowance2 :"
            + witness02Allowance2 + "increase :" + witness02Increase);
    logger.info("====== witness01Allowance1 :" + witness01Allowance1 + "  witness01Allowance2 :"
            + witness01Allowance2 + "  increase :" + witness01Increase);

    witnessAllowance = PublicMethed.getAllowance2(startNum, endNum, blockingStubFull);

    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress01))
            - witness01Increase)) <= 2);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress02))
            - witness02Increase)) <= 2);
    Assert.assertEquals(1000000L, blackHoleIncrease.longValue());
  }

  @Test(enabled = true, description = "Test trigger result is \"OUT_OF_TIME\""
          + " with energy fee to sr")
  public void test03OutOfTimeEnergyFeeToBlackHole() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    Assert.assertTrue(PublicMethed.sendcoin(deployAddress, maxFeeLimit * 10, fromAddress,
            testKey002, blockingStubFull));

    String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String code = Configuration.getByPath("testng.conf")
            .getString("code.code_TestStorageAndCpu_storageAndCpu");
    String abi = Configuration.getByPath("testng.conf")
            .getString("abi.abi_TestStorageAndCpu_storageAndCpu");
    byte[] contractAddress = null;
    contractAddress = PublicMethed.deployContract(contractName, abi, code,
            "", maxFeeLimit,
            0L, 100, null, deployKey, deployAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    startNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance1 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance1 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance1 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
       blockingStubFull).getBalance();
    txid = PublicMethed.triggerContract(contractAddress,
            "testUseCpu(uint256)", "90100", false,
            0, maxFeeLimit, deployAddress, deployKey, blockingStubFull);
    //    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    endNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
       .getBlockHeader().getRawData().getNumber();
    witness01Allowance2 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance2 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance2 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
       blockingStubFull).getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;

    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info("-----black hole 1: " + blackHoleBalance1 + "  black hole 2: " + blackHoleBalance2
            + "   increase :" + blackHoleIncrease);
    logger.info("====== witness02Allowance1 :" + witness02Allowance1 + "   witness02Allowance2 :"
            + witness02Allowance2 + "increase :" + witness02Increase);
    logger.info("====== witness01Allowance1 :" + witness01Allowance1 + "  witness01Allowance2 :"
            + witness01Allowance2 + "  increase :" + witness01Increase);
    Optional<Protocol.TransactionInfo> infoById =
            PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("InfoById:" + infoById);
    Map<String, Long> witnessAllowance =
            PublicMethed.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress01))
            - witness01Increase)) <= 2);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress02))
            - witness02Increase)) <= 2);
    Assert.assertEquals(witnessAllowance.get(blackHoleAdd), blackHoleIncrease);
  }

  @Test(enabled = true, description = "Test create account with netFee to sr")
  public void test04AccountCreate() {
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    startNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance1 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance1 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance1 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();

    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] lowBalAddress = ecKey.getAddress();
    Assert.assertTrue(PublicMethed.createAccount(fromAddress, lowBalAddress,
            testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    endNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
       .getBlockHeader().getRawData().getNumber();
    witness01Allowance2 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance2 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance2 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();

    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;
    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info("-----black hole 1: " + blackHoleBalance1 + "  black hole 2: " + blackHoleBalance2
            + "   increase :" + blackHoleIncrease);
    logger.info("====== witness02Allowance1 :" + witness02Allowance1 + "   witness02Allowance2 :"
            + witness02Allowance2 + "increase :" + witness02Increase);
    logger.info("====== witness01Allowance1 :" + witness01Allowance1 + "  witness01Allowance2 :"
            + witness01Allowance2 + "  increase :" + witness01Increase);

    Map<String, Long> witnessAllowance =
            PublicMethed.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress01))
            - witness01Increase)) <= 2);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress02))
            - witness02Increase)) <= 2);
    Assert.assertEquals(witnessAllowance.get(blackHoleAdd), blackHoleIncrease);
  }

  @Test(enabled = true, description = "Test trigger contract with netFee and energyFee to sr")
  public void test05NetFeeAndEnergyFee2Sr() {
    Random rand = new Random();
    Integer randNum = rand.nextInt(30) + 1;
    randNum = rand.nextInt(4000);

    Assert.assertTrue(PublicMethed.sendcoin(deployAddress, maxFeeLimit * 10, fromAddress,
            testKey002, blockingStubFull));

    String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String code = Configuration.getByPath("testng.conf")
            .getString("code.code_TestStorageAndCpu_storageAndCpu");
    String abi = Configuration.getByPath("testng.conf")
            .getString("abi.abi_TestStorageAndCpu_storageAndCpu");
    byte[] contractAddress = null;
    contractAddress = PublicMethed.deployContract(contractName, abi, code,
            "", maxFeeLimit,
            0L, 100, null, deployKey, deployAddress, blockingStubFull);
    for (int i = 0; i < 15; i++) {
      txid = PublicMethed.triggerContract(contractAddress,
              "testUseCpu(uint256)", "700", false,
              0, maxFeeLimit, deployAddress, deployKey, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    startNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance1 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance1 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance1 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();
    txid = PublicMethed.triggerContract(contractAddress,
            "testUseCpu(uint256)", "700", false,
            0, maxFeeLimit, deployAddress, deployKey, blockingStubFull);
    //    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    endNum = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build())
            .getBlockHeader().getRawData().getNumber();
    witness01Allowance2 =
            PublicMethed.queryAccount(witnessAddress01, blockingStubFull).getAllowance();
    witness02Allowance2 =
            PublicMethed.queryAccount(witnessAddress02, blockingStubFull).getAllowance();
    blackHoleBalance2 = PublicMethed.queryAccount(Commons.decode58Check(blackHoleAdd),
            blockingStubFull).getBalance();
    witness02Increase = witness02Allowance2 - witness02Allowance1;
    witness01Increase = witness01Allowance2 - witness01Allowance1;
    blackHoleIncrease = blackHoleBalance2 - blackHoleBalance1;

    logger.info("----startNum:" + startNum + " endNum:" + endNum);
    logger.info("-----black hole 1: " + blackHoleBalance1 + "  black hole 2: " + blackHoleBalance2
            + "   increase :" + blackHoleIncrease);
    logger.info("====== witness02Allowance1 :" + witness02Allowance1 + "   witness02Allowance2 :"
            + witness02Allowance2 + "increase :" + witness02Increase);
    logger.info("====== witness01Allowance1 :" + witness01Allowance1 + "  witness01Allowance2 :"
            + witness01Allowance2 + "  increase :" + witness01Increase);
    Optional<Protocol.TransactionInfo> infoById =
            PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("InfoById:" + infoById);
    Map<String, Long> witnessAllowance =
            PublicMethed.getAllowance2(startNum, endNum, blockingStubFull);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress01))
            - witness01Increase)) <= 2);
    Assert.assertTrue((Math.abs(witnessAllowance.get(ByteArray.toHexString(witnessAddress02))
            - witness02Increase)) <= 2);
    Assert.assertEquals(witnessAllowance.get(blackHoleAdd), blackHoleIncrease);
  }
  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.unFreezeBalance(deployAddress, deployKey, 1, deployAddress,
            blockingStubFull);
    PublicMethed.freedResource(deployAddress, deployKey, fromAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
