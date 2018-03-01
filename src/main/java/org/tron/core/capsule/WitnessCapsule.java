package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.protos.Protocal.Witness;

public class WitnessCapsule {

  private Witness witness;

  public WitnessCapsule(ByteString pubKey, String url) {
    Witness.Builder witnessBuilder = Witness.newBuilder();
    this.witness = witnessBuilder
        .setPubKey(pubKey)
        .setAddress(ByteString.copyFrom(ECKey.computeAddress(pubKey.toByteArray())))
        .setUrl(url).build();
  }

  public WitnessCapsule(Witness witness) {
    this.witness = witness;
  }

  public WitnessCapsule(ByteString address) {
    this.witness = Witness.newBuilder().setAddress(address).build();
  }

  public ByteString getAddress() {
    return witness.getAddress();
  }

  public long getLatestBlockNum() {
    return witness.getLatestBlockNum();
  }

  public void setPubKey(ByteString pubKey) {
    this.witness = this.witness.toBuilder().setPubKey(pubKey).build();
  }
}
