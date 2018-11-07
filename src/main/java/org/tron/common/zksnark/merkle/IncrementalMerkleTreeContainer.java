package org.tron.common.zksnark.merkle;


import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.protos.Contract.SHA256Compress;

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
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(
          treeCapsule.getParents().get(treeCapsule.getParents().size() - 1));
      if (!parentCompressCapsule.isExist()) {
        throw new RuntimeException("tree has non-canonical representation of parent");
      }
    }

    if ((!leftIsExist()) && rightIsExist()) {
      throw new RuntimeException("tree has non-canonical representation; right should not exist");
    }

    if ((!leftIsExist()) && treeCapsule.getParents().size() > 0) {
      throw new RuntimeException(
          "tree has non-canonical representation; parents should not be unempty");
    }
  }


  public SHA256Compress last() {

    if (rightIsExist()) {
      return treeCapsule.getRight();
    } else if (leftIsExist()) {
      return treeCapsule.getLeft();
    } else {
      throw new RuntimeException("tree has no cursor");
    }
  }


  public int size() {

    int ret = 0;
    if (leftIsExist()) {
      ret++;
    }
    if (rightIsExist()) {
      ret++;
    }
    for (int i = 0; i < treeCapsule.getParents().size(); i++) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(
          treeCapsule.getParents().get(i));
      if (parentCompressCapsule.isExist()) {
        ret += (1 << (i + 1));
      }
    }
    return ret;
  }


  public void append(SHA256Compress obj) {

    if (isComplete(DEPTH)) {
      throw new RuntimeException("tree is full");
    }

    if (!leftIsExist()) {
      treeCapsule.setLeft(obj);
    } else if (!rightIsExist()) {
      treeCapsule.setRight(obj);
    } else {
      SHA256CompressCapsule combined =
          SHA256CompressCapsule.combine(treeCapsule.getLeft(), treeCapsule.getRight(), 0);

      treeCapsule.setLeft(obj);
      treeCapsule.clearRight();

      for (int i = 0; i < DEPTH; i++) {
        if (i < treeCapsule.getParents().size()) {
          SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(
              treeCapsule.getParents().get(i));
          if (parentCompressCapsule.isExist()) {
            combined =
                SHA256CompressCapsule
                    .combine(treeCapsule.getParents().get(i), combined.getInstance(), i + 1);
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

    if (!leftIsExist() || !rightIsExist()) {
      return false;
    }

    if (treeCapsule.getParents().size() != (depth - 1)) {
      return false;
    }

    for (SHA256Compress parent : treeCapsule.getParents()) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
      if (!parentCompressCapsule.isExist()) {
        return false;
      }
    }

    return true;
  }


  public int next_depth(int skip) {

    if (!leftIsExist()) {
      if (skip != 0) {
        skip--;
      } else {
        return 0;
      }
    }

    if (!rightIsExist()) {
      if (skip != 0) {
        skip--;
      } else {
        return 0;
      }
    }

    int d = 1;

    for (SHA256Compress parent : treeCapsule.getParents()) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
      if (!parentCompressCapsule.isExist()) {
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

  public SHA256Compress root(long depth, Deque<SHA256Compress> filler_hashes) {

    PathFiller filler = new PathFiller(filler_hashes);

    SHA256Compress combine_left =
        leftIsExist() ? treeCapsule.getLeft() : filler.next(0);
    SHA256Compress combine_right =
        rightIsExist() ? treeCapsule.getRight() : filler.next(0);

    SHA256CompressCapsule root = SHA256CompressCapsule.combine(combine_left, combine_right, 0);

    int d = 1;
    for (SHA256Compress parent : treeCapsule.getParents()) {

      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
      if (parentCompressCapsule.isExist()) {
        root = SHA256CompressCapsule.combine(parent, root.getInstance(), d);
      } else {
        root = SHA256CompressCapsule.combine(root.getInstance(), filler.next(d), d);
      }

      d++;
    }

    while (d < depth) {
      root = SHA256CompressCapsule.combine(root.getInstance(), filler.next(d), d);
      d++;
    }

    return root.getInstance();
  }


  public MerklePath path() {
    Deque<SHA256Compress> filler_hashes = new ArrayDeque<>();
    return path(filler_hashes);
  }

  public MerklePath path(Deque<SHA256Compress> filler_hashes) {

    if (!leftIsExist()) {
      throw new RuntimeException(
          "can't create an authentication path for the beginning of the tree");
    }

    PathFiller filler = new PathFiller(filler_hashes);

    List<SHA256Compress> path = new ArrayList<>();
    List<Boolean> index = new ArrayList<>();

    if (rightIsExist()) {
      index.add(true);
      path.add(treeCapsule.getLeft());
    } else {
      index.add(false);
      path.add(filler.next(0));
    }

    int d = 1;

    for (SHA256Compress parent : treeCapsule.getParents()) {
      SHA256CompressCapsule parentCompressCapsule = new SHA256CompressCapsule(parent);
      if (parentCompressCapsule.isExist()) {
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

    List<List<Boolean>> merkle_path = new ArrayList<>();

    for (SHA256Compress b : path) {
      merkle_path.add(MerkleUtils.convertBytesVectorToVector(b.getContent().toByteArray()));
    }
    Lists.reverse(merkle_path);
    Lists.reverse(index);

    return new MerklePath(merkle_path, index);
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

  public static SHA256Compress empty_root() {
    return EmptyMerkleRoots.emptyMerkleRootsInstance.emptyRoot(DEPTH);
  }


  private boolean leftIsExist() {
    return !treeCapsule.getLeft().getContent().isEmpty();
  }

  private boolean rightIsExist() {
    return !treeCapsule.getRight().getContent().isEmpty();
  }


}
