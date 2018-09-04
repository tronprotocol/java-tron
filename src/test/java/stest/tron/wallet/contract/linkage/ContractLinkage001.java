package stest.tron.wallet.contract.linkage;

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
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractLinkage001 {

  //testng001、testng002、testng003、testng004
  private final String testKey002 =
      "FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

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
    Assert.assertTrue(PublicMethed.sendcoin(linkage001Address,20000000000L,fromAddress,
        testKey002,blockingStubFull));

  }

  @Test(enabled = true)
  public void deployContentValue() {
    String contractName = "tronNative";
    String noPayableCode = "608060405260008054600160a060020a031990811662010001179091556001805482166201000217905560028054821662010003179055600380548216620100041790556004805482166201000517905560058054821662010006179055600680549091166201000717905534801561007757600080fd5b506104ce806100876000396000f3006080604052600436106100da5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416630a90265081146100df5780630dfb51ac146100fc57806345bd20101461012d5780634efaaa1b1461014257806352ae1b811461016657806353c4263f1461017b5780635fd8c710146101905780637c369c90146101a55780637f2b7f93146101ba5780638259d5531461020f578063906fbec914610227578063961a8be71461023c578063cee14bb414610251578063ec9928bd14610275578063fb4f32aa14610292575b600080fd5b3480156100eb57600080fd5b506100fa6004356024356102a7565b005b34801561010857600080fd5b506101116102dc565b60408051600160a060020a039092168252519081900360200190f35b34801561013957600080fd5b506101116102eb565b34801561014e57600080fd5b506100fa600160a060020a03600435166024356102fa565b34801561017257600080fd5b50610111610320565b34801561018757600080fd5b5061011161032f565b34801561019c57600080fd5b506100fa61033e565b3480156101b157600080fd5b5061011161035d565b3480156101c657600080fd5b50604080516020600480358082013583810280860185019096528085526100fa9536959394602494938501929182918501908490808284375094975061036c9650505050505050565b34801561021b57600080fd5b506100fa6004356103c6565b34801561023357600080fd5b506101116103f7565b34801561024857600080fd5b50610111610406565b34801561025d57600080fd5b506100fa600160a060020a0360043516602435610415565b34801561028157600080fd5b506100fa600435602435151561044d565b34801561029e57600080fd5b506100fa610483565b60015460408051848152602081018490528151600160a060020a0390931692818301926000928290030181855af45050505050565b600654600160a060020a031681565b600354600160a060020a031681565b816080528060a0526000608060406080620100016000f4151561031c57600080fd5b5050565b600254600160a060020a031681565b600454600160a060020a031681565b600354604051600160a060020a03909116906000818181855af4505050565b600554600160a060020a031681565b6005546040518251600160a060020a039092169183919081906020808501910280838360005b838110156103aa578181015183820152602001610392565b50505050905001915050600060405180830381855af450505050565b600654604080518381529051600160a060020a039092169160208083019260009291908290030181855af450505050565b600054600160a060020a031681565b600154600160a060020a031681565b6000805460408051600160a060020a03868116825260208201869052825193169381830193909290918290030181855af45050505050565b6004546040805184815283151560208201528151600160a060020a0390931692818301926000928290030181855af45050505050565b600254604051600160a060020a03909116906000818181855af45050505600a165627a7a7230582076efe233a097282a46d3aefb879b720ed02a4ad3c6cf053cc5936a01e366c7dc0029";
    String noPayableAbi = "[{\"constant\":false,\"inputs\":[{\"name\":\"frozen_Balance\",\"type\":\"uint256\"},{\"name\":\"frozen_Duration\",\"type\":\"uint256\"}],\"name\":\"freezeBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"deleteProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"withdrawBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\"type\":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":\"voteUsingAssembly\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"unFreezeBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"approveProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"createProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"data\",\"type\":\"bytes32[]\"}],\"name\":\"createProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"uint256\"}],\"name\":\"deleteProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"voteContractAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"freezeBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\"type\":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":\"voteForSingleWitness\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"uint256\"},{\"name\":\"isApprove\",\"type\":\"bool\"}],\"name\":\"approveProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"unFreezeBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    String payableCode = "608060405260008054600160a060020a03199081166201000117909155600180548216620100021790556002805482166201000317905560038054821662010004179055600480548216620100051790556005805482166201000617905560068054909116620100071790556104ce8061007a6000396000f3006080604052600436106100da5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416630a90265081146100df5780630dfb51ac146100fc57806345bd20101461012d5780634efaaa1b1461014257806352ae1b811461016657806353c4263f1461017b5780635fd8c710146101905780637c369c90146101a55780637f2b7f93146101ba5780638259d5531461020f578063906fbec914610227578063961a8be71461023c578063cee14bb414610251578063ec9928bd14610275578063fb4f32aa14610292575b600080fd5b3480156100eb57600080fd5b506100fa6004356024356102a7565b005b34801561010857600080fd5b506101116102dc565b60408051600160a060020a039092168252519081900360200190f35b34801561013957600080fd5b506101116102eb565b34801561014e57600080fd5b506100fa600160a060020a03600435166024356102fa565b34801561017257600080fd5b50610111610320565b34801561018757600080fd5b5061011161032f565b34801561019c57600080fd5b506100fa61033e565b3480156101b157600080fd5b5061011161035d565b3480156101c657600080fd5b50604080516020600480358082013583810280860185019096528085526100fa9536959394602494938501929182918501908490808284375094975061036c9650505050505050565b34801561021b57600080fd5b506100fa6004356103c6565b34801561023357600080fd5b506101116103f7565b34801561024857600080fd5b50610111610406565b34801561025d57600080fd5b506100fa600160a060020a0360043516602435610415565b34801561028157600080fd5b506100fa600435602435151561044d565b34801561029e57600080fd5b506100fa610483565b60015460408051848152602081018490528151600160a060020a0390931692818301926000928290030181855af45050505050565b600654600160a060020a031681565b600354600160a060020a031681565b816080528060a0526000608060406080620100016000f4151561031c57600080fd5b5050565b600254600160a060020a031681565b600454600160a060020a031681565b600354604051600160a060020a03909116906000818181855af4505050565b600554600160a060020a031681565b6005546040518251600160a060020a039092169183919081906020808501910280838360005b838110156103aa578181015183820152602001610392565b50505050905001915050600060405180830381855af450505050565b600654604080518381529051600160a060020a039092169160208083019260009291908290030181855af450505050565b600054600160a060020a031681565b600154600160a060020a031681565b6000805460408051600160a060020a03868116825260208201869052825193169381830193909290918290030181855af45050505050565b6004546040805184815283151560208201528151600160a060020a0390931692818301926000928290030181855af45050505050565b600254604051600160a060020a03909116906000818181855af45050505600a165627a7a72305820bf65c4013bea4495f2cbccf685ee1442e2585d226cf4bd8184c636cdd1d485dc0029";
    String payableAbi = "[{\"constant\":false,\"inputs\":[{\"name\":\"frozen_Balance\",\"type\":\"uint256\"},{\"name\":\"frozen_Duration\",\"type\":\"uint256\"}],\"name\":\"freezeBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"deleteProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"withdrawBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\"type\":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":\"voteUsingAssembly\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"unFreezeBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"approveProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"withdrawBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"createProposalAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"data\",\"type\":\"bytes32[]\"}],\"name\":\"createProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"uint256\"}],\"name\":\"deleteProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"voteContractAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"freezeBalanceAddress\",\"outputs\":[{\"name\":\"\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"witnessAddr\",\"type\":\"address\"},{\"name\":\"voteValue\",\"type\":\"uint256\"}],\"name\":\"voteForSingleWitness\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"id\",\"type\":\"uint256\"},{\"name\":\"isApprove\",\"type\":\"bool\"}],\"name\":\"approveProposal\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"unFreezeBalance\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";

    Long maxFeeLimit = 20000000000L;
    //Value is equal balance,this will be failed.
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,payableAbi,
        payableCode,"",maxFeeLimit, 20000000000L, 100,null,
        linkage001Key,linkage001Address,blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(linkage001Address, 50000000L,
        3,1,linkage001Key,blockingStubFull));
    maxFeeLimit = 20000000000L - 50000000L;
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(linkage001Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    //Long storageLimit = accountResource.getStorageLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    //Long storageUsage = accountResource.getStorageUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    //logger.info("before storage limit is " + Long.toString(storageLimit));
    //logger.info("before storage usaged is " + Long.toString(storageUsage));


    Account account = PublicMethed.queryAccount(linkage001Key,blockingStubFull);
    Long beforeAccountBalance = account.getBalance();
    logger.info("before balance is " + Long.toString(account.getBalance()));
    //Value is 1
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,payableAbi,payableCode,
        "",maxFeeLimit, 1L, 100,null,linkage001Key,
        linkage001Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    byte[] contractAddress = infoById.get().getContractAddress().toByteArray();


    accountResource = PublicMethed.getAccountResource(linkage001Address,blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    //storageLimit = accountResource.getStorageLimit();
    energyUsage = accountResource.getEnergyUsed();
    //storageUsage = accountResource.getStorageUsed();

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    //logger.info("after storage limit is " + Long.toString(storageLimit));
    //logger.info("after storage usaged is " + Long.toString(storageUsage));

    account = PublicMethed.queryAccount(linkage001Key,blockingStubFull);
    Long afterAccountBalance = account.getBalance();
    logger.info(Long.toString(beforeAccountBalance));
    logger.info(Long.toString(afterAccountBalance));
    Assert.assertTrue(beforeAccountBalance - 1L == afterAccountBalance);
    account = PublicMethed.queryAccount(contractAddress,blockingStubFull);
    Assert.assertTrue(account.getBalance() == 1L);

    //Value is account all balance plus 1.
    account = PublicMethed.queryAccount(linkage001Key,blockingStubFull);
    Long valueBalance = account.getBalance();
    contractAddress = PublicMethed.deployContract(contractName,payableAbi,payableCode,"",
        maxFeeLimit, valueBalance + 1, 100,null,linkage001Key,
        linkage001Address,blockingStubFull);
    Assert.assertTrue(contractAddress == null);

    //Value is account all balance.
    Assert.assertTrue(PublicMethed.freezeBalance(linkage001Address, 5000000L,
        3,linkage001Key,blockingStubFull));
    account = PublicMethed.queryAccount(linkage001Key,blockingStubFull);
    valueBalance = account.getBalance();
    txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName,payableAbi,payableCode,
        "",maxFeeLimit, valueBalance, 100,null,linkage001Key,
        linkage001Address,blockingStubFull);
    infoById = PublicMethed.getTransactionInfoById(txid,blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    contractAddress = infoById.get().getContractAddress().toByteArray();

    Assert.assertTrue(PublicMethed.queryAccount(linkage001Key,blockingStubFull)
        .getBalance() == 0);
    Assert.assertTrue(PublicMethed.queryAccount(contractAddress,blockingStubFull)
        .getBalance() == valueBalance);





  }



  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


