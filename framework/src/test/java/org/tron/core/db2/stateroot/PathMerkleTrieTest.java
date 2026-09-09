package org.tron.core.db2.stateroot;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;
import org.tron.common.crypto.Hash;
import org.tron.core.trie.TrieImpl;

public class PathMerkleTrieTest {

  @Test
  public void emptyAndSingleLeafMatchIndependentTrieOracle() {
    InMemoryPathNodeStore store = new InMemoryPathNodeStore();
    PathMerkleTrie trie = new PathMerkleTrie(store);
    assertArrayEquals(Hash.EMPTY_TRIE_HASH, trie.rootHash());
    assertTrue(store.nodes.isEmpty());

    byte[] key = filledKey(0x11);
    byte[] value = Hex.decode("c20180");
    trie.put(key, value);

    assertArrayEquals(referenceRoot(new byte[][]{key}, new byte[][]{value}), trie.rootHash());
    assertArrayEquals(value, trie.get(key));
    assertEquals(1, trie.size());
    assertTrue(store.nodes.containsKey(""));
  }

  @Test
  public void mutationOrderProducesSameRootAndPathNodeSet() {
    byte[][] keys = {filledKey(0x11), keyWithTail(0x11, 0x12), filledKey(0x21),
        keyWithTail(0x21, 0x22)};
    byte[][] values = {value("one"), value("two"), value("three"), value("four")};

    InMemoryPathNodeStore forwardStore = new InMemoryPathNodeStore();
    PathMerkleTrie forward = new PathMerkleTrie(forwardStore);
    for (int i = 0; i < keys.length; i++) {
      forward.put(keys[i], values[i]);
    }

    InMemoryPathNodeStore reverseStore = new InMemoryPathNodeStore();
    PathMerkleTrie reverse = new PathMerkleTrie(reverseStore);
    for (int i = keys.length - 1; i >= 0; i--) {
      reverse.put(keys[i], values[i]);
    }

    byte[] expected = referenceRoot(keys, values);
    assertArrayEquals(
        Hex.decode("dc1c7bfcacb455baeca2454d8aedbe4b17da4a66364fbc0baa7e5919cacf2bdc"),
        expected);
    assertArrayEquals(expected, forward.rootHash());
    assertArrayEquals(expected, reverse.rootHash());
    assertNodeMapsEqual(forwardStore.nodes, reverseStore.nodes);
  }

  @Test
  public void updateAndDeleteCompressCanonicalPaths() {
    byte[] first = filledKey(0x33);
    byte[] second = keyWithTail(0x33, 0x34);
    byte[] third = filledKey(0x44);
    InMemoryPathNodeStore store = new InMemoryPathNodeStore();
    PathMerkleTrie trie = new PathMerkleTrie(store);
    trie.put(first, value("first"));
    trie.put(second, value("second"));
    trie.put(third, value("third"));
    trie.rootHash();
    int expandedNodeCount = store.nodes.size();

    trie.put(first, value("updated"));
    trie.delete(second);
    byte[] expected = referenceRoot(new byte[][]{first, third},
        new byte[][]{value("updated"), value("third")});
    assertArrayEquals(expected, trie.rootHash());
    assertNull(trie.get(second));
    assertTrue(store.nodes.size() < expandedNodeCount);

    trie.delete(first);
    trie.delete(third);
    assertArrayEquals(Hash.EMPTY_TRIE_HASH, trie.rootHash());
    assertTrue(store.nodes.isEmpty());
  }

  @Test
  public void rejectsInvalidKeysAndEmptyValues() {
    PathMerkleTrie trie = new PathMerkleTrie(new InMemoryPathNodeStore());
    assertThrows(NullPointerException.class, () -> new PathMerkleTrie(null));
    assertThrows(IllegalArgumentException.class, () -> trie.put(new byte[31], value("x")));
    assertThrows(IllegalArgumentException.class,
        () -> trie.put(new byte[PathMerkleTrie.SECURE_KEY_LENGTH], new byte[0]));
    assertThrows(NullPointerException.class, () -> trie.delete(null));
  }

