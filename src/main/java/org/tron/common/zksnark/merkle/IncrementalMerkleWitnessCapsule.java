package org.tron.common.zksnark.merkle;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.protos.Contract.IncrementalMerkleWitness;
import org.tron.protos.Contract.SHA256Compress;

@Slf4j
public class IncrementalMerkleWitnessCapsule implements ProtoCapsule<IncrementalMerkleWitness> {

  private IncrementalMerkleWitness witness;


  public IncrementalMerkleWitnessCapsule() {
    witness = IncrementalMerkleWitness.getDefaultInstance();
  }

  public IncrementalMerkleWitnessCapsule(IncrementalMerkleWitness witness) {
    this.witness = witness;
  }

  public IncrementalMerkleWitnessCapsule(final byte[] data) {
    try {
      this.witness = IncrementalMerkleWitness.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }


  public IncrementalMerkleTreeCapsule getTree() {
    return new IncrementalMerkleTreeCapsule(this.witness.getTree());
  }

  public void setTree(IncrementalMerkleTreeCapsule merkleTreeCapsule) {
    this.witness = this.witness.toBuilder().setTree(merkleTreeCapsule.getInstance()).build();
  }

  public List<SHA256Compress> getFilled() {
    return this.witness.getFilledList();
  }

  public void addFilled(SHA256Compress value) {
    this.witness = this.witness.toBuilder().addFilled(value).build();
  }

  public IncrementalMerkleTreeCapsule getCursor() {
    return new IncrementalMerkleTreeCapsule(this.witness.getCursor());
  }

  public void clearCursor() {
    this.witness = this.witness.toBuilder().clearCursor().build();
  }

  public void setCursor(IncrementalMerkleTreeCapsule cursor) {
    this.witness = this.witness.toBuilder().setCursor(cursor.getInstance()).build();
  }

  public long getCursorDepth() {
    return this.witness.getCursorDepth();
  }

  public void setCursorDepth(long cursorDepth) {
    this.witness = this.witness.toBuilder().setCursorDepth(cursorDepth).build();
  }

  @Override
  public byte[] getData() {
    return this.witness.toByteArray();
  }

  @Override
  public IncrementalMerkleWitness getInstance() {
    return this.witness;
  }

  public IncrementalMerkleWitnessContainer toMerkleWitnessContainer() {
    return new IncrementalMerkleWitnessContainer(this);
  }
}
