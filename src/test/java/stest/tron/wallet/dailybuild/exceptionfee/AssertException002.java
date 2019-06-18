package stest.tron.wallet.dailybuild.exceptionfee;

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
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class AssertException002 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String compilerVersion = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompilerVersion");


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


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
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Support function type")
  public void test1AssertException001() {
    ecKey1 = new ECKey(Utils.getRandom());
    grammarAddress = ecKey1.getAddress();
    testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/AssertException002.sol";
    String contractName = "AssertException";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    String num = "10" + "," + "0";
    txid = PublicMethed.triggerContract(contractAddress,
        "divideIHaveArgsReturn(int,int)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById：" + infoById);
    Optional<Transaction> ById = PublicMethed.getTransactionById(txid, blockingStubFull);
    logger.info("getRet：" + ById.get().getRet(0));
    logger.info("getNumber：" + ById.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue：" + ById.get().getRet(0).getContractRetValue());
    logger.info("getContractRet：" + ById.get().getRet(0).getContractRet());

    Assert.assertEquals(ById.get().getRet(0).getContractRet().getNumber(),
        contractResult.ILLEGAL_OPERATION_VALUE);
    Assert.assertEquals(ById.get().getRet(0).getContractRetValue(), 8);
    Assert.assertEquals(ById.get().getRet(0).getContractRet(), contractResult.ILLEGAL_OPERATION);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()), "");
    Assert.assertEquals(contractResult.ILLEGAL_OPERATION, infoById.get().getReceipt().getResult());

    logger.info("ById：" + ById);
    Assert.assertEquals(ById.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(ById.get().getRet(0).getRetValue(), 0);

    Long returnnumber2 = ByteArray.toLong(ByteArray.fromHexString(
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber2 == 100);
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
