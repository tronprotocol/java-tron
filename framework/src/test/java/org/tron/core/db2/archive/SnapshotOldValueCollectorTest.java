package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.tron.common.BaseMethodTest;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.common.DbSourceInter;
import org.tron.core.db2.ISession;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.Flusher;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.db2.core.SnapshotImpl;
import org.tron.core.db2.core.SnapshotManager;
import org.tron.core.db2.core.SnapshotRoot;
import org.tron.core.db2.stateroot.PathStateBasePublication;
import org.tron.core.db2.stateroot.PathStateBlockTransition;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateLayerLimits;
import org.tron.core.db2.stateroot.PathStateMutation;
import org.tron.core.db2.stateroot.PathStateNodeStoreSet;
import org.tron.core.db2.stateroot.PathStateRoot;
import org.tron.core.db2.stateroot.PathStateRootMetadata;
import org.tron.core.db2.stateroot.PathStateRuntimeAdmission;
import org.tron.core.db2.stateroot.PathStateRuntimeAttachment;
import org.tron.core.db2.stateroot.PathStateSnapshotDelta;
import org.tron.core.db2.stateroot.PathStateSnapshotHead;
import org.tron.core.db2.stateroot.PathStateStoreManifest;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;
import org.tron.core.exception.TronError;
import org.tron.core.store.AccountAssetStore;
import org.tron.core.store.CheckTmpStore;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;

public class SnapshotOldValueCollectorTest extends BaseMethodTest {

  @Test
  public void deferredPathStateCaptureDoesNotWaitForCollectorAndDrainsOnClose()
      throws Exception {
    MemoryDb codeDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase code = new Chainbase(new SnapshotRoot(codeDb));
    manager.add(code);
    manager.enable();
    CountDownLatch collecting = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<PathStateBlockTransition> published = new AtomicReference<>();
    PathStateRuntimeAttachment attachment = PathStateRuntimeAttachment.deferred(view -> {
      collecting.countDown();
      try {
        if (!release.await(5, TimeUnit.SECONDS)) {
          throw new IOException("timed out waiting to release deferred collector");
        }
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new IOException("deferred collector interrupted", interrupted);
      }
      BlockSnapshotMeta meta = view.getMeta();
      return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
          meta.getParentHash(), meta.getTimestamp(), P66Phase.P66_ON,
          Collections.singletonList(
              PathStateMutation.put("code", bytes("contract"), bytes("runtime"))));
    }, published::set, (blockNumber, blockHash) -> { }, null, (meta, transition) -> null);
    attachment.synchronizeReadyHead(PathStateRootMetadata.base(0, hash(0), hash(9), 0,
        P66Phase.P66_ON, hash(7), hash(8), hash(6)));
    manager.attachPathStateRuntime(attachment);

