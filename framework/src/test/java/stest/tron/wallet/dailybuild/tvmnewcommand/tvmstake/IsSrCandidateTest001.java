package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import org.testng.Assert;
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
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class IsSrCandidateTest001 {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private String testWitnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethed.printAddress(testKey001);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/isSRCandidate.sol";
    String contractName = "TestIsSRCandidate";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, testKey001,
            testAddress001, blockingStubFull);
  }

  @Test(enabled = false, description = "Witness Address should be true")
  void tvmStakeTest001() {
    String methodStr = "isSRCandidateTest(address)";
    String argsStr = "\"" + PublicMethed.getAddressString(testWitnessKey) + "\"";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,1);
  }

  @Test(enabled = false, description = "Account Address should be false")
  void tvmStakeTest002() {
    String methodStr = "isSRCandidateTest(address)";
    String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\"";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,0);
  }

  @Test(enabled = false, description = "zero Address(0x00) should be false")
  void tvmStakeTest003() {
    String methodStr = "zeroAddressTest()";
    String argsStr = "";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,0);
  }

  @Test(enabled = false, description = "Contract Address should be false")
  void tvmStakeTest004() {
    String methodStr = "localContractAddrTest()";
    String argsStr = "";
    TransactionExtention returns = PublicMethed
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
    int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,0);
  }

}
