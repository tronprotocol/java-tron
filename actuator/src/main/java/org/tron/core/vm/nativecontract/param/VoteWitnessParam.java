package org.tron.core.vm.nativecontract.param;

import com.google.protobuf.ByteString;
import org.tron.protos.Protocol;

import java.util.ArrayList;
import java.util.List;

public class VoteWitnessParam {

  private byte[] ownerAddress;

  private final List<Protocol.Vote> votes = new ArrayList<>();

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
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
