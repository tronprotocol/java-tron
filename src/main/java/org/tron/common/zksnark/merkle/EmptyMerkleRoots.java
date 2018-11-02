package org.tron.common.zksnark.merkle;

import java.util.ArrayList;
import java.util.List;
import org.tron.common.zksnark.SHA256Compress;

public class EmptyMerkleRoots {

  public static EmptyMerkleRoots emptyMerkleRootsInstance = new EmptyMerkleRoots();

  private List<SHA256Compress> emptyRoots = new ArrayList<>();

  public EmptyMerkleRoots() {
    emptyRoots.add(SHA256Compress.uncommitted());
    for (int d = 1; d <= IncrementalMerkleTree.DEPTH; d++) {
      emptyRoots
          .add(SHA256Compress.combine(emptyRoots.get(d - 1), emptyRoots.get(d - 1), d - 1));
    }
  }

  public SHA256Compress emptyRoot(int depth) {
    return emptyRoots.get(depth);
  }


}
