package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.config.args.Args;
import org.tron.core.db2.ISession;
import org.tron.core.db2.archive.BlockChangeView;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.PhysicalSnapshotPathStateCollector;
import org.tron.core.db2.archive.SnapshotOldValueCollector;
import org.tron.core.db2.common.LevelDB;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.stateroot.PathStateBlockTransition;
import org.tron.core.db2.stateroot.PathStateCanonicalizer;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateCheckpointMaterializer;
import org.tron.core.db2.stateroot.PathStateLayerLimits;
import org.tron.core.db2.stateroot.PathStateParticipantScope;
import org.tron.core.db2.stateroot.PathStatePhysicalOverlayHead;
import org.tron.core.db2.stateroot.PathStatePhysicalStoreSet;
import org.tron.core.db2.stateroot.PathStateRoot;
import org.tron.core.db2.stateroot.PathStateRootMetadata;
import org.tron.core.db2.stateroot.PathStateRuntimeAttachment;
import org.tron.core.db2.stateroot.PathStateSnapshotDelta;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;
import org.tron.protos.Protocol.Account;

public class P66SnapshotPipelineTest {
  private static final byte[] ADDRESS = address(1);
  private static final byte[] ASSET = Bytes.concat(ADDRESS, new byte[]{'1'});
  private static final byte[] FLAG = "ALLOW_ASSET_OPTIMIZATION"
      .getBytes(StandardCharsets.US_ASCII);

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void configure() {
    Args.setParam(new String[]{"--output-directory", "output_p66_pipeline"}, "config-test.conf");
  }

  @AfterClass
  public static void clear() {
    Args.clearParam();
  }

  @Test
  public void migratesOnceBeforeFreezeAndBothConsumersSeeExactPhysicalValues() throws Exception {
    try (Fixture f = fixture()) {
      Account before = account(false, 5);
      f.accounts.getHead().put(ADDRESS, before.toByteArray());
      AtomicReference<BlockChangeView> archiveView = new AtomicReference<>();
      AtomicReference<BlockChangeView> pathView = new AtomicReference<>();
      CountDownLatch archiveEntered = new CountDownLatch(1);
      CountDownLatch pathEntered = new CountDownLatch(1);
      f.manager.installArchiveCollector(view -> {
        archiveView.set(view);
        archiveEntered.countDown();
        await(pathEntered);
        return new SnapshotOldValueCollector().collect(view);
      }, diff -> { });
      f.manager.attachPathStateRuntime(new PathStateRuntimeAttachment(view -> {
        pathView.set(view);
        pathEntered.countDown();
        await(archiveEntered);
        return new PhysicalSnapshotPathStateCollector().collect(view);
      }, transition -> { }));
      try (ISession block = f.manager.buildSession()) {
        f.accounts.put(ADDRESS, account(false, 9).toByteArray());
        block.commit(meta(1));
      }
      assertTrue(archiveView.get() == pathView.get());
      assertEquals(9, Longs.fromByteArray(f.assets.getUnchecked(ASSET)));
      Account after = Account.parseFrom(f.accounts.getUnchecked(ADDRESS));
      assertTrue(after.getAssetOptimized());
      assertTrue(after.getAssetV2Map().isEmpty());
      assertNull(f.assets.getHead().getRoot().get(ASSET));
      assertArrayEquals(before.toByteArray(), f.accounts.getHead().getRoot().get(ADDRESS));
      BlockReverseDiff diff = ((SnapshotImpl) f.accounts.getHead()).getPreparedArchiveBlock();
      assertEquals(2, diff.getGroups().size());
      assertArrayEquals(before.toByteArray(), diff.getGroups().stream()
          .filter(group -> group.getDbName().equals("account")).findFirst().get()
          .getEntries().get(0).getOldValue().getValue());
      f.manager.fastPop();
      assertArrayEquals(before.toByteArray(), f.accounts.getUnchecked(ADDRESS));
      assertNull(f.assets.getUnchecked(ASSET));
    }
  }

