package org.tron.common.zksnark.merkle;

import java.util.ArrayDeque;
import java.util.Deque;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.OutputPoint;
import org.tron.protos.Contract.SHA256Compress;

public class IncrementalMerkleVoucherContainer {

  public static Integer DEPTH = IncrementalMerkleTreeContainer.DEPTH;

  private IncrementalMerkleVoucherCapsule witnessCapsule;

  public IncrementalMerkleVoucherContainer(IncrementalMerkleVoucherCapsule witnessCapsule) {
    this.witnessCapsule = witnessCapsule;
  }

  public IncrementalMerkleVoucherContainer(IncrementalMerkleTreeContainer tree) {
    this.witnessCapsule = new IncrementalMerkleVoucherCapsule();
    this.witnessCapsule.setTree(tree.getTreeCapsule());
  }

  private Deque<SHA256Compress> partialPath() {

    Deque<SHA256Compress> uncles = new ArrayDeque<>(witnessCapsule.getFilled());

    if (cursorExist()) {
      uncles.add(
          witnessCapsule.getCursor().toMerkleTreeContainer().root(witnessCapsule.getCursorDepth()));
    }

    return uncles;
  }

  public void append(SHA256Compress obj) {

    if (cursorExist()) {
      IncrementalMerkleTreeCapsule cursor = witnessCapsule.getCursor();
      cursor.toMerkleTreeContainer().append(obj);
      witnessCapsule.setCursor(cursor);

      long cursorDepth = witnessCapsule.getCursorDepth();

      if (witnessCapsule.getCursor().toMerkleTreeContainer().isComplete(cursorDepth)) {
        witnessCapsule.addFilled(
            witnessCapsule.getCursor().toMerkleTreeContainer().root(cursorDepth));
        witnessCapsule.clearCursor();
      }
    } else {
      long nextDepth =
          witnessCapsule
              .getTree()
              .toMerkleTreeContainer()
              .nextDepth(witnessCapsule.getFilled().size());

      witnessCapsule.setCursorDepth(nextDepth);

      if (nextDepth >= DEPTH) {
        throw new RuntimeException("tree is full");
      }

      if (nextDepth == 0) {
        witnessCapsule.addFilled(obj);
      } else {
        IncrementalMerkleTreeCapsule cursor = new IncrementalMerkleTreeCapsule();
        cursor.toMerkleTreeContainer().append(obj);
        witnessCapsule.setCursor(cursor);
      }
    }
  }

  public IncrementalMerkleVoucherCapsule getVoucherCapsule() {
    return witnessCapsule;
  }

  public MerklePath path() {
    return witnessCapsule.getTree().toMerkleTreeContainer().path(partialPath());
  }

  public SHA256Compress element() {
    return witnessCapsule.getTree().toMerkleTreeContainer().last();
  }

  public SHA256Compress root() {
    return witnessCapsule.getTree().toMerkleTreeContainer().root(DEPTH, partialPath());
  }

  public byte[] getMerkleVoucherKey() {
    OutputPoint outputPoint = witnessCapsule.getOutputPoint();

    if (outputPoint.getHash().isEmpty()) {
      throw new RuntimeException("outputPoint is not initialized");
    }
    return OutputPointUtil.outputPointToKey(outputPoint);
  }

  public byte[] getRootArray() {
    return root().getContent().toByteArray();
  }

  private boolean cursorExist() {
    return !witnessCapsule.getCursor().isEmptyTree();
  }

  public static class OutputPointUtil {

    public static byte[] outputPointToKey(OutputPoint outputPoint) {
      return outputPointToKey(outputPoint.getHash().toByteArray(), outputPoint.getIndex());
    }

    public static byte[] outputPointToKey(byte[] hashBytes, int index) {
      byte[] indexBytes = ByteArray.fromInt(index);
      byte[] rs = new byte[hashBytes.length + indexBytes.length];
      System.arraycopy(hashBytes, 0, rs, 0, hashBytes.length);
      System.arraycopy(indexBytes, 0, rs, hashBytes.length, indexBytes.length);
      return rs;
    }
  }

  public int size() {
    return witnessCapsule.getTree().toMerkleTreeContainer().size()
        + witnessCapsule.getFilled().size()
        + witnessCapsule.getCursor().toMerkleTreeContainer().size();
  }

  //for test only
  public void printSize() {
    System.out.println(
        "TreeSize:"
            + witnessCapsule.getTree().toMerkleTreeContainer().size()
            + ",FillSize:"
            + witnessCapsule.getFilled().size()
            + ",CursorSize:"
            + witnessCapsule.getCursor().toMerkleTreeContainer().size());
  }
}
