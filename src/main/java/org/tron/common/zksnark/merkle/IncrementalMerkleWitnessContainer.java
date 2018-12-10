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
      System.out.println("append ,cursorExist,cursor_depth +  " + witnessCapsule.getCursorDepth());
      IncrementalMerkleTreeCapsule cursor = witnessCapsule.getCursor();
      cursor.toMerkleTreeContainer().append(obj);
      witnessCapsule.setCursor(cursor);

      long cursor_depth = witnessCapsule.getCursorDepth();

      if (witnessCapsule.getCursor().toMerkleTreeContainer().isComplete(cursor_depth)) {
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

      System.out.println("append ,cursor not Exist,cursor_depth +  " + cursor_depth);

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
    return witnessCapsule.getTree().toMerkleTreeContainer().path(partialPath());
  }

  public SHA256Compress element() {
    return witnessCapsule.getTree().toMerkleTreeContainer().last();
  }

  public SHA256Compress root() {
    return witnessCapsule.getTree().toMerkleTreeContainer().root(DEPTH, partialPath());
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
    return witnessCapsule.getTree().toMerkleTreeContainer().size() + witnessCapsule.getFilled()
        .size()
        + witnessCapsule.getCursor().toMerkleTreeContainer().size();
  }

  public void printSize() {
    System.out.println(
        "TreeSize:" + witnessCapsule.getTree().toMerkleTreeContainer().size() +
            ",FillSize:" + witnessCapsule.getFilled().size() +
            ",CursorSize:" + witnessCapsule.getCursor()
            .toMerkleTreeContainer().size());
  }


}
