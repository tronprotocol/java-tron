package org.tron.core.db2.stateroot;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.args.Storage;
import org.tron.core.config.args.StorageConfig.StateArchiveHotStoreConfig;
import org.tron.core.db.Manager;
import org.tron.core.db2.ISession;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.LatestStateGenerationAdapter.SnapshotCapableStore;
import org.tron.core.db2.archive.LatestStateGenerationAdapter.StoreSnapshot;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.CommonCheckpointBaselineFile;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.DynamicPropertiesStore;

public class PathStateManagerStartupIntegrationTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void disabledAndMissingStartupDoNotCreatePathStateDirectory() throws Exception {
    Path output = temporaryFolder.newFolder("startup-gates").toPath();
    Manager disabled = new Manager();
    withConfig(output, false, () -> invoke(disabled, "initPathStateRoot"));
    assertNull(disabled.getPathStateSnapshotHead());
    assertFalse(Files.exists(output.resolve("path-state-root")));

    Manager missing = new Manager();
    assertThrows(IllegalStateException.class,
        () -> withConfig(output, true, () -> invoke(missing, "initPathStateRoot")));
    assertNull(missing.getPathStateSnapshotHead());
    assertFalse(Files.exists(output.resolve("path-state-root")));
  }

  @Test
  public void readyCurrentAttachesExactCanonicalHeadAndCloses() throws Exception {
    Path output = temporaryFolder.newFolder("startup-ready").toPath();
    Path root = output.resolve("path-state-root");
    PathStateRootMetadata base = publishEmptyPhysicalCurrent(root, 100, 1, 2);

    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(100L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(base.getBlockHash()));
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    setField(manager, "revokingStore", new SnapshotManager(""));

    withConfig(output, true, () -> invoke(manager, "initPathStateRoot"));
    assertNotNull(manager.getPathStateSnapshotHead());
    assertNotNull(manager.getPathStateRuntime());
    assertArrayEquals(base.encode(), manager.getPathStateSnapshotHead().getHead().encode());
    assertEquals(PathStateRuntimeAttachment.State.READY,
        manager.getPathStateRuntime().status().getState());
    assertEquals(100L, manager.getPathStateRuntime().status().getReadyBlockNumber());
    invoke(manager, "closePathStateRoot");
    assertNull(manager.getPathStateSnapshotHead());
    assertNull(manager.getPathStateRuntime());

    withConfig(output, true, () -> invoke(manager, "initPathStateRoot"));
    assertArrayEquals(base.encode(), manager.getPathStateSnapshotHead().getHead().encode());
    assertEquals(PathStateRuntimeAttachment.State.READY,
        manager.getPathStateRuntime().status().getState());
    invoke(manager, "closePathStateRoot");

    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(101L);
    assertThrows(IllegalStateException.class,
        () -> withConfig(output, true, () -> invoke(manager, "initPathStateRoot")));
    assertNull(manager.getPathStateSnapshotHead());
  }

  @Test
  public void readyManagerPublishesOneBlockFinalAndRestartsAtTheNewHead() throws Exception {
    Path output = temporaryFolder.newFolder("startup-one-block").toPath();
    Path root = output.resolve("path-state-root");
    PathStateRootMetadata base = publishEmptyPhysicalCurrent(root, 100, 1, 2);

    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(100L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(base.getBlockHash()));
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    SnapshotManager[] snapshotHolder = new SnapshotManager[1];

    byte[] childHash = bytes(9);
    withConfig(output, true, () -> {
      SnapshotManager snapshots = new SnapshotManager("");
      snapshots.getDbs().add(propertiesStoreWithP66Enabled());
      snapshots.enable();
      snapshotHolder[0] = snapshots;
      setField(manager, "revokingStore", snapshots);
      invoke(manager, "initPathStateRoot");
    });
    SnapshotManager snapshots = snapshotHolder[0];
    try (ISession session = snapshots.buildSession()) {
      byte[] preview = snapshots.previewPathStateRoot(BlockSnapshotMeta.forBlock(
          101, childHash, base.getBlockHash(), 303));
      assertNotNull(preview);
      assertArrayEquals(base.getStateRoot(), preview);
      session.commit(BlockSnapshotMeta.forBlock(101, childHash, base.getBlockHash(), 303));
    }
    assertEquals(101, manager.getPathStateSnapshotHead().getHead().getBlockNumber());
    assertEquals(PathStateRuntimeAttachment.State.READY,
        manager.getPathStateRuntime().status().getState());
    invoke(manager, "closePathStateRoot");

    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(101L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(childHash));
    withConfig(output, true, () -> invoke(manager, "initPathStateRoot"));
    assertEquals(101, manager.getPathStateSnapshotHead().getHead().getBlockNumber());
    assertArrayEquals(childHash, manager.getPathStateSnapshotHead().getHead().getBlockHash());
    invoke(manager, "closePathStateRoot");
  }

  @Test
  public void shortReorgBeyondPhysicalJournalFailsClosed() throws Exception {
    Path output = temporaryFolder.newFolder("short-reorg").toPath();
    Path root = output.resolve("path-state-root");
    PathStateRootMetadata base = publishEmptyPhysicalCurrent(root, 100, 1, 2);

    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(100L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(base.getBlockHash()));
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    setField(manager, "revokingStore", new SnapshotManager(""));

    withConfig(output, true, () -> invoke(manager, "initPathStateRoot"));
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(99L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(bytes(99)));
    invoke(manager, "rewindPathStateRootAfterPop");
    assertNotNull(manager.getPathStateRuntime().getFailure());
    assertEquals(PathStateRuntimeAttachment.FailureStage.REORG,
        manager.getPathStateRuntime().status().getFailureStage());
    assertEquals(99L, manager.getPathStateRuntime().status().getObservedBlockNumber());
    assertEquals(1L, manager.getPathStateRuntime().status().getRootLag());
    assertArrayEquals(base.encode(), manager.getPathStateSnapshotHead().getHead().encode());
    invoke(manager, "closePathStateRoot");
    try (PathStatePhysicalSnapshotHead reopened = PathStatePhysicalSnapshotHead.open(
        root, Engine.ROCKSDB)) {
      assertArrayEquals(base.encode(), reopened.getHead().encode());
    }
  }

  @Test
  public void managerPopsPhysicalChildAdvancesSiblingAndRestarts() throws Exception {
    Path output = temporaryFolder.newFolder("physical-manager-short-reorg").toPath();
    Path root = output.resolve("path-state-root");
    PathStateRootMetadata base = publishEmptyPhysicalCurrent(root, 100, 1, 2);

    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(100L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(base.getBlockHash()));
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));
    SnapshotManager snapshots = new SnapshotManager("");
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);

    withConfig(output, true, () -> {
      snapshots.getDbs().add(propertiesStoreWithP66Enabled());
      snapshots.enable();
      setField(manager, "revokingStore", snapshots);
      invoke(manager, "initPathStateRoot");
    });
    byte[] oldChildHash = bytes(21);
    try (ISession session = snapshots.buildSession()) {
      session.commit(BlockSnapshotMeta.forBlock(101, oldChildHash, base.getBlockHash(), 303));
    }
    assertEquals(101, manager.getPathStateSnapshotHead().getHead().getBlockNumber());

    snapshots.fastPop();
    invoke(manager, "rewindPathStateRootAfterPop");
    assertEquals(PathStateRuntimeAttachment.State.READY,
        manager.getPathStateRuntime().status().getState());
    assertArrayEquals(base.encode(), manager.getPathStateSnapshotHead().getHead().encode());

    byte[] siblingHash = bytes(22);
    try (ISession session = snapshots.buildSession()) {
      session.commit(BlockSnapshotMeta.forBlock(101, siblingHash, base.getBlockHash(), 306));
    }
    assertArrayEquals(siblingHash, manager.getPathStateSnapshotHead().getHead().getBlockHash());
    invoke(manager, "closePathStateRoot");

    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(101L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(siblingHash));
    withConfig(output, true, () -> invoke(manager, "initPathStateRoot"));
    assertArrayEquals(siblingHash, manager.getPathStateSnapshotHead().getHead().getBlockHash());
    assertEquals(PathStateRuntimeAttachment.State.READY,
        manager.getPathStateRuntime().status().getState());
    invoke(manager, "closePathStateRoot");
  }

  @Test
  public void missingCurrentRebuildsExactNativeSnapshotAndAttaches() throws Exception {
    Path output = temporaryFolder.newFolder("startup-rebuild").toPath();
    long blockNumber = 100L;
    long timestamp = 300L;
    BlockId blockId = new BlockId(Sha256Hash.wrap(bytes(1)), blockNumber);
    Sha256Hash parentHash = Sha256Hash.wrap(bytes(2));
    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(blockNumber);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(blockId);
    when(dynamic.getLatestBlockHeaderTimestamp()).thenReturn(timestamp);
    when(dynamic.getAllowAccountAssetOptimizationFromRoot()).thenReturn(1L);
    BlockCapsule block = mock(BlockCapsule.class);
    when(block.getNum()).thenReturn(blockNumber);
    when(block.getBlockId()).thenReturn(blockId);
    when(block.getParentHash()).thenReturn(parentHash);
    when(block.getTimeStamp()).thenReturn(timestamp);
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getBlockByNum(blockNumber)).thenReturn(block);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));

    AtomicInteger closed = new AtomicInteger();
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    SnapshotManager[] snapshotHolder = new SnapshotManager[1];

    withConfig(output, true, () -> {
      SnapshotManager snapshots = new SnapshotManager("");
      for (PathStateParticipantDescriptor.StoreIdentity participant
          : PathStateParticipantDescriptor.current().getStores()) {
        snapshots.getDbs().add(emptyNativeStore(participant.getDbName(), blockNumber,
            blockId.getBytes(), closed));
      }
      snapshots.enable();
      snapshotHolder[0] = snapshots;
      setField(manager, "revokingStore", snapshots);
      invoke(manager, "initPathStateRoot");
    });
    assertNotNull(manager.getPathStateSnapshotHead());
    PathStateRootMetadata head = manager.getPathStateSnapshotHead().getHead();
    assertArrayEquals(blockId.getBytes(), head.getBlockHash());
    assertArrayEquals(parentHash.getBytes(), head.getParentHash());
    assertNotNull(PathStatePhysicalStoreManifest.validateExisting(
        output.resolve("path-state-root"), Engine.ROCKSDB));
    assertFalse(Files.exists(output.resolve("path-state-root/base/nodes")));
    assertFalse(Files.exists(output.resolve("path-state-root/rebuild-spool")));
    assertEquals(PathStateParticipantDescriptor.current().getStores().size(), closed.get());

    byte[] childHash = bytes(4);
    try (ISession session = snapshotHolder[0].buildSession()) {
      BlockSnapshotMeta child = BlockSnapshotMeta.forBlock(101, childHash,
          blockId.getBytes(), 303);
      assertNotNull(snapshotHolder[0].previewPathStateRoot(child));
      session.commit(child);
    }
    assertEquals(101, manager.getPathStateSnapshotHead().getHead().getBlockNumber());
    invoke(manager, "closePathStateRoot");
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(101L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(Sha256Hash.wrap(childHash));
    withConfig(output, true, () -> invoke(manager, "initPathStateRoot"));
    assertEquals(101, manager.getPathStateSnapshotHead().getHead().getBlockNumber());
    assertFalse(Files.exists(output.resolve("path-state-root/base/nodes")));
    assertFalse(Files.exists(output.resolve("path-state-root/rebuild-spool")));
    invoke(manager, "closePathStateRoot");
  }

  @Test
  public void commonCheckpointFreshFlushAndRestartUseOneDurableBoundary() throws Exception {
    Path output = temporaryFolder.newFolder("common-checkpoint-startup").toPath();
    long baseNumber = 100L;
    long timestamp = 300L;
    BlockId baseId = new BlockId(Sha256Hash.wrap(bytes(31)), baseNumber);
    Sha256Hash baseParent = Sha256Hash.wrap(bytes(30));
    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(baseNumber);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(baseId);
    when(dynamic.getLatestBlockHeaderTimestamp()).thenReturn(timestamp);
    when(dynamic.getAllowAccountAssetOptimizationFromRoot()).thenReturn(1L);
    BlockCapsule baseBlock = mock(BlockCapsule.class);
    when(baseBlock.getNum()).thenReturn(baseNumber);
    when(baseBlock.getBlockId()).thenReturn(baseId);
    when(baseBlock.getParentHash()).thenReturn(baseParent);
    when(baseBlock.getTimeStamp()).thenReturn(timestamp);
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getBlockByNum(baseNumber)).thenReturn(baseBlock);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));

    AtomicInteger closed = new AtomicInteger();
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    SnapshotManager[] holder = new SnapshotManager[1];
    withCommonConfig(output, () -> {
      SnapshotManager snapshots = new SnapshotManager("");
      for (PathStateParticipantDescriptor.StoreIdentity participant
          : PathStateParticipantDescriptor.current().getStores()) {
        snapshots.getDbs().add(emptyNativeStore(participant.getDbName(), baseNumber,
            baseId.getBytes(), closed));
      }
      snapshots.enable();
      snapshots.setUnChecked(false);
      holder[0] = snapshots;
      setField(manager, "revokingStore", snapshots);
      invoke(manager, "initCommonCheckpoint");
    });
    SnapshotManager snapshots = holder[0];
    assertNotNull(manager.getCommonCheckpointRuntime());
    assertNotNull(manager.getPathStateSnapshotHead());
    assertTrue(Files.isRegularFile(output.resolve("common-checkpoint")
        .resolve(CommonCheckpointBaselineFile.FILE_NAME)));
    assertTrue(Files.isRegularFile(output.resolve("path-state-root")
        .resolve(PathStateCheckpointMaterializer.COMMON_MODE_FILE)));
    assertFalse(Files.exists(output.resolve("path-state-root/CURRENT")));
    assertFalse(Files.exists(output.resolve("state-archive/hot")));

    BlockId childId = new BlockId(Sha256Hash.wrap(bytes(32)), 101L);
    byte[] childHash = childId.getBytes();
    try (ISession session = snapshots.buildSession()) {
      session.commit(BlockSnapshotMeta.forBlock(101, childHash, baseId.getBytes(), 303L));
    }
    setSnapshotField(snapshots, "flushCount", 1);
    snapshots.flush();
    assertTrue(Files.isRegularFile(output.resolve("path-state-root/CURRENT")));
    assertTrue(Files.isRegularFile(output.resolve("state-archive/READABLE")));
    assertFalse(Files.exists(output.resolve("common-checkpoint/COMMON_CHECKPOINT")));

    BlockCapsule childBlock = mock(BlockCapsule.class);
    when(childBlock.getNum()).thenReturn(101L);
    when(childBlock.getBlockId()).thenReturn(childId);
    when(childBlock.getParentHash()).thenReturn(baseId);
    when(childBlock.getTimeStamp()).thenReturn(303L);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(101L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(childId);
    when(dynamic.getLatestBlockHeaderTimestamp()).thenReturn(303L);
    when(chainBase.getBlockByNum(101L)).thenReturn(childBlock);

    BlockId pendingId = new BlockId(Sha256Hash.wrap(bytes(33)), 102L);
    try (ISession session = snapshots.buildSession()) {
      session.commit(BlockSnapshotMeta.forBlock(102, pendingId.getBytes(), childHash, 306L));
    }
    setSnapshotField(snapshots, "flushCount", 1);
    BlockCapsule pendingBlock = mock(BlockCapsule.class);
    when(pendingBlock.getNum()).thenReturn(102L);
    when(pendingBlock.getBlockId()).thenReturn(pendingId);
    when(pendingBlock.getParentHash()).thenReturn(childId);
    when(pendingBlock.getTimeStamp()).thenReturn(306L);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(102L);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(pendingId);
    when(dynamic.getLatestBlockHeaderTimestamp()).thenReturn(306L);
    when(chainBase.getBlockByNum(102L)).thenReturn(pendingBlock);
    invoke(manager, "closeCommonCheckpoint");
    invoke(manager, "closePathStateRoot");

    withCommonConfig(output, () -> invoke(manager, "initCommonCheckpoint"));
    assertEquals(102L, manager.getPathStateSnapshotHead().getHead().getBlockNumber());
    assertArrayEquals(pendingId.getBytes(),
        manager.getPathStateSnapshotHead().getHead().getBlockHash());
    invoke(manager, "closeCommonCheckpoint");
    invoke(manager, "closePathStateRoot");
  }

  @Test
  public void hotCommonCheckpointUsesDualGateAndPersistentRecoverySources() throws Exception {
    Path output = temporaryFolder.newFolder("hot-common-checkpoint-startup").toPath();
    long baseNumber = 100L;
    BlockId baseId = new BlockId(Sha256Hash.wrap(bytes(61)), baseNumber);
    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(baseNumber);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(baseId);
    when(dynamic.getLatestBlockHeaderTimestamp()).thenReturn(300L);
    when(dynamic.getLatestBlockHeaderNumberFromDB()).thenReturn(baseNumber);
    when(dynamic.getLatestBlockHeaderHashFromDB()).thenReturn(baseId);
    when(dynamic.getAllowAccountAssetOptimizationFromRoot()).thenReturn(1L);
    BlockCapsule baseBlock = mock(BlockCapsule.class);
    when(baseBlock.getNum()).thenReturn(baseNumber);
    when(baseBlock.getBlockId()).thenReturn(baseId);
    when(baseBlock.getParentHash()).thenReturn(Sha256Hash.wrap(bytes(60)));
    when(baseBlock.getTimeStamp()).thenReturn(300L);
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getBlockByNum(baseNumber)).thenReturn(baseBlock);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));

    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    withHotCommonConfig(output, () -> {
      SnapshotManager snapshots = new SnapshotManager("");
      AtomicInteger closed = new AtomicInteger();
      for (PathStateParticipantDescriptor.StoreIdentity participant
          : PathStateParticipantDescriptor.current().getStores()) {
        snapshots.getDbs().add(emptyNativeStore(participant.getDbName(), baseNumber,
            baseId.getBytes(), closed));
      }
      snapshots.enable();
      snapshots.setUnChecked(false);
      setField(manager, "revokingStore", snapshots);
      invoke(manager, "initCommonCheckpoint");
    });

    assertNotNull(manager.getCommonCheckpointRuntime());
    assertTrue(Files.isRegularFile(output.resolve("state-archive/hot/CURRENT")));
    assertFalse(Files.exists(output.resolve("state-archive/READABLE")));
    assertFalse(Files.exists(output.resolve("state-archive/checkpoint-targets")));
    invoke(manager, "closeCommonCheckpoint");
    invoke(manager, "closePathStateRoot");
  }

  @Test
  public void commonCheckpointAdoptsCompatibleLegacyCurrentWithoutRebuild() throws Exception {
    Path output = temporaryFolder.newFolder("common-checkpoint-legacy-current").toPath();
    long baseNumber = 100L;
    long timestamp = 300L;
    Path root = output.resolve("path-state-root");
    BlockId baseId = new BlockId(Sha256Hash.wrap(bytes(41)), baseNumber);
    byte[] parentHash = bytes(40);
    PathStateRootMetadata legacy = publishEmptyPhysicalCurrent(root, baseNumber,
        baseId.getBytes(), parentHash);

    DynamicPropertiesStore dynamic = mock(DynamicPropertiesStore.class);
    when(dynamic.getLatestBlockHeaderNumber()).thenReturn(baseNumber);
    when(dynamic.getLatestBlockHeaderHash()).thenReturn(baseId);
    when(dynamic.getLatestBlockHeaderTimestamp()).thenReturn(timestamp);
    when(dynamic.getAllowAccountAssetOptimizationFromRoot()).thenReturn(1L);
    BlockCapsule baseBlock = mock(BlockCapsule.class);
    when(baseBlock.getNum()).thenReturn(baseNumber);
    when(baseBlock.getBlockId()).thenReturn(baseId);
    when(baseBlock.getParentHash()).thenReturn(Sha256Hash.wrap(parentHash));
    when(baseBlock.getTimeStamp()).thenReturn(timestamp);
    ChainBaseManager chainBase = mock(ChainBaseManager.class);
    when(chainBase.getDynamicPropertiesStore()).thenReturn(dynamic);
    when(chainBase.getBlockByNum(baseNumber)).thenReturn(baseBlock);
    when(chainBase.getAccountAssetStore()).thenReturn(mock(AccountAssetStore.class));

    AtomicInteger pinnedSources = new AtomicInteger();
    Manager manager = new Manager();
    setChainBaseManager(manager, chainBase);
    withCommonConfig(output, () -> {
      SnapshotManager snapshots = new SnapshotManager("");
      for (PathStateParticipantDescriptor.StoreIdentity participant
          : PathStateParticipantDescriptor.current().getStores()) {
        snapshots.getDbs().add(emptyNativeStore(participant.getDbName(), baseNumber,
            baseId.getBytes(), pinnedSources));
      }
      snapshots.enable();
      snapshots.setUnChecked(false);
      setField(manager, "revokingStore", snapshots);
      invoke(manager, "initCommonCheckpoint");
    });

    assertEquals(0, pinnedSources.get());
    assertArrayEquals(legacy.getStateRoot(), manager.getPathStateSnapshotHead().getHead()
        .getStateRoot());
    assertTrue(Files.isRegularFile(output.resolve("common-checkpoint")
        .resolve(CommonCheckpointBaselineFile.FILE_NAME)));
    assertTrue(Files.isRegularFile(root.resolve(PathStateCheckpointMaterializer.COMMON_MODE_FILE)));
    assertTrue(Files.isRegularFile(root.resolve("LEGACY_BASELINE")));
    assertTrue(Files.isRegularFile(root.resolve(
        PathStateCheckpointMaterializer.COMMON_BASELINE_HEAD_FILE)));
    assertFalse(Files.exists(root.resolve("CURRENT")));
    invoke(manager, "closeCommonCheckpoint");
    invoke(manager, "closePathStateRoot");
  }

  @SuppressWarnings("unchecked")
  private static Chainbase propertiesStoreWithP66Enabled() {
    DB<byte[], byte[]> database = mock(DB.class);
    when(database.getDbName()).thenReturn("properties");
    when(database.iterator()).thenReturn(Collections.emptyIterator());
    when(database.get(org.mockito.ArgumentMatchers.any(byte[].class)))
        .thenAnswer(invocation -> Arrays.equals((byte[]) invocation.getArgument(0),
            "ALLOW_ASSET_OPTIMIZATION".getBytes(java.nio.charset.StandardCharsets.UTF_8))
            ? java.nio.ByteBuffer.allocate(Long.BYTES).putLong(1L).array() : null);
    return new Chainbase(new SnapshotRoot(database));
  }

  @SuppressWarnings("unchecked")
  private static Chainbase emptyNativeStore(String dbName, long blockNumber, byte[] blockHash,
      AtomicInteger closed) throws Exception {
    DB<byte[], byte[]> database = mock(DB.class,
        withSettings().extraInterfaces(SnapshotCapableStore.class));
    SnapshotCapableStore capable = (SnapshotCapableStore) database;
    when(database.getDbName()).thenReturn(dbName);
    when(database.iterator()).thenReturn(Collections.emptyIterator());
    if ("properties".equals(dbName)) {
      when(database.get(org.mockito.ArgumentMatchers.any(byte[].class)))
          .thenAnswer(invocation -> Arrays.equals((byte[]) invocation.getArgument(0),
              "ALLOW_ASSET_OPTIMIZATION".getBytes(java.nio.charset.StandardCharsets.UTF_8))
              ? java.nio.ByteBuffer.allocate(Long.BYTES).putLong(1L).array() : null);
    }
    when(capable.getDbName()).thenReturn(dbName);
    when(capable.getSourceIdentity()).thenReturn("source-" + dbName);
    StoreSnapshot snapshot = mock(StoreSnapshot.class);
    when(snapshot.getDbName()).thenReturn(dbName);
    when(snapshot.getSourceIdentity()).thenReturn("source-" + dbName);
    when(snapshot.getBlockNumber()).thenReturn(blockNumber);
    when(snapshot.getBlockHash()).thenReturn(blockHash);
    when(snapshot.range(org.mockito.ArgumentMatchers.any(byte[].class),
        org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(Collections.emptyList());
    org.mockito.Mockito.doAnswer(invocation -> {
      closed.incrementAndGet();
      return null;
    }).when(snapshot).close();
    when(capable.pin(org.mockito.ArgumentMatchers.eq(blockNumber),
        org.mockito.ArgumentMatchers.any(byte[].class))).thenReturn(snapshot);
    return new Chainbase(new SnapshotRoot(database));
  }

  private static void withConfig(Path output, boolean enabled, ThrowingRunnable action)
      throws Exception {
    CommonParameter args = CommonParameter.getInstance();
    Storage oldStorage = args.getStorage();
    Storage storage = new Storage();
    String oldOutput = args.outputDirectory;
    try {
      args.outputDirectory = output.toString();
      args.storage = storage;
      storage.setDbEngine("ROCKSDB");
      storage.setPathStateRootEnabled(enabled);
      storage.setPathStateRootDirectory("path-state-root");
      storage.setPathStateRootReversibleLayerLimit(8);
      storage.setPathStateRootReversibleLayerBytes(1L << 20);
      action.run();
    } finally {
      args.outputDirectory = oldOutput;
      args.storage = oldStorage;
    }
  }

  private static void withCommonConfig(Path output, ThrowingRunnable action) throws Exception {
    withCommonConfig(output, false, action);
  }

  private static void withCommonConfig(Path output, boolean hotEnabled,
      ThrowingRunnable action) throws Exception {
    CommonParameter args = CommonParameter.getInstance();
    Storage oldStorage = args.getStorage();
    String oldOutput = args.outputDirectory;
    try {
      Storage storage = new Storage();
      args.outputDirectory = output.toString();
      args.storage = storage;
      storage.setDbEngine("ROCKSDB");
      storage.setStateArchiveEnabled(true);
      storage.setStateArchiveDirectory("state-archive");
      StateArchiveHotStoreConfig hotConfig = new StateArchiveHotStoreConfig();
      hotConfig.setEnabled(hotEnabled);
      storage.setStateArchiveHotStoreSettings(hotConfig);
      storage.setCommonCheckpointEnabled(true);
      storage.setCommonCheckpointDirectory("common-checkpoint");
      storage.setPathStateRootEnabled(true);
      storage.setPathStateRootDirectory("path-state-root");
      storage.setPathStateRootReversibleLayerLimit(8);
      storage.setPathStateRootReversibleLayerBytes(1L << 20);
      storage.setPathStateRootNodeCacheBytes(1L << 20);
      storage.setPathStateRootParticipantThreads(2);
      storage.setPathStateRootBranchThreads(2);
      action.run();
    } finally {
      args.outputDirectory = oldOutput;
      args.storage = oldStorage;
    }
  }

  private static void withHotCommonConfig(Path output, ThrowingRunnable action) throws Exception {
    withCommonConfig(output, true, action);
  }

  private static void setChainBaseManager(Manager manager, ChainBaseManager chainBase)
      throws Exception {
    setField(manager, "chainBaseManager", chainBase);
  }

  private static void setField(Manager manager, String name, Object value) throws Exception {
    java.lang.reflect.Field field = Manager.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(manager, value);
  }

  private static void setSnapshotField(SnapshotManager manager, String name, int value)
      throws Exception {
    java.lang.reflect.Field field = SnapshotManager.class.getDeclaredField(name);
    field.setAccessible(true);
    field.setInt(manager, value);
  }

  private static void invoke(Manager manager, String methodName) throws Exception {
    Method method = Manager.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    try {
      method.invoke(manager);
    } catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw failure;
    }
  }

  private static byte[] bytes(int seed) {
    byte[] value = new byte[32];
    for (int index = 0; index < value.length; index++) {
      value[index] = (byte) (seed + index);
    }
    return value;
  }

  private static PathStateRootMetadata publishEmptyPhysicalCurrent(Path root,
      long blockNumber, int blockSeed, int parentSeed) throws Exception {
    return publishEmptyPhysicalCurrent(root, blockNumber, bytes(blockSeed), bytes(parentSeed));
  }

  private static PathStateRootMetadata publishEmptyPhysicalCurrent(Path root,
      long blockNumber, byte[] blockHash, byte[] parentHash) throws Exception {
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(root,
        new PathStateCanonicalizer().participantScope(), Engine.ROCKSDB)) {
      PathStateRoot state = stores.buildRootFromFlat();
      PathStateRootMetadata metadata = PathStateRootMetadata.base(blockNumber, blockHash,
          parentHash, 300, P66Phase.P66_ON, stores.getFormatDigest(), state.rootHash(),
          bytes(3));
      stores.publishCurrent(metadata);
      return metadata;
    }
  }

  private static PathStateBlockTransition transition(long blockNumber, int seed,
      byte[] parentHash) {
    return new PathStateBlockTransition(blockNumber, bytes(seed), parentHash,
        blockNumber * 3, P66Phase.P66_ON, Collections.emptyList());
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
