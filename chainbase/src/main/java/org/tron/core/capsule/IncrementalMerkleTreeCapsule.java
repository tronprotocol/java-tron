package org.tron.core.capsule;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.zksnark.IncrementalMerkleTreeContainer;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleTree;
import org.tron.protos.contract.ShieldContract.PedersenHash;

@Slf4j
public class IncrementalMerkleTreeCapsule implements ProtoCapsule<IncrementalMerkleTree> {

  private IncrementalMerkleTree merkleTree;

  public IncrementalMerkleTreeCapsule() {
    merkleTree = IncrementalMerkleTree.getDefaultInstance();
  }

  public IncrementalMerkleTreeCapsule(IncrementalMerkleTree merkleTree) {
    this.merkleTree = merkleTree;
  }

  public IncrementalMerkleTreeCapsule(final byte[] data) {
    try {
      this.merkleTree = IncrementalMerkleTree.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public PedersenHash getLeft() {
    return this.merkleTree.getLeft();
  }

  public void setLeft(PedersenHash left) {
    this.merkleTree = this.merkleTree.toBuilder().setLeft(left).build();
  }

  public Boolean leftIsEmpty() {
    return this.merkleTree.getLeft().getContent().isEmpty();
  }

  public void clearLeft() {
    this.merkleTree = this.merkleTree.toBuilder().clearLeft().build();
  }

  public PedersenHash getRight() {
    return this.merkleTree.getRight();
  }

  public void setRight(PedersenHash right) {
    this.merkleTree = this.merkleTree.toBuilder().setRight(right).build();
  }

  public Boolean rightIsEmpty() {
    return this.merkleTree.getRight().getContent().isEmpty();
  }

  public void clearRight() {
    this.merkleTree = this.merkleTree.toBuilder().clearRight().build();
  }

  public List<PedersenHash> getParents() {
    return this.merkleTree.getParentsList();
  }

  public Boolean parentsIsEmpty() {
    return this.merkleTree.getParentsList().isEmpty();
  }

  public void setParents(int index, PedersenHash parents) {
    this.merkleTree = this.merkleTree.toBuilder().setParents(index, parents).build();
  }

  public void addParents(PedersenHash parents) {
    this.merkleTree = this.merkleTree.toBuilder().addParents(parents).build();
  }

  public void clearParents(int index) {
    this.merkleTree =
        this.merkleTree.toBuilder().setParents(index, PedersenHash.newBuilder().build()).build();
  }

  public boolean isEmptyTree() {
    return parentsIsEmpty() && leftIsEmpty() && rightIsEmpty();
  }

  public boolean notEmptyTree() {
    return !isEmptyTree();
  }

  @Override
  public byte[] getData() {
    return this.merkleTree.toByteArray();
  }

  @Override
  public IncrementalMerkleTree getInstance() {
    return this.merkleTree;
  }

  public IncrementalMerkleTreeContainer toMerkleTreeContainer() {
    return new IncrementalMerkleTreeContainer(this);
  }

  public IncrementalMerkleTreeCapsule deepCopy() {
    byte[] data = Arrays.copyOf(this.getData(), this.getData().length);
    return new IncrementalMerkleTreeCapsule(data);
  }
}
