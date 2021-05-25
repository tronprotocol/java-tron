package stest.tron.wallet.dailybuild.crosschain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.CrossChainVoteDetailList;
import org.tron.api.GrpcAPI.CrossChainVoteSummaryList;
import org.tron.api.GrpcAPI.CrossChainVoteSummaryPaginated;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ParaChainList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.CrossChainInfo;
import org.tron.protos.contract.CrossChain;
import org.tron.protos.contract.CrossChain.UnvoteCrossChainContract;
import org.tron.protos.contract.CrossChain.VoteCrossChainContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class ProposalForCrossChain {

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private ManagedChannel channelFull = null;
  private ManagedChannel crossChannelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub crossBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String crossFullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(1);
  private ByteString chainId;
  private ByteString crossChainId;
  private ByteString parentHash;
  private ByteString crossParentHash;
  private Long voteAmount = 110000000L;
  private String round ="1";
  private String crossRound ="2";
  private Long startSynBlockNum = 10L;
  private Long startSynTimeStamp;
  private Long crossStartSynTimeStamp;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] registerAccountAddress = ecKey1.getAddress();
  String registerAccountKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    crossChannelFull = ManagedChannelBuilder.forTarget(crossFullnode)
        .usePlaintext(true)
        .build();
    crossBlockingStubFull = WalletGrpc.newBlockingStub(crossChannelFull);

    chainId = PublicMethed.getBlock(1,blockingStubFull ).getBlockHeader().getRawData().getParentHash();
    crossChainId = PublicMethed.getBlock(1,crossBlockingStubFull).getBlockHeader().getRawData().getParentHash();
    parentHash = PublicMethed.getBlock(startSynBlockNum,blockingStubFull).getBlockHeader().getRawData().getParentHash();
    crossParentHash = PublicMethed.getBlock(startSynBlockNum,crossBlockingStubFull).getBlockHeader().getRawData().getParentHash();
    startSynTimeStamp = PublicMethed.getBlock(startSynBlockNum,blockingStubFull ).getBlockHeader().getRawData().getTimestamp();
    crossStartSynTimeStamp = PublicMethed.getBlock(startSynBlockNum,crossBlockingStubFull).getBlockHeader().getRawData().getTimestamp();

    registerAccountKey = "7400E3D0727F8A61041A8E8BF86599FE5597CE19DE451E59AED07D60967A5E25";
    registerAccountAddress = PublicMethed.getFinalAddress(registerAccountKey);
  }

  @Test(enabled = true,description = "Create proposal for cross chain")
  public void test01CreateProposalForGetAllowCrossChain() {
    PublicMethed.sendcoin(witness001Address, 5000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(witness002Address, 5000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    //Create proposal for first chain
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(54L, 1L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address, witnessKey001,
        proposalId, true, blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address, witnessKey002,
        proposalId, true, blockingStubFull));


    //Create proposal for second chain
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, crossBlockingStubFull));
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    proposalList = crossBlockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    final Integer crossProposalId = listProposals.get().getProposalsCount();
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address, witnessKey001,
        crossProposalId, true, crossBlockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address, witnessKey002,
        crossProposalId, true, crossBlockingStubFull));

  }


  @Test(enabled = true,description = "Create proposal for auction config")
  public void test02CreateProposalForGetAuctionConfig() {
    //Create proposal and approval it
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    String slotCount = "12";
    String endTime = String.valueOf(System.currentTimeMillis()/1000L + 300);
    logger.info("endTime:" + endTime);
    String duration = "111";
    long slotVaule = Long.valueOf(crossRound + slotCount + endTime + duration);
    proposalMap.put(55L, slotVaule);
    proposalMap.put(56L, 100000000L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));
    slotVaule = Long.valueOf(round + slotCount + endTime + duration);
    proposalMap.put(55L, slotVaule);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address, witnessKey001,
        proposalMap, crossBlockingStubFull));
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);

    //Approval proposal for first chain
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address, witnessKey001,
        proposalId, true, blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address, witnessKey002,
        proposalId, true, blockingStubFull));


    //Approval proposal for second chain
    proposalList = crossBlockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    final Integer crossProposalId = listProposals.get().getProposalsCount();
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address, witnessKey001,
        crossProposalId, true, crossBlockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address, witnessKey002,
        crossProposalId, true, crossBlockingStubFull));

  }

  @Test(enabled = true,description = "Register cross chain")
  public void test03RegisterCrossChain() throws InvalidProtocolBufferException {
    PublicMethed.sendcoin(registerAccountAddress, 500000000L,foundationAddress ,foundationKey ,blockingStubFull);
    PublicMethed.sendcoin(registerAccountAddress, 500000000L,foundationAddress ,foundationKey ,crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    List<ByteString> srList = new ArrayList<>();
    srList.add(ByteString.copyFrom(witness001Address));
    srList.add(ByteString.copyFrom(witness002Address));
    String txid = PublicMethed.registerCrossChainGetTxid(registerAccountAddress, registerAccountAddress,
        crossChainId,srList,startSynBlockNum,30000L,
        crossParentHash,crossStartSynTimeStamp,registerAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid1 = PublicMethed.registerCrossChainGetTxid(registerAccountAddress, registerAccountAddress,
        crossChainId,srList,startSynBlockNum,30000L,
        crossParentHash,crossStartSynTimeStamp,registerAccountKey,blockingStubFull);
    Assert.assertEquals(txid1, null);


    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    CrossChainInfo registerCrossChain = any.unpack(CrossChainInfo.class);
    Assert.assertEquals(registerCrossChain.getOwnerAddress(),ByteString.copyFrom(registerAccountAddress));
    Assert.assertEquals(registerCrossChain.getProxyAddress(),ByteString.copyFrom(registerAccountAddress));
    Assert.assertEquals(registerCrossChain.getChainId(),crossChainId);
    Assert.assertEquals(registerCrossChain.getParentBlockHash(),crossParentHash);
    Assert.assertEquals(registerCrossChain.getSrListList(),srList);
    Assert.assertEquals((Long)registerCrossChain.getBeginSyncHeight(),startSynBlockNum );
    Assert.assertEquals(registerCrossChain.getMaintenanceTimeInterval(),30000L );
    Assert.assertEquals((Long)registerCrossChain.getBlockTime(), crossStartSynTimeStamp);


    PublicMethed.registerCrossChainGetTxid(registerAccountAddress, registerAccountAddress,
        chainId,srList,startSynBlockNum,30000L,parentHash
        ,startSynTimeStamp,registerAccountKey,crossBlockingStubFull);
  }

  @Test(enabled = true,description = "Vote cross chain")
  public void test04VoteCrossChain() throws InvalidProtocolBufferException {
    Long beforeVoteBalance = PublicMethed.queryAccount(registerAccountKey,blockingStubFull ).getBalance();
    String txid = PublicMethed.voteCrossChainGetTxid(registerAccountAddress, crossChainId,
        voteAmount,Integer.valueOf(crossRound), registerAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterVoteBalance = PublicMethed.queryAccount(registerAccountKey,blockingStubFull ).getBalance();
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    VoteCrossChainContract voteCrossChainContract = any.unpack(VoteCrossChainContract.class);
    Assert.assertEquals((Long) voteCrossChainContract.getAmount(), voteAmount);
    Assert.assertEquals(voteCrossChainContract.getChainId(),crossChainId);
    Assert.assertEquals((Integer) voteCrossChainContract.getRound(), Integer.valueOf(crossRound));
    Assert.assertEquals(voteCrossChainContract.getOwnerAddress(),ByteString.copyFrom(registerAccountAddress));
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(infoById.get().getFee(),1000000L);
    Assert.assertTrue(beforeVoteBalance - afterVoteBalance ==voteAmount + infoById.get().getFee());

    //vote cross chain for second chain
    PublicMethed.voteCrossChainGetTxid(registerAccountAddress, chainId,
        voteAmount,Integer.valueOf(round), registerAccountKey,crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
  }


  @Test(enabled = true,description = "Unvote cross chain")
  public void test05UnVoteCrossChain() throws InvalidProtocolBufferException {
    Long beforeUnVoteBalance = PublicMethed.queryAccount(registerAccountKey,blockingStubFull ).getBalance();
    String txid = PublicMethed.unVoteCrossChainGetTxid(registerAccountAddress, crossChainId,Integer.valueOf(crossRound), registerAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterUnVoteBalance = PublicMethed.queryAccount(registerAccountKey,blockingStubFull ).getBalance();
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    UnvoteCrossChainContract voteCrossChainContract = any.unpack(UnvoteCrossChainContract.class);

    Assert.assertEquals(voteCrossChainContract.getChainId(),crossChainId);
    Assert.assertEquals((Integer) voteCrossChainContract.getRound(), Integer.valueOf(crossRound));
    Assert.assertEquals(voteCrossChainContract.getOwnerAddress(),ByteString.copyFrom(registerAccountAddress) );
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(infoById.get().getFee(),1000000L);
    Assert.assertEquals(afterUnVoteBalance - beforeUnVoteBalance,voteAmount - infoById.get().getFee());

    PublicMethed.voteCrossChainGetTxid(registerAccountAddress, crossChainId,
        voteAmount,Integer.valueOf(crossRound), registerAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = false,description = "Update cross chain")
  public void test06UpdateCrossChain() throws InvalidProtocolBufferException {
    List<ByteString> srList = new ArrayList<>();
    srList.add(ByteString.copyFrom(foundationAddress));
    srList.add(ByteString.copyFrom(registerAccountAddress));
    String updateParentHash = "0000000000000000fd45f1e9a38283a5555dd5616efd8691c8a736e91ce9f918";
    Long beforeUpdateBalance = PublicMethed.queryAccount(registerAccountKey,blockingStubFull ).getBalance();
    String txid = PublicMethed.updateCrossChainGetTxid(registerAccountAddress, foundationAddress,
        chainId,srList,2L,30000L,
        updateParentHash,1621491901000L,registerAccountKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterUpdateBalance = PublicMethed.queryAccount(registerAccountKey,blockingStubFull ).getBalance();
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    CrossChainInfo updateCrossChain = any.unpack(CrossChainInfo.class);

    Assert.assertEquals(updateCrossChain.getOwnerAddress(),ByteString.copyFrom(registerAccountAddress));
    Assert.assertEquals(updateCrossChain.getProxyAddress(),ByteString.copyFrom(foundationAddress));
    Assert.assertEquals(updateCrossChain.getChainId(),chainId);
    Assert.assertEquals(updateCrossChain.getParentBlockHash(),ByteString.copyFrom(ByteArray.fromHexString(updateParentHash)));
    Assert.assertEquals(updateCrossChain.getSrListList(),srList);
    Assert.assertEquals(updateCrossChain.getBeginSyncHeight(),2L );
    Assert.assertEquals(updateCrossChain.getMaintenanceTimeInterval(),30000L );
    Assert.assertEquals(updateCrossChain.getBlockTime(), 1621491901000L);

    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(infoById.get().getFee(),1000000L);
    Assert.assertEquals(beforeUpdateBalance - afterUpdateBalance,infoById.get().getFee());
  }

  @Test(enabled = true,description = "Get cross chain vote summary list")
  public void test07GetCrossChainVoteSummaryList() throws InvalidProtocolBufferException {
    Optional<CrossChainVoteSummaryList> crossChainVoteSummaryList =  PublicMethed
        .getCrossChainVoteSummaryList(Integer.valueOf(crossRound),blockingStubFull);
    crossChainVoteSummaryList.get().getCrossChainVoteSummaryCount();
    Assert.assertTrue(crossChainVoteSummaryList.get().getCrossChainVoteSummaryCount() >= 1);
    Assert.assertEquals(crossChainVoteSummaryList.get().getCrossChainVoteSummary(0).getChainId(),crossChainId);
    Assert.assertEquals((Long)crossChainVoteSummaryList.get().getCrossChainVoteSummary(0).getAmount(),voteAmount);

  }

  @Test(enabled = true,description = "Get cross chain parachain list")
  public void test08GetCrossChainParachainList() throws Exception {
    int waitTimes = 30;
    Optional<ParaChainList> paraChainList = null;
    while (waitTimes-- >= 0) {
      paraChainList = PublicMethed.getParaChainList(Integer.valueOf(crossRound),blockingStubFull);
      if (paraChainList.get().getParaChainIdsCount() != 0) {
        break;
      }
      Thread.sleep(30000);
    }

    Assert.assertTrue(paraChainList.get().getParaChainIdsCount() >= 1);


  }

  @Test(enabled = true,description = "Get cross chain vote detail list")
  public void test09GetCrossChainVoteDetailList() throws InvalidProtocolBufferException {
    Optional<CrossChainVoteDetailList> crossChainVoteDetailList = PublicMethed.getCrossChainVoteDetailList(Integer.valueOf(crossRound),crossChainId,blockingStubFull);

    Assert.assertTrue(crossChainVoteDetailList.get().getVoteCrossChainContractCount() >= 1);
    int i = 0;


  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


