package org.tron.core.services;

import static org.junit.Assert.assertNotNull;
import static org.tron.common.parameter.CommonParameter.getInstance;
import static org.tron.common.utils.client.WalletClient.decodeFromBase58Check;
import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TransferContract;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.Objects;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.tron.api.DatabaseGrpc;
import org.tron.api.DatabaseGrpc.DatabaseBlockingStub;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BlockReq;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeRequestMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountRequestMessage;
import org.tron.api.GrpcAPI.DelegatedResourceMessage;
import org.tron.api.GrpcAPI.DiversifierMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExpandedSpendingKeyMessage;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountRequestMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyDiversifierMessage;
import org.tron.api.GrpcAPI.IncomingViewingKeyMessage;
import org.tron.api.GrpcAPI.IvkDecryptAndMarkParameters;
import org.tron.api.GrpcAPI.IvkDecryptParameters;
import org.tron.api.GrpcAPI.IvkDecryptTRC20Parameters;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.OvkDecryptParameters;
import org.tron.api.GrpcAPI.OvkDecryptTRC20Parameters;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.api.GrpcAPI.PrivateParametersWithoutAsk;
import org.tron.api.GrpcAPI.ViewingKeyMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.api.WalletSolidityGrpc;
import org.tron.api.WalletSolidityGrpc.WalletSolidityBlockingStub;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.services.interfaceOnPBFT.RpcApiServiceOnPBFT;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader.raw;
import org.tron.protos.Protocol.MarketOrderPair;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AccountContract.SetAccountIdContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.AccountBalanceRequest;
import org.tron.protos.contract.BalanceContract.AccountIdentifier;
import org.tron.protos.contract.BalanceContract.BlockBalanceTrace.BlockIdentifier;
import org.tron.protos.contract.BalanceContract.CancelAllUnfreezeV2Contract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.protos.contract.ProposalContract.ProposalDeleteContract;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RpcApiServicesTest {
  private static TronApplicationContext context;
  private static DatabaseBlockingStub databaseBlockingStubFull = null;
  private static DatabaseBlockingStub databaseBlockingStubSolidity = null;
  private static DatabaseBlockingStub databaseBlockingStubPBFT = null;
  private static WalletBlockingStub blockingStubFull = null;
  private static WalletSolidityBlockingStub blockingStubSolidity = null;
  private static WalletSolidityBlockingStub blockingStubPBFT = null;
  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static ByteString ownerAddress;
  private static ByteString sk;
  private static ByteString ask;
  private static ByteString nsk;
  private static ByteString ovk;
  private static ByteString ak;
  private static ByteString nk;
  private static ByteString ivk;
  private static ByteString d;

  @BeforeClass
  public static void init() throws IOException {
    Args.setParam(new String[]{"-d", temporaryFolder.newFolder().toString()}, Constant.TEST_CONF);
    String OWNER_ADDRESS = Wallet.getAddressPreFixString()
        + "548794500882809695a8a687866e76d4271a1abc";
    getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    getInstance().setRpcOnSolidityPort(PublicMethod.chooseRandomPort());
    getInstance().setRpcOnPBFTPort(PublicMethod.chooseRandomPort());
    String fullNode = String.format("%s:%d", getInstance().getNodeLanIp(),
        getInstance().getRpcPort());
    String solidityNode = String.format("%s:%d", getInstance().getNodeLanIp(),
        getInstance().getRpcOnSolidityPort());
    String pBFTNode = String.format("%s:%d", getInstance().getNodeLanIp(),
        getInstance().getRpcOnPBFTPort());

    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullNode)
        .usePlaintext()
        .build();
    ManagedChannel channelPBFT = ManagedChannelBuilder.forTarget(pBFTNode)
        .usePlaintext()
        .build();
    ManagedChannel channelSolidity = ManagedChannelBuilder.forTarget(solidityNode)
        .usePlaintext()
        .build();
    context = new TronApplicationContext(DefaultConfig.class);
    databaseBlockingStubFull = DatabaseGrpc.newBlockingStub(channelFull);
    databaseBlockingStubSolidity = DatabaseGrpc.newBlockingStub(channelSolidity);
    databaseBlockingStubPBFT = DatabaseGrpc.newBlockingStub(channelPBFT);
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    blockingStubPBFT = WalletSolidityGrpc.newBlockingStub(channelPBFT);

    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    RpcApiServiceOnSolidity rpcApiServiceOnSolidity =
        context.getBean(RpcApiServiceOnSolidity.class);
    RpcApiServiceOnPBFT rpcApiServiceOnPBFT = context.getBean(RpcApiServiceOnPBFT.class);

    Manager manager = context.getBean(Manager.class);

    ownerAddress = ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS));
    AccountCapsule ownerCapsule = new AccountCapsule(ByteString.copyFromUtf8("owner"),
        ownerAddress, Protocol.AccountType.Normal, 10_000_000_000L);
    manager.getAccountStore().put(ownerCapsule.createDbKey(), ownerCapsule);
    manager.getDynamicPropertiesStore().saveAllowShieldedTransaction(1);
    manager.getDynamicPropertiesStore().saveAllowShieldedTRC20Transaction(1);
    Application appTest = ApplicationFactory.create(context);
    appTest.addService(rpcApiService);
    appTest.addService(rpcApiServiceOnSolidity);
    appTest.addService(rpcApiServiceOnPBFT);
    appTest.startup();
  }

  @AfterClass
  public static void destroy() {
    context.close();
    Args.clearParam();
  }

  @Test
  public void testGetBlockByNum() {
    NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertNotNull(databaseBlockingStubFull.getBlockByNum(message));
    assertNotNull(databaseBlockingStubSolidity.getBlockByNum(message));
    assertNotNull(databaseBlockingStubPBFT.getBlockByNum(message));
    assertNotNull(blockingStubFull.getBlockByNum(message));
    assertNotNull(blockingStubSolidity.getBlockByNum(message));
    assertNotNull(blockingStubPBFT.getBlockByNum(message));

    assertNotNull(blockingStubFull.getBlockByNum2(message));
    assertNotNull(blockingStubSolidity.getBlockByNum2(message));
    assertNotNull(blockingStubPBFT.getBlockByNum2(message));
  }

  @Test
  public void testGetDynamicProperties() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(databaseBlockingStubFull.getDynamicProperties(message));
    assertNotNull(databaseBlockingStubSolidity.getDynamicProperties(message));
    assertNotNull(databaseBlockingStubPBFT.getDynamicProperties(message));
  }

  @Test
  public void testGetAccount() {
    Account account = Account.newBuilder().setAddress(ownerAddress).build();
    assertNotNull(blockingStubFull.getAccount(account));
    assertNotNull(blockingStubSolidity.getAccount(account));
    assertNotNull(blockingStubPBFT.getAccount(account));
  }

  @Test
  public void testGetAccountById() {
    Account account = Account.newBuilder().setAccountId(ownerAddress).build();
    assertNotNull(blockingStubFull.getAccountById(account));
    assertNotNull(blockingStubSolidity.getAccountById(account));
    assertNotNull(blockingStubPBFT.getAccountById(account));
  }

  @Test
  public void testListWitnesses() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.listWitnesses(message));
    assertNotNull(blockingStubSolidity.listWitnesses(message));
    assertNotNull(blockingStubPBFT.listWitnesses(message));
  }

  @Test
  public void testGetAssetIssueList() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getAssetIssueList(message));
    assertNotNull(blockingStubSolidity.getAssetIssueList(message));
    assertNotNull(blockingStubPBFT.getAssetIssueList(message));
  }

  @Test
  public void testGetPaginatedAssetIssueList() {
    PaginatedMessage paginatedMessage = PaginatedMessage.newBuilder()
        .setOffset(0).setLimit(5).build();
    assertNotNull(blockingStubFull.getPaginatedAssetIssueList(paginatedMessage));
    assertNotNull(blockingStubSolidity.getPaginatedAssetIssueList(paginatedMessage));
    assertNotNull(blockingStubPBFT.getPaginatedAssetIssueList(paginatedMessage));
  }

  @Test
  public void testGetAssetIssueByName() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getAssetIssueByName(message));
    assertNotNull(blockingStubSolidity.getAssetIssueByName(message));
    assertNotNull(blockingStubPBFT.getAssetIssueByName(message));
  }

  @Test
  public void testGetAssetIssueListByName() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getAssetIssueListByName(message));
    assertNotNull(blockingStubSolidity.getAssetIssueListByName(message));
    assertNotNull(blockingStubPBFT.getAssetIssueListByName(message));
  }

  @Test
  public void testGetAssetIssueById() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getAssetIssueById(message));
    assertNotNull(blockingStubSolidity.getAssetIssueById(message));
    assertNotNull(blockingStubPBFT.getAssetIssueById(message));
  }

  @Test
  public void testGetBlockReference() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(databaseBlockingStubFull.getBlockReference(message));
    assertNotNull(databaseBlockingStubSolidity.getBlockReference(message));
    assertNotNull(databaseBlockingStubPBFT.getBlockReference(message));
  }

  @Test
  public void testGetNowBlock() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(databaseBlockingStubFull.getNowBlock(message));
    assertNotNull(databaseBlockingStubSolidity.getNowBlock(message));
    assertNotNull(databaseBlockingStubPBFT.getNowBlock(message));
    assertNotNull(blockingStubFull.getNowBlock(message));
    assertNotNull(blockingStubSolidity.getNowBlock(message));
    assertNotNull(blockingStubPBFT.getNowBlock(message));

    assertNotNull(blockingStubFull.getNowBlock2(message));
    assertNotNull(blockingStubSolidity.getNowBlock2(message));
    assertNotNull(blockingStubPBFT.getNowBlock2(message));
  }

  @Test
  public void testGetDelegatedResource() {
    DelegatedResourceMessage message = DelegatedResourceMessage.newBuilder()
        .setFromAddress(ownerAddress)
        .setToAddress(ownerAddress).build();
    assertNotNull(blockingStubFull.getDelegatedResource(message));
    assertNotNull(blockingStubSolidity.getDelegatedResource(message));
    assertNotNull(blockingStubPBFT.getDelegatedResource(message));

    assertNotNull(blockingStubFull.getDelegatedResourceV2(message));
    assertNotNull(blockingStubSolidity.getDelegatedResourceV2(message));
    assertNotNull(blockingStubPBFT.getDelegatedResourceV2(message));
  }

  @Test
  public void testGetDelegatedResourceAccountIndex() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getDelegatedResourceAccountIndex(message));
    assertNotNull(blockingStubSolidity.getDelegatedResourceAccountIndex(message));
    assertNotNull(blockingStubPBFT.getDelegatedResourceAccountIndex(message));

    assertNotNull(blockingStubFull.getDelegatedResourceAccountIndexV2(message));
    assertNotNull(blockingStubSolidity.getDelegatedResourceAccountIndexV2(message));
    assertNotNull(blockingStubPBFT.getDelegatedResourceAccountIndexV2(message));
  }

  @Test
  public void testGetCanDelegatedMaxSize() {
    CanDelegatedMaxSizeRequestMessage message = CanDelegatedMaxSizeRequestMessage.newBuilder()
        .setType(0).setOwnerAddress(ownerAddress).build();
    assertNotNull(blockingStubFull.getCanDelegatedMaxSize(message));
    assertNotNull(blockingStubSolidity.getCanDelegatedMaxSize(message));
    assertNotNull(blockingStubPBFT.getCanDelegatedMaxSize(message));
  }

  @Test
  public void testGetAvailableUnfreezeCount() {
    GetAvailableUnfreezeCountRequestMessage message = GetAvailableUnfreezeCountRequestMessage
        .newBuilder().setOwnerAddress(ownerAddress).build();
    assertNotNull(blockingStubFull.getAvailableUnfreezeCount(message));
    assertNotNull(blockingStubSolidity.getAvailableUnfreezeCount(message));
    assertNotNull(blockingStubPBFT.getAvailableUnfreezeCount(message));
  }

  @Test
  public void testGetCanWithdrawUnfreezeAmount() {
    CanWithdrawUnfreezeAmountRequestMessage message = CanWithdrawUnfreezeAmountRequestMessage
        .newBuilder().setOwnerAddress(ownerAddress).setTimestamp(0).build();
    assertNotNull(blockingStubFull.getCanWithdrawUnfreezeAmount(message));
    assertNotNull(blockingStubSolidity.getCanWithdrawUnfreezeAmount(message));
    assertNotNull(blockingStubPBFT.getCanWithdrawUnfreezeAmount(message));
  }

  @Test
  public void testGetExchangeById() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getExchangeById(message));
    assertNotNull(blockingStubSolidity.getExchangeById(message));
    assertNotNull(blockingStubPBFT.getExchangeById(message));
  }

  @Test
  public void testListExchanges() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.listExchanges(message));
    assertNotNull(blockingStubSolidity.listExchanges(message));
    assertNotNull(blockingStubPBFT.listExchanges(message));
  }

  @Test
  public void testGetTransactionCountByBlockNum() {
    NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertNotNull(blockingStubFull.getTransactionCountByBlockNum(message));
    assertNotNull(blockingStubSolidity.getTransactionCountByBlockNum(message));
    assertNotNull(blockingStubPBFT.getTransactionCountByBlockNum(message));
  }

  @Test
  public void testListNodes() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.listNodes(message));
  }

  @Test
  public void testGetTransactionById() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getTransactionById(message));
    assertNotNull(blockingStubSolidity.getTransactionById(message));
    assertNotNull(blockingStubPBFT.getTransactionById(message));
  }

  @Test
  public void testGetTransactionInfoById() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getTransactionInfoById(message));
    assertNotNull(blockingStubSolidity.getTransactionInfoById(message));
    assertNotNull(blockingStubPBFT.getTransactionInfoById(message));
  }

  @Test
  public void testGetRewardInfo() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getRewardInfo(message));
    assertNotNull(blockingStubSolidity.getRewardInfo(message));
    assertNotNull(blockingStubPBFT.getRewardInfo(message));
  }

  @Test
  public void testGetBrokerageInfo() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getBrokerageInfo(message));
    assertNotNull(blockingStubSolidity.getBrokerageInfo(message));
    assertNotNull(blockingStubPBFT.getBrokerageInfo(message));
  }

  @Test
  public void testGetBurnTrx() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getBurnTrx(message));
    assertNotNull(blockingStubSolidity.getBurnTrx(message));
    assertNotNull(blockingStubPBFT.getBurnTrx(message));
  }

  //  @Test
  //  public void testGetMerkleTreeVoucherInfo() {
  //    OutputPoint outputPoint = OutputPoint.newBuilder().build();
  //    OutputPointInfo message = OutputPointInfo.newBuilder()
  //        .addOutPoints(outputPoint).setBlockNum(0).build();
  //  assertNotNull(blockingStubFull.getMerkleTreeVoucherInfo(message));
  //  assertNotNull(blockingStubSolidity.getMerkleTreeVoucherInfo(message));
  //  assertNotNull(blockingStubPBFT.getMerkleTreeVoucherInfo(message));
  //  }

  @Test
  public void testScanNoteByIvk() {
    IvkDecryptParameters message = IvkDecryptParameters.newBuilder()
        .setStartBlockIndex(0)
        .setEndBlockIndex(1)
        .build();
    assertNotNull(blockingStubFull.scanNoteByIvk(message));
    assertNotNull(blockingStubSolidity.scanNoteByIvk(message));
    assertNotNull(blockingStubPBFT.scanNoteByIvk(message));
  }

  @Test
  public void testScanAndMarkNoteByIvk() {
    IvkDecryptAndMarkParameters message = IvkDecryptAndMarkParameters.newBuilder()
        .setStartBlockIndex(0)
        .setEndBlockIndex(1)
        .build();
    assertNotNull(blockingStubFull.scanAndMarkNoteByIvk(message));
    assertNotNull(blockingStubSolidity.scanAndMarkNoteByIvk(message));
    assertNotNull(blockingStubPBFT.scanAndMarkNoteByIvk(message));
  }

  @Test
  public void test08ScanNoteByOvk() {
    OvkDecryptParameters message = OvkDecryptParameters.newBuilder()
        .setStartBlockIndex(0)
        .setEndBlockIndex(1)
        .setOvk(ovk)
        .build();
    assertNotNull(blockingStubFull.scanNoteByOvk(message));
    assertNotNull(blockingStubSolidity.scanNoteByOvk(message));
    assertNotNull(blockingStubPBFT.scanNoteByOvk(message));
  }

  //  @Test
  //  public void testIsSpend() {
  //    NoteParameters message = NoteParameters.newBuilder()
  //        .build();
  //  assertNotNull(blockingStubFull.isSpend(message));
  //  assertNotNull(blockingStubSolidity.isSpend(message));
  //  assertNotNull(blockingStubPBFT.isSpend(message));
  //  }

  @Test
  public void testScanShieldedTRC20NotesByIvk() {
    IvkDecryptTRC20Parameters message = IvkDecryptTRC20Parameters.newBuilder()
        .setStartBlockIndex(1)
        .setEndBlockIndex(10)
        .build();
    assertNotNull(blockingStubFull.scanShieldedTRC20NotesByIvk(message));
    assertNotNull(blockingStubSolidity.scanShieldedTRC20NotesByIvk(message));
    assertNotNull(blockingStubPBFT.scanShieldedTRC20NotesByIvk(message));
  }

  @Test
  public void testScanShieldedTRC20NotesByOvk() {
    OvkDecryptTRC20Parameters message = OvkDecryptTRC20Parameters.newBuilder()
        .setStartBlockIndex(1)
        .setEndBlockIndex(10)
        .build();
    assertNotNull(blockingStubFull.scanShieldedTRC20NotesByOvk(message));
    assertNotNull(blockingStubSolidity.scanShieldedTRC20NotesByOvk(message));
    assertNotNull(blockingStubPBFT.scanShieldedTRC20NotesByOvk(message));
  }

  //  @Test
  //  public void testIsShieldedTRC20ContractNoteSpent() {
  //    NfTRC20Parameters message = NfTRC20Parameters.newBuilder().build();
  //  assertNotNull(blockingStubFull.isShieldedTRC20ContractNoteSpent(message));
  //  assertNotNull(blockingStubSolidity.isShieldedTRC20ContractNoteSpent(message));
  //  assertNotNull(blockingStubPBFT.isShieldedTRC20ContractNoteSpent(message));
  //  }

  //  @Test
  //  public void testGetTriggerInputForShieldedTRC20Contract() {
  //    ShieldedTRC20TriggerContractParameters message =
  //        ShieldedTRC20TriggerContractParameters.newBuilder()
  //        .setAmount("1000")
  //        .build();
  //  assertNotNull(blockingStubFull.getTriggerInputForShieldedTRC20Contract(message));
  //  }

  @Test
  public void testUpdateBrokerage() {
    UpdateBrokerageContract message = UpdateBrokerageContract.newBuilder()
        .setOwnerAddress(ownerAddress).setBrokerage(1).build();
    assertNotNull(blockingStubFull.updateBrokerage(message));
  }

  @Test
  public void testCreateCommonTransaction() {
    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(
            ByteString.copyFrom(Objects
                .requireNonNull(decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz"))))
        .setBrokerage(10);
    Transaction.Builder transaction = Transaction.newBuilder();
    Transaction.raw.Builder raw = Transaction.raw.newBuilder();
    Transaction.Contract.Builder contract = Transaction.Contract.newBuilder();
    contract.setType(Transaction.Contract.ContractType.UpdateBrokerageContract)
        .setParameter(Any.pack(updateBrokerageContract.build()));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    assertNotNull(blockingStubFull.createCommonTransaction(transaction.build()));
  }

  @Test
  public void testGetTransactionInfoByBlockNum() {
    NumberMessage message = NumberMessage.newBuilder().setNum(1).build();
    assertNotNull(blockingStubFull.getTransactionInfoByBlockNum(message));
    assertNotNull(blockingStubSolidity.getTransactionInfoByBlockNum(message));
  }

  @Test
  public void testMarketSellAsset() {
    String sellTokenId = "123";
    long sellTokenQuant = 100000000L;
    String buyTokenId = "456";
    long buyTokenQuant = 200000000L;
    MarketSellAssetContract message = MarketSellAssetContract.newBuilder()
        .setOwnerAddress(ownerAddress)
        .setBuyTokenQuantity(buyTokenQuant)
        .setBuyTokenId(ByteString.copyFrom(buyTokenId.getBytes()))
        .setSellTokenQuantity(sellTokenQuant)
        .setSellTokenId(ByteString.copyFrom(sellTokenId.getBytes()))
        .build();
    assertNotNull(blockingStubFull.marketSellAsset(message));
  }

  @Test
  public void testMarketCancelOrder() {
    MarketCancelOrderContract message = MarketCancelOrderContract.newBuilder()
        .setOwnerAddress(ownerAddress)
        .setOrderId(ByteString.copyFromUtf8("123"))
        .build();
    assertNotNull(blockingStubFull.marketCancelOrder(message));
  }

  @Test
  public void testGetMarketOrderByAccount() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
    assertNotNull(blockingStubFull.getMarketOrderByAccount(message));
    assertNotNull(blockingStubSolidity.getMarketOrderByAccount(message));
    assertNotNull(blockingStubPBFT.getMarketOrderByAccount(message));
  }

  //    @Test
  //    public void testGetMarketOrderById() {
  //      BytesMessage message = BytesMessage.newBuilder().setValue(ownerAddress).build();
  //    assertNotNull(blockingStubFull.getMarketOrderById(message));
  //    assertNotNull(blockingStubSolidity.getMarketOrderById(message));
  //    assertNotNull(blockingStubPBFT.getMarketOrderById(message));
  //    }

  @Test
  public void testGetMarketPriceByPair() {
    MarketOrderPair marketOrderPair = getMarketOrderPair();
    assertNotNull(blockingStubFull.getMarketPriceByPair(marketOrderPair));
    assertNotNull(blockingStubSolidity.getMarketPriceByPair(marketOrderPair));
    assertNotNull(blockingStubPBFT.getMarketPriceByPair(marketOrderPair));
  }

  @Test
  public void testGetMarketOrderListByPair() {
    MarketOrderPair marketOrderPair = getMarketOrderPair();
    assertNotNull(blockingStubFull.getMarketOrderListByPair(marketOrderPair));
    assertNotNull(blockingStubSolidity.getMarketOrderListByPair(marketOrderPair));
    assertNotNull(blockingStubPBFT.getMarketOrderListByPair(marketOrderPair));
  }

  private static MarketOrderPair getMarketOrderPair() {
    ByteString buyTokenId = ByteString.copyFrom(Objects
        .requireNonNull(ByteArray.fromString("_")));
    ByteString sellTokenId = ByteString.copyFrom(Objects
        .requireNonNull(ByteArray.fromString("_")));
    return MarketOrderPair.newBuilder()
        .setBuyTokenId(buyTokenId)
        .setSellTokenId(sellTokenId).build();
  }

  @Test
  public void testGetMarketPairList() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getMarketPairList(message));
    assertNotNull(blockingStubSolidity.getMarketPairList(message));
    assertNotNull(blockingStubPBFT.getMarketPairList(message));
  }

  @Test
  public void testGetTransactionFromPending() {
    BalanceContract.TransferContract tc =
        BalanceContract.TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, TransferContract);
    BytesMessage message = BytesMessage.newBuilder()
        .setValue(trx.getTransactionId().getByteString()).build();
    assertNotNull(blockingStubFull.getTransactionFromPending(message));
  }

  @Test
  public void testGetTransactionListFromPending() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getTransactionListFromPending(message));
  }

  @Test
  public void testGetPendingSize() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getPendingSize(message));
  }

  @Test
  public void testGetBlock() {
    BlockReq message = BlockReq.newBuilder().setIdOrNum("0").build();
    assertNotNull(blockingStubFull.getBlock(message));
    assertNotNull(blockingStubSolidity.getBlock(message));
    assertNotNull(blockingStubPBFT.getBlock(message));
  }

  @Test
  public void testGetAccountBalance() {
    AccountIdentifier accountIdentifier = AccountIdentifier.newBuilder()
        .setAddress(ownerAddress).build();
    BlockIdentifier blockIdentifier = getBlockIdentifier();
    AccountBalanceRequest message = AccountBalanceRequest.newBuilder()
        .setAccountIdentifier(accountIdentifier)
        .setBlockIdentifier(blockIdentifier)
        .build();
    assertNotNull(blockingStubFull.getAccountBalance(message));
  }

  @Test
  public void testGetBlockBalanceTrace() {
    BlockIdentifier blockIdentifier = getBlockIdentifier();
    assertNotNull(blockingStubFull.getBlockBalanceTrace(blockIdentifier));
  }

  private static BlockIdentifier getBlockIdentifier() {
    Block nowBlock = blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
    raw rawData = nowBlock.getBlockHeader().getRawData();
    BlockCapsule.BlockId blockId =
        new BlockCapsule.BlockId(Sha256Hash.of(getInstance().isECKeyCryptoEngine(),
            rawData.toByteArray()),
            rawData.getNumber());
    return BlockIdentifier.newBuilder()
        .setNumber(rawData.getNumber())
        .setHash(blockId.getByteString())
        .build();
  }

  @Test
  public void testCreateTransaction() {
    BalanceContract.TransferContract transferContract = BalanceContract.TransferContract
        .newBuilder()
        .setOwnerAddress(ownerAddress)
        .setToAddress(ownerAddress)
        .setAmount(1000)
        .build();
    assertNotNull(blockingStubFull.createTransaction(transferContract));
    assertNotNull(blockingStubFull.createTransaction2(transferContract));
  }

  @Test
  public void testGetTransactionSignWeight() {
    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(
            ByteString.copyFrom(Objects
                .requireNonNull(decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz"))))
        .setBrokerage(10);
    Transaction.Builder transaction = Transaction.newBuilder();
    Transaction.raw.Builder raw = Transaction.raw.newBuilder();
    Transaction.Contract.Builder contract = Transaction.Contract.newBuilder();
    contract.setType(Transaction.Contract.ContractType.UpdateBrokerageContract)
        .setParameter(Any.pack(updateBrokerageContract.build()));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    assertNotNull(blockingStubFull.getTransactionSignWeight(transaction.build()));
  }

  @Test
  public void testGetTransactionApprovedList() {
    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(
            ByteString.copyFrom(Objects
                .requireNonNull(decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz"))))
        .setBrokerage(10);
    Transaction.Builder transaction = Transaction.newBuilder();
    Transaction.raw.Builder raw = Transaction.raw.newBuilder();
    Transaction.Contract.Builder contract = Transaction.Contract.newBuilder();
    contract.setType(Transaction.Contract.ContractType.UpdateBrokerageContract)
        .setParameter(Any.pack(updateBrokerageContract.build()));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    assertNotNull(blockingStubFull.getTransactionApprovedList(transaction.build()));
  }

  @Test
  public void testCreateAssetIssue() {
    AssetIssueContract assetIssueContract = AssetIssueContract.newBuilder()
        .build();
    assertNotNull(blockingStubFull.createAssetIssue(assetIssueContract));
    assertNotNull(blockingStubFull.createAssetIssue2(assetIssueContract));
  }

  @Test
  public void testUnfreezeAsset() {
    UnfreezeAssetContract message = UnfreezeAssetContract.newBuilder().build();
    assertNotNull(blockingStubFull.unfreezeAsset(message));
    assertNotNull(blockingStubFull.unfreezeAsset2(message));
  }

  @Test
  public void testVoteWitnessAccount() {
    VoteWitnessContract message = VoteWitnessContract.newBuilder().build();
    assertNotNull(blockingStubFull.voteWitnessAccount(message));
    assertNotNull(blockingStubFull.voteWitnessAccount2(message));
  }

  @Test
  public void testUpdateSetting() {
    UpdateSettingContract message = UpdateSettingContract.newBuilder().build();
    assertNotNull(blockingStubFull.updateSetting(message));
  }

  @Test
  public void testUpdateEnergyLimit() {
    UpdateEnergyLimitContract message = UpdateEnergyLimitContract.newBuilder().build();
    assertNotNull(blockingStubFull.updateEnergyLimit(message));
  }

  @Test
  public void testClearContractABI() {
    ClearABIContract message = ClearABIContract.newBuilder().build();
    assertNotNull(blockingStubFull.clearContractABI(message));
  }

  @Test
  public void testCreateWitness() {
    WitnessCreateContract message = WitnessCreateContract.newBuilder().build();
    assertNotNull(blockingStubFull.createWitness(message));
    assertNotNull(blockingStubFull.createWitness2(message));
  }

  @Test
  public void testCreateAccount() {
    AccountCreateContract message = AccountCreateContract.newBuilder().build();
    assertNotNull(blockingStubFull.createAccount(message));
    assertNotNull(blockingStubFull.createAccount2(message));
  }

  @Test
  public void testUpdateWitness() {
    WitnessUpdateContract message = WitnessUpdateContract.newBuilder().build();
    assertNotNull(blockingStubFull.updateWitness(message));
    assertNotNull(blockingStubFull.updateWitness2(message));
  }

  @Test
  public void testUpdateAccount() {
    AccountUpdateContract message = AccountUpdateContract.newBuilder().build();
    assertNotNull(blockingStubFull.updateAccount(message));
    assertNotNull(blockingStubFull.updateAccount2(message));
  }

  @Test
  public void testSetAccountId() {
    SetAccountIdContract message = SetAccountIdContract.newBuilder().build();
    assertNotNull(blockingStubFull.setAccountId(message));
  }

  @Test
  public void testUpdateAsset() {
    UpdateAssetContract message = UpdateAssetContract.newBuilder().build();
    assertNotNull(blockingStubFull.updateAsset(message));
    assertNotNull(blockingStubFull.updateAsset2(message));
  }

  @Test
  public void testFreezeBalance2() {
    FreezeBalanceContract message = FreezeBalanceContract.newBuilder().build();
    assertNotNull(blockingStubFull.freezeBalance(message));
    assertNotNull(blockingStubFull.freezeBalance2(message));
  }

  @Test
  public void testFreezeBalanceV2() {
    FreezeBalanceV2Contract message = FreezeBalanceV2Contract.newBuilder().build();
    assertNotNull(blockingStubFull.freezeBalanceV2(message));
  }

  @Test
  public void testUnfreezeBalance() {
    UnfreezeBalanceContract message = UnfreezeBalanceContract.newBuilder().build();
    assertNotNull(blockingStubFull.unfreezeBalance(message));
    assertNotNull(blockingStubFull.unfreezeBalance2(message));
  }

  @Test
  public void testUnfreezeBalanceV2() {
    UnfreezeBalanceV2Contract message = UnfreezeBalanceV2Contract.newBuilder().build();
    assertNotNull(blockingStubFull.unfreezeBalanceV2(message));
  }

  @Test
  public void testWithdrawBalance() {
    WithdrawBalanceContract message = WithdrawBalanceContract.newBuilder().build();
    assertNotNull(blockingStubFull.withdrawBalance(message));
    assertNotNull(blockingStubFull.withdrawBalance2(message));
  }

  @Test
  public void testWithdrawExpireUnfreeze() {
    WithdrawExpireUnfreezeContract message = WithdrawExpireUnfreezeContract.newBuilder().build();
    assertNotNull(blockingStubFull.withdrawExpireUnfreeze(message));
  }

  @Test
  public void testDelegateResource() {
    DelegateResourceContract message = DelegateResourceContract.newBuilder().build();
    assertNotNull(blockingStubFull.delegateResource(message));
  }

  @Test
  public void testUnDelegateResource() {
    UnDelegateResourceContract message = UnDelegateResourceContract.newBuilder().build();
    assertNotNull(blockingStubFull.unDelegateResource(message));
  }

  @Test
  public void testCancelAllUnfreezeV2() {
    CancelAllUnfreezeV2Contract message = CancelAllUnfreezeV2Contract.newBuilder().build();
    assertNotNull(blockingStubFull.cancelAllUnfreezeV2(message));
  }

  @Test
  public void testProposalCreate() {
    ProposalCreateContract message = ProposalCreateContract.newBuilder().build();
    assertNotNull(blockingStubFull.proposalCreate(message));
  }

  @Test
  public void testProposalApprove() {
    ProposalApproveContract message = ProposalApproveContract.newBuilder().build();
    assertNotNull(blockingStubFull.proposalApprove(message));
  }

  @Test
  public void testProposalDelete() {
    ProposalDeleteContract message = ProposalDeleteContract.newBuilder().build();
    assertNotNull(blockingStubFull.proposalDelete(message));
  }

  @Test
  public void testExchangeCreate() {
    ExchangeCreateContract message = ExchangeCreateContract.newBuilder().build();
    assertNotNull(blockingStubFull.exchangeCreate(message));
  }

  @Test
  public void testExchangeInject() {
    ExchangeInjectContract message = ExchangeInjectContract.newBuilder().build();
    assertNotNull(blockingStubFull.exchangeInject(message));
  }

  @Test
  public void testExchangeWithdraw() {
    ExchangeWithdrawContract message = ExchangeWithdrawContract.newBuilder().build();
    assertNotNull(blockingStubFull.exchangeWithdraw(message));
  }

  @Test
  public void testExchangeTransaction() {
    ExchangeTransactionContract message = ExchangeTransactionContract.newBuilder().build();
    assertNotNull(blockingStubFull.exchangeTransaction(message));
  }

  @Test
  public void testTransferAsset() {
    TransferAssetContract message = TransferAssetContract.newBuilder().build();
    assertNotNull(blockingStubFull.transferAsset(message));
    assertNotNull(blockingStubFull.transferAsset2(message));
  }

  @Test
  public void testParticipateAssetIssue() {
    ParticipateAssetIssueContract message = ParticipateAssetIssueContract.newBuilder().build();
    assertNotNull(blockingStubFull.participateAssetIssue(message));
    assertNotNull(blockingStubFull.participateAssetIssue2(message));
  }

  @Test
  public void testGetAssetIssueByAccount() {
    Account message = Account.newBuilder().build();
    assertNotNull(blockingStubFull.getAssetIssueByAccount(message));
  }

  @Test
  public void testGetAccountNet() {
    Account message = Account.newBuilder().build();
    assertNotNull(blockingStubFull.getAccountNet(message));
  }

  @Test
  public void testGetAccountResource() {
    Account message = Account.newBuilder().build();
    assertNotNull(blockingStubFull.getAccountResource(message));
  }

  @Test
  public void testGetBlockById() {
    BytesMessage message = BytesMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getBlockById(message));
  }

  @Test
  public void testGetProposalById() {
    BytesMessage message = BytesMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getProposalById(message));
  }

  @Test
  public void testGetBlockByLimitNext() {
    BlockLimit message = BlockLimit.newBuilder().build();
    assertNotNull(blockingStubFull.getBlockByLimitNext(message));
    assertNotNull(blockingStubFull.getBlockByLimitNext2(message));
  }

  @Test
  public void testGetBlockByLatestNum() {
    NumberMessage message = NumberMessage.newBuilder().setNum(0).build();
    assertNotNull(blockingStubFull.getBlockByLatestNum(message));
    assertNotNull(blockingStubFull.getBlockByLatestNum2(message));
  }

  @Test
  public void testDeployContract() {
    CreateSmartContract message = CreateSmartContract.newBuilder().build();
    assertNotNull(blockingStubFull.deployContract(message));
  }

  @Test
  public void testTotalTransaction() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.totalTransaction(message));
  }

  @Test
  public void testGetNextMaintenanceTime() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getNextMaintenanceTime(message));
  }

  @Test
  public void testTriggerContract() {
    TriggerSmartContract message = TriggerSmartContract.newBuilder().build();
    assertNotNull(blockingStubFull.estimateEnergy(message));
    assertNotNull(blockingStubSolidity.estimateEnergy(message));
    assertNotNull(blockingStubPBFT.estimateEnergy(message));
  }

  @Test
  public void testEstimateEnergy() {
    TriggerSmartContract message = TriggerSmartContract.newBuilder().build();
    assertNotNull(blockingStubFull.estimateEnergy(message));
  }

  @Test
  public void testTriggerConstantContract() {
    TriggerSmartContract message = TriggerSmartContract.newBuilder().build();
    assertNotNull(blockingStubFull.triggerConstantContract(message));
  }

  @Test
  public void testGetContract() {
    BytesMessage message = BytesMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getContract(message));
  }

  @Test
  public void testGetContractInfo() {
    BytesMessage message = BytesMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getContractInfo(message));
  }

  @Test
  public void testListProposals() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.listProposals(message));
  }

  @Test
  public void testGetBandwidthPrices() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getBandwidthPrices(message));
    assertNotNull(blockingStubSolidity.getBandwidthPrices(message));
    assertNotNull(blockingStubPBFT.getBandwidthPrices(message));
  }

  @Test
  public void testGetEnergyPrices() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getEnergyPrices(message));
    assertNotNull(blockingStubSolidity.getEnergyPrices(message));
    assertNotNull(blockingStubPBFT.getEnergyPrices(message));
  }

  @Test
  public void testGetMemoFee() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getMemoFee(message));
  }

  @Test
  public void testGetPaginatedProposalList() {
    PaginatedMessage message = PaginatedMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getPaginatedProposalList(message));
  }

  @Test
  public void testGetPaginatedExchangeList() {
    PaginatedMessage message = PaginatedMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getPaginatedExchangeList(message));
  }

  @Test
  public void testGetChainParameters() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getChainParameters(message));
  }

  @Test
  public void testGetNodeInfo() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getNodeInfo(message));
  }

  @Test
  public void testAccountPermissionUpdate() {
    AccountPermissionUpdateContract message = AccountPermissionUpdateContract.newBuilder().build();
    assertNotNull(blockingStubFull.accountPermissionUpdate(message));
  }

  @Test
  public void testCreateShieldedTransaction() {
    PrivateParameters message = PrivateParameters.newBuilder().build();
    assertNotNull(blockingStubFull.createShieldedTransaction(message));
  }

  @Test
  public void testCreateShieldedTransactionWithoutSpendAuthSig() {
    PrivateParametersWithoutAsk message = PrivateParametersWithoutAsk.newBuilder().build();
    assertNotNull(blockingStubFull.createShieldedTransactionWithoutSpendAuthSig(message));
  }

  @Test
  public void testGetNewShieldedAddress() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getNewShieldedAddress(message));
  }

  @Test
  public void test01GetSpendingKey() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    BytesMessage spendingKey = blockingStubFull.getSpendingKey(message);
    assertNotNull(spendingKey);
    sk = spendingKey.getValue();
  }

  @Test
  public void testGetRcm() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    assertNotNull(blockingStubFull.getRcm(message));
  }

  @Test
  public void test02GetExpandedSpendingKey() {
    BytesMessage message = BytesMessage.newBuilder().setValue(sk).build();
    ExpandedSpendingKeyMessage eskMessage = blockingStubFull.getExpandedSpendingKey(message);
    assertNotNull(eskMessage);
    ask = eskMessage.getAsk();
    nsk = eskMessage.getNsk();
    ovk = eskMessage.getOvk();
  }

  @Test
  public void test03GetAkFromAsk() {
    BytesMessage message = BytesMessage.newBuilder().setValue(ask).build();
    BytesMessage akMessage = blockingStubFull.getAkFromAsk(message);
    assertNotNull(akMessage);
    ak = akMessage.getValue();
  }

  @Test
  public void test04GetNkFromNsk() {
    BytesMessage message = BytesMessage.newBuilder().setValue(nsk).build();
    BytesMessage nkFromNsk = blockingStubFull.getNkFromNsk(message);
    assertNotNull(nkFromNsk);
    nk = nkFromNsk.getValue();
  }

  @Test
  public void test05GetIncomingViewingKey() {
    ViewingKeyMessage viewingKeyMessage = ViewingKeyMessage.newBuilder()
        .setAk(ak)
        .setNk(nk)
        .build();
    IncomingViewingKeyMessage incomingViewingKey = blockingStubFull
        .getIncomingViewingKey(viewingKeyMessage);
    assertNotNull(incomingViewingKey);
    ivk = incomingViewingKey.getIvk();
  }

  @Test
  public void test06GetDiversifier() {
    EmptyMessage message = EmptyMessage.newBuilder().build();
    DiversifierMessage diversifier = blockingStubFull.getDiversifier(message);
    assertNotNull(diversifier);
    d = diversifier.getD();
  }

  @Test
  public void test07GetZenPaymentAddress() {
    DiversifierMessage diversifierMessage = DiversifierMessage.newBuilder().setD(d).build();
    IncomingViewingKeyMessage incomingViewingKey = IncomingViewingKeyMessage.newBuilder()
        .setIvk(ivk).build();
    IncomingViewingKeyDiversifierMessage message = IncomingViewingKeyDiversifierMessage
        .newBuilder()
        .setD(diversifierMessage)
        .setIvk(incomingViewingKey)
        .build();
    assertNotNull(blockingStubFull.getZenPaymentAddress(message));
  }

  //  @Test
  //  public void testCreateShieldNullifier() {
  //    NfParameters message = NfParameters
  //        .newBuilder().build();
  //  assertNotNull(blockingStubFull.createShieldNullifier(message));
  //  }

  //  @Test
  //  public void testCreateSpendAuthSig() {
  //    SpendAuthSigParameters message = SpendAuthSigParameters
  //        .newBuilder().build();
  //  assertNotNull(blockingStubFull.createSpendAuthSig(message));
  //  }

  //  @Test
  //  public void testGetShieldTransactionHash() {
  //    Transaction message = Transaction
  //        .newBuilder().build();
  //  assertNotNull(blockingStubFull.getShieldTransactionHash(message));
  //  }

  //  @Test
  //  public void testCreateShieldedContractParameters() {
  //    PrivateShieldedTRC20Parameters message = PrivateShieldedTRC20Parameters
  //        .newBuilder().build();
  //  assertNotNull(blockingStubFull.createShieldedContractParameters(message));
  //  }

  //  @Test
  //  public void testCreateShieldedContractParametersWithoutAsk() throws ZksnarkException {
  //    SpendingKey sk = SpendingKey.random();
  //    ExpandedSpendingKey expsk = sk.expandedSpendingKey();
  //    byte[] ovk = expsk.getOvk();
  //    PrivateShieldedTRC20ParametersWithoutAsk message = PrivateShieldedTRC20ParametersWithoutAsk
  //        .newBuilder()
  //        .setOvk(ByteString.copyFrom(ovk))
  //        .setFromAmount(BigInteger.valueOf(50).toString())
  //        .setShieldedTRC20ContractAddress(ownerAddress)
  //        .build();
  //  assertNotNull(blockingStubFull
  //    .createShieldedContractParametersWithoutAsk(message));
  //  }
}