  @Test
  public void activationWithoutChangedAccountsDoesNotScanOrMigrate() throws Exception {
    try (Fixture f = fixture()) {
      f.properties.getHead().put(FLAG, Longs.toByteArray(0));
      f.accounts.getHead().put(ADDRESS, account(false, 7).toByteArray());
      try (ISession block = f.manager.buildSession()) {
        f.properties.put(FLAG, Longs.toByteArray(1));
        P66CoupledMutationMaterializer.Statistics stats = f.materialize();
        assertEquals(0, stats.changedAccounts);
        assertEquals(0, stats.prefixQueries);
        block.commit(meta(1));
      }
      assertFalse(Account.parseFrom(f.accounts.getUnchecked(ADDRESS)).getAssetOptimized());
      assertNull(f.assets.getUnchecked(ASSET));
    }
  }

  @Test
  public void offEmptyAndOptimizedUnrelatedWritesDoNotQueryAssets() throws Exception {
    try (Fixture f = fixture()) {
      f.properties.getHead().put(FLAG, Longs.toByteArray(0));
      try (ISession block = f.manager.buildSession()) {
        f.properties.put(FLAG, Longs.toByteArray(0));
        f.accounts.put(ADDRESS, account(false, 7).toByteArray());
        assertEquals(0, f.materialize().assetPuts);
        assertFalse(Account.parseFrom(f.accounts.getUnchecked(ADDRESS)).getAssetOptimized());
      }
      f.properties.getHead().put(FLAG, Longs.toByteArray(1));
      for (boolean optimized : new boolean[]{false, true}) {
        try (ISession block = f.manager.buildSession()) {
          f.accounts.put(ADDRESS, account(optimized, 0).toBuilder().clearAssetV2()
              .setBalance(77).build().toByteArray());
          P66CoupledMutationMaterializer.Statistics stats = f.materialize();
          assertEquals(1, stats.emptyAssetSkips);
          assertEquals(0, stats.prefixQueries);
          assertEquals(0, stats.assetPuts);
          assertEquals(0, stats.assetDeletes);
        }
      }
    }
  }

  @Test
  public void zeroDeleteAndAccountDeleteRevokeAlongsideNestedTransactionWrites() throws Exception {
    try (Fixture f = fixture()) {
      f.accounts.getHead().put(ADDRESS, account(true, 0).toBuilder().clearAssetV2()
          .build().toByteArray());
      f.assets.getHead().put(ASSET, Longs.toByteArray(11));
      try (ISession block = f.manager.buildSession()) {
        try (ISession transaction = f.manager.buildSession()) {
          f.accounts.put(ADDRESS, account(true, 0).toByteArray());
          transaction.merge();
        }
        assertEquals(1, f.materialize().assetDeletes);
        assertNull(f.assets.getUnchecked(ASSET));
      }
      assertEquals(11, Longs.fromByteArray(f.assets.getUnchecked(ASSET)));
      try (ISession block = f.manager.buildSession()) {
        byte[] second = Bytes.concat(ADDRESS, new byte[]{'2'});
        f.assets.put(second, Longs.toByteArray(8));
        f.accounts.delete(ADDRESS);
        P66CoupledMutationMaterializer.Statistics stats = f.materialize();
        assertEquals(1, stats.prefixQueries);
        assertEquals(2, stats.assetDeletes);
        assertTrue(f.assets.prefixQuery(ADDRESS).isEmpty());
      }
      assertEquals(11, Longs.fromByteArray(f.assets.getUnchecked(ASSET)));
    }
  }

  @Test
  public void failedParallelBranchNeverAttachesOrPublishesAndJoinsBeforeRevoke() throws Exception {
    try (Fixture f = fixture()) {
      CountDownLatch pathEntered = new CountDownLatch(1);
      AtomicInteger published = new AtomicInteger();
      f.manager.installArchiveCollector(view -> {
        await(pathEntered);
        throw new IllegalStateException("injected archive failure");
      }, diff -> { });
      f.manager.attachPathStateRuntime(new PathStateRuntimeAttachment(view -> {
        pathEntered.countDown();
        return new PhysicalSnapshotPathStateCollector().collect(view);
      }, transition -> published.incrementAndGet()));
      try (ISession block = f.manager.buildSession()) {
        f.accounts.put(ADDRESS, account(false, 9).toByteArray());
        assertThrows(IllegalStateException.class, () -> block.commit(meta(1)));
        assertNull(((SnapshotImpl) f.accounts.getHead()).getBlockSnapshotMeta());
        assertEquals(0, published.get());
      }
      assertNull(f.accounts.getUnchecked(ADDRESS));
      assertNull(f.assets.getUnchecked(ASSET));
    }
  }

