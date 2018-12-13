package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestAccount013 {
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  Optional<TransactionInfo> infoById = null;
  long account013BeforeBalance;
  long freezeAmount = 10000000L;
  long freezeDuration = 0;

  byte[] account013Address;
  String testKeyForAccount013;
  byte[] receiverDelegateAddress;
  String receiverDelegateKey;
  byte[] emptyAddress;
  String emptyKey;
  byte[] account4DelegatedResourceAddress;
  String account4DelegatedResourceKey;
  byte[] account5DelegatedResourceAddress;
  String account5DelegatedResourceKey;
  byte[] accountForDeployAddress;
  String accountForDeployKey;
  byte[] accountForAssetIssueAddress;
  String accountForAssetIssueKey;


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey002);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true)
  public void test1DelegateResourceForBandwidthAndEnergy() {
    //Create account013
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    account013Address = ecKey1.getAddress();
    testKeyForAccount013 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    //Create receiver
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    receiverDelegateAddress = ecKey2.getAddress();
    receiverDelegateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    //Create Empty account
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    emptyAddress = ecKey3.getAddress();
    emptyKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    //Create Account4
    ECKey ecKey4 = new ECKey(Utils.getRandom());
    account4DelegatedResourceAddress = ecKey4.getAddress();
    account4DelegatedResourceKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
    //Create Account5
    ECKey ecKey5 = new ECKey(Utils.getRandom());
    account5DelegatedResourceAddress = ecKey5.getAddress();
    account5DelegatedResourceKey = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
    //Create Account6
    ECKey ecKey6 = new ECKey(Utils.getRandom());
    accountForDeployAddress = ecKey6.getAddress();
    accountForDeployKey = ByteArray.toHexString(ecKey6.getPrivKeyBytes());
    //PublicMethed.printAddress(accountForDeployKey);
    //Create Account7
    ECKey ecKey7 = new ECKey(Utils.getRandom());
    accountForAssetIssueAddress = ecKey7.getAddress();
    accountForAssetIssueKey = ByteArray.toHexString(ecKey7.getPrivKeyBytes());
    //sendcoin to Account013
    Assert.assertTrue(PublicMethed.sendcoin(account013Address,
        10000000000L, fromAddress, testKey002, blockingStubFull));
    //sendcoin to receiver
    Assert.assertTrue(PublicMethed.sendcoin(receiverDelegateAddress,
        10000000000L, fromAddress, testKey002, blockingStubFull));
    //sendcoin to Account4
    Assert.assertTrue(PublicMethed.sendcoin(account4DelegatedResourceAddress,
        10000000000L, fromAddress, testKey002, blockingStubFull));

    //sendcoin to Account5
    Assert.assertTrue(PublicMethed.sendcoin(account5DelegatedResourceAddress,
        10000000000L, fromAddress, testKey002, blockingStubFull));
    //sendcoin to Account6
    Assert.assertTrue(PublicMethed.sendcoin(accountForDeployAddress,
        10000000000L, fromAddress, testKey002, blockingStubFull));
    //sendcoin to Account7
    Assert.assertTrue(PublicMethed.sendcoin(accountForAssetIssueAddress,
        10000000000L, fromAddress, testKey002, blockingStubFull));
    //getAccountResource account013
    AccountResourceMessage account013Resource = PublicMethed
        .getAccountResource(account013Address, blockingStubFull);
    logger.info("013 energy limit is " + account013Resource.getEnergyLimit());
    logger.info("013 net limit is " + account013Resource.getNetLimit());
    //getAccountResource receiver
    AccountResourceMessage receiverResource = PublicMethed
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    logger.info("receiver energy limit is " + receiverResource.getEnergyLimit());
    logger.info("receiver net limit is " + receiverResource.getNetLimit());
    Protocol.Account account013infoBefore =
        PublicMethed.queryAccount(account013Address, blockingStubFull);
    //get resources of account013 before DelegateResource
    account013BeforeBalance = account013infoBefore.getBalance();
    AccountResourceMessage account013ResBefore = PublicMethed
        .getAccountResource(account013Address, blockingStubFull);
    final long account013BeforeBandWidth = account013ResBefore.getNetLimit();
    AccountResourceMessage receiverResourceBefore = PublicMethed
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    long receiverBeforeBandWidth = receiverResourceBefore.getNetLimit();
    //Account013 DelegateResource for BandWidth to receiver
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account013Address, freezeAmount, freezeDuration, 0,
        ByteString.copyFrom(receiverDelegateAddress), testKeyForAccount013, blockingStubFull));
    Protocol.Account account013infoAfter =
        PublicMethed.queryAccount(account013Address, blockingStubFull);
    //get balance of account013 after DelegateResource
    long account013AfterBalance = account013infoAfter.getBalance();
    AccountResourceMessage account013ResAfter = PublicMethed
        .getAccountResource(account013Address, blockingStubFull);
    //get BandWidth of account013 after DelegateResource
    long account013AfterBandWidth = account013ResAfter.getNetLimit();
    AccountResourceMessage receiverResourceAfter = PublicMethed
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    //Bandwidth of receiver after DelegateResource
    long receiverAfterBandWidth = receiverResourceAfter.getNetLimit();
    //Balance of Account013 reduced amount same as DelegateResource
    Assert.assertTrue(account013BeforeBalance == account013AfterBalance + freezeAmount);
    //Bandwidth of account013 is equally before and after DelegateResource
    Assert.assertTrue(account013AfterBandWidth == account013BeforeBandWidth);
    //Bandwidth of receiver after DelegateResource is greater than before
    Assert.assertTrue(receiverAfterBandWidth > receiverBeforeBandWidth);
    Protocol.Account account013Before1 =
        PublicMethed.queryAccount(account013Address, blockingStubFull);
    //balance of account013 before DelegateResource
    long account013BeforeBalance1 = account013Before1.getBalance();
    AccountResourceMessage account013ResBefore1 = PublicMethed
        .getAccountResource(account013Address, blockingStubFull);
    //Energy of account013 before DelegateResource
    long account013BeforeEnergy = account013ResBefore1.getEnergyLimit();
    AccountResourceMessage receiverResourceBefore1 = PublicMethed
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    //Energy of receiver before DelegateResource
    long receiverBeforeEnergy = receiverResourceBefore1.getEnergyLimit();
    //Account013 DelegateResource Energy to receiver
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account013Address, freezeAmount, freezeDuration, 1,
        ByteString.copyFrom(receiverDelegateAddress), testKeyForAccount013, blockingStubFull));
    Protocol.Account account013infoAfter1 =
        PublicMethed.queryAccount(account013Address, blockingStubFull);
    //balance of account013 after DelegateResource
    long account013AfterBalance1 = account013infoAfter1.getBalance();
    AccountResourceMessage account013ResAfter1 = PublicMethed
        .getAccountResource(account013Address, blockingStubFull);
    long account013AfterEnergy = account013ResAfter1.getEnergyLimit();
    //Energy of account013 after DelegateResource
    AccountResourceMessage receiverResourceAfter1 = PublicMethed
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    //Energy of receiver after DelegateResource
    long receiverAfterEnergy = receiverResourceAfter1.getEnergyLimit();
    //Balance of Account013 reduced amount same as DelegateResource
    Assert.assertTrue(account013BeforeBalance1 == account013AfterBalance1 + freezeAmount);
    //Bandwidth of account013 is equally before and after DelegateResource
    Assert.assertTrue(account013AfterEnergy == account013BeforeEnergy);
    //Bandwidth of receiver after DelegateResource is greater than before
    Assert.assertTrue(receiverAfterEnergy > receiverBeforeEnergy);
    //account013 DelegateResource to Empty failed
    Assert.assertFalse(PublicMethed.freezeBalanceForReceiver(
        account013Address, freezeAmount, freezeDuration, 0,
        ByteString.copyFrom(emptyAddress), testKeyForAccount013, blockingStubFull));
    //account013 DelegateResource to account013 failed
    Assert.assertFalse(PublicMethed.freezeBalanceForReceiver(
        account013Address, freezeAmount, freezeDuration, 0,
        ByteString.copyFrom(account013Address), testKeyForAccount013, blockingStubFull));
    account013Resource = PublicMethed.getAccountResource(account013Address, blockingStubFull);
    logger.info("After 013 energy limit is " + account013Resource.getEnergyLimit());
    logger.info("After 013 net limit is " + account013Resource.getNetLimit());

    receiverResource = PublicMethed.getAccountResource(receiverDelegateAddress, blockingStubFull);
    logger.info("After receiver energy limit is " + receiverResource.getEnergyLimit());
    logger.info("After receiver net limit is " + receiverResource.getNetLimit());
  }

  @Test(enabled = true)
  public void test2getDelegatedResourceAndDelegateResourceAccountIndex() {
    Protocol.Account account4infoBefore =
        PublicMethed.queryAccount(account4DelegatedResourceAddress, blockingStubFull);
    //Balance of Account4 before DelegateResource
    long account4BeforeBalance = account4infoBefore.getBalance();
    //account013 DelegateResource of bandwidth to Account4
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account013Address, freezeAmount, freezeDuration, 0, ByteString.copyFrom(
            account4DelegatedResourceAddress), testKeyForAccount013,
        blockingStubFull));
    //Account4 DelegateResource of energy to Account5
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account4DelegatedResourceAddress, freezeAmount, freezeDuration, 1, ByteString.copyFrom(
            account5DelegatedResourceAddress), account4DelegatedResourceKey,
        blockingStubFull));
    //check DelegatedResourceList，from:account013 to:Account4
    Optional<GrpcAPI.DelegatedResourceList> delegatedResourceResult1 =
        PublicMethed.getDelegatedResource(
            account013Address, account4DelegatedResourceAddress, blockingStubFull);
    long afterFreezeBandwidth =
        delegatedResourceResult1.get().getDelegatedResource(0).getFrozenBalanceForBandwidth();
    //check DelegatedResourceList，from:Account4 to:Account5
    Optional<GrpcAPI.DelegatedResourceList> delegatedResourceResult2 =
        PublicMethed.getDelegatedResource(account4DelegatedResourceAddress,
            account5DelegatedResourceAddress, blockingStubFull);
    long afterFreezeEnergy =
        delegatedResourceResult2.get().getDelegatedResource(0).getFrozenBalanceForEnergy();
    //FrozenBalanceForBandwidth > 0
    Assert.assertTrue(afterFreezeBandwidth > 0);
    //FrozenBalanceForEnergy > 0
    Assert.assertTrue(afterFreezeEnergy > 0);


    //check DelegatedResourceAccountIndex for Account4
    Optional<Protocol.DelegatedResourceAccountIndex> delegatedResourceIndexResult1 =
        PublicMethed.getDelegatedResourceAccountIndex(
            account4DelegatedResourceAddress, blockingStubFull);
    //result of From list, first Address is same as account013 address
    Assert.assertTrue(new String(account013Address).equals(new String(
        delegatedResourceIndexResult1.get().getFromAccounts(0).toByteArray())));
    //result of To list, first Address is same as Account5 address
    Assert.assertTrue(new String(account5DelegatedResourceAddress).equals(
        new String(delegatedResourceIndexResult1.get().getToAccounts(0).toByteArray())));

    //unfreezebalance of bandwidth from Account013 to Account4
    Assert.assertTrue(PublicMethed.unFreezeBalance(account013Address, testKeyForAccount013,
        0, account4DelegatedResourceAddress, blockingStubFull));
    //check DelegatedResourceAccountIndex of Account4
    Optional<Protocol.DelegatedResourceAccountIndex> delegatedResourceIndexResult1AfterUnfreeze =
        PublicMethed.getDelegatedResourceAccountIndex(
            account4DelegatedResourceAddress, blockingStubFull);
    //result of From list is empty
    Assert.assertTrue(delegatedResourceIndexResult1AfterUnfreeze.get()
        .getFromAccountsList().isEmpty());
    Assert.assertFalse(delegatedResourceIndexResult1AfterUnfreeze.get()
        .getToAccountsList().isEmpty());
    //Balance of Account013 after unfreezeBalance
    // (013 -> receiver(bandwidth), 013 -> receiver(Energy), 013 -> Account4(bandwidth))
    Assert.assertTrue(PublicMethed.queryAccount(account013Address, blockingStubFull)
        .getBalance() == account013BeforeBalance - 2 * freezeAmount);
    //bandwidth from Account013 to  Account4 gone
    Assert.assertTrue(PublicMethed.getAccountResource(account4DelegatedResourceAddress,
        blockingStubFull).getNetLimit() == 0);


    //unfreezebalance of Energy from Account4 to Account5
    Assert.assertTrue(PublicMethed.unFreezeBalance(
        account4DelegatedResourceAddress, account4DelegatedResourceKey,
        1, account5DelegatedResourceAddress, blockingStubFull));
    Protocol.Account account4infoAfterUnfreezeEnergy =
        PublicMethed.queryAccount(account4DelegatedResourceAddress, blockingStubFull);
    //balance of Account4 after unfreezebalance
    long account4BalanceAfterUnfreezeEnergy = account4infoAfterUnfreezeEnergy.getBalance();
    //balance of Account4 is same as before
    Assert.assertTrue(account4BeforeBalance == account4BalanceAfterUnfreezeEnergy);
    //Energy from Account4 to  Account5 gone
    Assert.assertTrue(PublicMethed.getAccountResource(
        account5DelegatedResourceAddress, blockingStubFull).getEnergyLimit() == 0);

    //Unfreezebalance of Bandwidth from Account4 to Account5 fail
    Assert.assertFalse(PublicMethed.unFreezeBalance(account4DelegatedResourceAddress,
        account4DelegatedResourceKey, 0, account5DelegatedResourceAddress, blockingStubFull));
  }

  @Test(enabled = true)
  public void test3DelegateResourceAboutTransferAsset() {
    //account013 DelegateResource of bandwidth to accountForAssetIssue
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account013Address, freezeAmount, freezeDuration, 0,
        ByteString.copyFrom(accountForAssetIssueAddress),
        testKeyForAccount013, blockingStubFull));
    //accountForAssetIssue AssetIssue
    long now = System.currentTimeMillis();
    String name = "testAccount013_" + Long.toString(now);
    long totalSupply = 10000000L;
    String description = "zfbnb";
    String url = "aaa.com";
    Assert.assertTrue(PublicMethed.createAssetIssue(accountForAssetIssueAddress,
        name, totalSupply, 1, 1, System.currentTimeMillis() + 2000,
        System.currentTimeMillis() + 1000000000, 1, description, url,
        2000L, 2000L, 500L, 1L,
        accountForAssetIssueKey, blockingStubFull));
    //Wait for 3s
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //get AssetIssue Id
    Protocol.Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(
        accountForAssetIssueAddress, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    //Account5 Participate AssetIssue
    Assert.assertTrue(PublicMethed.participateAssetIssue(
        accountForAssetIssueAddress, assetAccountId.toByteArray(), 1000000,
        account5DelegatedResourceAddress, account5DelegatedResourceKey, blockingStubFull));
    //get account013，accountForAssetIssue，Account5 account resources before transferAssets
    final long account013CurrentBandwidth = PublicMethed.getAccountResource(
        account013Address, blockingStubFull).getNetUsed();
    long accountForAssetIssueCurrentBandwidth = PublicMethed.getAccountResource(
        accountForAssetIssueAddress, blockingStubFull).getNetUsed();
    final long account5CurrentBandwidth = PublicMethed.getAccountResource(
        account5DelegatedResourceAddress, blockingStubFull).getNetUsed();
    //Account5 transfer Assets receiver
    Assert.assertTrue(PublicMethed.transferAsset(receiverDelegateAddress,
        assetAccountId.toByteArray(), 100000, account5DelegatedResourceAddress,
        account5DelegatedResourceKey, blockingStubFull));

    PublicMethed.printAddress(accountForAssetIssueKey);
    PublicMethed.printAddress(account5DelegatedResourceKey);

    //get account013，accountForAssetIssue，Account5 resource after transferAsset
    final long account013CurrentBandwidthAfterTrans = PublicMethed.getAccountResource(
        account013Address, blockingStubFull).getNetUsed();
    final long accountForAssetIssueCurrentBandwidthAfterTrans = PublicMethed.getAccountResource(
        accountForAssetIssueAddress, blockingStubFull).getFreeNetUsed();
    final long account5CurrentBandwidthAfterTrans = PublicMethed.getAccountResource(
        account5DelegatedResourceAddress, blockingStubFull).getNetUsed();
    AccountResourceMessage account5ResourceAfterTrans = PublicMethed.getAccountResource(
        account5DelegatedResourceAddress, blockingStubFull);
    String result = "";
    if (account5ResourceAfterTrans.getAssetNetLimitCount() > 0) {
      for (String name1 : account5ResourceAfterTrans.getAssetNetLimitMap().keySet()) {
        result += account5ResourceAfterTrans.getAssetNetUsedMap().get(name1);
      }
    }
    logger.info(result);

    long account5FreeAssetNetUsed = accountForAssetIssueCurrentBandwidthAfterTrans;

    //check resource diff
    Assert.assertTrue(Long.parseLong(result) > 0);
    Assert.assertTrue(account013CurrentBandwidth == account013CurrentBandwidthAfterTrans);
    Assert.assertTrue(account5CurrentBandwidth == account5CurrentBandwidthAfterTrans);
  }

  @Test(enabled = true)
  public void test4DelegateResourceAboutTriggerContract() {
    //deploy contract under Account6
    Integer consumeUserResourcePercent = 0;
    Long maxFeeLimit = Configuration.getByPath("testng.conf")
        .getLong("defaultParameter.maxFeeLimit");
    String contractName = "TestSStore";
    String code = "608060405234801561001057600080fd5b5061045c806100206000396000f30060806040526004361061006d576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806304c58438146100725780634f2be91f1461009f578063812db772146100b657806393cd5755146100e3578063d1cd64e914610189575b600080fd5b34801561007e57600080fd5b5061009d600480360381019080803590602001909291905050506101a0565b005b3480156100ab57600080fd5b506100b4610230565b005b3480156100c257600080fd5b506100e1600480360381019080803590602001909291905050506102a2565b005b3480156100ef57600080fd5b5061010e600480360381019080803590602001909291905050506102c3565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561014e578082015181840152602081019050610133565b50505050905090810190601f16801561017b5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561019557600080fd5b5061019e61037e565b005b6000600190505b8181101561022c5760008060018154018082558091505090600182039060005260206000200160006040805190810160405280600881526020017f31323334353637380000000000000000000000000000000000000000000000008152509091909150908051906020019061021d92919061038b565b505080806001019150506101a7565b5050565b60008060018154018082558091505090600182039060005260206000200160006040805190810160405280600881526020017f61626364656667680000000000000000000000000000000000000000000000008152509091909150908051906020019061029e92919061038b565b5050565b6000600190505b81811115156102bf5780806001019150506102a9565b5050565b6000818154811015156102d257fe5b906000526020600020016000915090508054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103765780601f1061034b57610100808354040283529160200191610376565b820191906000526020600020905b81548152906001019060200180831161035957829003601f168201915b505050505081565b6000808060010191505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106103cc57805160ff19168380011785556103fa565b828001600101855582156103fa579182015b828111156103f95782518255916020019190600101906103de565b5b509050610407919061040b565b5090565b61042d91905b80821115610429576000816000905550600101610411565b5090565b905600a165627a7a7230582087d9880a135295a17100f63b8941457f4369204d3ccc9ce4a1abf99820eb68480029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"add2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"add\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"index\",\"type\":\"uint256\"}],\"name\":\"fori2\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"args\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"fori\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}] ";
    logger.info("TestSStore");
    final byte[] contractAddress = PublicMethed.deployContract(contractName,abi,code,"",
        maxFeeLimit, 0L, consumeUserResourcePercent,null,accountForDeployKey,
        accountForDeployAddress,blockingStubFull);

    //Account4 DelegatedResource of Energy to Contract
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account4DelegatedResourceAddress,freezeAmount,freezeDuration,1,
        ByteString.copyFrom(contractAddress),account4DelegatedResourceKey,blockingStubFull));

    //Account4 DelegatedResource Energy to deploy
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(
        account4DelegatedResourceAddress,freezeAmount,freezeDuration,1,
        ByteString.copyFrom(accountForDeployAddress),
        account4DelegatedResourceKey,blockingStubFull));

    //get Energy of Account013，Account4，Contract before trigger contract
    final long account013CurrentEnergyUsed = PublicMethed.getAccountResource(
        account013Address,blockingStubFull).getEnergyUsed();
    final long account013CurrentBandwidthUsed = PublicMethed.getAccountResource(
        account013Address,blockingStubFull).getFreeNetUsed();
    final long account4CurrentEnergyUsed = PublicMethed.getAccountResource(
        account4DelegatedResourceAddress,blockingStubFull).getEnergyUsed();
    final long contractCurrentEnergyUsed = PublicMethed.getAccountResource(
        contractAddress,blockingStubFull).getEnergyUsed();
    final long deployCurrentEnergyUsed = PublicMethed.getAccountResource(
        accountForDeployAddress,blockingStubFull).getEnergyUsed();

    //Account013 trigger contract
    String txid = PublicMethed.triggerContract(contractAddress,
        "add2(uint256)", "1", false,
        0, 1000000000L,"0",0,account013Address, testKeyForAccount013, blockingStubFull);
    logger.info(txid);
    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info(String.valueOf(infoById.get().getResultValue()));
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    //get transaction info of Energy used and Bandwidth used
    final long contractTriggerEnergyUsed = infoById.get().getReceipt().getOriginEnergyUsage();
    final long contractTriggerBandwidthUsed = infoById.get().getReceipt().getNetUsage();

    //get Energy of Account013，Account4，Contract after trigger contract
    final long account013CurrentEnergyUsedAfterTrig = PublicMethed.getAccountResource(
        account013Address,blockingStubFull).getEnergyUsed();
    final long account013CurrentBandwidthUsedAfterTrig = PublicMethed.getAccountResource(
        account013Address,blockingStubFull).getFreeNetUsed();
    final long account4CurrentEnergyUsedAfterTrig = PublicMethed.getAccountResource(
        account4DelegatedResourceAddress,blockingStubFull).getEnergyUsed();
    final long contractCurrentEnergyUsedAfterTrig = PublicMethed.getAccountResource(
        contractAddress,blockingStubFull).getEnergyUsed();
    final long deployCurrentEnergyUsedAfterTrig = PublicMethed.getAccountResource(
        accountForDeployAddress,blockingStubFull).getEnergyUsed();
    //compare energy changed
    Assert.assertTrue(account013CurrentEnergyUsed == account013CurrentEnergyUsedAfterTrig);
    Assert.assertTrue(account4CurrentEnergyUsed == account4CurrentEnergyUsedAfterTrig);
    Assert.assertTrue(contractCurrentEnergyUsed == contractCurrentEnergyUsedAfterTrig);
    Assert.assertTrue(deployCurrentEnergyUsed
        == deployCurrentEnergyUsedAfterTrig - contractTriggerEnergyUsed);
    //compare bandwidth of Account013 before and after trigger contract
    Assert.assertTrue(account013CurrentBandwidthUsed
        == account013CurrentBandwidthUsedAfterTrig - contractTriggerBandwidthUsed);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}