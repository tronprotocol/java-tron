package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractRewardTest001 {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String witnessKey = Configuration.getByPath("testng.conf").getString("witness.key1");
  private String witnessAddress = PublicMethed.getAddressString(witnessKey);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;
  //= Base58.decode58Check("TQYK8QPAFtxjmse1dShHWYXEMsF836jxxe");

  @BeforeSuite(enabled = false, description = "stake beforeSuite delete")
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    PublicMethed.printAddress(testKey001);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);

    String filePath = "src/test/resources/soliditycode/stackContract001.sol";
    String contractName = "B";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000_000L, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + witnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
  }

  @Test(enabled = false,description = "querry SR account, reward should equal to gerRewardInfo")
  void rewardbalanceTest001() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(PublicMethed.getFinalAddress(witnessKey)))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();

    String methedStr = "rewardBalance(address)";
    String argStr = "\"" + witnessAddress + "\"";
    TransactionExtention txen = PublicMethed.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);
    System.out.println(txen);
    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(reward,rewardBalance);
  }

  @Test(enabled = false,description = "querry 0x00, reward should be 0")
  void rewardbalanceTest002() {
    String methedStr = "nullAddressTest()";
    String argStr = "";
    TransactionExtention txen = PublicMethed.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(rewardBalance,0);
  }

  @Test(enabled = false,description = "querry UnActive account , reward should be 0")
  void rewardbalanceTest003() {
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    String key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    String methedStr = "rewardBalance(address)";
    String argStr = "\"" + PublicMethed.getAddressString(key) + "\"";
    TransactionExtention txen = PublicMethed.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(rewardBalance,0);
  }

  @Test(enabled = false,description = "querry contract account,reward should equal to "
      + "gerRewardInfo")
  void rewardbalanceTest004() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(contractAddress))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();

    String methedStr = "rewardBalance(address)";
    String argStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    TransactionExtention txen = PublicMethed.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    logger.info("rewardBalance: " + rewardBalance);
    logger.info("reward: " + reward);
    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(rewardBalance,reward);
  }

  @Test(enabled = false,description = "querry ZeroReward account, reward should be 0")
  void rewardbalanceTest005() {
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(ByteString
        .copyFrom(PublicMethed.getFinalAddress(testFoundationKey)))
        .build();
    long reward = blockingStubFull.getRewardInfo(bytesMessage).getNum();

    String methedStr = "rewardBalance(address)";
    String argStr = "\"" + PublicMethed.getAddressString(testFoundationKey) + "\"";
    TransactionExtention txen = PublicMethed.triggerConstantContractForExtention(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    long rewardBalance = ByteArray.toLong(txen.getConstantResult(0).toByteArray());

    Assert.assertEquals(txen.getResult().getCode(), response_code.SUCCESS);
    Assert.assertEquals(reward,rewardBalance,0);
  }

  @Test(enabled = false,description = "withdrawBalance")
  void withdrawBalanceTest006() {
    //contractAddress = Base58.decode58Check("TBsf2FCSht83CEA8CSZ1ReQTRDByNB7FCe");

    String methedStr = "withdrawRewardTest()";
    String argStr = "";
    String txid = PublicMethed.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    int result = ByteArray.toInt(ext.getContractResult(0).toByteArray());
    Assert.assertEquals(result,0);
    Assert.assertEquals(ext.getResult(), code.SUCESS);
  }

  @Test(enabled = false,description = "withdrawBalance twice")
  void withdrawBalanceTest007() {
    String methedStr = "withdrawRewardTest()";
    String argStr = "";
    String txid = PublicMethed.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    int result = ByteArray.toInt(ext.getContractResult(0).toByteArray());
    Assert.assertEquals(result,0);
    Assert.assertEquals(ext.getResult(), code.SUCESS);
  }

  @Test(enabled = false,description = "withdrawBalance other contract")
  void withdrawBalanceTest008() {
    String filePath = "src/test/resources/soliditycode/stackContract001.sol";
    String contractName = "B";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] otherContract = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 100_000_000L, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    String methedStr = "contractBWithdrawRewardTest(address)";
    String argStr = "\"" + Base58.encode58Check(otherContract) + "\"";
    String txid = PublicMethed.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    int result = ByteArray.toInt(ext.getContractResult(0).toByteArray());
    Assert.assertEquals(result,0);
    Assert.assertEquals(ext.getResult(), TransactionInfo.code.SUCESS);
  }

  @Test(enabled = false,description = "new withdrawBalance constructor")
  void withdrawBalanceTest009() {
    String methedStr = "createA()";
    String argStr = "";
    String txid = PublicMethed.triggerContract(contractAddress,
        methedStr,argStr,false,0,maxFeeLimit,"0",0,testAddress001,testKey001,blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    TransactionInfo ext = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();

    int result = ByteArray.toInt(ext.getLog(0).getData().toByteArray());
    Assert.assertEquals(result,0);
    int result2 = ByteArray.toInt(ext.getLog(1).getData().toByteArray());
    Assert.assertEquals(result2,0);
    Assert.assertEquals(ext.getResult(), code.SUCESS);
  }

}