  @Test
  public void interruptedJoinWaitsForReaderThenRevokesAndPreservesInterrupt() throws Exception {
    try (Fixture f = fixture()) {
      CountDownLatch archiveEntered = new CountDownLatch(1);
      CountDownLatch releaseArchive = new CountDownLatch(1);
      CountDownLatch finished = new CountDownLatch(1);
      AtomicReference<Throwable> unexpected = new AtomicReference<>();
      f.manager.installArchiveCollector(view -> {
        archiveEntered.countDown();
        await(releaseArchive);
        return new SnapshotOldValueCollector().collect(view);
      }, diff -> { });
      f.manager.attachPathStateRuntime(new PathStateRuntimeAttachment(
          new PhysicalSnapshotPathStateCollector(), transition -> {
        throw new AssertionError("must not publish interrupted block");
      }));
      Thread committer = new Thread(() -> {
        try (ISession block = f.manager.buildSession()) {
          f.accounts.put(ADDRESS, account(false, 9).toByteArray());
          assertThrows(IllegalStateException.class, () -> block.commit(meta(1)));
          assertTrue(Thread.currentThread().isInterrupted());
        } catch (Throwable failure) {
          unexpected.set(failure);
        } finally {
          finished.countDown();
        }
      });
      committer.start();
      try {
        assertTrue(archiveEntered.await(5, TimeUnit.SECONDS));
        committer.interrupt();
        assertFalse("must retain Snapshot until reader exits", finished.await(100,
            TimeUnit.MILLISECONDS));
      } finally {
        releaseArchive.countDown();
        committer.join(5000);
      }
      assertFalse(committer.isAlive());
      assertNull(unexpected.get());
      assertNull(f.assets.getUnchecked(ASSET));
      assertNull(f.accounts.getUnchecked(ADDRESS));
    }
  }

  @Test
  public void malformedDeletionCannotTurnIntoUnboundedAssetPrefixScan() throws Exception {
    try (Fixture f = fixture(); ISession block = f.manager.buildSession()) {
      f.accounts.delete(new byte[0]);
      assertThrows(IllegalStateException.class, f::materialize);
    }
  }

  @Test
  public void accountAssetStoreUsesSnapshotForReadsWritesPrefixesAndRevoke() throws Exception {
    String oldOutput = Args.getInstance().getOutputDirectory();
    Args.getInstance().outputDirectory = temporaryFolder.newFolder().toString();
    SnapshotManager manager = new SnapshotManager("");
    Path path = temporaryFolder.newFolder().toPath();
    Chainbase accounts = Fixture.database(path, "account");
    Chainbase properties = Fixture.database(path, "properties");
    TestAssetStore assetStore = new TestAssetStore();
    try {
      manager.add(accounts);
      manager.add(properties);
      assetStore.enableSnapshots(manager);
      properties.put(FLAG, Longs.toByteArray(1));
      manager.enable();
      try (ISession block = manager.buildSession()) {
        accounts.put(ADDRESS, account(false, 17).toByteArray());
        block.commit(meta(1));
      }
      assertEquals(17, assetStore.getBalance(Account.parseFrom(accounts.getUnchecked(ADDRESS)),
          new byte[]{'1'}));
      assertEquals(1, assetStore.prefixQuery(ADDRESS).size());
      assertNull(assetStore.getFromRoot(ASSET));
      try (ISession pending = manager.buildSession()) {
        assetStore.updateByBatch(Collections.singletonMap(ASSET, Longs.toByteArray(23)));
        assertEquals(23, Longs.fromByteArray(assetStore.get(ASSET)));
      }
      assertEquals(17, Longs.fromByteArray(assetStore.get(ASSET)));
      assertThrows(IllegalStateException.class, () -> assetStore.updateByBatchSynced(
          Collections.singletonMap(ASSET, Longs.toByteArray(44))));
      manager.fastPop();
      assertNull(assetStore.get(ASSET));
      assertTrue(assetStore.prefixQuery(ADDRESS).isEmpty());
    } finally {
      manager.shutdown();
      accounts.close();
      properties.close();
      assetStore.close();
      Args.getInstance().outputDirectory = oldOutput;
    }
  }

