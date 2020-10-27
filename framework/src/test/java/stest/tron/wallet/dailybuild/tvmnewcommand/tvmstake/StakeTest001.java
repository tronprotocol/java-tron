package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class StakeTest001 {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private String testWitnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private String testWitnessKey2 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  private byte[] testWitnessAddress = PublicMethed.getFinalAddress(testWitnessKey);
  private byte[] testWitnessAddress2 = PublicMethed.getFinalAddress(testWitnessKey2);



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
        .sendcoin(testAddress001, 1000_000_00000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/testStakeSuicide.sol";
    String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000_000_0000L, 100, null, testKey001, testAddress001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Vote for witness")
  void tvmStakeTest001() {
    long balanceBefore = PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance();
    String methodStr = "Stake(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000;
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);

    Account request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    long balanceAfter = PublicMethed.queryAccount(contractAddress, blockingStubFull).getBalance();
    Assert.assertEquals(balanceAfter,balanceBefore - 1000000);
    byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0)
        .getVoteAddress().toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    Assert.assertEquals(1,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());



  }

  @Test(enabled = false, description = "Non-witness account")
  void tvmStakeTest002() {
    //account address
    String methodStr = "Stake(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\","  + 1000000;
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,0);

    //contract address
    methodStr = "Stake(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(contractAddress) + "\","  + 1000000;
    txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,0);


  }


  @Test(enabled = false, description = "Number of votes over balance")
  void tvmStakeTest003() {
    String methodStr = "Stake(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + Long.MAX_VALUE;
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());

    Assert.assertEquals(contractResult,0);

  }


  @Test(enabled = false, description = "Enough votes for a second ballot")
  void tvmStakeTest004() {

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String methodStr = "Stake(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 21000000;
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    Account request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0)
        .getVoteAddress().toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    System.out.println(blockingStubFull.getAccount(request).getVotesCount());
    Assert.assertEquals(21,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

    argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 11000000;
    txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0).getVoteAddress()
        .toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    Assert.assertEquals(11,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

  }


  @Test(enabled = false, description = "Revert test")
  void tvmStakeTest005() {

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String methodStr = "revertTest1(address,uint256,address)";
    String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000 + ",\""
        + Base58.encode58Check(testAddress001) + "\"";
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());

    Assert.assertEquals(contractResult,0);

  }


  @Test(enabled = false, description = "Contract Call Contract stake")
  void tvmStakeTest006() {
    String methodStr = "deployB()";
    String argsStr = "";
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("txid:" + txid);

    methodStr = "BStake(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 1000000;
    txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    long callvalue = 1000000000L;
    txid = PublicMethed.triggerContract(contractAddress, "deployB()", "#", false,
        callvalue, maxFeeLimit, testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    String addressHex =
        "41" + ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())
            .substring(24);
    byte[] contractAddressB = ByteArray.fromHexString(addressHex);
    long contractAddressBBalance = PublicMethed.queryAccount(contractAddressB, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBBalance);

    methodStr = "BStake(address,uint256)";
    argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\"," + 10000000;
    txid = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    int contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult, 1);
    Account account = PublicMethed.queryAccount(contractAddressB, blockingStubFull);
    long frozenBalance = account.getFrozen(0).getFrozenBalance();
    byte[] voteAddress = account.getVotes(0).getVoteAddress().toByteArray();
    long voteCount = account.getVotes(0).getVoteCount();
    long balanceAfter = account.getBalance();
    Assert.assertEquals(voteCount, 10);
    Assert.assertEquals(voteAddress, testWitnessAddress);
    Assert.assertEquals(frozenBalance, 10000000);
    Assert.assertEquals(balanceAfter, contractAddressBBalance - 10000000);

  }

  @Test(enabled = false, description = "Vote for the first witness and then vote for the second "
      + "witness.")
  void tvmStakeTest007() {

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String methodStr = "Stake(address,uint256)";
    String argsStr = "\"" + Base58.encode58Check(testWitnessAddress) + "\","  + 21000000;
    String txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    int contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    Account request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    byte[] voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0)
        .getVoteAddress().toByteArray());
    Assert.assertEquals(testWitnessAddress,voteAddress);
    System.out.println(blockingStubFull.getAccount(request).getVotesCount());
    Assert.assertEquals(21,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

    argsStr = "\"" + Base58.encode58Check(testWitnessAddress2) + "\","  + 11000000;
    txid  = PublicMethed
        .triggerContract(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit,
            testAddress001, testKey001, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    info =  PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    contractResult = ByteArray.toInt(info.get().getContractResult(0).toByteArray());
    Assert.assertEquals(contractResult,1);
    request = Account.newBuilder().setAddress(ByteString.copyFrom(contractAddress)).build();
    voteAddress = (blockingStubFull.getAccount(request).getVotesList().get(0).getVoteAddress()
        .toByteArray());
    Assert.assertEquals(testWitnessAddress2,voteAddress);
    Assert.assertEquals(11,blockingStubFull.getAccount(request).getVotes(0).getVoteCount());

  }

}

