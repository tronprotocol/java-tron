package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
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
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

public class StakeSuicideTest003 {

  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethed.getFinalAddress(testFoundationKey);
  private String testWitnessKey = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private String testWitnessAddress = PublicMethed.getAddressString(testWitnessKey);

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.sendcoin(testAddress001,10000000,testFoundationAddress,
        testFoundationKey,blockingStubFull);
  }

  @Test(enabled = false, description = "suicide target Address is owner Address")
  public void stakeSuicideTest001() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
    String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = PublicMethed.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Frozen ownerFrozen = ownerAccount.getFrozen(0);

    String methedStr = "SelfdestructTest(address)";
    String argStr = "\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account account = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(account.getFrozenCount(),0);

  }

  @Test(enabled = false, description = "suicide target Address is BlackHoleAddress Address")
  public void stakeSuicideTest002() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
    String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = PublicMethed.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Frozen ownerFrozen = ownerAccount.getFrozen(0);

    String blackHoleAddress = "TLsV52sRDL79HXGGm9yzwKibb6BeruhUzy";
    final Account accountBefore = PublicMethed
        .queryAccount(PublicMethed.decode58Check(blackHoleAddress),
            blockingStubFull);

    String methedStr = "SelfdestructTest(address)";
    String argStr = "\"" + blackHoleAddress + "\"";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account account = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(account.getFrozenCount(),0);

    Account accountAfter = PublicMethed
        .queryAccount(PublicMethed.decode58Check(blackHoleAddress),
            blockingStubFull);
    Assert.assertEquals(accountBefore.getBalance() + ex.get().getReceipt().getEnergyFee()
        + 10000000, accountAfter.getBalance());
  }

  @Test(enabled = false, description = "suicide target Address is BlackHoleAddress Address")
  public void stakeSuicideTest003() {
    String filePath = "src/test/resources/soliditycode/stackSuicide001.sol";
    String contractName = "testStakeSuicide";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit, 10000000L,
            100, null, testFoundationKey,
            testFoundationAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = PublicMethed.triggerContract(contractAddress,"Stake(address,uint256)",
        "\"" + testWitnessAddress + "\",10000000",false,0,maxFeeLimit,
        testFoundationAddress, testFoundationKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> ex = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);
    Assert.assertEquals(ByteArray.toInt(ex.get().getContractResult(0).toByteArray()),1);

    Account ownerAccount = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Frozen ownerFrozen = ownerAccount.getFrozen(0);

    final Account accountBefore = PublicMethed
        .queryAccount(PublicMethed.decode58Check("T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb"),
            blockingStubFull);

    String methedStr = "SelfdestructTest(address)";
    String argStr = "\"" + "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb" + "\"";
    txid = PublicMethed.triggerContract(contractAddress,methedStr,argStr,false,
        0,maxFeeLimit,testAddress001,testKey001,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ex = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertEquals(ex.get().getResult(), TransactionInfo.code.SUCESS);

    Account account = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Assert.assertEquals(account.getFrozenCount(),0);

    Account accountAfter = PublicMethed
        .queryAccount(PublicMethed.decode58Check("T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb"),
            blockingStubFull);
    Assert.assertEquals(accountBefore.getBalance() + 10000000, accountAfter.getBalance());
  }


}
