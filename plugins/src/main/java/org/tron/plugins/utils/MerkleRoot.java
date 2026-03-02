package org.tron.plugins.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;

public class MerkleRoot {

  private MerkleRoot() {

  }

  public static Sha256Hash root(List<Sha256Hash> hashList) {
    List<Leaf> leaves = createLeaves(hashList);
    while (leaves.size() > 1) {
      leaves = createParentLeaves(leaves);
    }
    return leaves.isEmpty() ? Sha256Hash.ZERO_HASH : leaves.get(0).hash;
  }

  private static List<Leaf> createParentLeaves(List<Leaf> leaves) {
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

  private static List<Leaf> createLeaves(List<Sha256Hash> hashList) {
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

  private static Leaf createLeaf(Leaf left, Leaf right) {
    Leaf leaf = new Leaf();
    leaf.hash = right == null ? left.hash : computeHash(left.hash, right.hash);
    return leaf;
  }

  private static Leaf createLeaf(Sha256Hash hash) {
    Leaf leaf = new Leaf();
    leaf.hash = hash;
    return leaf;
  }

  private static Sha256Hash computeHash(Sha256Hash leftHash, Sha256Hash rightHash) {
    return Sha256Hash.of(true,
        leftHash.getByteString().concat(rightHash.getByteString()).toByteArray());
  }

  @Getter
  private static class Leaf {

    private Sha256Hash hash;
  }
}
