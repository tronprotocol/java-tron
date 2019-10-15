package stest.tron.wallet.dailybuild.trctoken;

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
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractTrcToken077 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
    PublicMethed.printAddress(testKeyForGrammarAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = false)
  public void testAddress001() {
    PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);

    String filePath = "./src/test/resources/soliditycode/contractTrcToken077.sol";
    String contractName = "trcToken077";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);
    Optional<TransactionInfo> deployInfo = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    contractAddress = deployInfo.get().getContractAddress().toByteArray();
    logger.info("Deploy energy is " + deployInfo.get().getReceipt().getEnergyUsageTotal());

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "addressTest()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    logger.info("Trigger energy is " + infoById.get().getReceipt().getEnergyUsageTotal());

  }

  @Test(enabled = true, description = "The value of address is not at the beginning of 41")
  public void testAddress002() {
    PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName = "trcToken077";

    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600"
        + "080fd5b5060b0806100396000396000f3fe6080604052348015600f57600080fd5b50d38015601b57600080"
        + "fd5b50d28015602757600080fd5b5060043610605c577c01000000000000000000000000000000000000000"
        + "0000000000000000060003504636241c1d881146061575b600080fd5b60676079565b604080519182525190"
        + "81900360200190f35b60405130908190529056fea165627a7a723058207b9b52e71420f2fa4cb55ffd55641"
        + "355ec84e09d6d4545c629dde7cc01d74a100029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"addressTest\",\"outputs\":[{\"name"
        + "\":\"addressValue\",\"type\":\"bytes32\"}],\"payable\":false,\"stateMutability\":\"nonp"
        + "ayable\",\"type\":\"function\"}]";

    String deploytxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> deployById = PublicMethed
        .getTransactionInfoById(deploytxid, blockingStubFull);
    contractAddress = deployById.get().getContractAddress().toByteArray();
    logger.info("infoById:" + deployById);

    String txid = "";
    txid = PublicMethed.triggerContract(contractAddress,
        "addressTest()", "#", false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);

    Assert.assertNotNull(infoById);
    byte[] a = infoById.get().getContractResult(0).toByteArray();
    byte[] b = subByte(a, 11, 1);
    byte[] c = subByte(a, 0, 11);
    byte[] e = "41".getBytes();
    byte[] d = subByte(a, 12, 20);

    logger.info("a:" + ByteArray.toHexString(a));

    logger.info("b:" + ByteArray.toHexString(b));
    logger.info("c:" + ByteArray.toHexString(c));

    logger.info("d:" + ByteArray.toHexString(d));

    logger.info("41" + ByteArray.toHexString(d));
    String exceptedResult = "41" + ByteArray.toHexString(d);
    String realResult = ByteArray.toHexString(b);
    Assert.assertEquals(realResult, "00");
    Assert.assertNotEquals(realResult, "41");

    Assert.assertEquals(exceptedResult, ByteArray.toHexString(contractAddress));

  }


  /**
   * constructor.
   */

  public byte[] subByte(byte[] b, int off, int length) {
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(grammarAddress, testKeyForGrammarAddress, testNetAccountAddress,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
