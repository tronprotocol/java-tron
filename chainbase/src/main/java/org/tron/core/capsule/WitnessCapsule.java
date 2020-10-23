package org.tron.core.capsule;

import static org.tron.common.crypto.Hash.computeAddress;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Witness;

@Slf4j(topic = "capsule")
public class WitnessCapsule implements ProtoCapsule<Witness>, Comparable<WitnessCapsule> {

  private Witness witness;


  /**
   * WitnessCapsule constructor with pubKey and url.
   */
  public WitnessCapsule(final ByteString pubKey, final String url) {
    final Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(computeAddress(pubKey.toByteArray())))
        .setUrl(url).build();
  }

  public WitnessCapsule(final Witness witness) {
    this.witness = witness;
  }

  /**
   * WitnessCapsule constructor with address.
   */
  public WitnessCapsule(final ByteString address) {
    this.witness = Witness.newBuilder().setAddress(address).build();
  }

  /**
   * WitnessCapsule constructor with address and voteCount.
   */
  public WitnessCapsule(final ByteString address, final long voteCount, final String url) {
    final Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setAddress(address)
        .setVoteCount(voteCount).setUrl(url).build();
  }

  public WitnessCapsule(final byte[] data) {
    try {
      this.witness = Witness.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  @Override
  public int compareTo(WitnessCapsule otherObject) {
    return Long.compare(otherObject.getVoteCount(), this.getVoteCount());
  }

  public ByteString getAddress() {
    return this.witness.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.witness.toByteArray();
  }

  @Override
  public Witness getInstance() {
    return this.witness;
  }

  public void setPubKey(final ByteString pubKey) {
    this.witness = this.witness.toBuilder().setPubKey(pubKey).build();
  }

  public long getVoteCount() {
    return this.witness.getVoteCount();
  }

  public void setVoteCount(final long voteCount) {
    this.witness = this.witness.toBuilder().setVoteCount(voteCount).build();
  }

  public long getTotalProduced() {
    return this.witness.getTotalProduced();
  }

  public void setTotalProduced(final long totalProduced) {
    this.witness = this.witness.toBuilder().setTotalProduced(totalProduced).build();
  }

  public long getTotalMissed() {
    return this.witness.getTotalMissed();
  }

  public void setTotalMissed(final long totalMissed) {
    this.witness = this.witness.toBuilder().setTotalMissed(totalMissed).build();
  }

  public long getLatestBlockNum() {
    return this.witness.getLatestBlockNum();
  }

  public void setLatestBlockNum(final long latestBlockNum) {
    this.witness = this.witness.toBuilder().setLatestBlockNum(latestBlockNum).build();
  }

  public long getLatestSlotNum() {
    return this.witness.getLatestSlotNum();
  }

  public void setLatestSlotNum(final long latestSlotNum) {
    this.witness = this.witness.toBuilder().setLatestSlotNum(latestSlotNum).build();
  }

  public boolean getIsJobs() {
    return this.witness.getIsJobs();
  }

  public void setIsJobs(final boolean isJobs) {
    this.witness = this.witness.toBuilder().setIsJobs(isJobs).build();
  }

  public String getUrl() {
    return this.witness.getUrl();
  }

  public void setUrl(final String url) {
    this.witness = this.witness.toBuilder().setUrl(url).build();
  }
}
