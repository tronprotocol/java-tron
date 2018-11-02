package org.tron.common.zksnark.merkle;

import java.util.Deque;
import org.tron.common.zksnark.SHA256Compress;

public class PathFiller {

  private Deque<SHA256Compress> queue;

  public PathFiller(Deque<SHA256Compress> queue) {
    this.queue = queue;
  }

  public SHA256Compress next(int depth) {
    if (queue.size() > 0) {
      SHA256Compress h = queue.poll();
      return h;
    } else {
      return EmptyMerkleRoots.emptyMerkleRootsInstance.emptyRoot(depth);
    }
  }
}