  @Test
  public void rejectsNonCanonicalRlpWithoutDecodeReencoding() {
    InMemoryPathNodeStore store = new InMemoryPathNodeStore();
    byte[] nonCanonicalLeaf = new byte[37];
    nonCanonicalLeaf[0] = (byte) 0xe4;
    nonCanonicalLeaf[1] = (byte) 0xa1;
    nonCanonicalLeaf[2] = 0x20;
    nonCanonicalLeaf[35] = (byte) 0x81;
    nonCanonicalLeaf[36] = 0x01;
    store.nodes.put("", nonCanonicalLeaf);

    PathMerkleTrie restored = new PathMerkleTrie(store);
    restored.restoreRoot();
    assertThrows(IllegalStateException.class, () -> restored.get(new byte[32]));
  }

  @Test
  public void detectsMissingCorruptAndDirtyCommittedNodes() {
    InMemoryPathNodeStore corruptStore = new InMemoryPathNodeStore();
    PathMerkleTrie corruptTrie = new PathMerkleTrie(corruptStore);
    corruptTrie.put(filledKey(0x55), value("value"));
    corruptTrie.rootHash();
    corruptTrie.verifyNodeStore();
    corruptStore.nodes.put("", new byte[]{1});
    assertThrows(IllegalStateException.class, corruptTrie::verifyNodeStore);

    InMemoryPathNodeStore missingStore = new InMemoryPathNodeStore();
    PathMerkleTrie missingTrie = new PathMerkleTrie(missingStore);
    missingTrie.put(filledKey(0x66), value("value"));
    missingTrie.rootHash();
    missingStore.nodes.remove("");
    assertThrows(IllegalStateException.class, missingTrie::verifyNodeStore);

    PathMerkleTrie dirtyTrie = new PathMerkleTrie(new InMemoryPathNodeStore());
    dirtyTrie.put(filledKey(0x77), value("value"));
    assertThrows(IllegalStateException.class, dirtyTrie::verifyNodeStore);
  }

  @Test
  public void singleLeafUpdateRewritesOnlyItsMaterializedPath() {
    int leafCount = 32;
    byte[][] keys = new byte[leafCount][];
    byte[][] values = new byte[leafCount][];
    InMemoryPathNodeStore store = new InMemoryPathNodeStore();
    PathMerkleTrie trie = new PathMerkleTrie(store);
    for (int i = 0; i < leafCount; i++) {
      keys[i] = filledKey(i);
      values[i] = value("value-" + i);
      trie.put(keys[i], values[i]);
    }
    trie.rootHash();
    int fullNodeCount = store.nodes.size();

    values[17] = value("updated");
    trie.put(keys[17], values[17]);

    assertArrayEquals(referenceRoot(keys, values), trie.rootHash());
    assertTrue(fullNodeCount > 3);
    assertEquals(3, trie.getLastNodePuts());
    assertEquals(3, trie.getLastNodeDeletes());
    trie.verifyNodeStore();
  }

