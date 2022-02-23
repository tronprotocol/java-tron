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
public class SelectorTest {

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
    String filePath = "./src/test/resources/soliditycode/selector.sol";
    String contractName = "A";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    final String aContractAddress = ByteArray.toHexString(contractAddress);

    contractName = "testSelector";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    abi = retMap.get("abI").toString();
    code = retMap.get("byteCode").toString();
    code = PublicMethed.replaceCode(code, aContractAddress.substring(1));
    logger.info("code:" + code);

    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Get the selector of public or external library functions "
      + "through member variables")
  public void test01GetSelector() {
    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress, "getselector2()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    logger.info("result: " + ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    Assert.assertEquals("f8b2cb4f", PublicMethed.removeAll0sAtTheEndOfHexStr(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(0, 64)));
    Assert.assertEquals("b4cef28d", PublicMethed.removeAll0sAtTheEndOfHexStr(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(64)));
  }

}
