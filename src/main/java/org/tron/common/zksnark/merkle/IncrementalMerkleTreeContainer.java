package org.tron.common.zksnark.merkle;

import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.protos.Contract.SHA256Compress;

@Slf4j
public class IncrementalMerkleTreeContainer {

  public static Integer DEPTH = 29;

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
      SHA256CompressCapsule parentCompressCapsule =
          new SHA256CompressCapsule(
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
          "tree has non-canonical representation; parents should not be unempty");
    }
  }

  public SHA256Compress last() {

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
      SHA256CompressCapsule parentCompressCapsule =
          new SHA256CompressCapsule(treeCapsule.getParents().get(i));
      if (parentCompressCapsule.isPresent()) {
        ret += (1 << (i + 1));
      }
    }
    return ret;
  }

  public void append(SHA256Compress obj) {

    if (isComplete(DEPTH)) {
      throw new RuntimeException("tree is full");
    }

    if (!leftIsPresent()) {
      treeCapsule.setLeft(obj);
    } else if (!rightIsPresent()) {
      treeCapsule.setRight(obj);
    } else {
      SHA256CompressCapsule combined =
          SHA256CompressCapsule.combine(treeCapsule.getLeft(), treeCapsule.getRight(), 0);

      treeCapsule.setLeft(obj);
      treeCapsule.clearRight();

      for (int i = 0; i < DEPTH; i++) {
        if (i < treeCapsule.getParents().size()) {
          SHA256CompressCapsule parentCompressCapsule =
              new SHA256CompressCapsule(treeCapsule.getParents().get(i));
          if (parentCompressCapsule.isPresent()) {
            combined =
                SHA256CompressCapsule.combine(
                    treeCapsule.getParents().get(i), combined.getInstance(), i + 1);
            treeCapsule.clearParents(i);
            //
            // treeCapsule.setParents(i,SHA256CompressCapsule.uncommitted().getInstance());
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

    for (SHA256Compress parent : treeCapsule.getParents()) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
      if (!parentCompressCapsule.isPresent()) {
        return false;
      }
    }

    return true;
  }

  public int next_depth(int skip) {

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

    for (SHA256Compress parent : treeCapsule.getParents()) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
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

  public SHA256Compress root() {
    return root(DEPTH, new ArrayDeque<SHA256Compress>());
  }

  public SHA256Compress root(long depth) {
    Deque<SHA256Compress> filler_hashes = new ArrayDeque<SHA256Compress>();
    return root(depth, filler_hashes);
  }

  public SHA256Compress root(long depth, Deque<SHA256Compress> fillerHashes) {

    PathFiller filler = new PathFiller(fillerHashes);

    SHA256Compress combineLeft = leftIsPresent() ? treeCapsule.getLeft() : filler.next(0);
    SHA256Compress combineRight = rightIsPresent() ? treeCapsule.getRight() : filler.next(0);

    logger.info("leftIsPresent:" + leftIsPresent());
    logger.info("\n");
    logger.info("combineLeft:" + ByteArray.toHexString(combineLeft.getContent().toByteArray()));
    logger.info("\n");
    logger.info("rightIsPresent:" + rightIsPresent());
    logger.info("\n");
    logger.info("combineRight:" + ByteArray.toHexString(combineRight.getContent().toByteArray()));
    logger.info("\n");

    SHA256CompressCapsule root = SHA256CompressCapsule.combine(combineLeft, combineRight, 0);
    logger.info("root:" + ByteArray.toHexString(root.getContent().toByteArray()));
    logger.info("\n");
    int d = 1;
    logger.info("parent size:" + treeCapsule.getParents().size());
    logger.info("\n");
    for (SHA256Compress parent : treeCapsule.getParents()) {
      logger.info("d:" + d);
      logger.info("\n");
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
      if (parentCompressCapsule.isPresent()) {
        logger.info(
            "parent:" + ByteArray.toHexString(parentCompressCapsule.getContent().toByteArray()));
        logger.info("\n");
        root = SHA256CompressCapsule.combine(parent, root.getInstance(), d);
      } else {
        SHA256Compress next = filler.next(d);
        logger.info("filler.next(d):" + ByteArray.toHexString(next.getContent().toByteArray()));
        logger.info("\n");
        root = SHA256CompressCapsule.combine(root.getInstance(), next, d);
      }
      d++;
    }

    while (d < depth) {
      logger.info("d:" + d);
      logger.info("\n");
      SHA256Compress left = root.getInstance();
      SHA256Compress right = filler.next(d);
      logger.info("left:" + ByteArray.toHexString(left.getContent().toByteArray()));
      logger.info("\n");
      logger.info("right:" + ByteArray.toHexString(right.getContent().toByteArray()));
      logger.info("\n");
      SHA256CompressCapsule result = SHA256CompressCapsule.combine(left, right, d);
      logger.info("result:" + ByteArray.toHexString(right.getContent().toByteArray()));
      logger.info("\n");
      root = result;
      d++;
    }

    return root.getInstance();
  }

  public MerklePath path() {
    Deque<SHA256Compress> filler_hashes = new ArrayDeque<>();
    return path(filler_hashes);
  }

  public MerklePath path(Deque<SHA256Compress> filler_hashes) {

    if (!leftIsPresent()) {
      throw new RuntimeException(
          "can't create an authentication path for the beginning of the tree");
    }

    PathFiller filler = new PathFiller(filler_hashes);

    List<SHA256Compress> path = new ArrayList<>();
    List<Boolean> index = new ArrayList<>();

    if (rightIsPresent()) {
      index.add(true);
      path.add(treeCapsule.getLeft());
    } else {
      index.add(false);
      path.add(filler.next(0));
    }

    int d = 1;

    for (SHA256Compress parent : treeCapsule.getParents()) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
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

    for (SHA256Compress b : path) {
      merklePath.add(MerkleUtils.convertBytesVectorToVector(b.getContent().toByteArray()));
    }
    Lists.reverse(merklePath);
    Lists.reverse(index);

    return new MerklePath(merklePath, index);
  }

  public byte[] getMerkleTreeKey() {
    return getRootArray();
  }

  public byte[] getRootArray() {
    return root().getContent().toByteArray();
  }

  public IncrementalMerkleWitnessContainer toWitness() {
    return new IncrementalMerkleWitnessContainer(this);
  }

  public static SHA256Compress emptyRoot() {
    return EmptyMerkleRoots.emptyMerkleRootsInstance.emptyRoot(DEPTH);
  }

  private boolean leftIsPresent() {
    return !treeCapsule.getLeft().getContent().isEmpty();
  }

  private boolean rightIsPresent() {
    return !treeCapsule.getRight().getContent().isEmpty();
  }
}
