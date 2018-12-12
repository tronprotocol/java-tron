package org.tron.common.zksnark.merkle;

import java.util.ArrayList;
import java.util.List;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.protos.Contract.SHA256Compress;

public class EmptyMerkleRoots {

  public static EmptyMerkleRoots emptyMerkleRootsInstance = new EmptyMerkleRoots();

  private List<SHA256CompressCapsule> emptyRoots = new ArrayList<>();

  public EmptyMerkleRoots() {
    emptyRoots.add(SHA256CompressCapsule.uncommitted());
    for (int d = 1; d <= IncrementalMerkleTreeContainer.DEPTH; d++) {
      emptyRoots.add(
          SHA256CompressCapsule.combine(
              emptyRoots.get(d - 1).getInstance(), emptyRoots.get(d - 1).getInstance(), d - 1));
    }
  }

  public SHA256Compress emptyRoot(int depth) {
    return emptyRoots.get(depth).getInstance();
  }
}
