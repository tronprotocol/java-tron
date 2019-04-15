package org.tron.common.zksnark.zen.transaction;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.protos.Contract.GrothProof;

@Slf4j(topic = "capsule")
public class ProofCapsule implements ProtoCapsule<GrothProof> {

  private GrothProof grothProof;

  public ProofCapsule(final GrothProof grothProof) {
    this.grothProof = grothProof;
  }

  public ProofCapsule(final byte[] data) {
    try {
      this.grothProof = GrothProof.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public ProofCapsule(
      ByteString bytes) {
    this.grothProof =
        GrothProof.newBuilder()
            .setValues(bytes)
            .build();
  }

  public ByteString getValues() {
    return this.grothProof.getValues();
  }

  public void setValues(ByteString bytes) {
    this.grothProof = this.grothProof.toBuilder().setValues(bytes).build();
  }

  @Override
  public byte[] getData() {
    return this.grothProof.toByteArray();
  }

  @Override
  public GrothProof getInstance() {
    return this.grothProof;
  }
}
