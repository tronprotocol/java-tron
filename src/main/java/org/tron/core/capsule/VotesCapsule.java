package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.Protocol.Votes;

@Slf4j
public class VotesCapsule implements ProtoCapsule<Votes> {

  private Votes votes;

  public VotesCapsule(final Votes votes) {
    this.votes = votes;
  }

  public VotesCapsule(final byte[] data) {
    try {
      this.votes = Votes.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public VotesCapsule(ByteString address, List<Vote> oldVotes) {
    this.votes = Votes.newBuilder()
        .setAddress(address)
        .addAllOldVotes(oldVotes)
        .build();
  }

  public ByteString getAddress() {
    return this.votes.getAddress();
  }

  public void setAddress(ByteString address) {
    this.votes = this.votes.toBuilder().setAddress(address).build();
  }

  public List<Vote> getOldVotes() {
    return this.votes.getOldVotesList();
  }

  public void setOldVotes(List<Vote> oldVotes) {
    this.votes = this.votes.toBuilder()
        .addAllOldVotes(oldVotes)
        .build();
  }

  public List<Vote> getNewVotes() {
    return this.votes.getNewVotesList();
  }

  public void clearNewVotes() {
    this.votes = this.votes.toBuilder()
        .clearNewVotes()
        .build();
  }

  public void addNewVotes(ByteString voteAddress, long voteCount) {
    this.votes = this.votes.toBuilder()
        .addNewVotes(Vote.newBuilder().setVoteAddress(voteAddress).setVoteCount(voteCount).build())
        .build();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.votes.toByteArray();
  }

  @Override
  public Votes getInstance() {
    return this.votes;
  }

}
