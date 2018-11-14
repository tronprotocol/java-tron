package org.tron.common.zksnark.merkle;

import java.util.ArrayDeque;
import java.util.Deque;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.OutputPoint;
import org.tron.protos.Contract.SHA256Compress;

public class IncrementalMerkleWitnessContainer {

  public static Integer DEPTH = IncrementalMerkleTreeContainer.DEPTH;

  private IncrementalMerkleWitnessCapsule witnessCapsule;

  public IncrementalMerkleWitnessContainer(IncrementalMerkleWitnessCapsule witnessCapsule) {
    this.witnessCapsule = witnessCapsule;
  }

  public IncrementalMerkleWitnessContainer(IncrementalMerkleTreeContainer tree) {
    this.witnessCapsule = new IncrementalMerkleWitnessCapsule();
    this.witnessCapsule.setTree(tree.getTreeCapsule());
  }

  private Deque<SHA256Compress> partial_path() {

    Deque<SHA256Compress> uncles = new ArrayDeque<>(witnessCapsule.getFilled());

    if (cursorIsExist()) {
      uncles.add(
          witnessCapsule.getCursor().toMerkleTreeContainer().root(witnessCapsule.getCursorDepth()));
    }

    return uncles;
  }

  public void append(SHA256Compress obj) {

    if (cursorIsExist()) {
      witnessCapsule.getCursor().toMerkleTreeContainer().append(obj);

      long cursor_depth = witnessCapsule.getCursorDepth();

      if (witnessCapsule.getCursor().toMerkleTreeContainer().isComplete()) {
        witnessCapsule.addFilled(
            witnessCapsule.getCursor().toMerkleTreeContainer().root(cursor_depth));
        witnessCapsule.clearCursor();
      }
    } else {
      long cursor_depth =
          witnessCapsule
              .getTree()
              .toMerkleTreeContainer()
              .next_depth(witnessCapsule.getFilled().size());

      witnessCapsule.setCursorDepth(cursor_depth);

      if (cursor_depth >= DEPTH) {
        throw new RuntimeException("tree is full");
      }

      if (cursor_depth == 0) {
        witnessCapsule.addFilled(obj);
      } else {
        IncrementalMerkleTreeCapsule cursor = new IncrementalMerkleTreeCapsule();
        cursor.toMerkleTreeContainer().append(obj);
        witnessCapsule.setCursor(cursor);
      }
    }
  }

  public IncrementalMerkleWitnessCapsule getWitnessCapsule() {
    return witnessCapsule;
  }

  public MerklePath path() {
    return witnessCapsule.getTree().toMerkleTreeContainer().path(partial_path());
  }

  public SHA256Compress element() {
    return witnessCapsule.getTree().toMerkleTreeContainer().last();
  }

  public SHA256Compress root() {
    return witnessCapsule.getTree().toMerkleTreeContainer().root(DEPTH, partial_path());
  }

  public byte[] getMerkleWitnessKey() {
    OutputPoint outputPoint = witnessCapsule.getOutputPoint();

    if (outputPoint.getHash().isEmpty()) {
      throw new RuntimeException("outputPoint is not initialized");
    }
    return OutputPointUtil.outputPointToKey(outputPoint);
  }

  public byte[] getRootArray() {
    return root().getContent().toByteArray();
  }

  private boolean cursorIsExist() {
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
}
