package org.tron.core.db2.stateroot;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.arch.Arch;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.core.CommonCheckpointBaseline;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateRebuildCoordinator.EntryConsumer;
import org.tron.core.db2.stateroot.PathStateRebuildCoordinator.SnapshotIdentity;
import org.tron.core.db2.stateroot.PathStateRebuildCoordinator.SnapshotSource;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class PathStateNativeNodeStoreTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void nativeStoresOwnBytesAndPreserveSyncedMutationsAcrossReopen() throws Exception {
    for (Engine engine : availableEngines()) {
      Path directory = new File(temporaryFolder.getRoot(), "native-" + engine).toPath();
      byte[] path = new byte[]{1, 2, 3};
      byte[] node = new byte[]{4, 5, 6};
      try (PathStateNativeNodeStore store = PathStateNativeNodeStore.open(directory, engine)) {
        store.put(path, node);
        path[0] = 15;
        node[0] = 15;
        byte[] returned = store.get(new byte[]{1, 2, 3});
        returned[0] = 15;
        assertArrayEquals(new byte[]{4, 5, 6}, store.get(new byte[]{1, 2, 3}));
      }
      try (PathStateNativeNodeStore reopened = PathStateNativeNodeStore.open(directory, engine)) {
        assertArrayEquals(new byte[]{4, 5, 6}, reopened.get(new byte[]{1, 2, 3}));
        reopened.delete(new byte[]{1, 2, 3});
      }
      try (PathStateNativeNodeStore reopened = PathStateNativeNodeStore.open(directory, engine)) {
        assertNull(reopened.get(new byte[]{1, 2, 3}));
      }
    }
  }

  @Test
  public void nativeStoreRejectsInvalidEntriesAndUseAfterClose() throws Exception {
    Path directory = temporaryFolder.newFolder("native-invalid").toPath();
    PathStateNativeNodeStore store = PathStateNativeNodeStore.open(directory, Engine.ROCKSDB);
    assertThrows(IllegalArgumentException.class, () -> store.put(new byte[0], new byte[]{1}));
    assertThrows(IllegalArgumentException.class, () -> store.put(new byte[]{1}, new byte[0]));
    store.close();
    store.close();
    assertThrows(IllegalStateException.class, () -> store.get(new byte[0]));
  }

  @Test
  public void physicalStoresUseFixedSmallLargeAndGiantProfiles() {
    assertEquals("giant", PathStatePhysicalStoreSet.storageProfileNameFor("account"));
    assertEquals("giant", PathStatePhysicalStoreSet.storageProfileNameFor("account-asset"));
    assertEquals("giant", PathStatePhysicalStoreSet.storageProfileNameFor("storage-row"));
    assertEquals("large", PathStatePhysicalStoreSet.storageProfileNameFor("code"));
    assertEquals("large", PathStatePhysicalStoreSet.storageProfileNameFor("contract"));
    assertEquals("large", PathStatePhysicalStoreSet.storageProfileNameFor("delegation"));
    assertEquals("small", PathStatePhysicalStoreSet.storageProfileNameFor("proposal"));
  }

  @Test
  public void rocksProfileIsPersistedInNativeOptions() throws Exception {
    Path directory = temporaryFolder.newFolder("rocks-profile-options").toPath();
    try (PathStateNativeNodeStore store = PathStateNativeNodeStore.open(directory,
        Engine.ROCKSDB, "giant", NativeDbConfig.giant())) {
      assertEquals("giant", store.getStorageProfile());
      store.put(new byte[]{1}, new byte[]{2});
    }

    String nativeOptions = new String(Files.readAllBytes(latestOptionsFile(directory)),
        StandardCharsets.US_ASCII);
    assertTrue(nativeOptions.contains("write_buffer_size=67108864"));
    assertTrue(nativeOptions.contains("max_write_buffer_number=2"));
    assertTrue(nativeOptions.contains("compression=kSnappyCompression"));
    assertTrue(nativeOptions.contains("block_size=4096"));
    assertTrue(nativeOptions.contains("filter_policy=rocksdb.BuiltinBloomFilter"));
    assertTrue(nativeOptions.contains("checksum=kCRC32c"));
  }

  @Test
  public void transitionRecordingStoreCachesBaseReadsAndOwnsReturnedBytes() {
    AtomicInteger reads = new AtomicInteger();
    PathNodeStore base = new PathNodeStore() {
      @Override
      public byte[] get(byte[] path) {
        reads.incrementAndGet();
        return path[0] == 1 ? new byte[]{4, 5, 6} : null;
      }

      @Override
      public void put(byte[] path, byte[] encodedNode) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void delete(byte[] path) {
        throw new UnsupportedOperationException();
      }
    };
    PathStatePhysicalStoreSet.RecordingNodeStore store =
        new PathStatePhysicalStoreSet.RecordingNodeStore(base);

    byte[] first = store.get(new byte[]{1});
    first[0] = 9;
    assertArrayEquals(new byte[]{4, 5, 6}, store.get(new byte[]{1}));
    assertNull(store.get(new byte[]{2}));
    assertNull(store.get(new byte[]{2}));
    assertEquals(2, reads.get());
    assertEquals(2, store.getBaseReadMisses());
    assertEquals(2, store.getBaseReadHits());

    store.put(new byte[]{1}, new byte[]{7, 8});
    byte[] changed = store.get(new byte[]{1});
    changed[0] = 9;
    assertArrayEquals(new byte[]{7, 8}, store.get(new byte[]{1}));
    assertEquals(2, reads.get());
    assertEquals(2, store.getBaseReadMisses());
    assertEquals(2, store.getBaseReadHits());
  }

  @Test
  public void residentNodeStoreIsBoundedAndTracksCommittedValues() {
    java.util.Map<String, byte[]> durable = new java.util.HashMap<>();
    durable.put("1", new byte[]{4});
    AtomicInteger reads = new AtomicInteger();
    PathNodeStore base = new PathNodeStore() {
      @Override
      public byte[] get(byte[] path) {
        reads.incrementAndGet();
        byte[] value = durable.get(Integer.toString(path[0]));
        return value == null ? null : Arrays.copyOf(value, value.length);
      }

      @Override
      public void put(byte[] path, byte[] encodedNode) {
        durable.put(Integer.toString(path[0]), Arrays.copyOf(encodedNode, encodedNode.length));
      }

      @Override
      public void delete(byte[] path) {
        durable.remove(Integer.toString(path[0]));
      }
    };
    PathStatePhysicalStoreSet.ResidentNodeCache cache =
        new PathStatePhysicalStoreSet.ResidentNodeCache(150);
    PathStatePhysicalStoreSet.ResidentNodeStore store =
        new PathStatePhysicalStoreSet.ResidentNodeStore(base, cache, 1);

    assertArrayEquals(new byte[]{4}, store.get(new byte[]{1}));
    assertArrayEquals(new byte[]{4}, store.get(new byte[]{1}));
    assertNull(store.get(new byte[]{2}));
    assertEquals(2, reads.get());
    assertEquals(1, store.getHits());
    assertEquals(2, store.getNativeReads());

    store.put(new byte[]{1}, new byte[]{7});
    assertArrayEquals(new byte[]{7}, store.get(new byte[]{1}));
    store.delete(new byte[]{2});
    assertNull(store.get(new byte[]{2}));
    assertEquals(2, reads.get());

    assertNull(store.get(new byte[]{3}));
    assertTrue(cache.bytes() <= 150);
    assertTrue(cache.size() <= 2);
    assertArrayEquals(new byte[]{7}, store.get(new byte[]{1}));
    assertTrue(cache.bytes() <= 150);
    assertTrue(cache.evictions() > 0);
  }

  @Test
  public void residentNodeCacheSharesBudgetWithoutCrossStoreAliasing() {
    AtomicInteger firstReads = new AtomicInteger();
    AtomicInteger secondReads = new AtomicInteger();
    PathNodeStore firstBase = fixedNodeStore(new byte[]{1}, firstReads);
    PathNodeStore secondBase = fixedNodeStore(new byte[]{2}, secondReads);
    PathStatePhysicalStoreSet.ResidentNodeCache cache =
        new PathStatePhysicalStoreSet.ResidentNodeCache(300);
    PathStatePhysicalStoreSet.ResidentNodeStore first =
        new PathStatePhysicalStoreSet.ResidentNodeStore(firstBase, cache, 1);
    PathStatePhysicalStoreSet.ResidentNodeStore second =
        new PathStatePhysicalStoreSet.ResidentNodeStore(secondBase, cache, 2);

    byte[] samePath = new byte[]{7};
    assertArrayEquals(new byte[]{1}, first.get(samePath));
    assertArrayEquals(new byte[]{2}, second.get(samePath));
    assertArrayEquals(new byte[]{1}, first.get(samePath));
    assertArrayEquals(new byte[]{2}, second.get(samePath));
    assertEquals(1, firstReads.get());
    assertEquals(1, secondReads.get());
    assertTrue(cache.bytes() <= 300);

    first.clear();
    assertArrayEquals(new byte[]{2}, second.get(samePath));
    assertEquals(1, secondReads.get());
    assertArrayEquals(new byte[]{1}, first.get(samePath));
    assertEquals(2, firstReads.get());
  }

  private static PathNodeStore fixedNodeStore(byte[] fixedValue, AtomicInteger reads) {
    return new PathNodeStore() {
      @Override
      public byte[] get(byte[] path) {
        reads.incrementAndGet();
        return Arrays.copyOf(fixedValue, fixedValue.length);
      }

      @Override
      public void put(byte[] path, byte[] encodedNode) {
      }

      @Override
      public void delete(byte[] path) {
      }
    };
  }

  @Test
  public void streamsNativeScansWithoutCollectingTheResultSet() throws Exception {
    for (Engine engine : availableEngines()) {
      Path directory = new File(temporaryFolder.getRoot(), "stream-" + engine).toPath();
      List<PathStateNativeNodeStore.BatchMutation> mutations = new ArrayList<>();
      for (int index = 0; index < 64; index++) {
        mutations.add(PathStateNativeNodeStore.BatchMutation.put(
            new byte[]{1, (byte) index}, new byte[]{(byte) (index + 1)}));
      }
      try (PathStateNativeNodeStore store = PathStateNativeNodeStore.open(directory, engine)) {
        store.writeBatch(mutations);
        AtomicInteger prefixCount = new AtomicInteger();
        AtomicInteger allCount = new AtomicInteger();
        store.scanPrefix(new byte[]{1}, entry -> prefixCount.incrementAndGet());
        store.scanAll(entry -> allCount.incrementAndGet());
        assertEquals(64, prefixCount.get());
        assertEquals(64, allCount.get());
      }
    }
  }

  @Test
  public void baseStoreSetCreatesExact27PlusSuperAndPersistsRootNodes() throws Exception {
    PathStateStoreManifest manifest = manifest("base-set", Engine.ROCKSDB);
    byte[] rootHash;
    PathStateRootMetadata progress;
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
      PathStateRoot root = stores.createRoot();
      root.apply(Collections.singletonList(
          PathStateMutation.put("proposal", new byte[]{1}, new byte[]{2})));
      rootHash = root.rootHash();
      progress = PathStateRootMetadata.base(100, bytes(1), bytes(2), 300,
          P66Phase.P66_ON, manifest.getIdentityDigest(), rootHash, bytes(3));
      stores.commit(progress);
      assertArrayEquals(progress.encode(), stores.getProgress().encode());
      root.verifyNodeStores();
      assertThrows(IllegalStateException.class, stores::createRoot);
    }

    assertEquals(1, childDirectoryCount(manifest.getBaseDirectory()));
    try (PathStateNativeNodeStore nodes = PathStateNativeNodeStore.open(
        manifest.getBaseDirectory().resolve("nodes"), Engine.ROCKSDB)) {
      assertNotNull(nodes.get(namespaceRootKey(21)));
      assertNotNull(nodes.get(namespaceRootKey(0)));
      assertEquals(32, rootHash.length);
    }
    try (PathStateNodeStoreSet reopened = PathStateNodeStoreSet.openBase(manifest)) {
      assertArrayEquals(progress.encode(), reopened.getProgress().encode());
      PathStateRoot restored = reopened.createRoot();
      assertArrayEquals(rootHash, restored.rootHash());
      restored.verifyNodeStores();
    }
  }

  @Test
  public void missingDurableLeafFailsClosedDuringRootRestore() throws Exception {
    PathStateStoreManifest manifest = manifest("missing-leaf", Engine.ROCKSDB);
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
      PathStateRoot root = stores.createRoot();
      root.apply(Collections.singletonList(
          PathStateMutation.put("proposal", new byte[]{1}, new byte[]{2})));
      byte[] stateRoot = root.rootHash();
      stores.commit(PathStateRootMetadata.base(100, bytes(1), bytes(2), 300,
          P66Phase.P66_ON, manifest.getIdentityDigest(), stateRoot, bytes(3)));
    }
    try (PathStateNativeNodeStore nodes = PathStateNativeNodeStore.open(
        manifest.getBaseDirectory().resolve("nodes"), Engine.ROCKSDB)) {
      nodes.delete(durableLeafKey(21,
          PathStateCommitmentCodec.storeLeafKey(21, new byte[]{1})));
    }
    try (PathStateNodeStoreSet reopened = PathStateNodeStoreSet.openBase(manifest)) {
      assertThrows(IllegalStateException.class, reopened::createRoot);
    }
  }

  @Test
  public void restoredBaseCommitsLeafUpdatesAndDeletesAcrossSecondReopen() throws Exception {
    for (Engine engine : availableEngines()) {
      PathStateStoreManifest manifest = manifest("leaf-delta-" + engine, engine);
      byte[] firstRoot;
      try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
        PathStateRoot root = stores.createRoot();
        root.apply(Arrays.asList(
            PathStateMutation.put("proposal", new byte[]{1}, new byte[]{2}),
            PathStateMutation.put("account", new byte[]{3}, new byte[]{4})));
        firstRoot = root.rootHash();
        stores.commit(PathStateRootMetadata.base(100, bytes(1), bytes(2), 300,
            P66Phase.P66_ON, manifest.getIdentityDigest(), firstRoot, bytes(3)));
      }

      byte[] secondRoot;
      try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
        PathStateRoot root = stores.createRoot();
        assertArrayEquals(firstRoot, root.rootHash());
        root.apply(Arrays.asList(
            PathStateMutation.put("proposal", new byte[]{1}, new byte[]{5}),
            PathStateMutation.delete("account", new byte[]{3})));
        secondRoot = root.rootHash();
        stores.commit(PathStateRootMetadata.base(101, bytes(4), bytes(1), 303,
            P66Phase.P66_ON, manifest.getIdentityDigest(), secondRoot, bytes(6)));
      }

      try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
        PathStateRoot root = stores.createRoot();
        assertArrayEquals(secondRoot, root.rootHash());
        root.verifyNodeStores();
      }
    }
  }

  @Test
  public void rejectedProgressDoesNotFlushPendingNodes() throws Exception {
    PathStateStoreManifest manifest = manifest("rejected-progress", Engine.ROCKSDB);
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
      PathStateRoot root = stores.createRoot();
      root.apply(Collections.singletonList(
          PathStateMutation.put("proposal", new byte[]{1}, new byte[]{2})));
      root.rootHash();
      PathStateRootMetadata mismatch = PathStateRootMetadata.base(100, bytes(1), bytes(2), 300,
          P66Phase.P66_ON, manifest.getIdentityDigest(), bytes(9), bytes(3));
      assertThrows(IllegalArgumentException.class, () -> stores.commit(mismatch));
      assertNull(stores.getProgress());
    }

    try (PathStateNativeNodeStore nodes = PathStateNativeNodeStore.open(
        manifest.getBaseDirectory().resolve("nodes"), Engine.ROCKSDB)) {
      assertNull(nodes.get(namespaceRootKey(21)));
      assertNull(nodes.get(namespaceRootKey(0)));
    }
  }

  @Test
  public void levelAndRocksStoreSetsProduceTheSameCurrentRoot() throws Exception {
    org.junit.Assume.assumeFalse(Arch.isArm64());
    byte[] level = rootFor(manifest("set-level", Engine.LEVELDB));
    byte[] rocks = rootFor(manifest("set-rocks", Engine.ROCKSDB));
    assertArrayEquals(level, rocks);
  }

  @Test
  public void layerStoreSetIsBoundToMetadataAndCanonicalDirectory() throws Exception {
    PathStateStoreManifest manifest = manifest("layer-set", Engine.ROCKSDB);
    PathStateRootMetadata layer = PathStateRootMetadata.layer(101, bytes(1), bytes(2), 300,
        P66Phase.P66_ON, manifest.getIdentityDigest(), bytes(3), bytes(4), bytes(5));
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openLayer(manifest, layer)) {
      assertEquals(manifest.getLayerDirectory(101, bytes(1)).resolve("nodes"),
          stores.getDirectory());
    }

    PathStateRootMetadata foreign = PathStateRootMetadata.layer(101, bytes(1), bytes(2), 300,
        P66Phase.P66_ON, bytes(9), bytes(3), bytes(4), bytes(5));
    assertThrows(java.io.IOException.class,
        () -> PathStateNodeStoreSet.openLayer(manifest, foreign));
  }

  @Test
  public void immutableLayerMetadataSealsTheWritableNodeSet() throws Exception {
    PathStateStoreManifest manifest = manifest("sealed-layer", Engine.ROCKSDB);
    PathStateRootMetadata layer = PathStateRootMetadata.layer(101, bytes(1), bytes(2), 300,
        P66Phase.P66_ON, manifest.getIdentityDigest(), bytes(3), bytes(4), bytes(5));
    Path layerDirectory = manifest.getLayerDirectory(101, bytes(1));
    try (PathStateNodeStoreSet ignored = PathStateNodeStoreSet.openLayer(manifest, layer)) {
      assertNotNull(ignored);
    }
    PathStateMetadataFile.publishImmutable(
        layerDirectory.resolve(PathStateCurrentStore.METADATA_FILE), layer);

    assertThrows(java.io.IOException.class,
        () -> PathStateNodeStoreSet.openLayer(manifest, layer));
  }

  @Test
  public void physicalStoreSetCreatesExact27PlusSuperWithDisjointFNMKeyspaces() throws Exception {
    Path root = temporaryFolder.newFolder("physical-27-plus-super").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(4, new byte[]{1, 2, 3});
    byte[] sameSuffix = new byte[]{7, 8, 9};
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStatePhysicalStoreSet.PhysicalStore account = stores.participant("account");
      account.putFlat(secureKey, sameSuffix);
      account.nodeStore().put(secureKey, new byte[]{4, 5, 6});
      account.putMetadata(secureKey, new byte[]{1});
      assertArrayEquals(sameSuffix, account.getFlat(secureKey));
      assertArrayEquals(new byte[]{4, 5, 6}, account.nodeStore().get(secureKey));
      assertArrayEquals(new byte[]{1}, account.getMetadata(secureKey));

      stores.superStore().putFlat(secureKey, new byte[]{2});
      assertArrayEquals(new byte[]{2}, stores.superStore().getFlat(secureKey));
      assertNull(stores.superStore().nodeStore().get(secureKey));
      assertEquals(27, childDirectoryCount(root.resolve("stores")));
      AtomicInteger flatEntries = new AtomicInteger();
      account.scanFlat(ignored -> flatEntries.incrementAndGet());
      assertEquals(1, flatEntries.get());
      assertEquals(32, stores.getFormatDigest().length);

      PathStateRoot stateRoot = stores.createRoot();
      stateRoot.put("account", new byte[]{1, 2, 3}, new byte[]{4, 5, 6});
      assertEquals(32, stateRoot.rootHash().length);
      assertNotNull(account.nodeStore().get(new byte[0]));
      assertNotNull(stores.superStore().nodeStore().get(new byte[0]));
      assertThrows(IllegalStateException.class, stores::createRoot);
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(sameSuffix, reopened.participant("account").getFlat(secureKey));
      assertArrayEquals(new byte[]{4, 5, 6}, reopened.participant("account").nodeStore()
          .get(secureKey));
      AtomicInteger flatEntries = new AtomicInteger();
      reopened.participant("account").scanFlat(ignored -> flatEntries.incrementAndGet());
      assertEquals(1, flatEntries.get());
    }
  }

  @Test
  public void physicalStoreSetRejectsLegacySharedBaseNodes() throws Exception {
    Path root = temporaryFolder.newFolder("physical-legacy-rejection").toPath();
    Files.createDirectories(root.resolve("base").resolve("nodes"));
    assertThrows(java.io.IOException.class,
        () -> PathStatePhysicalStoreSet.open(root, new PathStateCanonicalizer().participantScope(),
            Engine.ROCKSDB));
  }

  @Test
  public void physicalStoreSetRejectsOldSharedManifestEvenWithoutNodeFiles() throws Exception {
    Path root = temporaryFolder.newFolder("physical-old-manifest-rejection").toPath();
    PathStateStoreManifest.createOrOpen(root, Engine.ROCKSDB);
    assertThrows(java.io.IOException.class,
        () -> PathStatePhysicalStoreSet.open(root, new PathStateCanonicalizer().participantScope(),
            Engine.ROCKSDB));
  }

  @Test
  public void physicalStoreSetRejectsAnExactNameScopeWithAChangedStableStoreId()
      throws Exception {
    Path root = temporaryFolder.newFolder("physical-store-id-rejection").toPath();
    List<PathStateParticipant> changed = new ArrayList<>();
    for (PathStateParticipant participant
        : new PathStateCanonicalizer().participantScope().getParticipants()) {
      changed.add("proposal".equals(participant.getDbName())
          ? new PathStateParticipant(99, participant.getDbName(),
              participant.getStoreFormatVersion()) : participant);
    }
    PathStateParticipantScope changedScope = new PathStateParticipantScope(changed);
    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalStoreSet.open(root, changedScope, Engine.ROCKSDB));
  }

  @Test
  public void physicalFlatSnapshotRestoresAndVerifiesTheRoot() throws Exception {
    Path root = temporaryFolder.newFolder("physical-flat-restore").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    byte[] expectedRoot;
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStateRoot stateRoot = stores.createRoot();
      stateRoot.put("account", new byte[]{1, 2}, new byte[]{3, 4});
      stateRoot.put("proposal", new byte[]{5, 6}, new byte[]{7, 8});
      expectedRoot = stateRoot.rootHash();
      stores.persistFlatSnapshot(stateRoot);
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, reopened.restoreRootFromFlat().rootHash());
    }
  }

  @Test
  public void physicalFlatRestoreFailsClosedForMissingOrCorruptLeaf() throws Exception {
    Path root = temporaryFolder.newFolder("physical-flat-corruption").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(4, new byte[]{1, 2});
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStateRoot stateRoot = stores.createRoot();
      stateRoot.put("account", new byte[]{1, 2}, new byte[]{3, 4});
      stateRoot.rootHash();
      stores.persistFlatSnapshot(stateRoot);
    }
    try (PathStatePhysicalStoreSet corrupted = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      corrupted.participant("account").putFlat(secureKey, new byte[]{1});
      assertThrows(IllegalStateException.class, corrupted::restoreRootFromFlat);
    }
    try (PathStatePhysicalStoreSet missing = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      missing.participant("account").deleteFlat(secureKey);
      assertThrows(IllegalStateException.class, missing::restoreRootFromFlat);
    }
  }

  @Test
  public void physicalFlatBuildStreamsIntoNAndReusesPerStoreCompletionOnRetry() throws Exception {
    Path root = temporaryFolder.newFolder("physical-flat-stream-build").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    byte[] expectedRoot;
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStateRoot stateRoot = stores.createRoot();
      stateRoot.put("account", new byte[]{1, 2}, new byte[]{3, 4});
      stateRoot.put("proposal", new byte[]{5, 6}, new byte[]{7, 8});
      expectedRoot = stateRoot.rootHash();
      stores.persistFlatSnapshot(stateRoot);
    }
    try (PathStatePhysicalStoreSet rebuilt = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, rebuilt.buildRootFromFlat().rootHash());
      assertNotNull(rebuilt.participant("account").getMetadata(
          "flat-complete".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
      assertNotNull(rebuilt.participant("proposal").getMetadata(
          "flat-complete".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }
    try (PathStatePhysicalStoreSet retried = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, retried.buildRootFromFlat().rootHash());
    }
  }

  @Test
  public void physicalFlatBuildClearsIncompleteNodesAndRetriesWithoutSource() throws Exception {
    Path root = temporaryFolder.newFolder("physical-flat-incomplete-retry").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    byte[] stalePath = new byte[]{15, 15, 15, 15};
    byte[] expectedRoot;
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStateRoot stateRoot = stores.createRoot();
      stateRoot.put("account", new byte[]{1, 2}, new byte[]{3, 4});
      stateRoot.put("proposal", new byte[]{5, 6}, new byte[]{7, 8});
      expectedRoot = stateRoot.rootHash();
      stores.persistFlatSnapshot(stateRoot);
    }

    AtomicInteger injectedFailures = new AtomicInteger();
    try (PathStatePhysicalStoreSet interrupted = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, () -> interrupted.buildRootFromFlat(
          (participant, storeRoot) -> {
            if ("proposal".equals(participant.getDbName())
                && injectedFailures.getAndIncrement() == 0) {
              throw new java.io.IOException("injected failure before FLAT_COMPLETE");
            }
          }));
      interrupted.participant("proposal").nodeStore().put(stalePath, new byte[]{99});
    }
    assertEquals(1, injectedFailures.get());

    try (PathStatePhysicalStoreSet retried = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertNotNull(retried.participant("proposal").nodeStore().get(stalePath));
      assertArrayEquals(expectedRoot, retried.buildRootFromFlat().rootHash());
      assertNull(retried.participant("proposal").nodeStore().get(stalePath));
      assertNotNull(retried.participant("proposal").getMetadata(
          "flat-complete".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, reopened.buildRootFromFlat().rootHash());
    }
  }

  @Test
  public void physicalGlobalPublicationRecoversEveryIntentCurrentCrashWindow() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    for (PathStatePhysicalStoreSet.PublicationStage stage
        : PathStatePhysicalStoreSet.PublicationStage.values()) {
      Path root = new File(temporaryFolder.getRoot(), "physical-publish-" + stage).toPath();
      byte[] expectedRoot = preparePhysicalTarget(root, scope);

      try (PathStatePhysicalStoreSet interrupted = PathStatePhysicalStoreSet.open(root, scope,
          Engine.ROCKSDB)) {
        assertThrows(java.io.IOException.class, () -> interrupted.publishCurrent(present -> {
          if (present == stage) {
            throw new java.io.IOException("injected publication failure at " + stage);
          }
        }));
      }

      try (PathStatePhysicalStoreSet recovered = PathStatePhysicalStoreSet.open(root, scope,
          Engine.ROCKSDB)) {
        PathStatePhysicalStoreSet.PublicationRecovery action = recovered.recoverPublication();
        assertEquals(stage == PathStatePhysicalStoreSet.PublicationStage.AFTER_RETIRE
                ? PathStatePhysicalStoreSet.PublicationRecovery.NONE
                : PathStatePhysicalStoreSet.PublicationRecovery.COMPLETED_INTENT,
            action);
        assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.NONE,
            recovered.recoverPublication());
      }
      assertFalse(Files.exists(root.resolve(PathStatePhysicalStoreSet.INTENT_FILE)));
      assertTrue(Files.isRegularFile(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      PathStatePhysicalGlobalIntent current = PathStatePhysicalGlobalIntent.decode(
          Files.readAllBytes(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      assertArrayEquals(expectedRoot, current.getSuperRoot());
      assertEquals(27, current.getParticipants().size());
    }
  }

  @Test
  public void physicalBlockFinalTransitionPreviewsPublishesAndRestarts() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-block-final").toPath();
    preparePublishedPhysicalTarget(root, scope);
    byte[] key = new byte[]{1, 2, 3};
    byte[] blockHash = bytes(31);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStateRootMetadata parent = stores.currentMetadata();
      PathStateBlockTransition transition = new PathStateBlockTransition(1, blockHash,
          parent.getBlockHash(), 3, P66Phase.P66_ON,
          Collections.singletonList(PathStateMutation.put("code", key, new byte[]{4, 5})));
      PathStateRootMetadata preview = stores.previewTransition(transition);
      assertEquals(0, stores.currentMetadata().getBlockNumber());
      assertEquals(0, stores.participant("code").getSyncedWriteBatchCalls());
      assertEquals(0, stores.participant("code").getUnsyncedWriteBatchCalls());
      PathStateRootMetadata committed = stores.applyAndPublish(transition);
      assertArrayEquals(preview.encode(), committed.encode());
      assertEquals(1, committed.getBlockNumber());
      assertEquals(0, stores.participant("code").getSyncedWriteBatchCalls());
      assertEquals(1, stores.participant("code").getUnsyncedWriteBatchCalls());
      assertEquals(0, stores.superStore().getSyncedWriteBatchCalls());
      assertEquals(1, stores.superStore().getUnsyncedWriteBatchCalls());
      assertArrayEquals(PathStateCommitmentCodec.presentLeafValue(new byte[]{4, 5}),
          stores.participant("code").getFlat(
              PathStateCommitmentCodec.storeLeafKey(scope.require("code").getStoreId(), key)));
    }

    try (PathStatePhysicalSnapshotHead head = PathStatePhysicalSnapshotHead.open(root,
        Engine.ROCKSDB)) {
      assertEquals(1, head.getHead().getBlockNumber());
      PathStateBlockTransition update = new PathStateBlockTransition(2, bytes(32), blockHash,
          6, P66Phase.P66_ON, Arrays.asList(
          PathStateMutation.put("code", key, new byte[]{6}),
          PathStateMutation.put("proposal", new byte[]{7}, new byte[]{8})));
      byte[] preview = head.preview(update);
      PathStateSnapshotDelta delta = head.prepareSnapshotDelta(
          BlockSnapshotMeta.forBlock(2, bytes(32), blockHash, 6), update);
      assertArrayEquals(preview, delta.getStateRoot());
      assertArrayEquals(delta.getStateRoot(), head.advance(update).getStateRoot());
      PathStateBlockTransition delete = new PathStateBlockTransition(3, bytes(33), bytes(32),
          9, P66Phase.P66_ON,
          Collections.singletonList(PathStateMutation.delete("code", key)));
      head.advance(delete);
      assertEquals(3, head.getHead().getBlockNumber());
    }

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(root, scope,
        Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.NONE,
          reopened.recoverPublication());
      assertEquals(3, reopened.currentMetadata().getBlockNumber());
      assertNull(reopened.participant("code").getFlat(
          PathStateCommitmentCodec.storeLeafKey(scope.require("code").getStoreId(), key)));
    }
  }

  @Test
  public void physicalSnapshotDeltaPreparesWithoutWritesAndReusesPlanForPublication()
      throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-snapshot-delta").toPath();
    preparePublishedPhysicalTarget(root, scope);
    byte[] key = new byte[]{1, 2, 3};
    byte[] blockHash = bytes(35);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStateBlockTransition transition = new PathStateBlockTransition(1, blockHash,
          new byte[32], 3, P66Phase.P66_ON,
          Collections.singletonList(PathStateMutation.put("code", key, new byte[]{4, 5})));
      PathStatePhysicalStoreSet.PreparedPhysicalTransition prepared =
          stores.prepareSnapshotDelta(BlockSnapshotMeta.forBlock(1, blockHash, new byte[32], 3),
              transition);
      PathStateSnapshotDelta delta = prepared.getSnapshotDelta();

      assertEquals(0, stores.currentMetadata().getBlockNumber());
      assertEquals(0, stores.participant("code").getUnsyncedWriteBatchCalls());
      assertEquals(0, stores.superStore().getUnsyncedWriteBatchCalls());
      assertEquals(1, delta.getStores().size());
      assertEquals("code", delta.getStores().get(0).getDbName());
      assertArrayEquals(PathStateCommitmentCodec.storeLeafKey(
              scope.require("code").getStoreId(), key),
          delta.getStores().get(0).getFlatMutations().get(0).getKey());
      assertTrue(Arrays.stream(PathStateSnapshotDelta.class.getDeclaredFields())
          .noneMatch(field -> field.getType() == PathStateRoot.Snapshot.class));

      PathStateRootMetadata committed = stores.applyAndPublish(prepared,
          PathStateLayerLimits.defaults());
      assertArrayEquals(delta.getStateRoot(), committed.getStateRoot());
      assertEquals(1, stores.participant("code").getUnsyncedWriteBatchCalls());
      assertEquals(1, stores.superStore().getUnsyncedWriteBatchCalls());

      PathStateBlockTransition next = new PathStateBlockTransition(2, bytes(36), blockHash,
          6, P66Phase.P66_ON,
          Collections.singletonList(PathStateMutation.put("code", key, new byte[]{6})));
      PathStatePhysicalStoreSet.PreparedPhysicalTransition nextPrepared =
          stores.prepareSnapshotDelta(BlockSnapshotMeta.forBlock(2, bytes(36), blockHash, 6),
              next);
      assertTrue(nextPrepared.reusedTrieSnapshot());
      assertEquals(1, stores.participant("code").getUnsyncedWriteBatchCalls());
      stores.applyAndPublish(nextPrepared, PathStateLayerLimits.defaults());
      assertEquals(2, stores.participant("code").getUnsyncedWriteBatchCalls());
    }
  }

  @Test
  public void volatileOverlayAdvancesAndRewindsWithoutJournalOrDurableWrites()
      throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-volatile-overlay").toPath();
    Path legacyRoot = temporaryFolder.newFolder("physical-volatile-overlay-legacy").toPath();
    preparePublishedPhysicalTarget(root, scope);
    preparePublishedPhysicalTarget(legacyRoot, scope);
    byte[] durableCurrent = Files.readAllBytes(
        root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE));
    byte[] firstHash = bytes(37);
    byte[] secondHash = bytes(38);

    try (PathStatePhysicalOverlayHead head = PathStatePhysicalOverlayHead.open(root,
        Engine.ROCKSDB, new PathStateLayerLimits(4, 1L << 20));
        PathStatePhysicalSnapshotHead legacy = PathStatePhysicalSnapshotHead.open(legacyRoot,
            Engine.ROCKSDB, new PathStateLayerLimits(4, 1L << 20))) {
      PathStateBlockTransition first = new PathStateBlockTransition(1, firstHash,
          new byte[32], 3, P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{2})));
      PathStateSnapshotDelta firstDelta = head.prepareSnapshotDelta(
          BlockSnapshotMeta.forBlock(1, firstHash, new byte[32], 3), first);
      PathStateRootMetadata firstOverlay = head.advance(first);
      assertArrayEquals(firstDelta.getStateRoot(), firstOverlay.getStateRoot());
      assertArrayEquals(legacy.advance(first).getStateRoot(), firstOverlay.getStateRoot());

      PathStateBlockTransition second = new PathStateBlockTransition(2, secondHash,
          firstHash, 6, P66Phase.P66_ON, Arrays.asList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{3}),
          PathStateMutation.put("proposal", new byte[]{4}, new byte[]{5})));
      head.prepareSnapshotDelta(BlockSnapshotMeta.forBlock(2, secondHash, firstHash, 6), second);
      PathStateRootMetadata secondOverlay = head.advance(second);
      assertEquals(2, secondOverlay.getBlockNumber());
      assertArrayEquals(legacy.advance(second).getStateRoot(), secondOverlay.getStateRoot());
      assertEquals(0, head.durableWriteBatchCalls());
      assertArrayEquals(durableCurrent,
          Files.readAllBytes(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      Path reverse = root.resolve("reverse");
      assertFalse(Files.exists(reverse));

      PathStateRootMetadata rewound = head.rewindTo(1, firstHash);
      assertEquals(1, rewound.getBlockNumber());
      assertArrayEquals(firstDelta.getStateRoot(), rewound.getStateRoot());
      assertEquals(0, head.durableWriteBatchCalls());
    }

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(root, scope,
        Engine.ROCKSDB)) {
      assertEquals(0, reopened.currentMetadata().getBlockNumber());
      assertNull(reopened.participant("code").getFlat(
          PathStateCommitmentCodec.storeLeafKey(scope.require("code").getStoreId(),
              new byte[]{1})));
    }
  }

  @Test
  public void volatileOverlayBoundsSnapshotParentDepthAfterHistoryTrim() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-volatile-retention").toPath();
    preparePublishedPhysicalTarget(root, scope);

    int maxHistory = 4;
    try (PathStatePhysicalOverlayHead head = PathStatePhysicalOverlayHead.open(root,
        Engine.ROCKSDB, new PathStateLayerLimits(maxHistory, 1L << 20))) {
      byte[] parentHash = head.getHead().getBlockHash();
      for (int number = 1; number <= 20; number++) {
        byte[] blockHash = bytes(140 + number);
        PathStateBlockTransition transition = new PathStateBlockTransition(number, blockHash,
            parentHash, number * 3L, P66Phase.P66_ON, Collections.singletonList(
            PathStateMutation.put("code", new byte[]{1}, new byte[]{(byte) number})));
        head.prepareSnapshotDelta(
            BlockSnapshotMeta.forBlock(number, blockHash, parentHash, number * 3L), transition);
        head.advance(transition);
        parentHash = blockHash;
      }

      PathStatePhysicalOverlayHead.RetentionCensus census = head.retentionCensus();
      assertEquals(maxHistory, census.getSuffixBlocks());
      assertTrue("snapshot parent depth must stay bounded: " + census.getMaxSnapshotDepth(),
          census.getMaxSnapshotDepth() <= maxHistory * 2);

      PathStateRootMetadata rewound = head.rewindTo(18, bytes(158));
      assertEquals(18, rewound.getBlockNumber());
      PathStateBlockTransition sibling = new PathStateBlockTransition(19, bytes(180), bytes(158),
          60, P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("proposal", new byte[]{2}, new byte[]{9})));
      head.prepareSnapshotDelta(BlockSnapshotMeta.forBlock(19, bytes(180), bytes(158), 60),
          sibling);
      assertEquals(19, head.advance(sibling).getBlockNumber());
      assertTrue(head.retentionCensus().getMaxSnapshotDepth() <= maxHistory * 2);
    }
  }

  @Test
  public void commonOverlayReplacesInMemoryTrieAfterStartupRedo() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-common-redo-overlay").toPath();
    preparePublishedPhysicalTarget(root, scope);
    byte[] formatIdentity = bytes(73);
    byte[] blockHash = bytes(74);

    try (PathStatePhysicalOverlayHead head = PathStatePhysicalOverlayHead.open(root,
        Engine.ROCKSDB, new PathStateLayerLimits(4, 1L << 20))) {
      PathStateRootMetadata baselineHead = head.getHead();
      CommonCheckpointBaseline baseline = new CommonCheckpointBaseline(formatIdentity,
          BlockSnapshotMeta.forBlock(baselineHead.getBlockNumber(), baselineHead.getBlockHash(),
              baselineHead.getParentHash(), baselineHead.getTimestamp()),
          baselineHead.getStateRoot());
      head.admitFreshCommonBaseline(baseline);

      PathStateBlockTransition transition = new PathStateBlockTransition(1, blockHash,
          baselineHead.getBlockHash(), 3, P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{2})));
      BlockSnapshotMeta block = BlockSnapshotMeta.forBlock(1, blockHash,
          baselineHead.getBlockHash(), 3);
      PathStateSnapshotDelta delta = head.prepareSnapshotDelta(block, transition);
      PathStateFlushTarget target = PathStateFlushTarget.coalesce(
          Collections.singletonList(delta));
      CommonCheckpointPayload payload = CommonCheckpointPayload.create(formatIdentity, target,
          Collections.singletonList(new BlockReverseDiff(block, Collections.emptyList())),
          Collections.emptyList());
      CommonCheckpointTarget checkpointTarget = CommonCheckpointTarget.from(payload);
      PathStateCheckpointMaterializer materializer = head.checkpointMaterializer(formatIdentity,
          baseline);
      materializer.materialize(payload, checkpointTarget);
      materializer.publish(checkpointTarget);

      head.synchronizePublishedCheckpoint(formatIdentity, block, P66Phase.P66_ON);
      assertEquals(1, head.getHead().getBlockNumber());
      assertArrayEquals(blockHash, head.getHead().getBlockHash());
      assertArrayEquals(delta.getStateRoot(), head.getHead().getStateRoot());
    }
  }

  @Test
  public void commonCheckpointRebasePreservesSuffixAndCutsSnapshotParents() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-common-memory-rebase").toPath();
    preparePublishedPhysicalTarget(root, scope);
    byte[] formatIdentity = bytes(110);

    try (PathStatePhysicalOverlayHead head = PathStatePhysicalOverlayHead.open(root,
        Engine.ROCKSDB, new PathStateLayerLimits(8, 1L << 20))) {
      PathStateRootMetadata initial = head.getHead();
      CommonCheckpointBaseline baseline = commonBaseline(formatIdentity, initial);
      head.admitFreshCommonBaseline(baseline);
      PathStateCheckpointMaterializer materializer = head.checkpointMaterializer(formatIdentity,
          baseline);
      List<PathStateSnapshotDelta> deltas = new ArrayList<>();
      for (int number = 1; number <= 3; number++) {
        byte[] blockHash = bytes(110 + number);
        byte[] parentHash = number == 1 ? initial.getBlockHash() : bytes(109 + number);
        PathStateBlockTransition transition = new PathStateBlockTransition(number, blockHash,
            parentHash, number * 3L, P66Phase.P66_ON, Collections.singletonList(
            PathStateMutation.put("code", new byte[]{1}, new byte[]{(byte) number})));
        BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(number, blockHash, parentHash,
            number * 3L);
        deltas.add(head.prepareSnapshotDelta(meta, transition));
        head.advance(transition);
      }
      PathStateRootMetadata liveHead = head.getHead();
      assertEquals(4, head.retentionCensus().getMaxSnapshotDepth());

      CommonCheckpointPayload payload = commonPayload(formatIdentity,
          deltas.subList(0, 2));
      CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
      materializer.materialize(payload, target);
      materializer.publish(target);
      head.prepareCommonCheckpointRebase(target).apply();

      PathStatePhysicalOverlayHead.RetentionCensus census = head.retentionCensus();
      assertEquals(3, census.getHeadBlockNumber());
      assertEquals(1, census.getSuffixBlocks());
      assertEquals(28, census.getTrieCount());
      assertEquals(2, census.getMaxSnapshotDepth());
      assertArrayEquals(liveHead.encode(), head.getHead().encode());

      PathStateRootMetadata rewound = head.rewindTo(2, bytes(112));
      assertArrayEquals(target.getStateRoot(), rewound.getStateRoot());
      PathStateBlockTransition sibling = new PathStateBlockTransition(3, bytes(119), bytes(112),
          10, P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("proposal", new byte[]{2}, new byte[]{9})));
      head.prepareSnapshotDelta(BlockSnapshotMeta.forBlock(3, bytes(119), bytes(112), 10),
          sibling);
      assertEquals(3, head.advance(sibling).getBlockNumber());
    }
  }

  @Test
  public void repeatedCommonCheckpointRebaseKeepsParentDepthConstant() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-common-memory-rebase-long").toPath();
    preparePublishedPhysicalTarget(root, scope);
    byte[] formatIdentity = bytes(120);

    try (PathStatePhysicalOverlayHead head = PathStatePhysicalOverlayHead.open(root,
        Engine.ROCKSDB, new PathStateLayerLimits(8, 1L << 20))) {
      PathStateRootMetadata initial = head.getHead();
      CommonCheckpointBaseline baseline = commonBaseline(formatIdentity, initial);
      head.admitFreshCommonBaseline(baseline);
      PathStateCheckpointMaterializer materializer = head.checkpointMaterializer(formatIdentity,
          baseline);
      byte[] parentHash = initial.getBlockHash();
      for (int number = 1; number <= 100; number++) {
        byte[] blockHash = bytes(120 + number);
        PathStateBlockTransition transition = new PathStateBlockTransition(number, blockHash,
            parentHash, number * 3L, P66Phase.P66_ON, Collections.singletonList(
            PathStateMutation.put("code", new byte[]{1}, new byte[]{(byte) number})));
        BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(number, blockHash, parentHash,
            number * 3L);
        PathStateSnapshotDelta delta = head.prepareSnapshotDelta(meta, transition);
        head.advance(transition);
        CommonCheckpointPayload payload = commonPayload(formatIdentity,
            Collections.singletonList(delta));
        CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
        materializer.materialize(payload, target);
        materializer.publish(target);
        head.prepareCommonCheckpointRebase(target).apply();
        PathStatePhysicalOverlayHead.RetentionCensus census = head.retentionCensus();
        assertEquals(0, census.getSuffixBlocks());
        assertEquals(1, census.getMaxSnapshotDepth());
        parentHash = blockHash;
      }
      assertEquals(100, head.getHead().getBlockNumber());
      assertEquals(100, head.retentionCensus().getHeadBlockNumber());
    }
  }

  @Test
  public void asyncPrepareQueuesTransitionAndCompletesOffCallerThread() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-async-overlay").toPath();
    preparePublishedPhysicalTarget(root, scope);
    byte[] durableCurrent = Files.readAllBytes(
        root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE));
    byte[] blockHash = bytes(39);
    PathStateBlockTransition transition = new PathStateBlockTransition(1, blockHash,
        new byte[32], 3, P66Phase.P66_ON, Collections.singletonList(
        PathStateMutation.put("code", new byte[]{1}, new byte[]{2})));

    PathStatePhysicalOverlayHead overlay = PathStatePhysicalOverlayHead.open(root,
        Engine.ROCKSDB, new PathStateLayerLimits(4, 1L << 20));
    try (PathStateAsyncPrepareHead async = new PathStateAsyncPrepareHead(overlay, 2)) {
      assertNull(async.prepareSnapshotDelta(
          BlockSnapshotMeta.forBlock(1, blockHash, new byte[32], 3), transition));
      assertEquals(0, async.advance(transition).getBlockNumber());
      PathStateRootMetadata completed = async.flushBaseThrough(1, blockHash);
      assertEquals(1, completed.getBlockNumber());
      assertArrayEquals(blockHash, async.getHead().getBlockHash());
      assertEquals(0, overlay.durableWriteBatchCalls());
      assertArrayEquals(durableCurrent,
          Files.readAllBytes(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      assertFalse(Files.exists(root.resolve("reverse")));
    }
  }

  @Test
  public void physicalBlockFinalCrashAfterSuperCompletesIntentOnRestart() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-block-final-super-crash").toPath();
    preparePublishedPhysicalTarget(root, scope);
    PathStateBlockTransition transition = new PathStateBlockTransition(1, bytes(41),
        new byte[32], 3, P66Phase.P66_ON,
        Collections.singletonList(PathStateMutation.put("code", new byte[]{1}, new byte[]{2})));
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, () -> stores.applyAndPublish(transition, stage -> {
        if (stage == PathStatePhysicalStoreSet.TransitionStage.AFTER_SUPER_BATCH) {
          throw new java.io.IOException("injected failure after super batch");
        }
      }));
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(root, scope,
        Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.COMPLETED_INTENT,
          reopened.recoverPublication());
      assertEquals(1, reopened.currentMetadata().getBlockNumber());
    }
  }

  @Test
  public void physicalBlockFinalCrashAfterParticipantBatchFailsClosed() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-block-final-participant-crash").toPath();
    preparePublishedPhysicalTarget(root, scope);
    PathStateBlockTransition transition = new PathStateBlockTransition(1, bytes(51),
        new byte[32], 3, P66Phase.P66_ON,
        Collections.singletonList(PathStateMutation.put("code", new byte[]{1}, new byte[]{2})));
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, () -> stores.applyAndPublish(transition, stage -> {
        if (stage == PathStatePhysicalStoreSet.TransitionStage.AFTER_PARTICIPANT_BATCH) {
          throw new java.io.IOException("injected failure after participant batch");
        }
      }));
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(root, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, reopened::recoverPublication);
    }
    assertTrue(Files.isRegularFile(root.resolve(PathStatePhysicalStoreSet.INTENT_FILE)));
    assertTrue(Files.isRegularFile(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
  }

  @Test
  public void physicalShortReorgRestoresAncestorAndAdvancesSiblingAcrossRestart()
      throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-short-reorg").toPath();
    byte[] baseRoot = preparePublishedPhysicalTarget(root, scope);
    byte[] key = new byte[]{1, 2};
    PathStateLayerLimits limits = new PathStateLayerLimits(4, 1L << 20);

    try (PathStatePhysicalSnapshotHead head = PathStatePhysicalSnapshotHead.open(root,
        Engine.ROCKSDB, limits)) {
      head.advance(new PathStateBlockTransition(1, bytes(61), new byte[32], 3,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", key, new byte[]{3}))));
      head.advance(new PathStateBlockTransition(2, bytes(62), bytes(61), 6,
          P66Phase.P66_ON, Arrays.asList(
          PathStateMutation.put("code", key, new byte[]{4}),
          PathStateMutation.put("proposal", new byte[]{5}, new byte[]{6}))));
      PathStateRootMetadata rewound = head.rewindTo(0, new byte[32]);
      assertEquals(0, rewound.getBlockNumber());
      assertArrayEquals(baseRoot, rewound.getStateRoot());
      PathStateRootMetadata sibling = head.advance(new PathStateBlockTransition(1, bytes(63),
          new byte[32], 9, P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", key, new byte[]{9}))));
      assertEquals(1, sibling.getBlockNumber());
      assertArrayEquals(bytes(63), sibling.getBlockHash());
    }

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.openExisting(root, scope,
        Engine.ROCKSDB)) {
      assertEquals(1, reopened.currentMetadata().getBlockNumber());
      assertArrayEquals(PathStateCommitmentCodec.presentLeafValue(new byte[]{9}),
          reopened.participant("code").getFlat(
              PathStateCommitmentCodec.storeLeafKey(scope.require("code").getStoreId(), key)));
      assertNull(reopened.participant("proposal").getFlat(
          PathStateCommitmentCodec.storeLeafKey(scope.require("proposal").getStoreId(),
              new byte[]{5})));
    }
  }

  @Test
  public void physicalShortReorgIsBoundedAndFailsBeforeAuthorityMoves() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-short-reorg-bounded").toPath();
    preparePublishedPhysicalTarget(root, scope);
    PathStateLayerLimits limits = new PathStateLayerLimits(2, 1L << 20);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(71), new byte[32], 3,
          P66Phase.P66_ON, Collections.emptyList()), limits, stage -> { });
      stores.applyAndPublish(new PathStateBlockTransition(2, bytes(72), bytes(71), 6,
          P66Phase.P66_ON, Collections.emptyList()), limits, stage -> { });
      stores.applyAndPublish(new PathStateBlockTransition(3, bytes(73), bytes(72), 9,
          P66Phase.P66_ON, Collections.emptyList()), limits, stage -> { });
      byte[] current = stores.currentMetadata().encode();
      assertThrows(java.io.IOException.class,
          () -> stores.rewindTo(0, new byte[32], limits));
      assertArrayEquals(current, stores.currentMetadata().encode());
      assertEquals(1, stores.rewindTo(1, bytes(71), limits).getBlockNumber());
    }
  }

  @Test
  public void physicalShortReorgCrashWindowsCompleteOrFailClosed() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path completed = temporaryFolder.newFolder("physical-rewind-super-crash").toPath();
    preparePublishedPhysicalTarget(completed, scope);
    PathStateLayerLimits limits = new PathStateLayerLimits(4, 1L << 20);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(completed, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(81), new byte[32], 3,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{2}))));
      assertThrows(java.io.IOException.class, () -> stores.rewindTo(0, new byte[32], limits,
          stage -> {
            if (stage == PathStatePhysicalStoreSet.RewindStage.AFTER_SUPER_BATCH) {
              throw new java.io.IOException("injected rewind failure after super");
            }
          }));
    }
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.openExisting(completed,
        scope, Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.COMPLETED_INTENT,
          stores.recoverPublication());
      assertEquals(0, stores.currentMetadata().getBlockNumber());
    }

    Path partial = temporaryFolder.newFolder("physical-rewind-participant-crash").toPath();
    preparePublishedPhysicalTarget(partial, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(partial, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(82), new byte[32], 3,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{2}))));
      assertThrows(java.io.IOException.class, () -> stores.rewindTo(0, new byte[32], limits,
          stage -> {
            if (stage == PathStatePhysicalStoreSet.RewindStage.AFTER_PARTICIPANT_BATCH) {
              throw new java.io.IOException("injected rewind failure after participant");
            }
          }));
    }
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.openExisting(partial, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, stores::recoverPublication);
    }
  }

  @Test
  public void physicalStartupRejectsCorruptReverseJournal() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-reverse-corrupt").toPath();
    preparePublishedPhysicalTarget(root, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(91), new byte[32], 3,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{2}))));
    }
    Path reverse;
    try (Stream<Path> files = Files.list(root.resolve("reverse"))) {
      reverse = files.findFirst().get();
    }
    byte[] corrupt = Files.readAllBytes(reverse);
    corrupt[corrupt.length - 1] ^= 1;
    Files.write(reverse, corrupt);
    assertThrows(java.io.IOException.class,
        () -> PathStatePhysicalSnapshotHead.open(root, Engine.ROCKSDB));
  }

  @Test
  public void physicalSteadyTransitionDoesNotRereadIndexedReverseJournal() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-reverse-runtime-index").toPath();
    preparePublishedPhysicalTarget(root, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(92), new byte[32], 3,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", new byte[]{1}, new byte[]{2}))));
      Path reverse;
      try (Stream<Path> files = Files.list(root.resolve("reverse"))) {
        reverse = files.findFirst().get();
      }
      byte[] corrupt = Files.readAllBytes(reverse);
      corrupt[corrupt.length - 1] ^= 1;
      Files.write(reverse, corrupt);

      assertEquals(2, stores.applyAndPublish(new PathStateBlockTransition(2, bytes(93), bytes(92),
          6, P66Phase.P66_ON, Collections.emptyList())).getBlockNumber());
    }
    assertThrows(java.io.IOException.class,
        () -> PathStatePhysicalSnapshotHead.open(root, Engine.ROCKSDB));
  }

  @Test
  public void physicalOracleWindowLoadsOneExactReadOnlyAncestorChain() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-oracle-window").toPath();
    preparePublishedPhysicalTarget(root, scope);
    PathStateLayerLimits limits = new PathStateLayerLimits(4, 1L << 20);
    byte[] key = new byte[]{1, 2};

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(94), new byte[32], 3,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.put("code", key, new byte[]{3}))), limits);
      stores.applyAndPublish(new PathStateBlockTransition(2, bytes(95), bytes(94), 6,
          P66Phase.P66_ON, Arrays.asList(
          PathStateMutation.put("code", key, new byte[]{4}),
          PathStateMutation.put("proposal", new byte[]{5}, new byte[]{6}))), limits);
      stores.applyAndPublish(new PathStateBlockTransition(3, bytes(96), bytes(95), 9,
          P66Phase.P66_ON, Collections.singletonList(
          PathStateMutation.delete("code", key))), limits);

      byte[] currentBefore = Files.readAllBytes(root.resolve(
          PathStatePhysicalStoreSet.CURRENT_FILE));
      PathStatePhysicalOracleWindow window = stores.loadOracleWindow(3, limits);
      assertEquals(3, window.getBlockCount());
      assertEquals(3, window.getCurrentMetadata().getBlockNumber());
      assertEquals(0, window.getOldestMetadata().getBlockNumber());
      assertArrayEquals(currentBefore, Files.readAllBytes(root.resolve(
          PathStatePhysicalStoreSet.CURRENT_FILE)));
      assertNull(stores.participant("code").getFlat(
          PathStateCommitmentCodec.storeLeafKey(scope.require("code").getStoreId(), key)));

      List<PathStatePhysicalReverseJournal> journals = window.journals();
      assertEquals(3, journals.size());
      for (int index = 0; index < journals.size(); index++) {
        PathStatePhysicalGlobalIntent child = PathStatePhysicalGlobalIntent.decode(
            journals.get(index).getChildTarget());
        PathStatePhysicalGlobalIntent parent = PathStatePhysicalGlobalIntent.decode(
            journals.get(index).getParentTarget());
        assertEquals(3 - index, child.getMetadata().getBlockNumber());
        assertEquals(2 - index, parent.getMetadata().getBlockNumber());
      }

    }

    Path scratch = new File(temporaryFolder.getRoot(), "physical-oracle-scratch").toPath();
    PathStatePhysicalOracle.Result result = PathStatePhysicalOracleTool.run(new String[]{
        "--root", root.toString(), "--scratch", scratch.toString(), "--blocks", "3",
        "--rows-per-flush", "2", "--engine", "ROCKSDB"});
    assertEquals(3, result.getBlockCount());
    assertEquals(3, result.getRowCount());
    assertEquals(3, result.getCurrent().getBlockNumber());
    assertEquals(0, result.getOldest().getBlockNumber());
    assertFalse(Files.exists(scratch));
  }

  @Test
  public void physicalOracleToolRejectsUnknownDuplicateAndInvalidOptions() {
    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalOracleTool.run(new String[]{"--unknown", "value"}));
    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalOracleTool.run(new String[]{
            "--root", "one", "--root", "two", "--scratch", "scratch", "--blocks", "1"}));
    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalOracleTool.run(new String[]{
            "--root", "root", "--scratch", "scratch", "--blocks", "0"}));
  }

  @Test
  public void physicalOracleDetectsFlatValueDriftAndPreservesFailureScratch() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-oracle-flat-drift").toPath();
    preparePublishedPhysicalTarget(root, scope);
    PathStateLayerLimits limits = new PathStateLayerLimits(2, 1L << 20);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(99), new byte[32], 3,
          P66Phase.P66_ON, Collections.emptyList()), limits);
      PathStatePhysicalOracleWindow window = stores.loadOracleWindow(1, limits);
      byte[] accountKey = PathStateCommitmentCodec.storeLeafKey(
          scope.require("account").getStoreId(), new byte[]{1, 2});
      stores.participant("account").putFlat(accountKey,
          PathStateCommitmentCodec.presentLeafValue(new byte[]{9, 9}));

      Path scratch = new File(temporaryFolder.getRoot(),
          "physical-oracle-failure-scratch").toPath();
      java.io.IOException failure = assertThrows(java.io.IOException.class,
          () -> PathStatePhysicalOracle.verify(stores, window, scratch, 2));
      assertTrue(failure.getMessage().contains("physical oracle root differs"));
      assertTrue(Files.isDirectory(scratch));
    }
  }

  @Test
  public void physicalOracleWindowRejectsPartialUnsettledOrOverLimitInput() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-oracle-window-invalid").toPath();
    preparePublishedPhysicalTarget(root, scope);
    PathStateLayerLimits limits = new PathStateLayerLimits(4, 1L << 20);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      stores.applyAndPublish(new PathStateBlockTransition(1, bytes(97), new byte[32], 3,
          P66Phase.P66_ON, Collections.emptyList()), limits);
      stores.applyAndPublish(new PathStateBlockTransition(2, bytes(98), bytes(97), 6,
          P66Phase.P66_ON, Collections.emptyList()), limits);

      assertThrows(java.io.IOException.class, () -> stores.loadOracleWindow(3, limits));
      assertThrows(java.io.IOException.class,
          () -> stores.loadOracleWindow(2, new PathStateLayerLimits(1, 1L << 20)));
      assertThrows(IllegalArgumentException.class,
          () -> stores.loadOracleWindow(0, limits));

      Files.write(root.resolve(PathStatePhysicalStoreSet.INTENT_FILE),
          Files.readAllBytes(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      assertThrows(java.io.IOException.class, () -> stores.loadOracleWindow(1, limits));
    }
  }

  @Test
  public void parallelParticipantWritesStartTogetherAndWaitForEveryCompletion() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch started = new CountDownLatch(2);
      CountDownLatch release = new CountDownLatch(1);
      AtomicBoolean bothStarted = new AtomicBoolean();
      List<Runnable> concurrentWrites = Arrays.asList(
          () -> awaitTestLatch(started, release),
          () -> awaitTestLatch(started, release));
      Thread releaser = new Thread(() -> {
        try {
          bothStarted.set(started.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException failure) {
          Thread.currentThread().interrupt();
        } finally {
          release.countDown();
        }
      });
      releaser.start();
      PathStatePhysicalStoreSet.awaitParallelWrites(executor, concurrentWrites);
      releaser.join();
      assertTrue(bothStarted.get());
      assertEquals(0, started.getCount());

      AtomicBoolean secondCompleted = new AtomicBoolean();
      assertThrows(java.io.IOException.class,
          () -> PathStatePhysicalStoreSet.awaitParallelWrites(executor, Arrays.asList(
              () -> {
                throw new IllegalStateException("injected participant failure");
              },
              () -> secondCompleted.set(true))));
      assertTrue(secondCompleted.get());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void physicalGlobalPublicationAcceptsOnlyOldCurrentOrExactIntentTarget()
      throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path uncommitted = temporaryFolder.newFolder("physical-publish-before-intent").toPath();
    preparePhysicalTarget(uncommitted, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(uncommitted, scope,
        Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.NONE,
          stores.recoverPublication());
    }
    assertFalse(Files.exists(uncommitted.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));

    Path tampered = temporaryFolder.newFolder("physical-publish-tampered-target").toPath();
    preparePhysicalTarget(tampered, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(tampered, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, () -> stores.publishCurrent(stage -> {
        if (stage == PathStatePhysicalStoreSet.PublicationStage.AFTER_INTENT) {
          throw new java.io.IOException("injected failure after INTENT");
        }
      }));
    }
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(tampered, scope,
        Engine.ROCKSDB)) {
      stores.participant("proposal").putMetadata(
          "store-generation".getBytes(java.nio.charset.StandardCharsets.US_ASCII), bytes(77));
      assertThrows(java.io.IOException.class, stores::recoverPublication);
    }
    assertFalse(Files.exists(tampered.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
    assertTrue(Files.isRegularFile(tampered.resolve(PathStatePhysicalStoreSet.INTENT_FILE)));

    Path oldCurrent = temporaryFolder.newFolder("physical-publish-old-current").toPath();
    byte[] expectedRoot = preparePhysicalTarget(oldCurrent, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(oldCurrent, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, stores.publishCurrent());
    }
    PathStateMetadataFile.publishImmutableBytes(
        oldCurrent.resolve(PathStatePhysicalStoreSet.INTENT_FILE), new byte[]{1});
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(oldCurrent, scope,
        Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.RETAINED_CURRENT,
          stores.recoverPublication());
    }
    assertFalse(Files.exists(oldCurrent.resolve(PathStatePhysicalStoreSet.INTENT_FILE)));
    assertArrayEquals(expectedRoot, PathStatePhysicalGlobalIntent.decode(Files.readAllBytes(
        oldCurrent.resolve(PathStatePhysicalStoreSet.CURRENT_FILE))).getSuperRoot());
  }

  @Test
  public void physicalGlobalRecordRejectsTruncationChecksumAndCorruptCurrent() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-global-record-corruption").toPath();
    preparePublishedPhysicalTarget(root, scope);
    Path currentPath = root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE);
    byte[] encoded = Files.readAllBytes(currentPath);

    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalGlobalIntent.decode(Arrays.copyOf(encoded, encoded.length - 1)));
    byte[] corruptBody = Arrays.copyOf(encoded, encoded.length);
    corruptBody[20] ^= 1;
    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalGlobalIntent.decode(corruptBody));
    byte[] corruptChecksum = Arrays.copyOf(encoded, encoded.length);
    corruptChecksum[corruptChecksum.length - 1] ^= 1;
    assertThrows(IllegalArgumentException.class,
        () -> PathStatePhysicalGlobalIntent.decode(corruptChecksum));

    Files.write(currentPath, corruptChecksum);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, stores::recoverPublication);
    }
  }

  @Test
  public void physicalPublicationRejectsMissingCompletionMetadataAndRootNodes()
      throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();

    Path missingRoot = temporaryFolder.newFolder("physical-missing-store-root").toPath();
    preparePublishedPhysicalTarget(missingRoot, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(missingRoot, scope,
        Engine.ROCKSDB)) {
      stores.participant("proposal").deleteMetadata(metadata("flat-complete"));
    }
    assertPublicationRejected(missingRoot, scope);

    Path missingDigest = temporaryFolder.newFolder("physical-missing-flat-digest").toPath();
    preparePublishedPhysicalTarget(missingDigest, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(missingDigest, scope,
        Engine.ROCKSDB)) {
      stores.participant("proposal").deleteMetadata(metadata("flat-digest"));
    }
    assertPublicationRejected(missingDigest, scope);

    Path corruptGeneration = temporaryFolder.newFolder(
        "physical-corrupt-store-generation").toPath();
    preparePublishedPhysicalTarget(corruptGeneration, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(corruptGeneration,
        scope, Engine.ROCKSDB)) {
      stores.participant("proposal").putMetadata(metadata("store-generation"), bytes(88));
    }
    assertPublicationRejected(corruptGeneration, scope);

    Path missingSuperGeneration = temporaryFolder.newFolder(
        "physical-missing-super-generation").toPath();
    preparePublishedPhysicalTarget(missingSuperGeneration, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(
        missingSuperGeneration, scope, Engine.ROCKSDB)) {
      stores.superStore().deleteMetadata(metadata("super-generation"));
    }
    assertPublicationRejected(missingSuperGeneration, scope);

    Path missingNode = temporaryFolder.newFolder("physical-missing-root-node").toPath();
    preparePublishedPhysicalTarget(missingNode, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(missingNode, scope,
        Engine.ROCKSDB)) {
      stores.participant("proposal").nodeStore().delete(new byte[0]);
    }
    assertPublicationRejected(missingNode, scope);
  }

  @Test
  public void physicalDeleteRecomputesSecureKeyCommitsChangedPathsAndPublishesCurrent()
      throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path root = temporaryFolder.newFolder("physical-delete-publish").toPath();
    byte[] originalRoot = preparePublishedPhysicalTarget(root, scope);
    byte[] expectedRoot = rootWithOnlyAccount(scope);
    byte[] proposalKey = new byte[]{5, 6};
    byte[] secureKey = PathStateCommitmentCodec.storeLeafKey(21, proposalKey);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      PathStatePhysicalStoreSet.PhysicalDeleteResult result = stores.deleteAndPublish(
          "proposal", proposalKey, stage -> { });
      assertFalse(Arrays.equals(originalRoot, result.getStateRoot()));
      assertArrayEquals(expectedRoot, result.getStateRoot());
      assertTrue(result.getParticipantNodeDeletes() > 0);
      assertTrue(result.getSuperNodePuts() > 0);
      assertNull(stores.participant("proposal").getFlat(secureKey));
    }

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(root, scope,
        Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.NONE,
          reopened.recoverPublication());
      PathStatePhysicalGlobalIntent current = PathStatePhysicalGlobalIntent.decode(
          Files.readAllBytes(root.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      assertArrayEquals(expectedRoot, current.getSuperRoot());
      PathStateRoot restored = reopened.createRoot();
      restored.restoreStoredRoots(current.getSuperRoot());
      restored.verifyNodeStores();
      assertArrayEquals(expectedRoot, restored.rootHash());
    }
  }

  @Test
  public void physicalDeleteFailsClosedBetweenParticipantSuperAndCurrent() throws Exception {
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    Path participantFailure = temporaryFolder.newFolder(
        "physical-delete-participant-failure").toPath();
    preparePublishedPhysicalTarget(participantFailure, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(participantFailure,
        scope, Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, () -> stores.deleteAndPublish("proposal",
          new byte[]{5, 6}, stage -> {
            if (stage == PathStatePhysicalStoreSet.DeleteStage.AFTER_PARTICIPANT_BATCH) {
              throw new java.io.IOException("injected failure after participant batch");
            }
          }));
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(participantFailure,
        scope, Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, reopened::recoverPublication);
      assertThrows(java.io.IOException.class, reopened::publishCurrent);
    }

    Path superFailure = temporaryFolder.newFolder("physical-delete-super-failure").toPath();
    preparePublishedPhysicalTarget(superFailure, scope);
    byte[] expectedRoot = rootWithOnlyAccount(scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(superFailure, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, () -> stores.deleteAndPublish("proposal",
          new byte[]{5, 6}, stage -> {
            if (stage == PathStatePhysicalStoreSet.DeleteStage.AFTER_SUPER_BATCH) {
              throw new java.io.IOException("injected failure after super batch");
            }
          }));
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(superFailure, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, reopened::recoverPublication);
      assertArrayEquals(expectedRoot, reopened.publishCurrent());
    }
    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(superFailure, scope,
        Engine.ROCKSDB)) {
      assertEquals(PathStatePhysicalStoreSet.PublicationRecovery.NONE,
          reopened.recoverPublication());
      PathStatePhysicalGlobalIntent current = PathStatePhysicalGlobalIntent.decode(
          Files.readAllBytes(superFailure.resolve(PathStatePhysicalStoreSet.CURRENT_FILE)));
      assertArrayEquals(expectedRoot, current.getSuperRoot());
      assertThrows(java.io.IOException.class,
          () -> reopened.deleteAndPublish("proposal", new byte[]{5, 6}));
    }
  }

  @Test
  public void physicalBootstrapBatchesFlatAndNodeWrites() throws Exception {
    Path directory = temporaryFolder.newFolder("physical-batched-bootstrap").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    List<PhysicalRow> rows = new ArrayList<>();
    for (int index = 0; index < PathStatePhysicalStoreSet.BOOTSTRAP_WRITE_BATCH_ENTRIES * 2 + 1;
        index++) {
      rows.add(new PhysicalRow(new byte[]{
          (byte) (index >>> 24), (byte) (index >>> 16), (byte) (index >>> 8), (byte) index},
          new byte[]{(byte) (index + 1)}));
    }
    ResumablePhysicalSource source = new ResumablePhysicalSource(
        "proposal", rows, bytes(43), Integer.MAX_VALUE);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      PathStatePhysicalStoreSet.PhysicalStore proposal = stores.participant("proposal");
      long ingestCalls = proposal.getWriteBatchCalls();
      long ingestMutations = proposal.getWriteBatchMutations();
      stores.ingestFlat("proposal", source, Long.MAX_VALUE, Long.MAX_VALUE);
      assertEquals(3, proposal.getWriteBatchCalls() - ingestCalls);
      assertEquals(rows.size() + 2, proposal.getWriteBatchMutations() - ingestMutations);

      long buildCalls = proposal.getWriteBatchCalls();
      long buildMutations = proposal.getWriteBatchMutations();
      stores.buildRootFromFlat();
      long nodeCalls = proposal.getWriteBatchCalls() - buildCalls;
      long nodeMutations = proposal.getWriteBatchMutations() - buildMutations;
      assertTrue(nodeMutations > rows.size());
      assertTrue(nodeCalls < 16);
      assertTrue(nodeCalls * 1000 < nodeMutations);
    }
  }

  @Test
  public void physicalFlatIngestFlushesBeforeByteLimit() throws Exception {
    Path directory = temporaryFolder.newFolder("physical-byte-batched-bootstrap").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    int valueBytes = 3 * 1024 * 1024;
    List<PhysicalRow> rows = Arrays.asList(
        new PhysicalRow(new byte[]{1}, new byte[valueBytes]),
        new PhysicalRow(new byte[]{2}, new byte[valueBytes]),
        new PhysicalRow(new byte[]{3}, new byte[valueBytes]));
    ResumablePhysicalSource source = new ResumablePhysicalSource(
        "proposal", rows, bytes(45), Integer.MAX_VALUE);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      PathStatePhysicalStoreSet.PhysicalStore proposal = stores.participant("proposal");
      long calls = proposal.getWriteBatchCalls();
      long mutations = proposal.getWriteBatchMutations();
      stores.ingestFlat("proposal", source, Long.MAX_VALUE, Long.MAX_VALUE);
      assertEquals(2, proposal.getWriteBatchCalls() - calls);
      assertEquals(rows.size() + 2, proposal.getWriteBatchMutations() - mutations);
    }
  }

  @Test
  public void physicalBootstrapRunsOneLargeAndOneSmallIngestQueue() throws Exception {
    Path directory = temporaryFolder.newFolder("physical-tiered-bootstrap").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    ConcurrentTierPhysicalSource source = new ConcurrentTierPhysicalSource();

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      stores.ingestAndBuild(source);
    }

    assertEquals(2, source.getMaxActive());
    assertTrue(source.sawThread("path-state-physical-bootstrap-large"));
    assertTrue(source.sawThread("path-state-physical-bootstrap-small"));
  }

  @Test
  public void physicalIngestResumesAfterFailureThenBuildsAndReopensTheSameRoot()
      throws Exception {
    Path directory = temporaryFolder.newFolder("physical-ingest-e2e").toPath();
    Path referenceDirectory = temporaryFolder.newFolder("physical-ingest-reference").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    List<PhysicalRow> rows = Arrays.asList(
        new PhysicalRow(new byte[]{1}, new byte[]{11}),
        new PhysicalRow(new byte[]{1, 0}, new byte[]{12}),
        new PhysicalRow(new byte[]{2}, new byte[]{22}));
    byte[] sourceIdentity = bytes(42);
    ResumablePhysicalSource interrupted = new ResumablePhysicalSource(
        "proposal", rows, sourceIdentity, 2);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class,
          () -> stores.ingestAndBuild(interrupted, 1, Long.MAX_VALUE));
      PathStatePhysicalIngestCheckpoint checkpoint = stores.ingestCheckpoint("proposal");
      assertEquals(2, checkpoint.getRows());
      assertArrayEquals(new byte[]{1, 0}, checkpoint.getCursor());
    }

    ResumablePhysicalSource resumed = new ResumablePhysicalSource(
        "proposal", rows, sourceIdentity, Integer.MAX_VALUE);
    byte[] rebuiltRoot;
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      rebuiltRoot = stores.ingestAndBuild(resumed, 1, Long.MAX_VALUE).rootHash();
      assertEquals(1, resumed.getScanCount("proposal"));
      assertEquals(0, resumed.getScanCount("abi"));
      assertArrayEquals(new byte[]{1, 0}, resumed.getLastCursor("proposal"));
      PathStatePhysicalIngestCheckpoint checkpoint = stores.ingestCheckpoint("proposal");
      assertEquals(3, checkpoint.getRows());
      assertArrayEquals(new byte[]{2}, checkpoint.getCursor());
    }

    byte[] referenceRoot;
    try (PathStatePhysicalStoreSet reference = PathStatePhysicalStoreSet.open(
        referenceDirectory, scope, Engine.ROCKSDB)) {
      PathStateRoot root = reference.createRoot();
      for (PhysicalRow row : rows) {
        root.put("proposal", row.key, row.value);
      }
      referenceRoot = root.rootHash();
    }
    assertArrayEquals(referenceRoot, rebuiltRoot);

    try (PathStatePhysicalStoreSet reopened = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(referenceRoot, reopened.buildRootFromFlat().rootHash());
    }
  }

  @Test
  public void physicalIngestReplaysUncheckpointedBatchAfterFailure() throws Exception {
    Path directory = temporaryFolder.newFolder("physical-uncheckpointed-replay").toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    List<PhysicalRow> rows = Arrays.asList(
        new PhysicalRow(new byte[]{1}, new byte[]{11}),
        new PhysicalRow(new byte[]{2}, new byte[]{12}),
        new PhysicalRow(new byte[]{3}, new byte[]{13}));
    byte[] sourceIdentity = bytes(46);
    ResumablePhysicalSource interrupted = new ResumablePhysicalSource(
        "proposal", rows, sourceIdentity, 2);

    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class,
          () -> stores.ingestFlat("proposal", interrupted, Long.MAX_VALUE, Long.MAX_VALUE));
      assertNull(stores.ingestCheckpoint("proposal"));
      assertEquals(0, stores.participant("proposal").getWriteBatchCalls());
    }

    ResumablePhysicalSource resumed = new ResumablePhysicalSource(
        "proposal", rows, sourceIdentity, Integer.MAX_VALUE);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      stores.ingestFlat("proposal", resumed, Long.MAX_VALUE, Long.MAX_VALUE);
      assertNull(resumed.getLastCursor("proposal"));
      PathStatePhysicalIngestCheckpoint checkpoint = stores.ingestCheckpoint("proposal");
      assertEquals(3, checkpoint.getRows());
      assertArrayEquals(new byte[]{3}, checkpoint.getCursor());
    }
  }

  private byte[] rootFor(PathStateStoreManifest manifest) throws Exception {
    byte[] stateRoot;
    PathStateRootMetadata progress;
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
      PathStateRoot root = stores.createRoot();
      root.apply(Arrays.asList(
          PathStateMutation.put("proposal", new byte[]{1}, new byte[]{2}),
          PathStateMutation.put("account", new byte[]{3}, new byte[]{4})));
      stateRoot = root.rootHash();
      progress = PathStateRootMetadata.base(100, bytes(1), bytes(2), 300,
          P66Phase.P66_ON, manifest.getIdentityDigest(), stateRoot, bytes(3));
      stores.commit(progress);
    }
    try (PathStateNodeStoreSet reopened = PathStateNodeStoreSet.openBase(manifest)) {
      assertArrayEquals(progress.encode(), reopened.getProgress().encode());
      assertArrayEquals(stateRoot, reopened.createRoot().rootHash());
    }
    return stateRoot;
  }

  private byte[] preparePhysicalTarget(Path directory, PathStateParticipantScope scope)
      throws Exception {
    byte[] expectedRoot;
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      PathStateRoot stateRoot = stores.createRoot();
      stateRoot.put("account", new byte[]{1, 2}, new byte[]{3, 4});
      stateRoot.put("proposal", new byte[]{5, 6}, new byte[]{7, 8});
      expectedRoot = stateRoot.rootHash();
      stores.persistFlatSnapshot(stateRoot);
    }
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, stores.buildRootFromFlat().rootHash());
    }
    return expectedRoot;
  }

  private byte[] rootWithOnlyAccount(PathStateParticipantScope scope) throws Exception {
    Path directory = temporaryFolder.newFolder("physical-delete-reference").toPath();
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      PathStateRoot root = stores.createRoot();
      root.put("account", new byte[]{1, 2}, new byte[]{3, 4});
      return root.rootHash();
    }
  }

  private byte[] preparePublishedPhysicalTarget(Path directory,
      PathStateParticipantScope scope) throws Exception {
    byte[] expectedRoot = preparePhysicalTarget(directory, scope);
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      assertArrayEquals(expectedRoot, stores.publishCurrent());
    }
    return expectedRoot;
  }

  private static CommonCheckpointBaseline commonBaseline(byte[] formatIdentity,
      PathStateRootMetadata head) {
    return new CommonCheckpointBaseline(formatIdentity,
        BlockSnapshotMeta.forBlock(head.getBlockNumber(), head.getBlockHash(),
            head.getParentHash(), head.getTimestamp()), head.getStateRoot());
  }

  private static CommonCheckpointPayload commonPayload(byte[] formatIdentity,
      List<PathStateSnapshotDelta> deltas) {
    PathStateFlushTarget target = PathStateFlushTarget.coalesce(deltas);
    List<BlockReverseDiff> archive = new ArrayList<>();
    for (PathStateSnapshotDelta delta : deltas) {
      archive.add(new BlockReverseDiff(delta.getMeta(), Collections.emptyList()));
    }
    return CommonCheckpointPayload.create(formatIdentity, target, archive,
        Collections.emptyList());
  }

  private void assertPublicationRejected(Path directory, PathStateParticipantScope scope)
      throws Exception {
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(directory, scope,
        Engine.ROCKSDB)) {
      assertThrows(java.io.IOException.class, stores::recoverPublication);
    }
  }

  private static byte[] metadata(String name) {
    return name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  }

  private PathStateStoreManifest manifest(String name, Engine engine) throws Exception {
    return PathStateStoreManifest.createOrOpen(temporaryFolder.newFolder(name).toPath(), engine);
  }

  private static List<Engine> availableEngines() {
    return Arch.isArm64() ? Collections.singletonList(Engine.ROCKSDB)
        : Arrays.asList(Engine.LEVELDB, Engine.ROCKSDB);
  }

  private static long childDirectoryCount(Path directory) throws Exception {
    try (Stream<Path> paths = Files.list(directory)) {
      return paths.filter(Files::isDirectory).count();
    }
  }

  private static byte[] namespaceRootKey(int storeId) {
    return java.nio.ByteBuffer.allocate(Integer.BYTES).putInt(storeId).array();
  }

  private static byte[] durableLeafKey(int storeId, byte[] secureKey) {
    return java.nio.ByteBuffer.allocate(Integer.BYTES * 2 + secureKey.length)
        .putInt(-2)
        .putInt(storeId)
        .put(secureKey)
        .array();
  }

  private static byte[] bytes(int seed) {
    byte[] value = new byte[32];
    for (int index = 0; index < value.length; index++) {
      value[index] = (byte) (seed + index);
    }
    return value;
  }

  private static final class PhysicalRow {

    private final byte[] key;
    private final byte[] value;

    private PhysicalRow(byte[] key, byte[] value) {
      this.key = key;
      this.value = value;
    }
  }

  private static final class ResumablePhysicalSource implements SnapshotSource {

    private final String dbName;
    private final List<PhysicalRow> rows;
    private final byte[] identity;
    private final int failAfter;
    private final java.util.Map<String, Integer> scanCounts = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, byte[]> lastCursors = new java.util.LinkedHashMap<>();

    private ResumablePhysicalSource(String dbName, List<PhysicalRow> rows, byte[] identity,
        int failAfter) {
      this.dbName = dbName;
      this.rows = rows;
      this.identity = identity;
      this.failAfter = failAfter;
    }

    @Override
    public SnapshotIdentity identity() {
      return new SnapshotIdentity(100, bytes(1), bytes(2), 300, P66Phase.P66_ON);
    }

    @Override
    public Collection<String> databases() {
      List<String> names = new ArrayList<>();
      for (PathStateParticipantDescriptor.StoreIdentity store
          : PathStateParticipantDescriptor.current().getStores()) {
        names.add(store.getDbName());
      }
      return names;
    }

    @Override
    public byte[] sourceIdentityDigest() {
      return Arrays.copyOf(identity, identity.length);
    }

    @Override
    public void scan(String name, EntryConsumer consumer) throws java.io.IOException {
      scanAfter(name, null, consumer);
    }

    @Override
    public synchronized void scanAfter(String name, byte[] cursor, EntryConsumer consumer)
        throws java.io.IOException {
      scanCounts.put(name, scanCounts.getOrDefault(name, 0) + 1);
      lastCursors.put(name, cursor == null ? null : Arrays.copyOf(cursor, cursor.length));
      if (!dbName.equals(name)) {
        return;
      }
      int emitted = 0;
      for (PhysicalRow row : rows) {
        if (cursor != null && compareUnsigned(row.key, cursor) <= 0) {
          continue;
        }
        consumer.accept(row.key, row.value);
        emitted++;
        if (emitted >= failAfter) {
          throw new java.io.IOException("injected physical ingest interruption");
        }
      }
    }

    @Override
    public void verifyIdentity(SnapshotIdentity expected) {
    }

    private synchronized int getScanCount(String name) {
      return scanCounts.getOrDefault(name, 0);
    }

    private synchronized byte[] getLastCursor(String name) {
      byte[] cursor = lastCursors.get(name);
      return cursor == null ? null : Arrays.copyOf(cursor, cursor.length);
    }
  }

  private static final class ConcurrentTierPhysicalSource implements SnapshotSource {

    private final java.util.concurrent.CountDownLatch started =
        new java.util.concurrent.CountDownLatch(2);
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger maxActive = new AtomicInteger();
    private final java.util.Set<String> threads = java.util.Collections.synchronizedSet(
        new java.util.HashSet<>());

    @Override
    public SnapshotIdentity identity() {
      return new SnapshotIdentity(100, bytes(1), bytes(2), 300, P66Phase.P66_ON);
    }

    @Override
    public Collection<String> databases() {
      List<String> names = new ArrayList<>();
      for (PathStateParticipantDescriptor.StoreIdentity store
          : PathStateParticipantDescriptor.current().getStores()) {
        names.add(store.getDbName());
      }
      return names;
    }

    @Override
    public byte[] sourceIdentityDigest() {
      return bytes(44);
    }

    @Override
    public void scan(String name, EntryConsumer consumer) throws java.io.IOException {
      scanAfter(name, null, consumer);
    }

    @Override
    public void scanAfter(String name, byte[] cursor, EntryConsumer consumer)
        throws java.io.IOException {
      if (!"account".equals(name) && !"proposal".equals(name)) {
        return;
      }
      int current = active.incrementAndGet();
      maxActive.accumulateAndGet(current, Math::max);
      threads.add(Thread.currentThread().getName());
      started.countDown();
      try {
        if (!started.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
          throw new java.io.IOException("physical tier queues did not overlap");
        }
        consumer.accept(new byte[]{1}, new byte[]{(byte) ("account".equals(name) ? 1 : 2)});
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new java.io.IOException("physical tier queue test interrupted", interrupted);
      } finally {
        active.decrementAndGet();
      }
    }

    @Override
    public void verifyIdentity(SnapshotIdentity expected) {
    }

    private int getMaxActive() {
      return maxActive.get();
    }

    private boolean sawThread(String name) {
      return threads.contains(name);
    }
  }

  private static int compareUnsigned(byte[] left, byte[] right) {
    for (int index = 0; index < Math.min(left.length, right.length); index++) {
      int compared = Integer.compare(left[index] & 0xff, right[index] & 0xff);
      if (compared != 0) {
        return compared;
      }
    }
    return Integer.compare(left.length, right.length);
  }

  private static Path latestOptionsFile(Path directory) throws java.io.IOException {
    try (Stream<Path> files = Files.list(directory)) {
      return files.filter(path -> path.getFileName().toString().startsWith("OPTIONS-"))
          .max(java.util.Comparator.comparing(path -> path.getFileName().toString()))
          .orElseThrow(() -> new java.io.IOException("RocksDB OPTIONS file is missing"));
    }
  }

  private static void awaitTestLatch(CountDownLatch started, CountDownLatch release) {
    started.countDown();
    try {
      if (!release.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("timed out waiting to release participant write");
      }
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("participant write test interrupted", failure);
    }
  }
}
