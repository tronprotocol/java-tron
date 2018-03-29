package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol.Witness;

@Slf4j
public class WitnessCapsule implements ProtoCapsule<Witness>, Comparable<WitnessCapsule> {

  public static final long MIN_BALANCE = 100;

  private Witness witness;

  
  @Override
  public int compareTo(WitnessCapsule otherObject) {
    return Long.compare(otherObject.getVoteCount(), this.getVoteCount());
  }

  /**
   * WitnessCapsule constructor with pubKey and url.
   */
  public WitnessCapsule(final ByteString pubKey, final String url) {
    final Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(ECKey.computeAddress(pubKey.toByteArray())))
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

  public long getLatestBlockNum() {
    return this.witness.getLatestBlockNum();
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

  public void setIsJobs(final boolean isJobs) {
    this.witness = this.witness.toBuilder().setIsJobs(isJobs).build();
  }

  public boolean getIsJobs() {
    return this.witness.getIsJobs();
  }
}