  @Test
  public void restoresRootAndLoadsOnlyTheChangedPath() {
    int leafCount = 1_000;
    byte[][] keys = new byte[leafCount][];
    byte[][] values = new byte[leafCount][];
    InMemoryPathNodeStore sourceStore = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(sourceStore);
    for (int index = 0; index < leafCount; index++) {
      keys[index] = indexedKey(index);
      values[index] = value("value-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] originalRoot = source.rootHash();

    InMemoryPathNodeStore lazyStore = new InMemoryPathNodeStore();
    lazyStore.nodes.putAll(sourceStore.nodes);
    PathMerkleTrie restored = new PathMerkleTrie(lazyStore);
    restored.restoreRoot(originalRoot);
    assertEquals(1, lazyStore.gets);
    assertArrayEquals(values[517], restored.get(keys[517]));
    assertTrue(lazyStore.gets <= PathMerkleTrie.SECURE_KEY_LENGTH * 2 + 2);

    values[517] = value("updated-lazily");
    restored.put(keys[517], values[517]);
    assertArrayEquals(referenceRoot(keys, values), restored.rootHash());
    assertTrue(restored.getLastNodePuts() <= PathMerkleTrie.SECURE_KEY_LENGTH * 2 + 1);
  }

  @Test
  public void leavesHashedSiblingsUnresolvedUntilTheirPathIsVisited() {
    byte[][] keys = new byte[16][];
    byte[][] values = new byte[16][];
    InMemoryPathNodeStore sourceStore = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(sourceStore);
    for (int index = 0; index < keys.length; index++) {
      keys[index] = filledKey(index << 4);
      values[index] = value("hashed-child-value-that-is-long-enough-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] root = source.rootHash();

    InMemoryPathNodeStore restoredStore = new InMemoryPathNodeStore();
    restoredStore.nodes.putAll(sourceStore.nodes);
    PathMerkleTrie restored = new PathMerkleTrie(restoredStore);
    restored.restoreRoot(root);
    int readsAfterRoot = restoredStore.gets;
    String corruptSibling = Hex.toHexString(new byte[]{(byte) 0x0a});
    assertTrue(restoredStore.nodes.containsKey(corruptSibling));
    restoredStore.nodes.put(corruptSibling, new byte[]{1});

    assertArrayEquals(values[3], restored.get(keys[3]));
    assertEquals(readsAfterRoot + 1, restoredStore.gets);
    assertEquals(16, restored.getHashReferenceCreateCount());
    assertEquals(1, restored.getHashReferenceResolveCount());
    assertThrows(IllegalStateException.class, restored::verifyAllNodeStore);
  }

  @Test
  public void lazyDenseBranchHasAStableEagerReadBaseline() {
    byte[][] keys = new byte[16][];
    byte[][] values = new byte[16][];
    InMemoryPathNodeStore sourceStore = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(sourceStore);
    for (int index = 0; index < keys.length; index++) {
      keys[index] = filledKey(index << 4);
      values[index] = value("hashed-child-value-that-is-long-enough-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] root = source.rootHash();

    InMemoryPathNodeStore eagerStore = new InMemoryPathNodeStore();
    eagerStore.nodes.putAll(copyNodeMap(sourceStore.nodes));
    PathMerkleTrie eager = new PathMerkleTrie(eagerStore, false);
    eager.restoreRoot(root);
    assertArrayEquals(values[3], eager.get(keys[3]));

    InMemoryPathNodeStore lazyStore = new InMemoryPathNodeStore();
    lazyStore.nodes.putAll(copyNodeMap(sourceStore.nodes));
    PathMerkleTrie lazy = new PathMerkleTrie(lazyStore);
    lazy.restoreRoot(root);
    assertArrayEquals(values[3], lazy.get(keys[3]));

    assertEquals(17, eagerStore.gets);
    assertEquals(2, lazyStore.gets);
    assertEquals(17, eager.getNodeHashVerifyCount());
    assertEquals(2, lazy.getNodeHashVerifyCount());
    assertEquals(eager.getNodeDecodeCount(), lazy.getNodeDecodeCount());
    assertEquals(16, lazy.getHashReferenceCreateCount());
    assertEquals(1, lazy.getHashReferenceResolveCount());

    assertArrayEquals(values[3], lazy.get(keys[3]));
    assertEquals(2, lazyStore.gets);
    assertEquals(2, lazy.getNodeHashVerifyCount());
    assertEquals(1, lazy.getHashReferenceResolveCount());
  }

  @Test
  public void lazyRestoreProducesTheSameUpdatedPathNodeSet() {
    int leafCount = 128;
    byte[][] keys = new byte[leafCount][];
    byte[][] values = new byte[leafCount][];
    InMemoryPathNodeStore sourceStore = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(sourceStore);
    for (int index = 0; index < leafCount; index++) {
      keys[index] = Hash.sha3(value("key-" + index));
      values[index] = value("initial-value-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] initialRoot = source.rootHash();
    Map<String, byte[]> initialNodes = copyNodeMap(sourceStore.nodes);

    values[17] = value("updated-value");
    InMemoryPathNodeStore eagerStore = new InMemoryPathNodeStore();
    eagerStore.nodes.putAll(copyNodeMap(initialNodes));
    PathMerkleTrie eager = new PathMerkleTrie(eagerStore, false);
    eager.restoreRoot(initialRoot);
    eager.put(keys[17], values[17]);
    eager.delete(keys[33]);
    byte[] expectedRoot = eager.rootHash();

    InMemoryPathNodeStore lazyStore = new InMemoryPathNodeStore();
    lazyStore.nodes.putAll(initialNodes);
    PathMerkleTrie lazy = new PathMerkleTrie(lazyStore);
    lazy.restoreRoot(initialRoot);
    lazy.put(keys[17], values[17]);
    lazy.delete(keys[33]);

    assertArrayEquals(expectedRoot, lazy.rootHash());
    assertNodeMapsEqual(eagerStore.nodes, lazyStore.nodes);
  }

  @Test
  public void prefixBatchMatchesSequentialMixedChangesFromLazyRoot() {
    int leafCount = 512;
    byte[][] keys = new byte[leafCount][];
    byte[][] values = new byte[leafCount][];
    InMemoryPathNodeStore sourceStore = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(sourceStore);
    for (int index = 0; index < leafCount; index++) {
      keys[index] = Hash.sha3(value("batch-key-" + index));
      values[index] = value("batch-value-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] initialRoot = source.rootHash();
    Map<String, byte[]> initialNodes = copyNodeMap(sourceStore.nodes);

    InMemoryPathNodeStore sequentialStore = new InMemoryPathNodeStore();
    sequentialStore.nodes.putAll(copyNodeMap(initialNodes));
    PathMerkleTrie sequential = new PathMerkleTrie(sequentialStore);
    sequential.restoreRoot(initialRoot);
    List<PathMerkleTrie.BatchMutation> changes = new ArrayList<>();
    for (int index = 0; index < 40; index++) {
      byte[] updated = value("batch-updated-" + index);
      sequential.put(keys[index], updated);
      changes.add(new PathMerkleTrie.BatchMutation(keys[index], updated));
    }
    for (int index = 40; index < 60; index++) {
      sequential.delete(keys[index]);
      changes.add(new PathMerkleTrie.BatchMutation(keys[index], null));
    }
    for (int index = 0; index < 40; index++) {
      byte[] insertedKey = Hash.sha3(value("batch-inserted-key-" + index));
      byte[] insertedValue = value("batch-inserted-value-" + index);
      sequential.put(insertedKey, insertedValue);
      changes.add(new PathMerkleTrie.BatchMutation(insertedKey, insertedValue));
    }
    byte[] expectedRoot = sequential.rootHash();

    InMemoryPathNodeStore batchStore = new InMemoryPathNodeStore();
    batchStore.nodes.putAll(copyNodeMap(initialNodes));
    PathMerkleTrie batch = new PathMerkleTrie(batchStore);
    batch.restoreRoot(initialRoot);
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      batch.applyBatch(changes, executor);
      assertArrayEquals(expectedRoot, batch.rootHash());
      assertNodeMapsEqual(sequentialStore.nodes, batchStore.nodes);
      assertTrue(batchStore.gets <= sequentialStore.gets);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void blockScopedEncodingEncodesOnlyTheFrozenNodeGraph() {
    int leafCount = 512;
    byte[][] keys = new byte[leafCount][];
    InMemoryPathNodeStore sourceStore = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(sourceStore);
    for (int index = 0; index < leafCount; index++) {
      keys[index] = Hash.sha3(value("arena-key-" + index));
      source.put(keys[index], value("arena-value-" + index));
    }
    byte[] initialRoot = source.rootHash();
    Map<String, byte[]> initialNodes = copyNodeMap(sourceStore.nodes);
    List<PathMerkleTrie.BatchMutation> changes = new ArrayList<>();
    for (int index = 0; index < 96; index++) {
      changes.add(new PathMerkleTrie.BatchMutation(keys[index],
          value("arena-updated-" + index)));
    }
    for (int index = 96; index < 128; index++) {
      changes.add(new PathMerkleTrie.BatchMutation(keys[index], null));
    }

    InMemoryPathNodeStore eagerStore = new InMemoryPathNodeStore();
    eagerStore.nodes.putAll(copyNodeMap(initialNodes));
    PathMerkleTrie eager = new PathMerkleTrie(eagerStore);
    eager.restoreRoot(initialRoot);
    ExecutorService eagerExecutor = Executors.newFixedThreadPool(4);
    long eagerEncodesBefore = PathMerkleTrie.nodeEncodeCountTotal();
    long eagerEncodes;
    try {
      eager.applyBatch(changes, eagerExecutor, false);
      eager.rootHash();
      eagerEncodes = PathMerkleTrie.nodeEncodeCountTotal() - eagerEncodesBefore;
    } finally {
      eagerExecutor.shutdownNow();
    }

    InMemoryPathNodeStore arenaStore = new InMemoryPathNodeStore();
    arenaStore.nodes.putAll(copyNodeMap(initialNodes));
    PathMerkleTrie arena = new PathMerkleTrie(arenaStore);
    arena.restoreRoot(initialRoot);
    ExecutorService arenaExecutor = Executors.newFixedThreadPool(4);
    long arenaCreatesBefore = PathMerkleTrie.nodeCreateCountTotal();
    long arenaEncodesBefore = PathMerkleTrie.nodeEncodeCountTotal();
    long arenaCreates;
    long arenaEncodes;
    try {
      arena.applyBatch(changes, arenaExecutor, true);
      assertArrayEquals(eager.rootHash(), arena.rootHash());
      arenaCreates = PathMerkleTrie.nodeCreateCountTotal() - arenaCreatesBefore;
      arenaEncodes = PathMerkleTrie.nodeEncodeCountTotal() - arenaEncodesBefore;
      long encodesAfterFreeze = PathMerkleTrie.nodeEncodeCountTotal();
      arena.rootHash();
      arena.snapshot();
      assertEquals(encodesAfterFreeze, PathMerkleTrie.nodeEncodeCountTotal());
    } finally {
      arenaExecutor.shutdownNow();
    }

    assertNodeMapsEqual(eagerStore.nodes, arenaStore.nodes);
    assertTrue(arenaCreates > arenaEncodes);
    assertTrue(arenaEncodes < eagerEncodes);
    assertTrue(arenaEncodes >= arena.getLastNodePuts());
  }

  @Test
  public void resolvedSubtreeSurvivesConsecutiveSnapshotSwitches() {
    byte[][] keys = new byte[16][];
    byte[][] values = new byte[16][];
    InMemoryPathNodeStore store = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(store);
    for (int index = 0; index < keys.length; index++) {
      keys[index] = filledKey(index << 4);
      values[index] = value("snapshot-value-long-enough-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] initialRoot = source.rootHash();

    PathMerkleTrie restored = new PathMerkleTrie(store);
    restored.restoreRoot(initialRoot);
    assertArrayEquals(values[3], restored.get(keys[3]));
    PathMerkleTrie.Snapshot parent = restored.snapshot();

    PathMerkleTrie firstBlock = PathMerkleTrie.fromSnapshot(store, parent);
    values[4] = value("snapshot-block-one-updated");
    firstBlock.put(keys[4], values[4]);
    firstBlock.rootHash();
    PathMerkleTrie.Snapshot firstChild = firstBlock.snapshot();

    int readsBeforeInheritedLookup = store.gets;
    PathMerkleTrie secondBlock = PathMerkleTrie.fromSnapshot(store, firstChild);
    assertArrayEquals(values[3], secondBlock.get(keys[3]));
    assertEquals(readsBeforeInheritedLookup, store.gets);

    values[5] = value("snapshot-block-two-updated");
    secondBlock.put(keys[5], values[5]);
    assertArrayEquals(referenceRoot(keys, values), secondBlock.rootHash());
  }

  @Test
  public void detachedSnapshotDoesNotDuplicateTheResolvedNodeIndex() {
    int leafCount = 512;
    byte[][] keys = new byte[leafCount][];
    byte[][] values = new byte[leafCount][];
    InMemoryPathNodeStore store = new InMemoryPathNodeStore();
    PathMerkleTrie source = new PathMerkleTrie(store);
    for (int index = 0; index < leafCount; index++) {
      keys[index] = Hash.sha3(value("detached-key-" + index));
      values[index] = value("detached-value-" + index);
      source.put(keys[index], values[index]);
    }
    byte[] root = source.rootHash();

    PathMerkleTrie restored = new PathMerkleTrie(store);
    restored.restoreRoot(root);
    for (int index = 0; index < leafCount; index++) {
      assertArrayEquals(values[index], restored.get(keys[index]));
    }
    PathMerkleTrie.Snapshot resolved = restored.snapshot();
    assertTrue(resolved.materializedIndexSize() > leafCount);

    PathMerkleTrie.Snapshot detached = resolved.detach();
    assertEquals(1, detached.depth());
    assertEquals(0, detached.materializedIndexSize());

    PathMerkleTrie next = PathMerkleTrie.fromSnapshot(store, detached);
    values[17] = value("detached-updated-value");
    next.put(keys[17], values[17]);
    assertArrayEquals(referenceRoot(keys, values), next.rootHash());
  }

  private static byte[] referenceRoot(byte[][] keys, byte[][] values) {
    TrieImpl reference = new TrieImpl();
    reference.setAsync(false);
    for (int i = 0; i < keys.length; i++) {
      reference.put(keys[i], values[i]);
    }
    return reference.getRootHash();
  }

  private static byte[] filledKey(int value) {
    byte[] key = new byte[PathMerkleTrie.SECURE_KEY_LENGTH];
    Arrays.fill(key, (byte) value);
    return key;
  }

  private static byte[] keyWithTail(int prefix, int tail) {
    byte[] key = filledKey(prefix);
    key[key.length - 1] = (byte) tail;
    return key;
  }

  private static byte[] indexedKey(int index) {
    byte[] key = new byte[PathMerkleTrie.SECURE_KEY_LENGTH];
    ByteBuffer.wrap(key, key.length - Integer.BYTES, Integer.BYTES).putInt(index);
    return key;
  }

  private static byte[] value(String value) {
    return PathStateCommitmentCodec.presentLeafValue(value.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertNodeMapsEqual(Map<String, byte[]> expected,
      Map<String, byte[]> actual) {
    assertEquals(expected.keySet(), actual.keySet());
    for (String path : expected.keySet()) {
      assertArrayEquals(expected.get(path), actual.get(path));
    }
  }

  private static Map<String, byte[]> copyNodeMap(Map<String, byte[]> source) {
    Map<String, byte[]> copy = new LinkedHashMap<>();
    for (Map.Entry<String, byte[]> entry : source.entrySet()) {
      copy.put(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
    }
    return copy;
  }

  private static final class InMemoryPathNodeStore implements PathNodeStore {

    private final Map<String, byte[]> nodes = new LinkedHashMap<>();
    private int gets;

    @Override
    public byte[] get(byte[] path) {
      gets++;
      byte[] value = nodes.get(Hex.toHexString(path));
      return value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public void put(byte[] path, byte[] encodedNode) {
      nodes.put(Hex.toHexString(path), Arrays.copyOf(encodedNode, encodedNode.length));
    }

    @Override
    public void delete(byte[] path) {
      nodes.remove(Hex.toHexString(path));
    }
  }
}