  @Test
  public void pathFailureStillJoinsArchiveBeforeReleasingFrozenSnapshot() throws Exception {
    try (Fixture f = fixture()) {
      CountDownLatch archiveEntered = new CountDownLatch(1);
      CountDownLatch pathFailed = new CountDownLatch(1);
      AtomicInteger archiveFinished = new AtomicInteger();
      f.manager.installArchiveCollector(view -> {
        archiveEntered.countDown();
        await(pathFailed);
        BlockReverseDiff result = new SnapshotOldValueCollector().collect(view);
        archiveFinished.incrementAndGet();
        return result;
      }, diff -> { });
      f.manager.attachPathStateRuntime(new PathStateRuntimeAttachment(view -> {
        await(archiveEntered);
        pathFailed.countDown();
        throw new IllegalStateException("injected PathState failure");
      }, transition -> {
        throw new AssertionError("must not publish");
      }));
      try (ISession block = f.manager.buildSession()) {
        f.accounts.put(ADDRESS, account(false, 9).toByteArray());
        assertThrows(IllegalStateException.class, () -> block.commit(meta(1)));
        assertEquals(1, archiveFinished.get());
        assertNull(((SnapshotImpl) f.accounts.getHead()).getBlockSnapshotMeta());
      }
      assertNull(f.assets.getUnchecked(ASSET));
    }
  }

  private static final class TestAssetStore extends org.tron.core.store.AccountAssetStore {
    private TestAssetStore() {
      super("account-asset");
    }
  }