    try (ISession block = manager.buildSession()) {
      code.put(bytes("contract"), bytes("runtime"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L));
    }
    assertTrue(collecting.await(5, TimeUnit.SECONDS));
    assertNull(published.get());
    assertFalse(attachment.isReadyForHeaderDiagnostic(1L, hash(1)));
    release.countDown();
    attachment.close();
    assertEquals(1, published.get().getBlockNumber());
    assertEquals(PathStateRuntimeAttachment.State.READY, attachment.status().getState());
    assertTrue(attachment.isReadyForHeaderDiagnostic(1L, hash(1)));
    assertSame(attachment, manager.detachPathStateRuntime(attachment));
    manager.shutdown();
  }

  @Test
  public void deferredPathStateWorkerErrorFailsRuntimeWithoutLeavingAProducerBlocked()
      throws Exception {
    SnapshotManager manager = new SnapshotManager("");
    Chainbase code = new Chainbase(new SnapshotRoot(new MemoryDb("code")));
    manager.add(code);
    manager.enable();
    CountDownLatch attempted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AssertionError workerFailure = new AssertionError("injected deferred worker error");
    PathStateRuntimeAttachment attachment = PathStateRuntimeAttachment.deferred(view -> {
      attempted.countDown();
      try {
        if (!release.await(5, TimeUnit.SECONDS)) {
          throw new AssertionError("timed out waiting to fail deferred worker");
        }
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new AssertionError("deferred worker interrupted", interrupted);
      }
      throw workerFailure;
    }, transition -> { }, (blockNumber, blockHash) -> { }, null,
        (meta, transition) -> null);
    attachment.synchronizeReadyHead(PathStateRootMetadata.base(0, hash(0), hash(9), 0,
        P66Phase.P66_ON, hash(7), hash(8), hash(6)));

    BlockChangeView first = captureView(manager, code,
        BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    attachment.capture(first);
    attachment.publish(null);
    assertTrue(attempted.await(5, TimeUnit.SECONDS));

    for (int number = 2; number <= 65; number++) {
      BlockChangeView queued = captureView(manager, code,
          BlockSnapshotMeta.forBlock(number, hash(number), hash(number - 1), number));
      attachment.capture(queued);
      attachment.publish(null);
    }
    BlockChangeView blocked = captureView(manager, code,
        BlockSnapshotMeta.forBlock(66, hash(66), hash(65), 66L));
    attachment.capture(blocked);
    ExecutorService publisher = Executors.newSingleThreadExecutor();
    try {
      Future<?> blockedPublish = publisher.submit(() -> attachment.publish(null));
      release.countDown();
      blockedPublish.get(5, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      publisher.shutdownNow();
    }

    assertSame(workerFailure, attachment.getFailure());
    IOException closeFailure = assertThrows(IOException.class, attachment::close);
    assertSame(workerFailure, closeFailure.getCause());
    assertEquals(PathStateRuntimeAttachment.State.FAILED, attachment.status().getState());
    assertEquals(PathStateRuntimeAttachment.FailureStage.CAPTURE,
        attachment.status().getFailureStage());
    assertEquals(PathStateRuntimeAttachment.FailureKind.RUNTIME,
        attachment.status().getFailureKind());

    BlockChangeView second = captureView(manager, code,
        BlockSnapshotMeta.forBlock(67, hash(67), hash(66), 67L));
    attachment.capture(second);
    attachment.publish(null);
    assertSame(workerFailure, attachment.getFailure());
    manager.shutdown();
  }

  @Test
  public void pathStateRuntimeCoexistsWithArchiveAtBlockFinalBoundary() throws Exception {
    MemoryDb propertiesDb = new MemoryDb("properties");
    propertiesDb.put(HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
        Longs.toByteArray(1L));
    MemoryDb codeDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase properties = new Chainbase(new SnapshotRoot(propertiesDb));
    Chainbase code = new Chainbase(new SnapshotRoot(codeDb));
    manager.add(properties);
    manager.add(code);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });
    AtomicReference<PathStateBlockTransition> published = new AtomicReference<>();
    SnapshotPathStateTransitionCollector collector = new SnapshotPathStateTransitionCollector(
        key -> Collections.emptyMap());
    PathStateRuntimeAttachment attachment = new PathStateRuntimeAttachment(
        collector::collect, published::set,
        (blockNumber, blockHash) -> { }, null, (meta, transition) -> null);
    manager.attachPathStateRuntime(attachment);

    byte[] key = bytes("contract");
    try (ISession block = manager.buildSession()) {
      code.put(key, bytes("runtime"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L));
    }

    assertFalse(attachment.isFailed());
    assertEquals(1, published.get().getBlockNumber());
    assertEquals(1, published.get().getMutations().size());
    assertEquals("code", published.get().getMutations().get(0).getDbName());
    assertArrayEquals(key, published.get().getMutations().get(0).getCanonicalKey());
    assertSame(attachment, manager.detachPathStateRuntime(attachment));
    manager.shutdown();
  }

  @Test
  public void pathStateForwardDeltaIsOwnedByTheSameBlockSnapshotLayer() throws Exception {
    MemoryDb codeDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase code = new Chainbase(new SnapshotRoot(codeDb));
    manager.add(code);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });
    AtomicReference<PathStateBlockTransition> published = new AtomicReference<>();
    AtomicReference<PathStateSnapshotDelta> prepared = new AtomicReference<>();
    PathStateRuntimeAttachment attachment = new PathStateRuntimeAttachment(view -> {
      BlockSnapshotMeta meta = view.getMeta();
      return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
          meta.getParentHash(), meta.getTimestamp(), P66Phase.P66_ON,
          Collections.singletonList(
              PathStateMutation.put("code", bytes("contract"), bytes("runtime"))));
    }, published::set, (blockNumber, blockHash) -> { }, null, (meta, transition) -> {
      PathStateSnapshotDelta delta = mock(PathStateSnapshotDelta.class);
      when(delta.getMeta()).thenReturn(meta);
      when(delta.getTransitionPayloadDigest()).thenReturn(transition.getPayloadDigest());
      prepared.set(delta);
      return delta;
    });
    manager.attachPathStateRuntime(attachment);

    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    try (ISession block = manager.buildSession()) {
      code.put(bytes("contract"), bytes("runtime"));
      block.commit(meta);
    }

    SnapshotImpl layer = (SnapshotImpl) code.getHead();
    assertEquals(meta, layer.getBlockSnapshotMeta());
    assertSame(prepared.get(), layer.getPreparedPathStateDelta());
    assertEquals(1, published.get().getBlockNumber());
    assertFalse(attachment.isFailed());
    manager.detachPathStateRuntime(attachment);
    manager.shutdown();
  }

  @Test
  public void disabledAndShadowCapturePreserveCanonicalBlockAndStateOutcome() throws Exception {
    byte[] codeKey = bytes("equivalence-code");
    byte[] before = bytes("before");
    byte[] after = bytes("after");
    byte[] parentHash = hash(0);
    byte[] accountStateRoot = hash(9);
    Transaction transaction = Transaction.newBuilder()
        .setRawData(Transaction.raw.newBuilder().setTimestamp(11L))
        .addRet(Transaction.Result.newBuilder().setFee(7L)
            .setContractRet(Transaction.Result.contractResult.SUCCESS))
        .build();
    BlockCapsule template = new BlockCapsule(12L, ByteString.copyFrom(parentHash), 1L,
        Collections.singletonList(transaction));
    template.setMerkleRoot();
    template.setAccountStateRoot(accountStateRoot);
    byte[] canonicalBlock = template.getData();
    byte[] canonicalRaw = template.getInstance().getBlockHeader().getRawData().toByteArray();
    byte[] canonicalBlockId = template.getBlockId().getBytes();
    BlockCapsule controlBlock = new BlockCapsule(canonicalBlock);
    BlockCapsule shadowBlock = new BlockCapsule(canonicalBlock);

    SnapshotManager control = new SnapshotManager("");
    SnapshotManager shadow = new SnapshotManager("");
    Chainbase controlCode = equivalenceStore(control, "code", codeKey, before);
    Chainbase shadowCode = equivalenceStore(shadow, "code", codeKey, before);
    equivalenceProperties(control);
    equivalenceProperties(shadow);
    control.enable();
    shadow.enable();

    Path controlDirectory = temporaryFolder.getRoot().toPath().resolve("disabled-path-state");
    assertEquals(PathStateRuntimeAdmission.Status.DISABLED,
        PathStateRuntimeAdmission.inspect(false, controlDirectory, Engine.ROCKSDB).getStatus());
    assertFalse(Files.exists(controlDirectory));

    Path shadowDirectory = temporaryFolder.newFolder("enabled-path-state").toPath();
    PathStateStoreManifest manifest = PathStateStoreManifest.createOrOpen(
        shadowDirectory, Engine.ROCKSDB);
    PathStateRootMetadata base;
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
      PathStateRoot root = stores.createRoot();
      org.tron.core.db2.stateroot.PathStateCanonicalizer canonicalizer =
          new org.tron.core.db2.stateroot.PathStateCanonicalizer();
      root.apply(Arrays.asList(
          canonicalizer.put(P66Phase.P66_ON, "properties",
              HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
              Longs.toByteArray(1L)),
          canonicalizer.put(P66Phase.P66_ON, "code", codeKey, before)));
      base = PathStateRootMetadata.base(0L, parentHash, hash(99), 1L,
          P66Phase.P66_ON, manifest.getIdentityDigest(), root.rootHash(), hash(7));
      new PathStateBasePublication(manifest).publish(stores, base);
    }
    PathStateSnapshotHead owner = PathStateSnapshotHead.open(
        manifest, PathStateLayerLimits.defaults());
    PathStateRuntimeAttachment runtime = new PathStateRuntimeAttachment(
        new SnapshotPathStateTransitionCollector(ignored -> Collections.emptyMap()),
        owner::advance, (blockNumber, blockHash) -> { },
        transition -> owner.prepare(transition).getStateRoot());
    runtime.synchronizeReadyHead(base);
    assertTrue(runtime.isReadyForHeaderDiagnostic(base.getBlockNumber(), base.getBlockHash()));
    assertFalse(runtime.isReadyForHeaderDiagnostic(base.getBlockNumber() + 1,
        base.getBlockHash()));
    assertFalse(runtime.isReadyForHeaderDiagnostic(base.getBlockNumber(), hash(100)));
    shadow.attachPathStateRuntime(runtime);

    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1L, canonicalBlockId, parentHash, 12L);
    try (ISession session = control.buildSession()) {
      controlCode.put(codeKey, after);
      assertNull(control.previewPathStateRoot(meta));
      session.commit(meta);
    }
    try (ISession session = shadow.buildSession()) {
      shadowCode.put(codeKey, after);
      byte[] candidate = shadow.previewPathStateRoot(meta);
      assertEquals(32, candidate.length);
      assertArrayEquals(base.getStateRoot(), owner.getHead().getStateRoot());
      shadowBlock.setStateRoot(candidate);
      session.commit(meta);
    }

    assertArrayEquals(controlCode.getUnchecked(codeKey), shadowCode.getUnchecked(codeKey));
    assertArrayEquals(after, shadowCode.getUnchecked(codeKey));
    assertEquals(((SnapshotImpl) controlCode.getHead()).getBlockSnapshotMeta(),
        ((SnapshotImpl) shadowCode.getHead()).getBlockSnapshotMeta());
    assertEquals(PathStateRuntimeAttachment.State.READY, runtime.status().getState());
    assertTrue(runtime.isReadyForHeaderDiagnostic(1L, canonicalBlockId));
    assertEquals(1L, owner.getHead().getBlockNumber());
    assertFalse(Arrays.equals(base.getStateRoot(), owner.getHead().getStateRoot()));

    assertArrayEquals(canonicalBlock, controlBlock.getData());
    assertFalse(Arrays.equals(canonicalBlock, shadowBlock.getData()));
    assertArrayEquals(canonicalRaw,
        shadowBlock.getInstance().getBlockHeader().getRawData().toByteArray());
    assertArrayEquals(canonicalBlockId, controlBlock.getBlockId().getBytes());
    assertArrayEquals(canonicalBlockId, shadowBlock.getBlockId().getBytes());
    assertArrayEquals(accountStateRoot, shadowBlock.getInstance().getBlockHeader().getRawData()
        .getAccountStateRoot().toByteArray());
    assertEquals(transaction.getRetList(), shadowBlock.getInstance().getTransactions(0)
        .getRetList());
    assertArrayEquals(owner.getHead().getStateRoot(), shadowBlock.getStateRoot());

    runtime.diagnoseHeader(1L, canonicalBlockId, shadowBlock.getStateRoot(),
        owner.getHead().getStateRoot());
    assertEquals(PathStateRuntimeAttachment.HeaderDiagnostic.MATCH,
        runtime.status().getHeaderDiagnostic());
    runtime.diagnoseHeader(1L, canonicalBlockId, new byte[31], owner.getHead().getStateRoot());
    assertEquals(PathStateRuntimeAttachment.HeaderDiagnostic.INVALID_LENGTH,
        runtime.status().getHeaderDiagnostic());
    runtime.diagnoseHeader(1L, canonicalBlockId, hash(88), owner.getHead().getStateRoot());
    assertEquals(PathStateRuntimeAttachment.HeaderDiagnostic.MISMATCH,
        runtime.status().getHeaderDiagnostic());
    runtime.diagnoseHeader(1L, canonicalBlockId, new byte[0], owner.getHead().getStateRoot());
    assertEquals(PathStateRuntimeAttachment.HeaderDiagnostic.ABSENT,
        runtime.status().getHeaderDiagnostic());
    assertEquals(PathStateRuntimeAttachment.State.READY, runtime.status().getState());

    assertSame(runtime, shadow.detachPathStateRuntime(runtime));
    control.shutdown();
    shadow.shutdown();
  }

  @Test
  public void pathStateP66ActivationScansPostStateThenResumesIncrementalCapture()
      throws Exception {
    byte[] firstAddress = archiveAddress(21);
    byte[] secondAddress = archiveAddress(22);
    Account firstBefore = Account.newBuilder()
        .setAddress(ByteString.copyFrom(firstAddress))
        .putAssetV2("1000021", 21L)
        .build();
    Account firstPost = firstBefore.toBuilder().putAssetV2("1000021", 210L).build();
    Account second = Account.newBuilder()
        .setAddress(ByteString.copyFrom(secondAddress))
        .putAssetV2("1000022", 22L)
        .build();
    MemoryDb accountDb = new MemoryDb("account");
    accountDb.put(firstAddress, firstBefore.toByteArray());
    accountDb.put(secondAddress, second.toByteArray());
    MemoryDb propertiesDb = new MemoryDb("properties");
    propertiesDb.put(HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
        Longs.toByteArray(0L));
    SnapshotManager manager = new SnapshotManager("");
    Chainbase account = new Chainbase(new SnapshotRoot(accountDb));
    Chainbase properties = new Chainbase(new SnapshotRoot(propertiesDb));
    Chainbase code = new Chainbase(new SnapshotRoot(new MemoryDb("code")));
    manager.add(account);
    manager.add(properties);
    manager.add(code);
    manager.enable();
    List<PathStateBlockTransition> published = new ArrayList<>();
    SnapshotPathStateTransitionCollector collector = new SnapshotPathStateTransitionCollector(
        ignored -> Collections.emptyMap(), consumer -> {
      Iterator<Map.Entry<byte[], byte[]>> entries = account.iterator();
      while (entries.hasNext()) {
        Map.Entry<byte[], byte[]> entry = entries.next();
        consumer.accept(entry.getKey(), entry.getValue());
      }
    });
    PathStateRuntimeAttachment attachment = new PathStateRuntimeAttachment(collector,
        published::add);
    manager.attachPathStateRuntime(attachment);

    try (ISession block = manager.buildSession()) {
      properties.put(HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
          Longs.toByteArray(1L));
      account.put(firstAddress, firstPost.toByteArray());
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }

    assertFalse(attachment.isFailed());
    assertEquals(1, published.size());
    PathStateBlockTransition activation = published.get(0);
    assertEquals(P66Phase.P66_ACTIVATION, activation.getPhase());
    assertEquals(5, activation.getMutations().size());
    assertEquals(2, mutationCount(activation, "account"));
    assertEquals(2, mutationCount(activation, "account-asset"));
    PathStateMutation firstAccount = mutation(activation, "account", firstAddress);
    Account canonicalFirst = Account.parseFrom(firstAccount.getCanonicalValue());
    assertTrue(canonicalFirst.getAssetOptimized());
    assertTrue(canonicalFirst.getAssetV2Map().isEmpty());
    PathStateMutation firstAsset = mutation(activation, "account-asset",
        Bytes.concat(firstAddress, bytes("1000021")));
    assertArrayEquals(Longs.toByteArray(210L), firstAsset.getCanonicalValue());
    assertTrue(firstAccount.isPreviousValueKnown());
    assertFalse(firstAsset.isPreviousValueKnown());

    byte[] codeKey = bytes("after-activation");
    try (ISession block = manager.buildSession()) {
      code.put(codeKey, bytes("incremental"));
      block.commit(BlockSnapshotMeta.forBlock(2, hash(2), hash(1), 2L));
    }

    assertFalse(attachment.isFailed());
    assertEquals(2, published.size());
    assertEquals(P66Phase.P66_ON, published.get(1).getPhase());
    assertEquals(1, published.get(1).getMutations().size());
    assertEquals("code", published.get(1).getMutations().get(0).getDbName());
    assertTrue(published.get(1).getMutations().get(0).isPreviousValueKnown());
    assertNull(published.get(1).getMutations().get(0).getPreviousPhysicalValue());
    manager.detachPathStateRuntime(attachment);
    manager.shutdown();
  }

  @Test
  public void pathStateFailureDoesNotRejectArchiveDisabledBlockCommit() throws Exception {
    SnapshotManager manager = new SnapshotManager("");
    Chainbase code = new Chainbase(new SnapshotRoot(new MemoryDb("code")));
    manager.add(code);
    manager.enable();
    AtomicInteger published = new AtomicInteger();
    PathStateRuntimeAttachment attachment = new PathStateRuntimeAttachment(view -> {
      throw new IOException("capture failed");
    }, transition -> published.incrementAndGet());
    manager.attachPathStateRuntime(attachment);

    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    try (ISession block = manager.buildSession()) {
      code.put(bytes("contract"), bytes("runtime"));
      block.commit(meta);
    }

    assertTrue(attachment.isFailed());
    assertEquals("capture failed", attachment.getFailure().getMessage());
    assertEquals(0, published.get());
    assertEquals(meta, ((SnapshotImpl) code.getHead()).getBlockSnapshotMeta());
    manager.detachPathStateRuntime(attachment);
    manager.shutdown();
  }

  @Test
  public void pathStateStatusExposesLagStorageFailureAndCaptureGap() throws Exception {
    SnapshotManager manager = new SnapshotManager("");
    Chainbase code = new Chainbase(new SnapshotRoot(new MemoryDb("code")));
    manager.add(code);
    manager.enable();
    AtomicInteger publications = new AtomicInteger();
    PathStateRuntimeAttachment attachment = new PathStateRuntimeAttachment(view -> {
      BlockSnapshotMeta meta = view.getMeta();
      return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
          meta.getParentHash(), meta.getTimestamp(), P66Phase.P66_ON,
          Collections.emptyList());
    }, transition -> {
      if (publications.incrementAndGet() == 2) {
        throw new IOException("No space left on device");
      }
    });

    PathStateBlockTransition first = attachment.capture(
        captureView(manager, code, BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L)));
    assertEquals(PathStateRuntimeAttachment.State.NOT_READY, attachment.status().getState());
    attachment.publish(first);
    assertEquals(PathStateRuntimeAttachment.State.READY, attachment.status().getState());
    assertEquals(0L, attachment.status().getRootLag());

    PathStateBlockTransition second = attachment.capture(
        captureView(manager, code, BlockSnapshotMeta.forBlock(2, hash(2), hash(1), 2L)));
    assertEquals(PathStateRuntimeAttachment.State.NOT_READY, attachment.status().getState());
    assertEquals(1L, attachment.status().getRootLag());
    attachment.publish(second);
    assertEquals(PathStateRuntimeAttachment.State.FAILED, attachment.status().getState());
    assertEquals(PathStateRuntimeAttachment.FailureStage.PUBLISH,
        attachment.status().getFailureStage());
    assertEquals(PathStateRuntimeAttachment.FailureKind.STORAGE_FULL,
        attachment.status().getFailureKind());
    attachment.capture(
        captureView(manager, code, BlockSnapshotMeta.forBlock(3, hash(3), hash(2), 3L)));
    assertEquals(2L, attachment.status().getRootLag());

    PathStateRuntimeAttachment gap = new PathStateRuntimeAttachment(view -> {
      BlockSnapshotMeta meta = view.getMeta();
      return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
          meta.getParentHash(), meta.getTimestamp(), P66Phase.P66_ON,
          Collections.emptyList());
    }, transition -> { });
    PathStateBlockTransition admitted = gap.capture(
        captureView(manager, code, BlockSnapshotMeta.forBlock(4, hash(4), hash(3), 4L)));
    gap.publish(admitted);
    assertEquals(PathStateRuntimeAttachment.State.READY, gap.status().getState());
    assertEquals(null, gap.capture(
        captureView(manager, code, BlockSnapshotMeta.forBlock(6, hash(6), hash(5), 6L))));
    assertEquals(PathStateRuntimeAttachment.FailureStage.CAPTURE_GAP,
        gap.status().getFailureStage());
    assertEquals(2L, gap.status().getRootLag());

    PathStateRuntimeAttachment corrupt = new PathStateRuntimeAttachment(view -> {
      throw new AssertionError("capture is not used");
    }, transition -> { }, (blockNumber, blockHash) -> {
      throw new IOException("native progress checksum mismatch");
    });
    corrupt.flushBaseThrough(4L, hash(4));
    assertEquals(PathStateRuntimeAttachment.State.FAILED, corrupt.status().getState());
    assertEquals(PathStateRuntimeAttachment.FailureStage.BASE_FLUSH,
        corrupt.status().getFailureStage());
    assertEquals(PathStateRuntimeAttachment.FailureKind.CORRUPTION,
        corrupt.status().getFailureKind());
    manager.shutdown();
  }

  @Test
  public void pathStateCompactsOnlyAfterChainbaseRefreshesThePrefix() throws Exception {
    SnapshotManager manager = new SnapshotManager("");
    Chainbase code = new Chainbase(new SnapshotRoot(new MemoryDb("code")));
    manager.add(code);
    manager.enable();
    manager.setUnChecked(false);
    CheckTmpStore checkpoint = mock(CheckTmpStore.class);
    DbSourceInter<byte[]> checkpointDb = mock(DbSourceInter.class);
    when(checkpointDb.iterator()).thenReturn(Collections.emptyIterator());
    when(checkpoint.getDbSource()).thenReturn(checkpointDb);
    manager.setCheckTmpStore(checkpoint);
    AtomicReference<Long> flushedNumber = new AtomicReference<>();
    AtomicReference<byte[]> flushedHash = new AtomicReference<>();
    PathStateRuntimeAttachment attachment = new PathStateRuntimeAttachment(
        view -> {
          BlockSnapshotMeta meta = view.getMeta();
          return new PathStateBlockTransition(meta.getBlockNumber(), meta.getBlockHash(),
              meta.getParentHash(), meta.getTimestamp(), P66Phase.P66_ON,
              Collections.emptyList());
        }, transition -> { },
        (blockNumber, blockHash) -> {
          flushedNumber.set(blockNumber);
          flushedHash.set(blockHash);
        });
    manager.attachPathStateRuntime(attachment);

    BlockSnapshotMeta first = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L);
    BlockSnapshotMeta second = BlockSnapshotMeta.forBlock(2, hash(2), hash(1), 2L);
    commitBlock(manager, code, first, "first");
    commitBlock(manager, code, second, "second");
    setFlushCount(manager, 1);

    manager.flush();

    assertEquals(Long.valueOf(1L), flushedNumber.get());
    assertArrayEquals(first.getBlockHash(), flushedHash.get());
    assertFalse(attachment.isFailed());
    manager.detachPathStateRuntime(attachment);
    manager.shutdown();
  }

  @Test
  public void borrowedRuntimeAttachmentIsAtomicAndIdentityBound() throws Exception {
    SnapshotManager manager = new SnapshotManager("");
    manager.add(new Chainbase(new SnapshotRoot(new MemoryDb("code"))));
    OldValueCollector collector = mock(OldValueCollector.class);
    DurableBlockReverseDiffSink sink = mock(DurableBlockReverseDiffSink.class,
        withSettings().extraInterfaces(Closeable.class));
    ArchiveRuntimeAttachment attachment =
        new ArchiveRuntimeAttachment(collector, sink);
    ArchiveRuntimeAttachment foreign =
        new ArchiveRuntimeAttachment(collector, sink);

    manager.attachArchiveRuntime(attachment);

    assertThrows(IllegalStateException.class, () -> manager.attachArchiveRuntime(foreign));
    assertThrows(IllegalStateException.class, () -> manager.detachArchiveRuntime(foreign));
    assertThrows(IllegalStateException.class,
        () -> manager.installArchiveCollector(collector, sink));
    assertSame(attachment, manager.detachArchiveRuntime(attachment));
    assertThrows(IllegalStateException.class, () -> manager.detachArchiveRuntime(attachment));
    verify((Closeable) sink, never()).close();

    BlockReverseDiffSink legacySink = mock(BlockReverseDiffSink.class,
        withSettings().extraInterfaces(Closeable.class));
    manager.installArchiveCollector(collector, legacySink);
    manager.shutdown();

    verify((Closeable) legacySink).close();
    verify((Closeable) sink, never()).close();
  }

  @Test
  public void collectsBlockPreStateAfterNestedSessionsFinish() {
    MemoryDb memoryDb = new MemoryDb("code");
    byte[] changed = bytes("changed");
    byte[] deleted = bytes("deleted");
    byte[] created = bytes("created");
    byte[] empty = bytes("empty");
    byte[] createThenDelete = bytes("create-then-delete");
    memoryDb.put(changed, bytes("old"));
    memoryDb.put(deleted, bytes("gone"));
    memoryDb.put(empty, new byte[0]);

    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    BlockReverseDiffSink sink = mock(BlockReverseDiffSink.class);
    manager.installArchiveCollector(new SnapshotOldValueCollector(), sink);

    byte[] hash = new byte[32];
    hash[31] = 1;
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash, new byte[32], 3_000L);
    try (ISession block = manager.buildSession()) {
      database.put(changed, bytes("intermediate"));
      try (ISession transaction = manager.buildSession()) {
        database.put(changed, bytes("new"));
        database.put(created, bytes("created-value"));
        transaction.merge();
      }
      try (ISession revertedTransaction = manager.buildSession()) {
        database.put(bytes("reverted"), bytes("not-visible"));
      }
      database.delete(deleted);
      database.put(empty, new byte[0]);
      database.put(createThenDelete, bytes("temporary"));
      database.delete(createThenDelete);
      block.commit(meta);
    }

    BlockReverseDiff diff = prepared(database);
    verify(sink, never()).accept(any(BlockReverseDiff.class));
    assertEquals(meta, diff.getMeta());
    assertEquals(meta, ((SnapshotImpl) database.getHead()).getBlockSnapshotMeta());
    assertEquals(1, diff.getGroups().size());
    DbGroup group = diff.getGroups().get(0);
    assertEquals("code", group.getDbName());
    assertEquals(3, group.getEntries().size());

    assertArrayEquals(bytes("old"), find(group, changed).getOldValue().getValue());
    assertArrayEquals(bytes("gone"), find(group, deleted).getOldValue().getValue());
    assertFalse(find(group, created).getOldValue().isPresent());
    assertFalse(contains(group, empty));
    assertFalse(contains(group, createThenDelete));
    assertFalse(contains(group, bytes("reverted")));
    manager.shutdown();
  }

  @Test
  public void preservesStorageRowPhysicalKeyWithoutLogicalProjection() {
    MemoryDb memoryDb = new MemoryDb("storage-row");
    byte[] addressHash = new byte[32];
    byte[] slotPart = new byte[32];
    for (int i = 0; i < 32; i++) {
      addressHash[i] = (byte) i;
      slotPart[i] = (byte) (0x80 + i);
    }
    byte[] physicalKey = new byte[32];
    System.arraycopy(addressHash, 0, physicalKey, 0, 16);
    System.arraycopy(slotPart, 16, physicalKey, 16, 16);
    memoryDb.put(physicalKey, bytes("old-word"));

    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });

    try (ISession block = manager.buildSession()) {
      database.put(physicalKey, bytes("new-word"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }

    BlockReverseDiff diff = prepared(database);
    assertEquals(1, diff.getGroups().size());
    DbGroup group = diff.getGroups().get(0);
    assertEquals("storage-row", group.getDbName());
    assertEquals(1, group.getEntries().size());
    assertArrayEquals(physicalKey, group.getEntries().get(0).getKey());
    assertArrayEquals(bytes("old-word"), group.getEntries().get(0).getOldValue().getValue());
    manager.shutdown();
  }

  @Test
  public void retainsAbiChangesInCanonicalStateHistory() {
    SnapshotManager manager = new SnapshotManager("");
    Chainbase abi = new Chainbase(new SnapshotRoot(new MemoryDb("abi")));
    Chainbase code = new Chainbase(new SnapshotRoot(new MemoryDb("code")));
    manager.add(abi);
    manager.add(code);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });

    try (ISession block = manager.buildSession()) {
      abi.put(bytes("contract"), bytes("abi-metadata"));
      code.put(bytes("contract"), bytes("runtime-code"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }

    BlockReverseDiff diff = prepared(code);
    assertEquals(2, diff.getGroups().size());
    assertEquals("abi", diff.getGroups().get(0).getDbName());
    assertEquals("code", diff.getGroups().get(1).getDbName());
    assertFalse(find(diff.getGroups().get(0), bytes("contract")).getOldValue().isPresent());
    assertEquals(diff, prepared(abi));
    manager.shutdown();
  }

  @Test
  public void preservesPresentEmptyAndEmitsNoopBlockMetadata() {
    MemoryDb memoryDb = new MemoryDb("code");
    byte[] key = bytes("key");
    memoryDb.put(key, new byte[0]);
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });

    try (ISession block = manager.buildSession()) {
      database.put(key, bytes("value"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }
    BlockReverseDiff first = prepared(database);
    assertTrue(find(first.getGroups().get(0), key).getOldValue().isPresent());
    assertEquals(0,
        find(first.getGroups().get(0), key).getOldValue().getValue().length);

    try (ISession block = manager.buildSession()) {
      block.commit(BlockSnapshotMeta.forBlock(2, hash(2), hash(1), 2L));
    }
    assertTrue(prepared(database).getGroups().isEmpty());
    manager.shutdown();
  }

  @Test
  public void rejectsUnknownOrDuplicateRegisteredDatabaseNames() {
    SnapshotManager unknown = new SnapshotManager("");
    unknown.add(new Chainbase(new SnapshotRoot(new MemoryDb("new-store"))));
    IllegalStateException unknownError = assertThrows(IllegalStateException.class,
        () -> unknown.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { }));
    assertTrue(unknownError.getMessage().contains("new-store"));
    unknown.shutdown();

    SnapshotManager duplicate = new SnapshotManager("");
    duplicate.add(new Chainbase(new SnapshotRoot(new MemoryDb("code"))));
    duplicate.add(new Chainbase(new SnapshotRoot(new MemoryDb("code"))));
    IllegalStateException duplicateError = assertThrows(IllegalStateException.class,
        () -> duplicate.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { }));
    assertTrue(duplicateError.getMessage().contains("Duplicate"));
    duplicate.shutdown();
  }

  @Test
  public void classifiesEveryChainbaseRegisteredByTheApplication() {
    SnapshotManager applicationManager = context.getBean(SnapshotManager.class);
    ArchiveStoreScope.validate(applicationManager.getDbs());
    assertEquals(27, ArchiveStoreScope.getStateDatabases().size());
    assertTrue(ArchiveStoreScope.isStateDatabase("abi"));
    assertFalse(ArchiveStoreScope.isExcludedDatabase("abi"));
    assertTrue(ArchiveStoreScope.isClassified("abi"));
  }

  @Test
  public void matchesReferenceStateForRandomBlockOperations() {
    MemoryDb memoryDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(), diff -> { });

    Random random = new Random(0x5a17L);
    Map<String, byte[]> reference = new HashMap<>();
    for (int blockNumber = 1; blockNumber <= 40; blockNumber++) {
      Map<String, byte[]> before = copy(reference);
      try (ISession block = manager.buildSession()) {
        for (int operation = 0; operation < 25; operation++) {
          String key = "key-" + random.nextInt(12);
          int action = random.nextInt(4);
          boolean nested = random.nextBoolean();
          boolean keepNested = random.nextBoolean();
          if (nested) {
            try (ISession transaction = manager.buildSession()) {
              byte[] post = applyRandomOperation(database, key, action, blockNumber, operation);
              if (keepNested) {
                updateReference(reference, key, post);
                transaction.merge();
              }
            }
          } else {
            byte[] post = applyRandomOperation(database, key, action, blockNumber, operation);
            updateReference(reference, key, post);
          }
        }
        block.commit(BlockSnapshotMeta.forBlock(blockNumber, hash(blockNumber),
            hash(blockNumber - 1), blockNumber));
      }

      BlockReverseDiff diff = prepared(database);
      Map<String, OldValue> actual = new HashMap<>();
      if (!diff.getGroups().isEmpty()) {
        diff.getGroups().get(0).getEntries().forEach(entry -> actual.put(
            new String(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8),
            entry.getOldValue()));
      }
      Set<String> keys = new HashSet<>(before.keySet());
      keys.addAll(reference.keySet());
      for (String key : keys) {
        byte[] oldValue = before.get(key);
        byte[] postValue = reference.get(key);
        if (Arrays.equals(oldValue, postValue)) {
          assertFalse("no-op key was emitted: " + key, actual.containsKey(key));
        } else {
          assertTrue("changed key was not emitted: " + key, actual.containsKey(key));
          OldValue archived = actual.get(key);
          assertEquals(oldValue != null, archived.isPresent());
          if (oldValue != null) {
            assertArrayEquals(oldValue, archived.getValue());
          }
        }
      }
      assertEquals(keys.stream().filter(key -> !Arrays.equals(before.get(key), reference.get(key)))
          .count(), actual.size());
    }
    manager.shutdown();
  }

  @Test
  public void projectsAccountAssetTransitionBeforeRootMerge() {
    byte[] address = archiveAddress(1);
    byte[] token = bytes("1000001");
    Account oldAccount = Account.newBuilder()
        .setAddress(ByteString.copyFrom(address))
        .putAssetV2("1000001", 100L)
        .build();
    Account postAccount = oldAccount.toBuilder().putAssetV2("1000001", 80L).build();

    MemoryDb memoryDb = new MemoryDb("account");
    memoryDb.put(address, oldAccount.toByteArray());
    AccountAssetStore assetStore = mock(AccountAssetStore.class);
    when(assetStore.prefixQuery(any(byte[].class))).thenReturn(new HashMap<>());

    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    AccountAssetArchiveProjector projector = new AccountAssetArchiveProjector();
    manager.installArchiveCollector(new SnapshotOldValueCollector(projector,
        accountKey -> assetStore.prefixQuery(accountKey), () -> true), diff -> { });

    try (ISession block = manager.buildSession()) {
      database.put(address, postAccount.toByteArray());
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }

    BlockReverseDiff diff = prepared(database);
    DbGroup accountGroup = diff.getGroups().stream()
        .filter(group -> "account".equals(group.getDbName()))
        .findFirst().orElseThrow(AssertionError::new);
    Account archivedAccount;
    try {
      archivedAccount = Account.parseFrom(find(accountGroup, address).getOldValue().getValue());
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw new AssertionError(e);
    }
    assertFalse(archivedAccount.getAssetOptimized());
    assertEquals(100L, archivedAccount.getAssetV2Map().get("1000001").longValue());

    DbGroup assetGroup = diff.getGroups().stream()
        .filter(group -> "account-asset".equals(group.getDbName()))
        .findFirst().orElseThrow(AssertionError::new);
    Entry assetEntry = find(assetGroup, Bytes.concat(address, token));
    assertFalse(assetEntry.getOldValue().isPresent());
    manager.shutdown();
  }

  @Test
  public void projectsOldPhysicalAssetValueForOptimizedAccount() {
    byte[] address = archiveAddress(2);
    byte[] token = bytes("1000002");
    byte[] assetKey = Bytes.concat(address, token);
    Account oldAccount = Account.newBuilder()
        .setAddress(ByteString.copyFrom(address))
        .setAssetOptimized(true)
        .build();
    Account postAccount = oldAccount.toBuilder().putAssetV2("1000002", 80L).build();

    MemoryDb memoryDb = new MemoryDb("account");
    memoryDb.put(address, oldAccount.toByteArray());
    AccountAssetStore assetStore = mock(AccountAssetStore.class);
    Map<WrappedByteArray, byte[]> persisted = new HashMap<>();
    persisted.put(WrappedByteArray.copyOf(assetKey), Longs.toByteArray(100L));
    when(assetStore.prefixQuery(any(byte[].class))).thenReturn(persisted);

    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(
        new AccountAssetArchiveProjector(),
        accountKey -> assetStore.prefixQuery(accountKey), () -> true), diff -> { });

    try (ISession block = manager.buildSession()) {
      database.put(address, postAccount.toByteArray());
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }

    BlockReverseDiff diff = prepared(database);
    assertFalse(diff.getGroups().stream()
        .anyMatch(group -> "account".equals(group.getDbName())));
    DbGroup assetGroup = diff.getGroups().stream()
        .filter(group -> "account-asset".equals(group.getDbName()))
        .findFirst().orElseThrow(AssertionError::new);
    assertArrayEquals(Longs.toByteArray(100L),
        find(assetGroup, assetKey).getOldValue().getValue());
    manager.shutdown();
  }

  @Test
  public void pureProjectionRequiresAndCopiesExplicitOldPhysicalAssets() {
    byte[] address = archiveAddress(4);
    byte[] assetKey = Bytes.concat(address, bytes("1000009"));
    Account optimized = Account.newBuilder()
        .setAddress(ByteString.copyFrom(address))
        .setAssetOptimized(true)
        .build();
    Map<WrappedByteArray, byte[]> oldPhysicalAssets = new HashMap<>();
    oldPhysicalAssets.put(WrappedByteArray.copyOf(assetKey), Longs.toByteArray(900L));
    AccountAssetArchiveProjector projector = new AccountAssetArchiveProjector();

    assertThrows(ArchivePersistenceException.class,
        () -> projector.project(address, optimized.toByteArray(),
            BlockChangeView.PostValue.absent(), true, null));
    Map<WrappedByteArray, byte[]> wrongAccountAssets = new HashMap<>();
    wrongAccountAssets.put(WrappedByteArray.copyOf(bytes("another-account-token")),
        Longs.toByteArray(1L));
    assertThrows(ArchivePersistenceException.class,
        () -> projector.project(address, optimized.toByteArray(),
            BlockChangeView.PostValue.absent(), true, wrongAccountAssets));

    AccountAssetArchiveProjector.Projection projection = projector.project(address,
        optimized.toByteArray(), BlockChangeView.PostValue.absent(), true,
        oldPhysicalAssets);
    oldPhysicalAssets.clear();

    assertEquals(1, projection.reverseAssets.size());
    assertArrayEquals(assetKey, projection.reverseAssets.get(0).getKey());
    assertArrayEquals(Longs.toByteArray(900L),
        projection.reverseAssets.get(0).getOldValue().getValue());
  }

  @Test
  public void targetAssetOptimizationOverridesLegacySupplierAndCoversDelete() {
    byte[] address = archiveAddress(5);
    byte[] assetKey = Bytes.concat(address, bytes("1000003"));
    Account rawPost = Account.newBuilder()
        .setAddress(ByteString.copyFrom(address))
        .putAssetV2("1000003", 300L)
        .build();
    AccountAssetArchiveProjector projector = new AccountAssetArchiveProjector();

    AccountAssetArchiveProjector.Projection enabled = projector.project(address, null,
        BlockChangeView.PostValue.present(rawPost.toByteArray()), true, Collections.emptyMap());
    assertTrue(parseAccount(enabled.postAccount.getValue()).getAssetOptimized());

    AccountAssetArchiveProjector.Projection disabled =
        new AccountAssetArchiveProjector().project(address, null,
            BlockChangeView.PostValue.present(rawPost.toByteArray()), false,
            Collections.emptyMap());
    assertArrayEquals(rawPost.toByteArray(), disabled.postAccount.getValue());
    Map<WrappedByteArray, byte[]> mixedPhysical = new HashMap<>();
    mixedPhysical.put(WrappedByteArray.copyOf(assetKey), Longs.toByteArray(300L));
    assertThrows(ArchivePersistenceException.class,
        () -> projector.project(address, null,
            BlockChangeView.PostValue.present(rawPost.toByteArray()), true, mixedPhysical));

    Account optimizedOld = rawPost.toBuilder()
        .setAssetOptimized(true)
        .clearAssetV2()
        .build();
    Map<WrappedByteArray, byte[]> persisted = new HashMap<>();
    persisted.put(WrappedByteArray.copyOf(assetKey), Longs.toByteArray(300L));
    AccountAssetArchiveProjector.Projection deleted = projector.project(address,
        optimizedOld.toByteArray(), BlockChangeView.PostValue.absent(), true, persisted);
    assertFalse(deleted.postAccount.isPresent());
    assertEquals(1, deleted.reverseAssets.size());
  }

  @Test
  public void targetAssetOptimizationComesFromTheCapturedPropertiesPostState() {
    List<byte[]> invalidValues = Arrays.asList(null, new byte[]{1}, Longs.toByteArray(2));
    for (byte[] invalidValue : invalidValues) {
      SnapshotManager manager = new SnapshotManager("");
      Chainbase account = new Chainbase(new SnapshotRoot(new MemoryDb("account")));
      MemoryDb propertiesRoot = new MemoryDb("properties");
      if (invalidValue != null) {
        propertiesRoot.put(HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
            invalidValue);
      }
      Chainbase properties = new Chainbase(new SnapshotRoot(propertiesRoot));
      manager.add(account);
      manager.add(properties);
      manager.enable();
      manager.installArchiveCollector(new SnapshotOldValueCollector(
          new AccountAssetArchiveProjector(), ignored -> Collections.emptyMap(),
          SnapshotOldValueCollector::resolveTargetAssetOptimization), diff -> { });
      byte[] address = archiveAddress(12);
      Account post = Account.newBuilder().setAddress(ByteString.copyFrom(address))
          .putAssetV2("1000012", 12L).build();

      assertThrows(ArchivePersistenceException.class, () -> {
        try (ISession block = manager.buildSession()) {
          account.put(address, post.toByteArray());
          block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
        }
      });
      assertEquals(0, manager.getActiveSession());
      assertEquals(0, manager.size());
      manager.shutdown();
    }

    SnapshotManager manager = new SnapshotManager("");
    Chainbase account = new Chainbase(new SnapshotRoot(new MemoryDb("account")));
    MemoryDb propertiesRoot = new MemoryDb("properties");
    propertiesRoot.put(HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
        Longs.toByteArray(0));
    Chainbase properties = new Chainbase(new SnapshotRoot(propertiesRoot));
    manager.add(account);
    manager.add(properties);
    manager.enable();
    manager.installArchiveCollector(new SnapshotOldValueCollector(
        new AccountAssetArchiveProjector(), ignored -> Collections.emptyMap(),
        SnapshotOldValueCollector::resolveTargetAssetOptimization), diff -> { });
    byte[] address = archiveAddress(13);
    Account post = Account.newBuilder().setAddress(ByteString.copyFrom(address))
        .putAssetV2("1000013", 13L).build();
    try (ISession block = manager.buildSession()) {
      properties.put(HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
          Longs.toByteArray(1));
      account.put(address, post.toByteArray());
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }
    DbGroup assetGroup = prepared(account).getGroups().stream()
        .filter(group -> "account-asset".equals(group.getDbName()))
        .findFirst().orElseThrow(AssertionError::new);
    assertFalse(find(assetGroup, Bytes.concat(address, bytes("1000013")))
        .getOldValue().isPresent());
    manager.shutdown();
  }

  @Test
  public void sharedProjectionMatchesSnapshotRootBytesWithProposalSixtySix() {
    byte[] address = new byte[21];
    address[0] = 65;
    address[20] = 79;
    String token = "1000005";
    byte[] assetKey = Bytes.concat(address, bytes(token));
    Account rawPost = Account.newBuilder()
        .setAddress(ByteString.copyFrom(address))
        .putAssetV2(token, 500L)
        .build();
    AccountAssetArchiveProjector.Projection projection =
        new AccountAssetArchiveProjector().project(address, null,
            BlockChangeView.PostValue.present(rawPost.toByteArray()), true,
            Collections.emptyMap());

    chainBaseManager.getDynamicPropertiesStore().setAllowAccountAssetOptimization(0);
    chainBaseManager.getDynamicPropertiesStore().setAllowAssetOptimization(1);
    MemoryDb accountRootDb = new MemoryDb("account");
    SnapshotRoot accountRoot = new SnapshotRoot(accountRootDb);
    accountRoot.put(address, rawPost.toByteArray());

    assertArrayEquals(projection.postAccount.getValue(), accountRootDb.get(address));
    assertArrayEquals(Longs.toByteArray(500L),
        chainBaseManager.getAccountAssetStore().get(assetKey));
  }

  @Test
  public void archiveDurabilityFailurePreventsCheckpointAndRefresh() throws Exception {
    MemoryDb memoryDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.setUnChecked(false);
    CheckTmpStore checkpoint = mock(CheckTmpStore.class);
    manager.setCheckTmpStore(checkpoint);
    DurableBlockReverseDiffSink sink = mock(DurableBlockReverseDiffSink.class);
    manager.installArchiveCollector(new SnapshotOldValueCollector(), sink);
    try (ISession block = manager.buildSession()) {
      database.put(bytes("key"), bytes("value"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }
    setFlushCount(manager, 1);
    doThrow(new ArchivePersistenceException("injected"))
        .when(sink).awaitCommitted(1L);

    assertThrows(TronError.class, manager::flush);
    verify(sink).acceptAll(Collections.singletonList(prepared(database)));
    verify(sink).awaitCommitted(1L);
    verify(checkpoint, never()).updateByBatch(any(Map.class));
    manager.shutdown();
  }

  @Test
  public void flushRetriesDurabilityAndEvidenceWithoutResubmittingHistory() throws Exception {
    MemoryDb memoryDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.setUnChecked(false);
    CheckTmpStore checkpoint = mock(CheckTmpStore.class);
    DbSourceInter<byte[]> checkpointDb = mock(DbSourceInter.class);
    when(checkpointDb.iterator()).thenReturn(Collections.emptyIterator());
    when(checkpoint.getDbSource()).thenReturn(checkpointDb);
    manager.setCheckTmpStore(checkpoint);
    ArchiveHistoryWriter writer = new ArchiveHistoryWriter(
        temporaryFolder.newFolder("flush-evidence-retry").toPath(), 4096,
        ArchiveStoreScope.getStateDatabases());
    FailOnceEvidenceSink sink = new FailOnceEvidenceSink(writer);
    manager.installArchiveCollector(new SnapshotOldValueCollector(), sink);

    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L);
    commitBlock(manager, database, meta, "key-1");
    setFlushCount(manager, 1);

    assertThrows(TronError.class, manager::flush);
    verify(checkpoint, never()).updateByBatch(any(Map.class));

    assertThrows(TronError.class, manager::flush);
    verify(checkpoint, never()).updateByBatch(any(Map.class));

    manager.flush();

    assertEquals(1, sink.acceptAllCalls);
    assertEquals(3, sink.awaitCalls);
    assertEquals(2, sink.evidenceCalls);
    manager.shutdown();
    writer.close();
  }

  @Test
  public void fastPopDiscardsPreparedPayloadWithoutRevertingDurableHistory() {
    MemoryDb memoryDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    BlockReverseDiffSink sink = mock(BlockReverseDiffSink.class);
    manager.installArchiveCollector(new SnapshotOldValueCollector(), sink);

    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L);
    try (ISession block = manager.buildSession()) {
      database.put(bytes("key"), bytes("value"));
      block.commit(meta);
    }

    assertEquals(meta, prepared(database).getMeta());
    manager.fastPop();

    assertEquals(0, manager.size());
    assertTrue(database.getHead() instanceof SnapshotRoot);
    verify(sink, never()).accept(any(BlockReverseDiff.class));
    verify(sink, never()).revert(any(BlockSnapshotMeta.class));
    manager.shutdown();
  }

  @Test
  public void collectorFailureLeavesSessionOwnedSoCloseRevokesLayer() {
    MemoryDb memoryDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    OldValueCollector collector = mock(OldValueCollector.class);
    BlockReverseDiffSink sink = mock(BlockReverseDiffSink.class);
    when(collector.collect(any(BlockChangeView.class)))
        .thenThrow(new IllegalStateException("injected collector failure"));
    manager.installArchiveCollector(collector, sink);

    assertThrows(IllegalStateException.class, () -> {
      try (ISession block = manager.buildSession()) {
        database.put(bytes("key"), bytes("value"));
        block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
      }
    });

    assertEquals(0, manager.getActiveSession());
    assertEquals(0, manager.size());
    assertTrue(database.getHead() instanceof SnapshotRoot);
    verify(sink, never()).accept(any(BlockReverseDiff.class));
    manager.shutdown();
  }

  @Test
  public void flushPublishesOnlyTheNonRevertibleRange() throws Exception {
    MemoryDb memoryDb = new MemoryDb("code");
    SnapshotManager manager = new SnapshotManager("");
    Chainbase database = new Chainbase(new SnapshotRoot(memoryDb));
    manager.add(database);
    manager.enable();
    manager.setUnChecked(false);
    CheckTmpStore checkpoint = mock(CheckTmpStore.class);
    DbSourceInter<byte[]> checkpointDb = mock(DbSourceInter.class);
    when(checkpointDb.iterator()).thenReturn(Collections.emptyIterator());
    when(checkpoint.getDbSource()).thenReturn(checkpointDb);
    manager.setCheckTmpStore(checkpoint);
    DurableBlockReverseDiffSink sink = mock(DurableBlockReverseDiffSink.class);
    manager.installArchiveCollector(new SnapshotOldValueCollector(), sink);

    try (ISession block = manager.buildSession()) {
      database.put(bytes("key-1"), bytes("value-1"));
      block.commit(BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 1L));
    }
    BlockReverseDiff first = prepared(database);
    try (ISession block = manager.buildSession()) {
      database.put(bytes("key-2"), bytes("value-2"));
      block.commit(BlockSnapshotMeta.forBlock(2, hash(2), hash(1), 2L));
    }
    BlockReverseDiff second = prepared(database);
    setFlushCount(manager, 1);
    HistoryCommitMarker committed = marker(first.getMeta());
    when(sink.createMarkerRangeEvidence(1)).thenReturn(
        new DurableHistoryMarkerRangeEvidence(new DurableHistoryMarkerRangeEvidence.Source() {
          @Override
          public HistoryCommitMarker marker(long epoch) {
            return committed;
          }

          @Override
          public BlockReverseDiff readCommitted(long epoch) {
            return first;
          }
        }, 1));

    manager.flush();

    verify(sink).acceptAll(Collections.singletonList(first));
    verify(sink, never()).acceptAll(Collections.singletonList(second));
    verify(sink).awaitCommitted(1L);
    verify(sink).releaseThrough(1L);
    manager.shutdown();
  }

  private static Entry find(DbGroup group, byte[] key) {
    return group.getEntries().stream()
        .filter(entry -> Arrays.equals(entry.getKey(), key))
        .findFirst()
        .orElseThrow(AssertionError::new);
  }

  private static Account parseAccount(byte[] value) {
    try {
      return Account.parseFrom(value);
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw new AssertionError(e);
    }
  }

  private static BlockReverseDiff prepared(Chainbase database) {
    return ((SnapshotImpl) database.getHead()).getPreparedArchiveBlock();
  }

  private static void commitBlock(SnapshotManager manager, Chainbase database,
      BlockSnapshotMeta meta, String key) {
    try (ISession block = manager.buildSession()) {
      database.put(bytes(key), bytes("value-" + key));
      block.commit(meta);
    }
  }

  private static HistoryCommitMarker marker(BlockSnapshotMeta meta) {
    int epoch = (int) meta.getEpoch();
    List<String> participants = new ArrayList<>(ArchiveStoreScope.getStateDatabases());
    Collections.sort(participants);
    return new HistoryCommitMarker(meta, epoch - 1,
        new HistoryLocation(0, epoch * 100L, 100, epoch, hash(epoch + 20)),
        new HistoryIndexLocation(epoch * 50L, 50, hash(epoch + 30)),
        Arrays.copyOf(hash(epoch + 40), 16), participants);
  }

  private static void setFlushCount(SnapshotManager manager, int count) throws Exception {
    java.lang.reflect.Field flushCount = SnapshotManager.class.getDeclaredField("flushCount");
    flushCount.setAccessible(true);
    flushCount.setInt(manager, count);
  }

  private static boolean contains(DbGroup group, byte[] key) {
    return group.getEntries().stream().anyMatch(entry -> Arrays.equals(entry.getKey(), key));
  }

  private static int mutationCount(PathStateBlockTransition transition, String dbName) {
    return (int) transition.getMutations().stream()
        .filter(mutation -> dbName.equals(mutation.getDbName()))
        .count();
  }

  private static PathStateMutation mutation(PathStateBlockTransition transition,
      String dbName, byte[] key) {
    return transition.getMutations().stream()
        .filter(candidate -> dbName.equals(candidate.getDbName())
            && Arrays.equals(key, candidate.getCanonicalKey()))
        .findFirst()
        .orElseThrow(AssertionError::new);
  }

  private static BlockChangeView captureView(SnapshotManager manager, Chainbase database,
      BlockSnapshotMeta meta) {
    try (ISession session = manager.buildSession()) {
      database.put(bytes("status-" + meta.getBlockNumber()),
          bytes("value-" + meta.getBlockNumber()));
      BlockChangeView view = BlockChangeView.capture(meta, manager.getDbs());
      session.commit();
      return view;
    }
  }

  private static Chainbase equivalenceStore(SnapshotManager manager, String dbName,
      byte[] key, byte[] value) {
    MemoryDb database = new MemoryDb(dbName);
    database.put(key, value);
    Chainbase chainbase = new Chainbase(new SnapshotRoot(database));
    manager.add(chainbase);
    return chainbase;
  }

  private static void equivalenceProperties(SnapshotManager manager) {
    equivalenceStore(manager, HistoricalAccountAssetBalanceResolver.PROPERTIES_DATABASE,
        HistoricalAccountAssetBalanceResolver.proposal66PhysicalKey(),
        Longs.toByteArray(1L));
  }

  private static byte[] bytes(String value) {
    return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static byte[] archiveAddress(int suffix) {
    byte[] address = new byte[21];
    address[0] = 0x41;
    address[20] = (byte) suffix;
    return address;
  }

  private static byte[] hash(int suffix) {
    byte[] hash = new byte[32];
    hash[31] = (byte) suffix;
    return hash;
  }

  private static byte[] applyRandomOperation(Chainbase database, String key, int action,
      int blockNumber, int operation) {
    byte[] encodedKey = bytes(key);
    if (action == 0) {
      database.delete(encodedKey);
      return null;
    }
    byte[] value = action == 1 ? new byte[0]
        : bytes("value-" + blockNumber + '-' + operation + '-' + action);
    database.put(encodedKey, value);
    return value;
  }

  private static void updateReference(Map<String, byte[]> reference, String key, byte[] value) {
    if (value == null) {
      reference.remove(key);
    } else {
      reference.put(key, Arrays.copyOf(value, value.length));
    }
  }

  private static Map<String, byte[]> copy(Map<String, byte[]> source) {
    Map<String, byte[]> copy = new HashMap<>();
    source.forEach((key, value) -> copy.put(key, Arrays.copyOf(value, value.length)));
    return copy;
  }

  private static final class FailOnceEvidenceSink implements DurableBlockReverseDiffSink {
    private final ArchiveHistoryWriter writer;
    private int acceptAllCalls;
    private int awaitCalls;
    private int evidenceCalls;

    private FailOnceEvidenceSink(ArchiveHistoryWriter writer) {
      this.writer = writer;
    }

    @Override
    public void accept(BlockReverseDiff diff) {
      writer.accept(diff);
    }

    @Override
    public void acceptAll(List<BlockReverseDiff> diffs) {
      acceptAllCalls++;
      writer.acceptAll(diffs);
    }

    @Override
    public void revert(BlockSnapshotMeta meta) {
      writer.revert(meta);
    }

    @Override
    public void awaitCommitted(long epoch) {
      awaitCalls++;
      if (awaitCalls == 1) {
        throw new ArchivePersistenceException("injected durable wait failure");
      }
      writer.awaitCommitted(epoch);
    }

    @Override
    public DurableHistoryMarkerRangeEvidence createMarkerRangeEvidence(int maxMarkers) {
      evidenceCalls++;
      if (evidenceCalls == 1) {
        return new DurableHistoryMarkerRangeEvidence(
            new DurableHistoryMarkerRangeEvidence.Source() {
              @Override
              public HistoryCommitMarker marker(long epoch) {
                return null;
              }

              @Override
              public BlockReverseDiff readCommitted(long epoch) {
                return writer.readCommitted(epoch);
              }
            }, maxMarkers);
      }
      return writer.createMarkerRangeEvidence(maxMarkers);
    }

    @Override
    public void releaseThrough(long epoch) {
      writer.releaseThrough(epoch);
    }
  }

  private static final class MemoryDb implements DB<byte[], byte[]>, Flusher {
    private final String name;
    private final Map<WrappedByteArray, byte[]> values = new LinkedHashMap<>();

    private MemoryDb(String name) {
      this.name = name;
    }

    @Override
    public byte[] get(byte[] key) {
      byte[] value = values.get(WrappedByteArray.of(key));
      return value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public void put(byte[] key, byte[] value) {
      values.put(WrappedByteArray.copyOf(key), Arrays.copyOf(value, value.length));
    }

    @Override
    public long size() {
      return values.size();
    }

    @Override
    public boolean isEmpty() {
      return values.isEmpty();
    }

    @Override
    public void remove(byte[] key) {
      values.remove(WrappedByteArray.of(key));
    }

    @Override
    public Iterator<Map.Entry<byte[], byte[]>> iterator() {
      List<Map.Entry<byte[], byte[]>> entries = new ArrayList<>();
      values.forEach((key, value) -> entries.add(new AbstractMap.SimpleImmutableEntry<>(
          key.getBytes(), Arrays.copyOf(value, value.length))));
      return entries.iterator();
    }

    @Override
    public void close() {
      values.clear();
    }

    @Override
    public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
      batch.forEach((key, value) -> {
        if (value == null || value.getBytes() == null) {
          values.remove(key);
        } else {
          values.put(WrappedByteArray.copyOf(key.getBytes()), value.getBytes());
        }
      });
    }

    @Override
    public void reset() {
      values.clear();
    }

    @Override
    public String getDbName() {
      return name;
    }

    @Override
    public void stat() {
    }

    @Override
    public DB<byte[], byte[]> newInstance() {
      return new MemoryDb(name);
    }
  }
}
