package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.protos.Protocal.Witness;

public class WitnessCapsule implements ProtoCapsule<Witness> {

  private static final Logger logger = LoggerFactory.getLogger("WitnessCapsule");
  private Witness witness;

  private byte[] data;
  private boolean unpacked;

  /**
   * WitnessCapsule constructor with pubKey and url.
   */
  public WitnessCapsule(final ByteString pubKey, final String url) {
    final Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(ECKey.computeAddress(pubKey.toByteArray())))
        .setUrl(url).build();
    this.unpacked = true;
  }

  public WitnessCapsule(final Witness witness) {
    this.witness = witness;
    this.unpacked = true;
  }

  /**
   * WitnessCapsule constructor with address.
   */
  public WitnessCapsule(final ByteString address) {
    this.witness = Witness.newBuilder().setAddress(address).build();
    this.unpacked = true;
  }

  /**
   * WitnessCapsule constructor with address and voteCount.
   */
  public WitnessCapsule(final ByteString address, final long voteCount) {
    final Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setAddress(address)
        .setVoteCount(voteCount).build();
    this.unpacked = true;
  }

  public WitnessCapsule(final byte[] data) {
    this.data = data;
    this.unpacked = false;
  }

  private synchronized void unPack() {
    try {
      if (this.unpacked) {
        return;
      }
      this.witness = Witness.parseFrom(this.data);
      this.unpacked = true;
    } catch (final InvalidProtocolBufferException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
    }
  }

  private void pack() {
    if (this.data == null) {
      this.data = this.witness.toByteArray();
    }
  }

  private void clearData() {
    this.data = null;
    this.unpacked = true;
  }

  public ByteString getAddress() {
    this.unPack();
    return this.witness.getAddress();
  }

  @Override
  public byte[] getData() {
    this.pack();
    return this.data;
  }

  @Override
  public Witness getInstance() {
    return this.witness;
  }

  public long getLatestBlockNum() {
    this.unPack();
    return this.witness.getLatestBlockNum();
  }

  public void setPubKey(final ByteString pubKey) {
    this.unPack();
    this.witness = this.witness.toBuilder().setPubKey(pubKey).build();
    this.clearData();
  }

  public long getVoteCount() {
    this.unPack();
    return this.witness.getVoteCount();
  }

  public void setVoteCount(final long voteCount) {
    this.unPack();
    this.witness = this.witness.toBuilder().setVoteCount(voteCount).build();
    this.clearData();
  }
}
