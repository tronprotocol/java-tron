package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.protos.Protocal.Witness;

public class WitnessCapsule {

  private static final Logger logger = LoggerFactory.getLogger("WitnessCapsule");
  private Witness witness;

  private byte[] data;
  private boolean unpacked;

  /**
   * WitnessCapsule constructor with pubKey and url.
   */
  public WitnessCapsule(ByteString pubKey, String url) {
    Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(ECKey.computeAddress(pubKey.toByteArray())))
        .setUrl(url).build();
    this.unpacked = true;
  }

  public WitnessCapsule(Witness witness) {
    this.witness = witness;
    this.unpacked = true;
  }

  /**
   * WitnessCapsule constructor with address.
   */
  public WitnessCapsule(ByteString address) {
    this.witness = Witness.newBuilder().setAddress(address).build();
    this.unpacked = true;
  }

  /**
   * WitnessCapsule constructor with address and voteCount.
   */
  public WitnessCapsule(ByteString address, long voteCount) {
    Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setAddress(address)
        .setVoteCount(voteCount).build();
    this.unpacked = true;
  }

  public WitnessCapsule(byte[] data) {
    this.data = data;
    this.unpacked = false;
  }

  private synchronized void unPack() {
    try {
      if (unpacked) {
        return;
      }
      this.witness = Witness.parseFrom(data);
      this.unpacked = true;
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
    }
  }

  private void pack() {
    if (this.data == null) {
      this.data = this.witness.toByteArray();
    }
  }

  public ByteString getAddress() {
    unPack();
    return this.witness.getAddress();
  }

  public byte[] getData() {
    pack();
    return this.data;
  }

  public long getLatestBlockNum() {
    unPack();
    return this.witness.getLatestBlockNum();
  }

  public void setPubKey(ByteString pubKey) {
    unPack();
    this.witness = this.witness.toBuilder().setPubKey(pubKey).build();
  }

  public long getVoteCount() {
    unPack();
    return this.witness.getVoteCount();
  }

  public void setVoteCount(long voteCount) {
    unPack();
    this.witness = this.witness.toBuilder().setVoteCount(voteCount).build();
  }
}
