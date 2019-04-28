package org.tron.core.zen.merkle;

import java.util.Deque;
import org.tron.protos.Contract.PedersenHash;

public class PathFiller {

  private Deque<PedersenHash> queue;

  public PathFiller(Deque<PedersenHash> queue) {
    this.queue = queue;
  }

  public PedersenHash next(int depth) {
    if (queue.size() > 0) {
      return queue.poll();
    } else {
      return EmptyMerkleRoots.emptyMerkleRootsInstance.emptyRoot(depth);
    }
  }
}
