package org.tron.common.zksnark.merkle;

import java.util.ArrayList;
import java.util.List;
import org.tron.common.zksnark.PedersenHashCapsule;
import org.tron.protos.Contract.PedersenHash;

public class EmptyMerkleRoots {

  public static EmptyMerkleRoots emptyMerkleRootsInstance = new EmptyMerkleRoots();

  private List<PedersenHashCapsule> emptyRoots = new ArrayList<>();

  public EmptyMerkleRoots() {
    emptyRoots.add(PedersenHashCapsule.uncommitted());
    for (int d = 1; d <= IncrementalMerkleTreeContainer.DEPTH; d++) {
      emptyRoots.add(
          PedersenHashCapsule.combine(
              emptyRoots.get(d - 1).getInstance(), emptyRoots.get(d - 1).getInstance(), d - 1));
    }
  }

  public PedersenHash emptyRoot(int depth) {
    return emptyRoots.get(depth).getInstance();
  }
}
