package org.tron.common.zksnark.merkle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.tron.common.zksnark.SHA256Compress;

public class IncrementalWitness {

  public static Integer DEPTH = IncrementalMerkleTree.DEPTH;

  private IncrementalMerkleTree tree;
  private List<SHA256Compress> filled;
  private Optional<IncrementalMerkleTree> cursor;
  private int cursor_depth = 0;

  private Deque<SHA256Compress> partial_path() {

    Deque<SHA256Compress> uncles = new ArrayDeque<>(filled);

    if (cursor.isPresent()) {
      uncles.add(cursor.get().root(cursor_depth));
    }

    return uncles;
  }

  public IncrementalWitness(IncrementalMerkleTree tree) {
    this.tree = tree;
  }


  public MerklePath path() {
    return tree.path(partial_path());
  }

  public SHA256Compress element() {
    return tree.last();
  }

  public SHA256Compress root() {
    return tree.root(DEPTH, partial_path());
  }

  public void append(SHA256Compress obj) {

    if (cursor.isPresent()) {
      cursor.get().append(obj);

      if (cursor.get().isComplete(cursor_depth)) {
        filled.add(cursor.get().root(cursor_depth));
        cursor = Optional.empty();
      }
    } else {
      cursor_depth = tree.next_depth(filled.size());

      if (cursor_depth >= DEPTH) {
        throw new RuntimeException("tree is full");
      }

      if (cursor_depth == 0) {
        filled.add(obj);
      } else {
        cursor = Optional.of(new IncrementalMerkleTree());
        cursor.get().append(obj);
      }
    }
  }
}
