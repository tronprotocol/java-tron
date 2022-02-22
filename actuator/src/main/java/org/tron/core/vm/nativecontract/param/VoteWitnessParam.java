package org.tron.core.vm.nativecontract.param;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;

import org.tron.common.utils.StringUtil;
import org.tron.protos.Protocol;

/**
 * Param used by VoteWitnessProcessor
 */
public class VoteWitnessParam {

  // Address of voter
  private byte[] voterAddress;

  // List of voter`s votes. Every entry contains witness address and vote count
  private final List<Protocol.Vote> votes = new ArrayList<>();

  public byte[] getVoterAddress() {
    return voterAddress;
  }

  public void setVoterAddress(byte[] voterAddress) {
    this.voterAddress = voterAddress;
  }

  public List<Protocol.Vote> getVotes() {
    return votes;
  }

  public void addVote(byte[] witnessAddress, long tronPower) {
    this.votes.add(Protocol.Vote.newBuilder()
        .setVoteAddress(ByteString.copyFrom(witnessAddress))
        .setVoteCount(tronPower)
        .build());
  }

  public String toJsonStr() {
    StringBuilder sb = new StringBuilder("{\"votes\":[");
    String template = "{\"vote_address\":\"%s\",\"vote_count\":%d}";
    for (Protocol.Vote vote : votes) {
      sb.append(String.format(template,
          StringUtil.encode58Check(vote.getVoteAddress().toByteArray()),
          vote.getVoteCount())).append(",");
    }
    if (!votes.isEmpty()) {
      sb.deleteCharAt(sb.length() - 1);
    }
    sb.append("]}");
    return sb.toString();
  }
}
