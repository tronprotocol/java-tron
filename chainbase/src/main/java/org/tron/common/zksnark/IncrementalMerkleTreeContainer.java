package org.tron.common.zksnark;

import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.IncrementalMerkleTreeCapsule;
import org.tron.core.capsule.PedersenHashCapsule;
import org.tron.core.exception.ZksnarkException;
import org.tron.protos.contract.ShieldContract.PedersenHash;

@Slf4j
public class IncrementalMerkleTreeContainer {

  @Getter
  @Setter
  private static Integer DEPTH = 32;

  private IncrementalMerkleTreeCapsule treeCapsule;

  public IncrementalMerkleTreeContainer(IncrementalMerkleTreeCapsule treeCapsule) {
    this.treeCapsule = treeCapsule;
  }

  public static PedersenHash emptyRoot() {
    return EmptyMerkleRoots.emptyMerkleRootsInstance.emptyRoot(DEPTH);
  }

  public IncrementalMerkleTreeCapsule getTreeCapsule() {
    return treeCapsule;
  }

  public void wfcheck() throws ZksnarkException {
    if (treeCapsule.getParents().size() >= DEPTH) {
      throw new ZksnarkException("tree has too many parents");
    }
    if (!treeCapsule.parentsIsEmpty()) {
      PedersenHashCapsule parentCompressCapsule =
          new PedersenHashCapsule(
              treeCapsule.getParents().get(treeCapsule.getParents().size() - 1));
      if (!parentCompressCapsule.isPresent()) {
        throw new ZksnarkException("tree has non-canonical representation of parent");
      }
    }

    if ((!leftIsPresent()) && rightIsPresent()) {
      throw new ZksnarkException("tree has non-canonical representation; right should not exist");
    }

    if ((!leftIsPresent()) && treeCapsule.getParents().size() > 0) {
      throw new ZksnarkException(
          "tree has non-canonical representation; parents should be empty");
    }
  }

  public PedersenHash last() throws ZksnarkException {
    if (rightIsPresent()) {
      return treeCapsule.getRight();
    } else if (leftIsPresent()) {
      return treeCapsule.getLeft();
    } else {
      throw new ZksnarkException("tree has no cursor");
    }
  }

  public int size() {
    int ret = 0;
    if (leftIsPresent()) {
      ret++;
    }
    if (rightIsPresent()) {
      ret++;
    }
    for (int i = 0; i < treeCapsule.getParents().size(); i++) {
      PedersenHashCapsule parentCompressCapsule =
          new PedersenHashCapsule(treeCapsule.getParents().get(i));
      if (parentCompressCapsule.isPresent()) {
        ret += (1 << (i + 1));
      }
    }
    return ret;
  }

  /**
   * append PedersenHash to the merkletree.
   */
  public void append(PedersenHash obj) throws ZksnarkException {
    if (isComplete(DEPTH)) {
      throw new ZksnarkException("tree is full");
    }
    if (!leftIsPresent()) {
      treeCapsule.setLeft(obj);
    } else if (!rightIsPresent()) {
      treeCapsule.setRight(obj);
    } else {
      PedersenHashCapsule combined =
          PedersenHashCapsule.combine(treeCapsule.getLeft(), treeCapsule.getRight(), 0);
      treeCapsule.setLeft(obj);
      treeCapsule.clearRight();
      for (int i = 0; i < DEPTH; i++) {
        if (i < treeCapsule.getParents().size()) {
          PedersenHashCapsule parentCompressCapsule =
              new PedersenHashCapsule(treeCapsule.getParents().get(i));
          if (parentCompressCapsule.isPresent()) {
            combined =
                PedersenHashCapsule.combine(
                    treeCapsule.getParents().get(i), combined.getInstance(), i + 1);
            treeCapsule.clearParents(i);
          } else {
            treeCapsule.setParents(i, combined.getInstance());
            break;
          }
        } else {
          treeCapsule.addParents(combined.getInstance());
          break;
        }
      }
    }
  }

  public boolean isComplete() {
    return isComplete(DEPTH);
  }

  public boolean isComplete(long depth) {
    if (!leftIsPresent() || !rightIsPresent()) {
      return false;
    }
    if (treeCapsule.getParents().size() != (depth - 1)) {
      return false;
    }
    for (PedersenHash parent : treeCapsule.getParents()) {
      PedersenHashCapsule parentCompressCapsule = new PedersenHashCapsule(parent);
      if (!parentCompressCapsule.isPresent()) {
        return false;
      }
    }
    return true;
  }

  /**
   * get the depth of the skip exist element.
   */
  public int nextDepth(int skip) {
    if (!leftIsPresent()) {
      if (skip != 0) {
        skip--;
      } else {
        return 0;
      }
    }
    if (!rightIsPresent()) {
      if (skip != 0) {
        skip--;
      } else {
        return 0;
      }
    }
    int d = 1;
    for (PedersenHash parent : treeCapsule.getParents()) {
      PedersenHashCapsule parentCompressCapsule = new PedersenHashCapsule(parent);
      if (!parentCompressCapsule.isPresent()) {
        if (skip != 0) {
          skip--;
        } else {
          return d;
        }
      }
      d++;
    }
    return d + skip;
  }

