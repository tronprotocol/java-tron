package org.tron.stresstest.dispatch.strategy;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.stresstest.dispatch.GoodCaseTransactonCreator;
import org.tron.stresstest.dispatch.Stats;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

@ToString
public abstract class Level2Strategy extends Bucket implements IStrategy<Protocol.Transaction> {

  @Setter
  protected String name;

  @Getter
  protected Queue<Stats> stats = new ConcurrentLinkedQueue<>();

  @Override
  public Protocol.Transaction dispatch() {
    Protocol.Transaction transaction = create();
    stats(transaction);
    return transaction;
  }

  private void stats(Protocol.Transaction transaction) {
    transaction.getRawData().getContractList().forEach(this::fillStats);
  }

  @Override
  public List<Stats> stats() {
    return ImmutableList.copyOf(stats);
  }

  protected abstract Protocol.Transaction create();

  private void fillStats(Protocol.Transaction.Contract contract) {
    try {
      Any contractParameter = contract.getParameter();
      ByteString owner = null;
      ByteString to = null;
      long amount = 0;

      switch (contract.getType()) {
        case TransferContract:
          owner = contractParameter.unpack(Contract.TransferContract.class).getOwnerAddress();
          to = contractParameter.unpack(Contract.TransferContract.class).getToAddress();
          amount = contractParameter.unpack(Contract.TransferContract.class).getAmount();
          break;
        case TransferAssetContract:
          owner = contractParameter.unpack(Contract.TransferAssetContract.class).getOwnerAddress();
          to = contractParameter.unpack(Contract.TransferAssetContract.class).getToAddress();
          amount = contractParameter.unpack(Contract.TransferAssetContract.class).getAmount();
          break;
        case VoteAssetContract:
          owner = contractParameter.unpack(Contract.VoteAssetContract.class).getOwnerAddress();
          List<ByteString> voteAddressList = contractParameter.unpack(Contract.VoteAssetContract.class).getVoteAddressList();
          if (CollectionUtils.isNotEmpty(voteAddressList)) {
            to = voteAddressList.get(voteAddressList.size() - 1);
          }
          amount = contractParameter.unpack(Contract.VoteAssetContract.class).getCount();
          break;
        case VoteWitnessContract:
          owner = contractParameter.unpack(Contract.VoteWitnessContract.class).getOwnerAddress();
          List<Contract.VoteWitnessContract.Vote> votes =
              contractParameter.unpack(Contract.VoteWitnessContract.class).getVotesList();
          if (CollectionUtils.isNotEmpty(votes)) {
            to = votes.get(votes.size() - 1).getVoteAddress();
            amount = votes.get(votes.size() - 1).getVoteCount();
          }
          break;
        case AssetIssueContract:
          // todo
          owner = contractParameter.unpack(Contract.AssetIssueContract.class).getOwnerAddress();
          break;
        case ParticipateAssetIssueContract:
          owner = contractParameter.unpack(Contract.ParticipateAssetIssueContract.class).getOwnerAddress();
          to = contractParameter.unpack(Contract.ParticipateAssetIssueContract.class).getToAddress();
          amount = contractParameter.unpack(Contract.ParticipateAssetIssueContract.class).getAmount();
          break;
        case FreezeBalanceContract:
          owner = contractParameter.unpack(Contract.FreezeBalanceContract.class).getOwnerAddress();
          amount = contractParameter.unpack(Contract.FreezeBalanceContract.class).getFrozenBalance();
          break;
        // todo add other contract
        default:
          break;
      }

      if (owner != null) {
        Stats stats1 = new Stats();
        stats1.setAddress(owner);
        stats1.setType(contract.getType());
        stats1.setNice(this instanceof GoodCaseTransactonCreator);
        stats1.setAmount(-amount);
        stats.add(stats1);
      }

      if (to != null) {
        Stats stats2 = new Stats();
        stats2.setAddress(to);
        stats2.setType(contract.getType());
        stats2.setNice(this instanceof GoodCaseTransactonCreator);
        stats2.setAmount(amount);
        stats.add(stats2);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
