package org.tron.core.vm.nativecontract.param;

import com.google.protobuf.ByteString;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.List;

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
}