  public PedersenHash root() throws ZksnarkException {
    return root(DEPTH, new ArrayDeque<PedersenHash>());
  }

  public PedersenHash root(long depth) throws ZksnarkException {
    Deque<PedersenHash> fillerHashes = new ArrayDeque<PedersenHash>();
    return root(depth, fillerHashes);
  }

  /**
   * merge treeCapsule and fillerHashes to construct root path. if not present, use fillerHashes
   * instead. if depth of treeCapsule < depth, use fillerHashes instead.
   *
   * @return root of merged tree
   */
  public PedersenHash root(long depth, Deque<PedersenHash> fillerHashes) throws ZksnarkException {
    PathFiller filler = new PathFiller(fillerHashes);
    PedersenHash combineLeft = leftIsPresent() ? treeCapsule.getLeft() : filler.next(0);
    PedersenHash combineRight = rightIsPresent() ? treeCapsule.getRight() : filler.next(0);
    PedersenHashCapsule root = PedersenHashCapsule.combine(combineLeft, combineRight, 0);

    int d = 1;
    for (PedersenHash parent : treeCapsule.getParents()) {
      PedersenHashCapsule parentCompressCapsule = new PedersenHashCapsule(parent);
      if (parentCompressCapsule.isPresent()) {
        root = PedersenHashCapsule.combine(parent, root.getInstance(), d);
      } else {
        PedersenHash next = filler.next(d);
        root = PedersenHashCapsule.combine(root.getInstance(), next, d);
      }
      d++;
    }

    while (d < depth) {
      PedersenHash left = root.getInstance();
      PedersenHash right = filler.next(d);
      PedersenHashCapsule result = PedersenHashCapsule.combine(left, right, d);
      root = result;
      d++;
    }

    return root.getInstance();
  }

  public MerklePath path() throws ZksnarkException {
    Deque<PedersenHash> fillerHashes = new ArrayDeque<>();
    return path(fillerHashes);
  }

  /**
   * construct whole path from bottom right to root. if not present in treeCapsule, choose
   * fillerHashes
   *
   * @return list of PedersenHash, list of existence, reversed.
   */
  public MerklePath path(Deque<PedersenHash> fillerHashes) throws ZksnarkException {
    if (!leftIsPresent()) {
      throw new ZksnarkException(
          "can't create an authentication path for the beginning of the tree");
    }
    PathFiller filler = new PathFiller(fillerHashes);
    List<PedersenHash> path = new ArrayList<>();
    List<Boolean> index = new ArrayList<>();
    if (rightIsPresent()) {
      index.add(true);
      path.add(treeCapsule.getLeft());
    } else {
      index.add(false);
      path.add(filler.next(0));
    }

    int d = 1;
    for (PedersenHash parent : treeCapsule.getParents()) {
      PedersenHashCapsule parentCompressCapsule = new PedersenHashCapsule(parent);
      if (parentCompressCapsule.isPresent()) {
        index.add(true);
        path.add(parent);
      } else {
        index.add(false);
        path.add(filler.next(d));
      }
      d++;
    }

    while (d < DEPTH) {
      index.add(false);
      path.add(filler.next(d));
      d++;
    }

    List<List<Boolean>> merklePath = new ArrayList<>();
    for (PedersenHash b : path) {
      merklePath.add(ByteUtil.convertBytesVectorToVector(b.getContent().toByteArray()));
    }
    merklePath = Lists.reverse(merklePath);
    index = Lists.reverse(index);
    return new MerklePath(merklePath, index);
  }

  public byte[] getMerkleTreeKey() throws ZksnarkException {
    return getRootArray();
  }

  public byte[] getRootArray() throws ZksnarkException {
    return root().getContent().toByteArray();
  }

  public IncrementalMerkleVoucherContainer toVoucher() {
    return new IncrementalMerkleVoucherContainer(this);
  }

  private boolean leftIsPresent() {
    return !treeCapsule.getLeft().getContent().isEmpty();
  }

  private boolean rightIsPresent() {
    return !treeCapsule.getRight().getContent().isEmpty();
  }

  public static class PathFiller {

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

  public static class EmptyMerkleRoots {

    @Setter
    @Getter
    private static EmptyMerkleRoots emptyMerkleRootsInstance = new EmptyMerkleRoots();
    private List<PedersenHashCapsule> emptyRoots = new ArrayList<>();

    public EmptyMerkleRoots() {
      try {
        emptyRoots.add(PedersenHashCapsule.uncommitted());
        for (int d = 1; d <= DEPTH; d++) {
          PedersenHash a = emptyRoots.get(d - 1).getInstance();
          PedersenHash b = emptyRoots.get(d - 1).getInstance();
          emptyRoots.add(PedersenHashCapsule.combine(a, b, d - 1));
        }
      } catch (ZksnarkException e) {
        logger.error("generate EmptyMerkleRoots error!", e);
      }
    }

    public PedersenHash emptyRoot(int depth) {
      return emptyRoots.get(depth).getInstance();
    }
  }
}
