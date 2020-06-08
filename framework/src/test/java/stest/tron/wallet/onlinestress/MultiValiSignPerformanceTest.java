package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class MultiValiSignPerformanceTest {

  private final String fromKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(fromKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractDepAddress = ecKey1.getAddress();
  String contractDepKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
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
  private byte[] ecrecoverContractAddress = null;
  private byte[] multiValiSignContractAddress = null;

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
    PublicMethed.printAddress(contractDepKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }


  @Test(enabled = true, description = "deploy ecrecover contract")
  public void test01DeployEcrecoverContract() {
    Assert.assertTrue(PublicMethed.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(contractDepAddress, contractDepKey, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(contractDepAddress), fromKey, blockingStubFull));

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contractDepAddress,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(contractDepKey, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "src/test/resources/soliditycode/multiValiSignPerformance01.sol";
    String contractName = "ecrecoverValidateSign";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, contractDepKey,
            contractDepAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(contractDepAddress, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(contractDepKey, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    ecrecoverContractAddress = infoById.get().getContractAddress().toByteArray();
    logger.info("ecrecoverContractAddress:" + infoById.get().getContractAddress());
    SmartContract smartContract = PublicMethed.getContract(ecrecoverContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "deploy multvalisign contract")
  public void test02DeployMultvalisignContract() {
    Assert.assertTrue(PublicMethed.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(contractDepAddress, contractDepKey, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(contractDepAddress), fromKey, blockingStubFull));

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contractDepAddress,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(contractDepKey, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));

    String filePath = "src/test/resources/soliditycode/multiValiSignPerformance02.sol";
    String contractName = "multiValidateSignContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    final String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, contractDepKey,
            contractDepAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethed.getAccountResource(contractDepAddress, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(contractDepKey, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    multiValiSignContractAddress = infoById.get().getContractAddress().toByteArray();
    logger.info("multiValiSignContractAddress:" + infoById.get().getContractAddress());
    SmartContract smartContract = PublicMethed.getContract(multiValiSignContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "trigger ecrecover contract test")
  public void test03triggerEcrecoverContract() {
    /*Assert.assertTrue(PublicMethed.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    try {
      Thread.sleep(new Long(30000));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }*/
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
    byte[] hash = ByteArray
        .fromHexString("7d889f42b4a56ebe78264631a3b4daf21019e1170cce71929fb396761cdf532e");
    logger.info("hash:" + Hex.toHexString(hash));
    int cnt = 15;
    for (int i = 0; i < cnt; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(StringUtil.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');

    String txid = "";
    long start = System.currentTimeMillis();
    txid = PublicMethed
        .triggerContract(PublicMethed.decode58Check("TDgdUs1gmn1JoeGMqQGkkxE1pcMNSo8kFj"),
            "validateSign(bytes32,bytes[],address[])", input,
            false, 0, maxFeeLimit, contractDepAddress, contractDepKey, blockingStubFull);
    long timeCosts = System.currentTimeMillis() - start;
    logger.info(
        "Ecrecover--cnt:" + cnt + ",timeCost:" + timeCosts + ",ms:" + (timeCosts * 1.0 / cnt));
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }


  @Test(enabled = true, description = "trigger mulivalisign contract test")
  public void test04triggerMuliValiSignContract() {
    /*Assert.assertTrue(PublicMethed.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    try {
      Thread.sleep(new Long(30000));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }*/
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();

    byte[] hash = ByteArray
        .fromHexString("7d889f42b4a56ebe78264631a3b4daf21019e1170cce71929fb396761cdf532e");
    logger.info("hash:" + Hex.toHexString(hash));
    int cnt = 15;
    for (int i = 0; i < cnt; i++) {
      ECKey key = new ECKey();
      byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(StringUtil.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');

    String txid = "";
    long start = System.currentTimeMillis();
    txid = PublicMethed
        .triggerContract(PublicMethed.decode58Check("TVpTLZbBbP82aufo7p3qmb4ELiowH3mjQW"),
            "testArray(bytes32,bytes[],address[])", input, false,
            0, maxFeeLimit, contractDepAddress, contractDepKey, blockingStubFull);
    long timeCosts = System.currentTimeMillis() - start;
    logger.info(
        "MuliValiSign--cnt:" + cnt + ",timeCost:" + timeCosts + ",ms:" + (timeCosts * 1.0 / cnt));
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
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
