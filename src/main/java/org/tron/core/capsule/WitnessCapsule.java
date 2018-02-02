package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import org.tron.protos.Protocal.Witness;

public class WitnessCapsule {

  private Witness witness;

  public WitnessCapsule(Witness witness) {
    this.witness = witness;
  }

  public WitnessCapsule(ByteString address) {
    this.witness = Witness.newBuilder().setAddress(address).build();
  }

  public ByteString getAddress() {
    return witness.getAddress();
  }

}
