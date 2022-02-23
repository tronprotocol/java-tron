package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class StringSplitTest {

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

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/stringSplit.sol";
    String contractName = "testStringSplit";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "get s1 n1")
  public void test01GetS1N1() {
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS1()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s12,./",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS1N1()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s12,./",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

  @Test(enabled = true, description = "get s2 n2")
  public void test01GetS2N2() {
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS2()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s123?\\'.",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS2N2()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s123?\'.",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

  @Test(enabled = true, description = "get s3 n3")
  public void test01GetS3N3() {
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS3()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS3N3()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

  @Test(enabled = true, description = "get s4 n4")
  public void test01GetS4N4() {
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS4()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getS4N4()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethed.hexStringToString(PublicMethed.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

}
