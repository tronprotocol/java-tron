package stest.tron.wallet.manual;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractLinkage001 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] linkage001Address = ecKey1.getAddress();
  String linkage001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }


  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(linkage001Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = true)
  public void deployContentValue() {
    Assert.assertTrue(PublicMethed.sendcoin(linkage001Address, 20000000000L, fromAddress,
        testKey002, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    info = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeNetLimit = resourceInfo.getNetLimit();
    Long beforeFreeNetLimit = resourceInfo.getFreeNetLimit();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeEnergyLimit = resourceInfo.getEnergyLimit();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyLimit:" + beforeEnergyLimit);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeFreeNetLimit:" + beforeFreeNetLimit);
    logger.info("beforeNetLimit:" + beforeNetLimit);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    //Value is equal balance,this will be failed.Only use FreeNet,Other not change.
    String payableCode = "608060405260008054600160a060020a031990811662010001179091556001805482166"
        + "20100021790556002805482166201000317905560038054821662010004179055600480548216620100051"
        + "790556005805482166201000617905560068054909116620100071790556104ce8061007a6000396000f30"
        + "06080604052600436106100da5763ffffffff7c010000000000000000000000000000000000000000000000"
        + "00000000006000350416630a90265081146100df5780630dfb51ac146100fc57806345bd20101461012d578"
        + "0634efaaa1b1461014257806352ae1b811461016657806353c4263f1461017b5780635fd8c710146101905"
        + "780637c369c90146101a55780637f2b7f93146101ba5780638259d5531461020f578063906fbec91461022"
        + "7578063961a8be71461023c578063cee14bb414610251578063ec9928bd14610275578063fb4f32aa146102"
        + "92575b600080fd5b3480156100eb57600080fd5b506100fa6004356024356102a7565b005b3480156101085"
        + "7600080fd5b506101116102dc565b60408051600160a060020a039092168252519081900360200190f35b3"
        + "4801561013957600080fd5b506101116102eb565b34801561014e57600080fd5b506100fa600160a060020"
        + "a03600435166024356102fa565b34801561017257600080fd5b50610111610320565b34801561018757600"
        + "080fd5b5061011161032f565b34801561019c57600080fd5b506100fa61033e565b3480156101b15760008"
        + "0fd5b5061011161035d565b3480156101c657600080fd5b506040805160206004803580820135838102808"
        + "60185019096528085526100fa9536959394602494938501929182918501908490808284375094975061036"
        + "c9650505050505050565b34801561021b57600080fd5b506100fa6004356103c6565b34801561023357600"
        + "080fd5b506101116103f7565b34801561024857600080fd5b50610111610406565b34801561025d5760008"
        + "0fd5b506100fa600160a060020a0360043516602435610415565b34801561028157600080fd5b506100fa6"
        + "00435602435151561044d565b34801561029e57600080fd5b506100fa610483565b6001546040805184815"
        + "2602081018490528151600160a060020a0390931692818301926000928290030181855af45050505050565"
        + "b600654600160a060020a031681565b600354600160a060020a031681565b816080528060a052600060806"
        + "0406080620100016000f4151561031c57600080fd5b5050565b600254600160a060020a031681565b60045"
        + "4600160a060020a031681565b600354604051600160a060020a03909116906000818181855af4505050565"
        + "b600554600160a060020a031681565b6005546040518251600160a060020a0390921691839190819060208"
        + "08501910280838360005b838110156103aa578181015183820152602001610392565b50505050905001915"
        + "050600060405180830381855af450505050565b600654604080518381529051600160a060020a039092169"
        + "160208083019260009291908290030181855af450505050565b600054600160a060020a031681565b600154"
        + "600160a060020a031681565b6000805460408051600160a060020a038681168252602082018690528251931"
        + "69381830193909290918290030181855af45050505050565b60045460408051848152831515602082015281"
        + "51600160a060020a0390931692818301926000928290030181855af45050505050565b60025460405160016"
        + "0a060020a03909116906000818181855af45050505600a165627a7a72305820bf65c4013bea4495f2cbccf6"
        + "85ee1442e2585d226cf4bd8184c636cdd1d485dc0029";
    String payableAbi = "[{\"constant\":false,\"inputs\":[{\"name\":\"frozen_Balance\",\"type\":"
        + "\"uint256\"},{\"name\":\"frozen_Duration\",\"type\":\"uint256\"}],\"name\":\""
        + "freezeBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\","
        + "\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"deleteProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":"
        + "true,\"inputs\":[],\"name\":\"withdrawBalanceAddress\",\"outputs\":[{\"name\":\"\","
        + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\"type\""
        + ":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":"
        + "\"voteUsingAssembly\",\"outputs\":[],\"payable\":false,\"stateMutability\":"
        + "\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"unFreezeBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":"
        + "true,\"inputs\":[],\"name\":\"approveProposalAddress\",\"outputs\":[{\"name\":\"\","
        + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawBalance\","
        + "\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":"
        + "\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"createProposalAddress\","
        + "\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,"
        + "\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":["
        + "{\"name\":\"data\",\"type\":\"bytes32[]\"}],\"name\":\"createProposal\",\"outputs\":"
        + "[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},"
        + "{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"uint256\"}],\"name\":"
        + "\"deleteProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\""
        + ",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":"
        + "\"voteContractAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],"
        + "\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":"
        + "true,\"inputs\":[],\"name\":\"freezeBalanceAddress\",\"outputs\":[{\"name\":\"\","
        + "\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":"
        + "\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\"type\":"
        + "\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":"
        + "\"voteForSingleWitness\",\"outputs\":[],\"payable\":false,\"stateMutability\":"
        + "\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id"
        + "\",\"type\":\"uint256\"},{\"name\":\"isApprove\",\"type\":\"bool\"}],\"name\":"
        + "\"approveProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":"
        + "\"unFreezeBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable"
        + "\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\""
        + "payable\",\"type\":\"constructor\"}]";
    Account accountGet = PublicMethed.queryAccount(linkage001Key, blockingStubFull);
    Long accountBalance = accountGet.getBalance();
    String contractName = "tronNative";
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, payableAbi,
        payableCode, "", maxFeeLimit, accountBalance, 100, null,
        linkage001Key, linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    Long fee = infoById.get().getFee();
    Long energyFee = infoById.get().getReceipt().getEnergyFee();
    Long netUsed = infoById.get().getReceipt().getNetUsage();
    Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
    Long netFee = infoById.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal:" + energyUsageTotal);
    logger.info("fee:" + fee);
    logger.info("energyFee:" + energyFee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);

    Account infoafter = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyLimit = resourceInfoafter.getEnergyLimit();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterFreeNetLimit = resourceInfoafter.getFreeNetLimit();
    Long afterNetLimit = resourceInfoafter.getNetLimit();
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyLimit:" + afterEnergyLimit);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterFreeNetLimit:" + afterFreeNetLimit);
    logger.info("afterNetLimit:" + afterNetLimit);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertTrue(fee == 0);
    Assert.assertTrue(afterNetUsed == 0);
    Assert.assertTrue(afterEnergyUsed == 0);
    Assert.assertTrue(afterFreeNetUsed > 0);

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(linkage001Address, 50000000L,
        3, 1, linkage001Key, blockingStubFull));
    maxFeeLimit = maxFeeLimit - 50000000L;
    AccountResourceMessage resourceInfo1 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info1 = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeEnergyLimit1 = resourceInfo1.getEnergyLimit();
    Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
    Long beforeFreeNetLimit1 = resourceInfo1.getFreeNetLimit();
    Long beforeNetLimit1 = resourceInfo1.getNetLimit();
    Long beforeNetUsed1 = resourceInfo1.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeEnergyLimit1:" + beforeEnergyLimit1);
    logger.info("beforeEnergyUsed1:" + beforeEnergyUsed1);
    logger.info("beforeFreeNetLimit1:" + beforeFreeNetLimit1);
    logger.info("beforeNetLimit1:" + beforeNetLimit1);
    logger.info("beforeNetUsed1:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed1:" + beforeFreeNetUsed1);

    //Value is 1,use BalanceGetEnergy,use FreeNet,fee==0.
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, payableAbi, payableCode,
            "", maxFeeLimit, 1L, 100, null, linkage001Key,
            linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long energyUsageTotal1 = infoById1.get().getReceipt().getEnergyUsageTotal();
    Long fee1 = infoById1.get().getFee();
    Long energyFee1 = infoById1.get().getReceipt().getEnergyFee();
    Long netUsed1 = infoById1.get().getReceipt().getNetUsage();
    Long energyUsed1 = infoById1.get().getReceipt().getEnergyUsage();
    Long netFee1 = infoById1.get().getReceipt().getNetFee();
    logger.info("energyUsageTotal1:" + energyUsageTotal1);
    logger.info("fee1:" + fee1);
    logger.info("energyFee1:" + energyFee1);
    logger.info("netUsed1:" + netUsed1);
    logger.info("energyUsed1:" + energyUsed1);
    logger.info("netFee1:" + netFee1);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    Account infoafter1 = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter1 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyLimit1 = resourceInfoafter1.getEnergyLimit();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterFreeNetLimit1 = resourceInfoafter1.getFreeNetLimit();
    Long afterNetLimit1 = resourceInfoafter1.getNetLimit();
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterEnergyLimit1:" + afterEnergyLimit1);
    logger.info("afterEnergyUsed1:" + afterEnergyUsed1);
    logger.info("afterFreeNetLimit1:" + afterFreeNetLimit1);
    logger.info("afterNetLimit1:" + afterNetLimit1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);

    Assert.assertTrue(beforeBalance1 - fee1 - 1L == afterBalance1);
    byte[] contractAddress = infoById1.get().getContractAddress().toByteArray();
    Account account = PublicMethed.queryAccount(contractAddress, blockingStubFull);
    Assert.assertTrue(account.getBalance() == 1L);
    Assert.assertTrue(afterNetUsed1 == 0);
    Assert.assertTrue(afterEnergyUsed1 > 0);
    Assert.assertTrue(afterFreeNetUsed1 > 0);

    //Value is account all balance plus 1. balance is not sufficient,Nothing changde.
    AccountResourceMessage resourceInfo2 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info2 = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeEnergyLimit2 = resourceInfo2.getEnergyLimit();
    Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
    Long beforeFreeNetLimit2 = resourceInfo2.getFreeNetLimit();
    Long beforeNetLimit2 = resourceInfo2.getNetLimit();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    logger.info("beforeBalance2:" + beforeBalance2);
    logger.info("beforeEnergyLimit2:" + beforeEnergyLimit2);
    logger.info("beforeEnergyUsed2:" + beforeEnergyUsed2);
    logger.info("beforeFreeNetLimit2:" + beforeFreeNetLimit2);
    logger.info("beforeNetLimit2:" + beforeNetLimit2);
    logger.info("beforeNetUsed2:" + beforeNetUsed2);
    logger.info("beforeFreeNetUsed2:" + beforeFreeNetUsed2);

    account = PublicMethed.queryAccount(linkage001Key, blockingStubFull);
    Long valueBalance = account.getBalance();
    contractAddress = PublicMethed.deployContract(contractName, payableAbi, payableCode, "",
        maxFeeLimit, valueBalance + 1, 100, null, linkage001Key,
        linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(contractAddress == null);
    Account infoafter2 = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter2 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyLimit2 = resourceInfoafter2.getEnergyLimit();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterFreeNetLimit2 = resourceInfoafter2.getFreeNetLimit();
    Long afterNetLimit2 = resourceInfoafter2.getNetLimit();
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    logger.info("afterBalance2:" + afterBalance2);
    logger.info("afterEnergyLimit2:" + afterEnergyLimit2);
    logger.info("afterEnergyUsed2:" + afterEnergyUsed2);
    logger.info("afterFreeNetLimit2:" + afterFreeNetLimit2);
    logger.info("afterNetLimit2:" + afterNetLimit2);
    logger.info("afterNetUsed2:" + afterNetUsed2);
    logger.info("afterFreeNetUsed2:" + afterFreeNetUsed2);
    Assert.assertTrue(afterNetUsed2 == 0);
    Assert.assertTrue(afterEnergyUsed2 > 0);
    Assert.assertTrue(afterFreeNetUsed2 > 0);
    Assert.assertEquals(beforeBalance2, afterBalance2);

    //Value is account all balance.use freezeBalanceGetEnergy ,freezeBalanceGetNet .Balance ==0
    Assert.assertTrue(PublicMethed.freezeBalance(linkage001Address, 5000000L,
        3, linkage001Key, blockingStubFull));
    AccountResourceMessage resourceInfo3 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Account info3 = PublicMethed.queryAccount(linkage001Address, blockingStubFull);
    Long beforeBalance3 = info3.getBalance();
    Long beforeEnergyLimit3 = resourceInfo3.getEnergyLimit();
    Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
    Long beforeFreeNetLimit3 = resourceInfo3.getFreeNetLimit();
    Long beforeNetLimit3 = resourceInfo3.getNetLimit();
    Long beforeNetUsed3 = resourceInfo3.getNetUsed();
    Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    logger.info("beforeBalance3:" + beforeBalance3);
    logger.info("beforeEnergyLimit3:" + beforeEnergyLimit3);
    logger.info("beforeEnergyUsed3:" + beforeEnergyUsed3);
    logger.info("beforeFreeNetLimit3:" + beforeFreeNetLimit3);
    logger.info("beforeNetLimit3:" + beforeNetLimit3);
    logger.info("beforeNetUsed3:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed3:" + beforeFreeNetUsed3);
    account = PublicMethed.queryAccount(linkage001Key, blockingStubFull);
    valueBalance = account.getBalance();
    txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, payableAbi, payableCode,
            "", maxFeeLimit, valueBalance, 100, null, linkage001Key,
            linkage001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Account infoafter3 = PublicMethed.queryAccount(linkage001Address, blockingStubFull1);
    AccountResourceMessage resourceInfoafter3 = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull1);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterEnergyLimit3 = resourceInfoafter3.getEnergyLimit();
    Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
    Long afterFreeNetLimit3 = resourceInfoafter3.getFreeNetLimit();
    Long afterNetLimit3 = resourceInfoafter3.getNetLimit();
    Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    logger.info("afterBalance3:" + afterBalance3);
    logger.info("afterEnergyLimit3:" + afterEnergyLimit3);
    logger.info("afterEnergyUsed3:" + afterEnergyUsed3);
    logger.info("afterFreeNetLimit3:" + afterFreeNetLimit3);
    logger.info("afterNetLimit3:" + afterNetLimit3);
    logger.info("afterNetUsed3:" + afterNetUsed3);
    logger.info("afterFreeNetUsed3:" + afterFreeNetUsed3);

    Assert.assertTrue(afterNetUsed3 > 0);
    Assert.assertTrue(afterEnergyUsed3 > 0);
    Assert.assertTrue(afterFreeNetUsed3 > 0);
    Assert.assertEquals(beforeBalance2, afterBalance2);
    Assert.assertTrue(afterBalance3 == 0);
    Assert.assertTrue(PublicMethed.queryAccount(contractAddress, blockingStubFull)
        .getBalance() == valueBalance);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


