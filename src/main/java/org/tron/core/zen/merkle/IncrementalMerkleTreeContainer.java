package org.tron.core.zen.merkle;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.tron.protos.Contract.PedersenHash;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Slf4j
public class IncrementalMerkleTreeContainer {

  public static Integer DEPTH = 32;

  private IncrementalMerkleTreeCapsule treeCapsule;

  public IncrementalMerkleTreeContainer(IncrementalMerkleTreeCapsule treeCapsule) {
    this.treeCapsule = treeCapsule;
  }

  public IncrementalMerkleTreeCapsule getTreeCapsule() {
    return treeCapsule;
  }

  public int DynamicMemoryUsage() {
    return 32 + 32 + treeCapsule.getParents().size() * 32;
  }

  public void wfcheck() {
    if (treeCapsule.getParents().size() >= DEPTH) {
      throw new RuntimeException("tree has too many parents");
    }
    if (!treeCapsule.parentsIsEmpty()) {
      PedersenHashCapsule parentCompressCapsule =
          new PedersenHashCapsule(
              treeCapsule.getParents().get(treeCapsule.getParents().size() - 1));
      if (!parentCompressCapsule.isPresent()) {
        throw new RuntimeException("tree has non-canonical representation of parent");
      }
    }

    if ((!leftIsPresent()) && rightIsPresent()) {
      throw new RuntimeException("tree has non-canonical representation; right should not exist");
    }

    if ((!leftIsPresent()) && treeCapsule.getParents().size() > 0) {
      throw new RuntimeException(
          "tree has non-canonical representation; parents should be empty");
    }
  }

  public PedersenHash last() {

    if (rightIsPresent()) {
      return treeCapsule.getRight();
    } else if (leftIsPresent()) {
      return treeCapsule.getLeft();
    } else {
      throw new RuntimeException("tree has no cursor");
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
   * @param obj
   */
  public void append(PedersenHash obj) {

    if (isComplete(DEPTH)) {
      throw new RuntimeException("tree is full");
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
   * @param skip
   * @return
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

  public PedersenHash root() {
    return root(DEPTH, new ArrayDeque<PedersenHash>());
  }

  public PedersenHash root(long depth) {
    Deque<PedersenHash> fillerHashes = new ArrayDeque<PedersenHash>();
    return root(depth, fillerHashes);
  }

  /**
   * merge treeCapsule and fillerHashes to construct root path. if not present, use fillerHashes instead.
   * if depth of treeCapsule < depth, use fillerHashes instead.
   * @param depth
   * @param fillerHashes
   * @return root of merged tree
   */
  public PedersenHash root(long depth, Deque<PedersenHash> fillerHashes) {

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

  public MerklePath path() {
    Deque<PedersenHash> fillerHashes = new ArrayDeque<>();
    return path(fillerHashes);
  }

  /**
   * construct whole path from bottom right to root. if not present in treeCapsule, choose fillerHashes
   * @param fillerHashes
   * @return list of PedersenHash, list of existence, reversed.
   */
  public MerklePath path(Deque<PedersenHash> fillerHashes) {

    if (!leftIsPresent()) {
      throw new RuntimeException(
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
      merklePath.add(MerkleUtils.convertBytesVectorToVector(b.getContent().toByteArray()));
    }
    merklePath = Lists.reverse(merklePath);
    index = Lists.reverse(index);

    return new MerklePath(merklePath, index);
  }

  public byte[] getMerkleTreeKey() {
    return getRootArray();
  }

  public byte[] getRootArray() {
    return root().getContent().toByteArray();
  }

  public IncrementalMerkleVoucherContainer toVoucher() {
    return new IncrementalMerkleVoucherContainer(this);
  }

  public static PedersenHash emptyRoot() {
    return EmptyMerkleRoots.emptyMerkleRootsInstance.emptyRoot(DEPTH);
  }

  private boolean leftIsPresent() {
    return !treeCapsule.getLeft().getContent().isEmpty();
  }

  private boolean rightIsPresent() {
    return !treeCapsule.getRight().getContent().isEmpty();
  }
}
