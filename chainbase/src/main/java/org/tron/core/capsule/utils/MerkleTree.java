package org.tron.core.capsule.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.Sha256Hash;

@Getter
public class MerkleTree {

  private static volatile MerkleTree instance;
  private List<Sha256Hash> hashList;
  private List<Leaf> leaves;
  private Leaf root;

  public static MerkleTree getInstance() {
    if (instance == null) {
      synchronized (MerkleTree.class) {
        if (instance == null) {
          instance = new MerkleTree();
        }
      }
    }
    return instance;
  }

  public MerkleTree createTree(List<Sha256Hash> hashList) {
    this.leaves = new ArrayList<>();
    this.hashList = hashList;
    List<Leaf> leaves = createLeaves(hashList);

    while (leaves.size() > 1) {
      leaves = createParentLeaves(leaves);
    }

    this.root = leaves.get(0);
    return this;
  }

  public List<ProofLeaf> generateProofPath(List<Sha256Hash> hashList, Sha256Hash src) {
    List<ProofLeaf> proofPath = new ArrayList<>();
    List<Leaf> leaves = createLeaves(hashList, src, proofPath);

    while (leaves.size() > 1) {
      leaves = createParentLeaves(leaves, proofPath);
    }

    return proofPath;
  }

  public boolean validProof(Sha256Hash root, List<ProofLeaf> proofPath, Sha256Hash src) {
    Sha256Hash result = src;
    for (ProofLeaf leaf : proofPath) {
      if (leaf.leftOrRight) {
        result = computeHash(result, leaf.hash);
      } else {
        result = computeHash(leaf.hash, result);
      }
    }
    return root.equals(result);
  }

  private List<Leaf> createLeaves(List<Sha256Hash> hashList, Sha256Hash src,
      List<ProofLeaf> proofPath) {
    int step = 2;
    int len = hashList.size();
    return IntStream.iterate(0, i -> i + step)
        .limit(len)
        .filter(i -> i < len)
        .mapToObj(i -> {
          Sha256Hash leftHash = hashList.get(i);
          Sha256Hash rightHash = null;
          Leaf left = createLeaf(leftHash);
          Leaf right = null;
          boolean tmp = false;
          if (i + 1 < len) {
            rightHash = hashList.get(i + 1);
            right = createLeaf(rightHash);
            if (rightHash.equals(src)) {
              proofPath.add(new ProofLeaf(leftHash, false));
              tmp = true;
            } else if (leftHash.equals(src)) {
              proofPath.add(new ProofLeaf(rightHash, true));
              tmp = true;
            }
          } else {
            if (leftHash.equals(src)) {
              tmp = true;
            }
          }
          Leaf parent = createLeaf(left, right);
          parent.setRemark(tmp);
          return parent;
        }).collect(Collectors.toList());
  }

  private List<Leaf> createParentLeaves(List<Leaf> leaves, List<ProofLeaf> proofPath) {
    int step = 2;
    int len = leaves.size();
    return IntStream.iterate(0, i -> i + step)
        .limit(len)
        .filter(i -> i < len)
        .mapToObj(i -> {
          Leaf left = leaves.get(i);
          Leaf right = i + 1 < len ? leaves.get(i + 1) : null;
          boolean tmp = false;
          if (right != null && left.remark) {
            proofPath.add(new ProofLeaf(right.getHash(), true));
            tmp = true;
          } else if (right != null && right.remark) {
            proofPath.add(new ProofLeaf(left.getHash(), false));
            tmp = true;
          } else if (right == null && left.remark) {
            tmp = true;
          }
          Leaf parent = createLeaf(left, right);
          parent.setRemark(tmp);
          return parent;
        }).collect(Collectors.toList());
  }

  private List<Leaf> createParentLeaves(List<Leaf> leaves) {
    int step = 2;
    int len = leaves.size();
    return IntStream.iterate(0, i -> i + step)
        .limit(len)
        .filter(i -> i < len)
        .mapToObj(i -> {
          Leaf right = i + 1 < len ? leaves.get(i + 1) : null;
          return createLeaf(leaves.get(i), right);
        }).collect(Collectors.toList());
  }

  private List<Leaf> createLeaves(List<Sha256Hash> hashList) {
    int step = 2;
    int len = hashList.size();
    return IntStream.iterate(0, i -> i + step)
        .limit(len)
        .filter(i -> i < len)
        .mapToObj(i -> {
          Leaf right = i + 1 < len ? createLeaf(hashList.get(i + 1)) : null;
          return createLeaf(createLeaf(hashList.get(i)), right);
        }).collect(Collectors.toList());
  }

  private Leaf createLeaf(Leaf left, Leaf right) {
    Leaf leaf = new Leaf();
    leaf.hash = right == null ? left.hash : computeHash(left.hash, right.hash);
    leaf.left = left;
    leaf.right = right;
    this.leaves.add(leaf);
    return leaf;
  }

  private Leaf createLeaf(Sha256Hash hash) {
    Leaf leaf = new Leaf();
    leaf.hash = hash;
    this.leaves.add(leaf);
    return leaf;
  }

  public Sha256Hash computeHash(Sha256Hash leftHash, Sha256Hash rightHash) {
    return Sha256Hash.of(leftHash.getByteString().concat(rightHash.getByteString()).toByteArray());
  }

  @Getter
  @Setter
  public class Leaf {

    private Sha256Hash hash;
    private Leaf left, right;
    private boolean remark;
  }

  @Getter
  @Setter
  public class ProofLeaf {

    private Sha256Hash hash;
    private boolean leftOrRight;//left leaf is false,right is true

    public ProofLeaf(Sha256Hash hash, boolean leftOrRight) {
      this.hash = hash;
      this.leftOrRight = leftOrRight;
    }
  }
}