  @Test
  public void realPathStateForkRewindCheckpointAndNativeReopenMatchPhysicalOracle()
      throws Exception {
    Path path = temporaryFolder.newFolder().toPath();
    Path oraclePath = temporaryFolder.newFolder().toPath();
    Path checkpoint = temporaryFolder.newFolder().toPath();
    PathStateParticipantScope scope = new PathStateCanonicalizer().participantScope();
    PathStateRootMetadata baselineHead;
    byte[] format = CommonCheckpointFormat.identity();
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(path, scope,
        Engine.LEVELDB)) {
      PathStateRoot root = stores.createRoot();
      root.put("properties", FLAG, Longs.toByteArray(1));
      stores.persistFlatSnapshot(root);
    }
    try (PathStatePhysicalStoreSet stores = PathStatePhysicalStoreSet.open(path, scope,
        Engine.LEVELDB)) {
      PathStateRoot root = stores.buildRootFromFlat();
      baselineHead = PathStateRootMetadata.base(0, addressHash(0), addressHash(0), 0,
          P66Phase.P66_ON, stores.getFormatDigest(), root.rootHash(), new byte[32]);
      stores.publishCurrent(baselineHead);
    }
    CommonCheckpointBaseline baseline = new CommonCheckpointBaseline(format,
        BlockSnapshotMeta.forBlock(0, addressHash(0), addressHash(0), 0),
        baselineHead.getStateRoot());
    byte[] expected;
    CommonCheckpointTarget target;
    try (Fixture f = fixture();
        PathStatePhysicalOverlayHead head = PathStatePhysicalOverlayHead.open(path,
            Engine.LEVELDB, new PathStateLayerLimits(16, 16L << 20))) {
      head.admitFreshCommonBaseline(baseline);
      PathStateRuntimeAttachment attachment = PathStateRuntimeAttachment.commonCheckpoint(
          new PhysicalSnapshotPathStateCollector(), transition -> head.advance(transition),
          head::preview, head::prepareSnapshotDelta);
      attachment.synchronizeReadyHead(baselineHead);
      f.manager.attachPathStateRuntime(attachment);
      f.manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });
      try (ISession block = f.manager.buildSession()) {
        f.accounts.put(ADDRESS, account(false, 9).toByteArray());
        block.commit(meta(1));
      }
      f.manager.fastPop();
      attachment.synchronizeReadyHead(head.rewindTo(0, addressHash(0)));
      BlockSnapshotMeta fork = BlockSnapshotMeta.forBlock(1, addressHash(7), addressHash(0), 3000);
      try (ISession block = f.manager.buildSession()) {
        f.accounts.put(ADDRESS, account(false, 13).toByteArray());
        block.commit(fork);
      }
      try (PathStatePhysicalStoreSet oracle = PathStatePhysicalStoreSet.open(oraclePath, scope,
          Engine.LEVELDB)) {
        PathStateRoot root = oracle.createRoot();
        root.put("properties", FLAG, Longs.toByteArray(1));
        root.put("account", ADDRESS, f.accounts.getUnchecked(ADDRESS));
        root.put("account-asset", ASSET, Longs.toByteArray(13));
        expected = root.rootHash();
      }
      assertArrayEquals(expected, head.getHead().getStateRoot());
      PathStateSnapshotDelta attached = ((SnapshotImpl) f.accounts.getHead())
          .getPreparedPathStateDelta();
      assertArrayEquals(expected, attached.getStateRoot());
      CommonCheckpointPayload payload = new CommonCheckpointPayloadFactory().capture(format,
          f.manager.getDbs(), 1);
      new CommonCheckpointFile(checkpoint).publish(payload);
      target = CommonCheckpointTarget.from(payload);
      ChainbaseCheckpointMaterializer chain = new ChainbaseCheckpointMaterializer(
          checkpoint.resolve("chain"), format, f.manager.getDbs(), baseline);
      PathStateCheckpointMaterializer materializer = head.checkpointMaterializer(format, baseline);
      chain.materialize(payload, target);
      materializer.materialize(payload, target);
      chain.publish(target);
      materializer.publish(target);
      head.prepareCommonCheckpointRebase(target).apply();
      new CommonCheckpointSnapshotRebaser().prepare(f.manager.getDbs(), target, 1).apply();
      assertEquals(13, Longs.fromByteArray(f.assets.getHead().getRoot().get(ASSET)));
    }
    try (PathStatePhysicalOverlayHead reopened = PathStatePhysicalOverlayHead.openCommonCheckpoint(
        path, Engine.LEVELDB, new PathStateLayerLimits(16, 16L << 20), 1L << 20, 2, 2,
        format, target.getLastBlock(), P66Phase.P66_ON)) {
      assertArrayEquals(expected, reopened.getHead().getStateRoot());
    }
  }

  @Test
  public void checkpointCapturesBothStoresAndReplaysAfterPartialNativeWrite() throws Exception {
    Path checkpoint = temporaryFolder.newFolder().toPath();
    CommonCheckpointPayload payload;
    try (Fixture f = fixture()) {
      f.manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });
      f.manager.attachPathStateRuntime(new PathStateRuntimeAttachment(
          new PhysicalSnapshotPathStateCollector(), transition -> { }, (number, hash) -> { },
          null, (meta, transition) -> delta(meta, transition)));
      try (ISession block = f.manager.buildSession()) {
        f.accounts.put(ADDRESS, account(false, 9).toByteArray());
        block.commit(meta(1));
      }
      payload = new CommonCheckpointPayloadFactory().capture(CommonCheckpointFormat.identity(),
          f.manager.getDbs(), 1);
      assertTrue(payload.getChainbaseStores().stream()
          .anyMatch(store -> store.getDbName().equals("account-asset")));
      CommonCheckpointFile wal = new CommonCheckpointFile(checkpoint);
      wal.publish(payload);
      // Account durable, AccountAsset still absent: emulate interruption between Store batches.
      Map<WrappedByteArray, WrappedByteArray> accountBatch = new LinkedHashMap<>();
      accountBatch.put(WrappedByteArray.of(ADDRESS),
          WrappedByteArray.of(f.accounts.getUnchecked(ADDRESS)));
      ((SnapshotRoot) f.accounts.getHead().getRoot()).applyCheckpointMutations(accountBatch);
      assertNull(f.assets.getHead().getRoot().get(ASSET));
      f.manager.fastPop();
      ChainbaseCheckpointMaterializer materializer = new ChainbaseCheckpointMaterializer(
          checkpoint.resolve("chainbase"), CommonCheckpointFormat.identity(),
          f.manager.getDbs());
      CommonCheckpointPayload loaded = wal.loadRequired();
      CommonCheckpointTarget target = CommonCheckpointTarget.from(loaded);
      materializer.materialize(loaded, target);
      materializer.publish(target);
      assertEquals(9, Longs.fromByteArray(f.assets.getUnchecked(ASSET)));
      materializer.materialize(loaded, target);
      assertEquals(CommonCheckpointMaterializer.Status.PUBLISHED, materializer.inspect(target));
      ChainbaseCheckpointMaterializer.loadPublishedHead(
          checkpoint.resolve("chainbase"), CommonCheckpointFormat.identity());
      assertThrows(java.io.IOException.class, () -> ChainbaseCheckpointMaterializer
          .loadPublishedHead(checkpoint.resolve("chainbase"), addressHash(7)));
    }
  }

  private static PathStateSnapshotDelta delta(BlockSnapshotMeta meta,
      PathStateBlockTransition transition) {
    PathStateSnapshotDelta delta = mock(PathStateSnapshotDelta.class);
    when(delta.getMeta()).thenReturn(meta);
    when(delta.getParentStateRoot()).thenReturn(new byte[32]);
    when(delta.getStateRoot()).thenReturn(addressHash(3));
    when(delta.getTransitionPayloadDigest()).thenReturn(transition.getPayloadDigest());
    when(delta.getStores()).thenReturn(Collections.emptyList());
    when(delta.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return delta;
  }

  private Fixture fixture() throws Exception {
    return new Fixture(temporaryFolder.newFolder().toPath());
  }

  private static void await(CountDownLatch latch) {
    try {
      assertTrue("both branches must run concurrently", latch.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(failure);
    }
  }

  private static byte[] address(int seed) {
    byte[] bytes = new byte[21];
    bytes[0] = 0x41;
    bytes[20] = (byte) seed;
    return bytes;
  }

  private static byte[] addressHash(int seed) {
    byte[] bytes = new byte[32];
    bytes[31] = (byte) seed;
    return bytes;
  }

  private static BlockSnapshotMeta meta(int number) {
    return BlockSnapshotMeta.forBlock(number, addressHash(number), addressHash(number - 1),
        number * 3000L);
  }

  private static Account account(boolean optimized, long balance) {
    return Account.newBuilder().setAddress(ByteString.copyFrom(ADDRESS))
        .setAssetOptimized(optimized).putAssetV2("1", balance).build();
  }

  private static final class Fixture implements AutoCloseable {
    private final SnapshotManager manager = new SnapshotManager("");
    private final Chainbase accounts;
    private final Chainbase assets;
    private final Chainbase properties;

    private Fixture(Path path) {
      accounts = database(path, "account");
      assets = database(path, "account-asset");
      properties = database(path, "properties");
      manager.add(accounts);
      manager.add(properties);
      manager.installP66SnapshotLane(assets);
      properties.getHead().put(FLAG, Longs.toByteArray(1));
      manager.enable();
    }

    private P66CoupledMutationMaterializer.Statistics materialize() {
      return new P66CoupledMutationMaterializer(accounts, assets, properties).materialize();
    }

    private static Chainbase database(Path path, String name) {
      return new Chainbase(new SnapshotRoot(new LevelDB(
          new LevelDbDataSourceImpl(path.toString(), name))));
    }

    @Override
    public void close() {
      manager.shutdown();
      accounts.close();
      assets.close();
      properties.close();
    }
  }
}
