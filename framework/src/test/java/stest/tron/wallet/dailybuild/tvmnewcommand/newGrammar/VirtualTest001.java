package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

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
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class VirtualTest001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

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
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy 0.5.15 about virtual")
  public void test01OverrideContract515() {
    String contractName = "Z";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_virtual001");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_virtual001");

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethed.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethed.triggerContract(contractAddress, "setBool(bool)", "true", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethed.triggerContract(contractAddress, "setString(string)", "\"1q2w\"", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "y()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "z()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "3171327700000000000000000000000000000000000000000000000000000000",
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.6.0 about virtual")
  public void test02OverrideContract060() {
    String filePath = "./src/test/resources/soliditycode/virtual001.sol";
    String contractName = "Z";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethed.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethed.triggerContract(contractAddress, "setBool(bool)", "true", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethed.triggerContract(contractAddress, "setString(string)", "\"1q2w\"", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "y()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "z()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "3171327700000000000000000000000000000000000000000000000000000000",
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethed.sendcoin(fromAddress, balance, dev001Address, dev001Key,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}



