package org.tron.common.zksnark;

import java.util.ArrayDeque;
import java.util.Deque;
import lombok.Getter;
import lombok.Setter;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.IncrementalMerkleVoucherCapsule;
import org.tron.core.exception.ZksnarkException;
import org.tron.protos.contract.ShieldContract.PedersenHash;

public class IncrementalMerkleVoucherContainer {

  @Getter
  @Setter
  private static Integer DEPTH = IncrementalMerkleTreeContainer.getDEPTH();

  private IncrementalMerkleVoucherCapsule voucherCapsule;

  public IncrementalMerkleVoucherContainer(IncrementalMerkleVoucherCapsule voucherCapsule) {
    this.voucherCapsule = voucherCapsule;
  }

  public IncrementalMerkleVoucherContainer(IncrementalMerkleTreeContainer tree) {
    this.voucherCapsule = new IncrementalMerkleVoucherCapsule();
    this.voucherCapsule.setTree(tree.getTreeCapsule());
  }

  private Deque<PedersenHash> partialPath() throws ZksnarkException {
    Deque<PedersenHash> uncles = new ArrayDeque<>(voucherCapsule.getFilled());
    if (cursorExist()) {
      uncles.add(
          voucherCapsule.getCursor().toMerkleTreeContainer().root(voucherCapsule.getCursorDepth()));
    }
    return uncles;
  }

  public void append(PedersenHash obj) throws ZksnarkException {
    if (cursorExist()) {
      IncrementalMerkleTreeCapsule cursor = voucherCapsule.getCursor();
      cursor.toMerkleTreeContainer().append(obj);
      voucherCapsule.setCursor(cursor);
      long cursorDepth = voucherCapsule.getCursorDepth();
      if (voucherCapsule.getCursor().toMerkleTreeContainer().isComplete(cursorDepth)) {
        voucherCapsule.addFilled(
            voucherCapsule.getCursor().toMerkleTreeContainer().root(cursorDepth));
        voucherCapsule.clearCursor();
      }
    } else {
      long nextDepth =
          voucherCapsule
              .getTree()
              .toMerkleTreeContainer()
              .nextDepth(voucherCapsule.getFilled().size());
      voucherCapsule.setCursorDepth(nextDepth);
      if (nextDepth >= DEPTH) {
        throw new ZksnarkException("tree is full");
      }
      if (nextDepth == 0) {
        voucherCapsule.addFilled(obj);
      } else {
        IncrementalMerkleTreeCapsule cursor = new IncrementalMerkleTreeCapsule();
        cursor.toMerkleTreeContainer().append(obj);
        voucherCapsule.setCursor(cursor);
      }
    }
  }

  public IncrementalMerkleVoucherCapsule getVoucherCapsule() {
    return voucherCapsule;
  }

  public MerklePath path() throws ZksnarkException {
    return voucherCapsule.getTree().toMerkleTreeContainer().path(partialPath());
  }

  public PedersenHash element() throws ZksnarkException {
    return voucherCapsule.getTree().toMerkleTreeContainer().last();
  }

  public long position() {
    return (long) (voucherCapsule.getTree().toMerkleTreeContainer().size() - 1);
  }

  public PedersenHash root() throws ZksnarkException {
    return voucherCapsule.getTree().toMerkleTreeContainer().root(DEPTH, partialPath());
  }

  private boolean cursorExist() {
    return !voucherCapsule.getCursor().isEmptyTree();
  }

  public int size() {
    return voucherCapsule.getTree().toMerkleTreeContainer().size()
        + voucherCapsule.getFilled().size()
        + voucherCapsule.getCursor().toMerkleTreeContainer().size();
  }

  //for test only
  public void printSize() {
    System.out.println(
        "TreeSize:"
            + voucherCapsule.getTree().toMerkleTreeContainer().size()
            + ",FillSize:"
            + voucherCapsule.getFilled().size()
            + ",CursorSize:"
            + voucherCapsule.getCursor().toMerkleTreeContainer().size());
  }
}
