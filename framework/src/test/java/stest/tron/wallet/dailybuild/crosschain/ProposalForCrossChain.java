package stest.tron.wallet.dailybuild.crosschain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.CrossChainAuctionConfigDetailList;
import org.tron.api.GrpcAPI.CrossChainVoteDetailList;
import org.tron.api.GrpcAPI.CrossChainVoteSummaryList;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ParaChainList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.GrpcAPI.RegisterCrossChainList;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.BalanceContract.CrossChainInfo;
import org.tron.protos.contract.CrossChain;
import org.tron.protos.contract.CrossChain.UnvoteCrossChainContract;
import org.tron.protos.contract.CrossChain.VoteCrossChainContract;
import stest.tron.wallet.common.client.utils.CrossChainBase;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class ProposalForCrossChain extends CrossChainBase {
  String slotCount = "";
  String endTime = "";
  String duration = "";
  Long registerNum = 206L;

  @Test(enabled = true, description = "Create proposal for cross chain")
  public void test01CreateProposalForGetAllowCrossChain() {
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


  @Test(enabled = true, description = "Create proposal for auction config")
  public void test02CreateProposalForGetAuctionConfig() {
    //Create proposal and approval it
    slotCount = "07";
    endTime = String.valueOf(System.currentTimeMillis() / 1000L + 300);
    logger.info("endTime:" + endTime);
    duration = "099";
    long slotVaule = Long.valueOf(crossRound + slotCount + endTime + duration);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
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

  @Test(enabled = true, description = "Register cross chain")
  public void test03RegisterCrossChain() throws InvalidProtocolBufferException {
    PublicMethed.sendcoin(registerAccountAddress, 500000000L,
        foundationAddress, foundationKey, blockingStubFull);
    PublicMethed.sendcoin(registerAccountAddress, 500000000L,
        foundationAddress, foundationKey, crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    final Long beforeRegisterBalance = PublicMethed.queryAccount(registerAccountKey,
        blockingStubFull).getBalance();
    List<ByteString> srList = new ArrayList<>();
    srList.add(ByteString.copyFrom(witness001Address));
    srList.add(ByteString.copyFrom(witness002Address));
    String txid = registerCrossChainGetTxid(registerAccountAddress, registerAccountAddress,
        registerNum, crossChainId, srList, startSynBlockNum, 300000L,
        crossParentHash, crossStartSynTimeStamp, registerAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid1 = registerCrossChainGetTxid(registerAccountAddress, registerAccountAddress,
        registerNum, crossChainId, srList, startSynBlockNum, 300000L,
        crossParentHash, crossStartSynTimeStamp, registerAccountKey, blockingStubFull);
    Assert.assertEquals(txid1, null);


    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    CrossChainInfo registerCrossChain = any.unpack(CrossChainInfo.class);
    Assert.assertEquals(registerCrossChain.getOwnerAddress(),
        ByteString.copyFrom(registerAccountAddress));
    Assert.assertEquals(registerCrossChain.getProxyAddress(),
        ByteString.copyFrom(registerAccountAddress));
    Assert.assertEquals(registerCrossChain.getChainId(), crossChainId);
    Assert.assertEquals(registerCrossChain.getParentBlockHash(), crossParentHash);
    Assert.assertEquals(registerCrossChain.getSrListList(), srList);
    Assert.assertEquals((Long) registerCrossChain.getBeginSyncHeight(), startSynBlockNum);
    Assert.assertEquals(registerCrossChain.getMaintenanceTimeInterval(), 300000L);
    Assert.assertEquals((Long) registerCrossChain.getBlockTime(), crossStartSynTimeStamp);


    final Long afterRegisterBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long actualFee = infoById.get().getFee();
    Assert.assertEquals(actualFee, (Long) (100000000L + infoById.get().getReceipt().getNetFee()));
    Assert.assertEquals((Long) (beforeRegisterBalance - afterRegisterBalance), actualFee);

    registerCrossChainGetTxid(registerAccountAddress, registerAccountAddress,
        registerNum, chainId, srList, startSynBlockNum, 300000L, parentHash,
        startSynTimeStamp, registerAccountKey, crossBlockingStubFull);
  }

  @Test(enabled = true, description = "Get register cross chain list")
  public void test04GetRegisterCrossChainList() throws InvalidProtocolBufferException {
    Optional<RegisterCrossChainList> registerCrossChainList
        = getRegisterCrossChainList(10, 0, blockingStubFull);

    Assert.assertTrue(registerCrossChainList.get().getCrossChainInfoCount() >= 1);
    Assert.assertEquals(registerCrossChainList.get()
        .getCrossChainInfo(registerCrossChainList.get()
            .getCrossChainInfoCount() - 1).getChainId(), crossChainId);


  }

  @Test(enabled = true, description = "Vote cross chain")
  public void test05VoteCrossChain() throws InvalidProtocolBufferException {
    final Long beforeVoteBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    String txid = voteCrossChainGetTxid(registerAccountAddress, registerNum,
        voteAmount, Integer.valueOf(crossRound), registerAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterVoteBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    VoteCrossChainContract voteCrossChainContract = any.unpack(VoteCrossChainContract.class);
    Assert.assertEquals((Long) voteCrossChainContract.getAmount(), voteAmount);
    Assert.assertEquals(voteCrossChainContract.getRegisterNum() - registerNum, 0);
    Assert.assertEquals((Integer) voteCrossChainContract.getRound(), Integer.valueOf(crossRound));
    Assert.assertEquals(voteCrossChainContract.getOwnerAddress(),
        ByteString.copyFrom(registerAccountAddress));
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long actualFee = infoById.get().getFee();
    Assert.assertEquals(actualFee, (Long) (1000000L + infoById.get().getReceipt().getNetFee()));
    Assert.assertEquals((Long) (beforeVoteBalance
        - afterVoteBalance), (Long) (actualFee + voteAmount));


    //vote cross chain for second chain
    voteCrossChainGetTxid(registerAccountAddress, registerNum,
        voteAmount, Integer.valueOf(round), registerAccountKey, crossBlockingStubFull);
    PublicMethed.waitProduceNextBlock(crossBlockingStubFull);
  }


  @Test(enabled = false, description = "Unvote cross chain")
  public void test06UnVoteCrossChain() throws InvalidProtocolBufferException {
    final Long beforeUnVoteBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    String txid = unVoteCrossChainGetTxid(registerAccountAddress, registerNum,
        Integer.valueOf(crossRound), registerAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterUnVoteBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid,  blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    UnvoteCrossChainContract voteCrossChainContract = any.unpack(UnvoteCrossChainContract.class);

    Assert.assertEquals(voteCrossChainContract.getRegisterNum() - registerNum, 0);
    Assert.assertEquals((Integer) voteCrossChainContract.getRound(), Integer.valueOf(crossRound));
    Assert.assertEquals(voteCrossChainContract.getOwnerAddress(),
        ByteString.copyFrom(registerAccountAddress));
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long actualFee = infoById.get().getFee();
    Assert.assertEquals(actualFee, (Long) (1000000L + infoById.get().getReceipt().getNetFee()));
    Assert.assertEquals((Long) (afterUnVoteBalance
        - beforeUnVoteBalance), (Long) (voteAmount - actualFee));

    voteCrossChainGetTxid(registerAccountAddress, registerNum,
        voteAmount, Integer.valueOf(crossRound), registerAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = false, description = "Update cross chain")
  public void test07UpdateCrossChain() throws InvalidProtocolBufferException {
    List<ByteString> srList = new ArrayList<>();
    srList.add(ByteString.copyFrom(foundationAddress));
    srList.add(ByteString.copyFrom(registerAccountAddress));
    String updateParentHash = "0000000000000000fd45f1e9a38283a5555dd5616efd8691c8a736e91ce9f918";
    final Long beforeUpdateBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    String txid = updateCrossChainGetTxid(registerAccountAddress, foundationAddress,
        chainId, srList, 2L, 30000L,
        updateParentHash, 1621491901000L, registerAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long afterUpdateBalance = PublicMethed
        .queryAccount(registerAccountKey, blockingStubFull).getBalance();
    Optional<Transaction> byId = PublicMethed.getTransactionById(txid, blockingStubFull);
    Any any = byId.get().getRawData().getContract(0).getParameter();
    CrossChainInfo updateCrossChain = any.unpack(CrossChainInfo.class);

    Assert.assertEquals(updateCrossChain.getOwnerAddress(),
        ByteString.copyFrom(registerAccountAddress));
    Assert.assertEquals(updateCrossChain.getProxyAddress(),
        ByteString.copyFrom(foundationAddress));
    Assert.assertEquals(updateCrossChain.getChainId(), chainId);
    Assert.assertEquals(updateCrossChain.getParentBlockHash(),
        ByteString.copyFrom(ByteArray.fromHexString(updateParentHash)));
    Assert.assertEquals(updateCrossChain.getSrListList(), srList);
    Assert.assertEquals(updateCrossChain.getBeginSyncHeight(), 2L);
    Assert.assertEquals(updateCrossChain.getMaintenanceTimeInterval(), 30000L);
    Assert.assertEquals(updateCrossChain.getBlockTime(), 1621491901000L);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Long actualFee = infoById.get().getFee();
    Assert.assertEquals(actualFee, (Long) (1000000L + infoById.get().getReceipt().getNetFee()));
    Assert.assertEquals((Long) (beforeUpdateBalance - afterUpdateBalance), actualFee);
  }

  @Test(enabled = true, description = "Get cross chain vote summary list")
  public void test08GetCrossChainVoteSummaryList() throws InvalidProtocolBufferException {
    Optional<CrossChainVoteSummaryList> crossChainVoteSummaryList
        = getCrossChainVoteSummaryList(Integer.valueOf(crossRound), blockingStubFull);
    crossChainVoteSummaryList.get().getCrossChainVoteSummaryCount();
    Assert.assertTrue(crossChainVoteSummaryList.get().getCrossChainVoteSummaryCount() >= 1);
    Assert.assertEquals(crossChainVoteSummaryList.get()
        .getCrossChainVoteSummary(0).getRegisterNum() - registerNum, 0);
    Assert.assertEquals((Long) crossChainVoteSummaryList
        .get().getCrossChainVoteSummary(0).getAmount(), voteAmount);

  }

  @Test(enabled = true, description = "Get cross chain parachain list")
  public void test09GetCrossChainParachainList() throws Exception {
    int waitTimes = 30;
    Optional<ParaChainList> paraChainList = null;
    while (waitTimes-- >= 0) {
      paraChainList = getParaChainList(Integer.valueOf(crossRound), blockingStubFull);
      if (paraChainList.get().getParaChainIdsCount() != 0) {
        break;
      }
      Thread.sleep(30000);
    }

    Assert.assertTrue(paraChainList.get().getParaChainIdsCount() >= 1);


  }

  @Test(enabled = true, description = "Get cross chain vote detail list")
  public void test10GetCrossChainVoteDetailList() throws InvalidProtocolBufferException {
    Optional<CrossChainVoteDetailList> crossChainVoteDetailList
        = getCrossChainVoteDetailList(Integer.valueOf(crossRound), registerNum, blockingStubFull);

    Assert.assertTrue(crossChainVoteDetailList.get().getVoteCrossChainContractCount() >= 1);

  }


  @Test(enabled = false, description = "get auction config list")
  public void test11GetAuctionConfigList() {
    Optional<CrossChainAuctionConfigDetailList> crossChainVoteDetailList
        = getAuctionConfigList(blockingStubFull);
    List<CrossChain.AuctionRoundContract> list =
        crossChainVoteDetailList.get().getAuctionConfigDetailList();
    Assert.assertTrue(list.size() == 1);
    Assert.assertEquals(list.get(0).getRound() + "", crossRound);
    Assert.assertEquals(list.get(0).getSlotCount(), slotCount);
    Assert.assertEquals(list.get(0).getDuration(), duration);
    Assert.assertEquals(list.get(0).getEndTime(), endTime);

  }

}


