package stest.tron.wallet.dailybuild.originenergylimit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
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
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractOriginEnergyLimit001 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey1.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
    PublicMethed.printAddress(testKeyForGrammarAddress3);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }


  //Origin_energy_limit001,028,029
  @Test(enabled = true, description = "Boundary value and update test")
  public void testOrigin_energy_limit001() {
    Assert.assertTrue(PublicMethed
        .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/contractOriginEnergyLimit001.sol";
    String contractName = "findArgsContractTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String contractAddress = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, -1, "0", 0,
            null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(contractAddress == null);

    String contractAddress1 = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 0, "0", 0,
            null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    Assert.assertTrue(contractAddress1 == null);

    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 9223372036854775807L, "0",
            0, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethed.updateEnergyLimit(contractAddress2, -1L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertFalse(PublicMethed.updateEnergyLimit(contractAddress2, 0L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract1 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract1.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertTrue(PublicMethed.updateEnergyLimit(contractAddress2,
        9223372036854775807L, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull));
    SmartContract smartContract2 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract2.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertTrue(PublicMethed.updateEnergyLimit(contractAddress2, 'c',
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract3 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertEquals(smartContract3.getOriginEnergyLimit(), 99);

    Assert.assertFalse(PublicMethed.updateEnergyLimit(contractAddress2, 1L,
        testNetAccountKey, testNetAccountAddress, blockingStubFull));
    SmartContract smartContract4 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertEquals(smartContract4.getOriginEnergyLimit(), 99);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(grammarAddress3, testKeyForGrammarAddress3, testNetAccountAddress,
        blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
