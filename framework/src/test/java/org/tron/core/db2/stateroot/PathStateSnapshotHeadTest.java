package org.tron.core.db2.stateroot;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.common.arch.Arch;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.OldValue;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointPayload.Mutation;
import org.tron.core.db2.core.CommonCheckpointPayload.StoreMutations;
import org.tron.core.db2.core.CommonCheckpointPayloadCodec;
import org.tron.core.db2.stateroot.PathStateCanonicalizer.P66Phase;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class PathStateSnapshotHeadTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void publishesOnlyCommittedContinuousSnapshotsAcrossReopen() throws Exception {
    for (Engine engine : availableEngines()) {
      Fixture fixture = fixture("advance-" + engine, engine);
      PathStateSnapshotHead owner = PathStateSnapshotHead.open(
          fixture.manifest, PathStateLayerLimits.defaults());
      PreparedPathStateTransition prepared = owner.prepare(transition(101, 11,
          fixture.base.getBlockHash(), Collections.singletonList(
              PathStateMutation.put("proposal", new byte[]{1}, new byte[]{5}))));
      PreparedPathStateTransition stale = owner.prepare(transition(101, 21,
          fixture.base.getBlockHash(), Collections.singletonList(
              PathStateMutation.put("proposal", new byte[]{1}, new byte[]{6}))));
      assertTrue(prepared.getNodeMutationCount() > 0);
      try (Stream<java.nio.file.Path> layers = Files.list(
          fixture.manifest.getLayersDirectory())) {
        assertFalse(layers.findAny().isPresent());
      }
      assertArrayEquals(fixture.base.encode(),
          new PathStateCurrentStore(fixture.manifest).current().encode());
      PathStateRootMetadata first = owner.advancePrepared(prepared);
      assertArrayEquals(first.getStateRoot(), owner.getSnapshot().getStateRoot());
      assertThrows(IOException.class, () -> owner.advancePrepared(stale));
      assertArrayEquals(first.encode(), owner.getHead().encode());

      assertThrows(IOException.class, () -> owner.advance(transition(103, 13,
          first.getBlockHash(), Collections.emptyList())));
      assertArrayEquals(first.encode(), owner.getHead().encode());
      assertFalse(owner.isFailed());

      PathStateRootMetadata second = owner.advance(transition(102, 12,
          first.getBlockHash(), Collections.emptyList()));
      assertArrayEquals(first.getStateRoot(), second.getStateRoot());
      try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openCurrent(fixture.manifest)) {
        assertArrayEquals(second.getStateRoot(), stores.createRoot().rootHash());
      }
    }
  }

  @Test
  public void commitAdmissionFailureKeepsOwnedSnapshotAndCurrent() throws Exception {
    Fixture fixture = fixture("admission", Engine.ROCKSDB);
    PathStateSnapshotHead owner = PathStateSnapshotHead.open(
        fixture.manifest, new PathStateLayerLimits(10, 1));

    assertThrows(IOException.class, () -> owner.advance(transition(101, 11,
        fixture.base.getBlockHash(), Collections.singletonList(
            PathStateMutation.put("proposal", new byte[]{1}, new byte[]{5})))));

    assertArrayEquals(fixture.base.encode(), owner.getHead().encode());
    assertArrayEquals(fixture.base.encode(),
        new PathStateCurrentStore(fixture.manifest).current().encode());
    assertFalse(owner.isFailed());
  }

  @Test
  public void preparedTransitionFreezesAnImmutableSnapshotForwardDelta() throws Exception {
    Fixture fixture = fixture("snapshot-delta", Engine.ROCKSDB);
    PathStateSnapshotHead owner = PathStateSnapshotHead.open(
        fixture.manifest, PathStateLayerLimits.defaults());
    PathStateBlockTransition transition = transition(101, 11, fixture.base.getBlockHash(),
        Collections.singletonList(
            PathStateMutation.put("proposal", new byte[]{1}, new byte[]{5})));
    PreparedPathStateTransition prepared = owner.prepare(transition);
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(transition.getBlockNumber(),
        transition.getBlockHash(), transition.getParentHash(), transition.getTimestamp());

    PathStateSnapshotDelta delta = prepared.toSnapshotDelta(meta);

    assertEquals(meta, delta.getMeta());
    assertArrayEquals(fixture.base.getStateRoot(), delta.getParentStateRoot());
    assertArrayEquals(prepared.getStateRoot(), delta.getStateRoot());
    assertArrayEquals(transition.getPayloadDigest(), delta.getTransitionPayloadDigest());
    assertEquals(1, delta.getStores().size());
    PathStateSnapshotDelta.StoreDelta store = delta.getStores().get(0);
    assertEquals("proposal", store.getDbName());
    assertEquals(1, store.getFlatMutations().size());
    assertTrue(store.getNodeMutations().size() > 0);
    assertTrue(delta.getSuperNodeMutations().size() > 0);

    byte[] exposedRoot = delta.getStateRoot();
    exposedRoot[0] ^= 1;
    assertArrayEquals(prepared.getStateRoot(), delta.getStateRoot());
    byte[] exposedKey = store.getFlatMutations().get(0).getKey();
    exposedKey[0] ^= 1;
    assertArrayEquals(PathStateCommitmentCodec.storeLeafKey(store.getStoreId(), new byte[]{1}),
        store.getFlatMutations().get(0).getKey());

    BlockSnapshotMeta wrongMeta = BlockSnapshotMeta.forBlock(101, bytes(12),
        fixture.base.getBlockHash(), transition.getTimestamp());
    assertThrows(IllegalArgumentException.class, () -> prepared.toSnapshotDelta(wrongMeta));
  }

  @Test
  public void coalescesConsecutiveSnapshotDeltasAndRetainsEveryBlockBinding() throws Exception {
    Fixture fixture = fixture("snapshot-delta-coalesce", Engine.ROCKSDB);
    PathStateSnapshotHead owner = PathStateSnapshotHead.open(
        fixture.manifest, PathStateLayerLimits.defaults());

    PathStateBlockTransition firstTransition = transition(101, 11,
        fixture.base.getBlockHash(), Collections.singletonList(
            PathStateMutation.put("proposal", new byte[]{1}, new byte[]{5})));
    PreparedPathStateTransition firstPrepared = owner.prepare(firstTransition);
    PathStateSnapshotDelta first = firstPrepared.toSnapshotDelta(BlockSnapshotMeta.forBlock(
        101, firstTransition.getBlockHash(), firstTransition.getParentHash(), 303));
    owner.advancePrepared(firstPrepared);

    PathStateBlockTransition secondTransition = transition(102, 12,
        firstTransition.getBlockHash(), Arrays.asList(
            PathStateMutation.put("proposal", new byte[]{1}, new byte[]{6}),
            PathStateMutation.delete("account", new byte[]{3})));
    PreparedPathStateTransition secondPrepared = owner.prepare(secondTransition);
    PathStateSnapshotDelta second = secondPrepared.toSnapshotDelta(BlockSnapshotMeta.forBlock(
        102, secondTransition.getBlockHash(), secondTransition.getParentHash(), 306));
    owner.advancePrepared(secondPrepared);

    PathStateBlockTransition thirdTransition = transition(103, 13,
        secondTransition.getBlockHash(), Collections.emptyList());
    PathStateSnapshotDelta third = owner.prepare(thirdTransition).toSnapshotDelta(
        BlockSnapshotMeta.forBlock(103, thirdTransition.getBlockHash(),
            thirdTransition.getParentHash(), 309));

    PathStateFlushTarget target = PathStateFlushTarget.coalesce(
        Arrays.asList(first, second, third));

    assertEquals(3, target.getBlocks().size());
    assertEquals(101, target.getBlocks().get(0).getMeta().getBlockNumber());
    assertEquals(103, target.getBlocks().get(2).getMeta().getBlockNumber());
    assertArrayEquals(first.getParentStateRoot(), target.getParentStateRoot());
    assertArrayEquals(third.getStateRoot(), target.getStateRoot());
    assertEquals(2, target.getStores().size());
    PathStateFlushTarget.StoreTarget proposal = target.getStores().stream()
        .filter(store -> "proposal".equals(store.getDbName())).findFirst().get();
    assertEquals(1, proposal.getFlatMutations().size());
    assertArrayEquals(PathStateCommitmentCodec.presentLeafValue(new byte[]{6}),
        proposal.getFlatMutations().get(0).getValue());
    PathStateFlushTarget.StoreTarget account = target.getStores().stream()
        .filter(store -> "account".equals(store.getDbName())).findFirst().get();
    assertEquals(1, account.getFlatMutations().size());
    assertTrue(account.getFlatMutations().get(0).isDelete());
    assertTrue(target.getMutationBytes() > 0);

    java.util.List<BlockReverseDiff> archiveBlocks = new ArrayList<>();
    for (PathStateFlushTarget.BlockBinding block : target.getBlocks()) {
      BlockReverseDiff.DbGroup group = new BlockReverseDiff.DbGroup("proposal",
          Collections.singletonList(
              new BlockReverseDiff.Entry(new byte[]{1}, OldValue.present(new byte[]{2}))));
      archiveBlocks.add(new BlockReverseDiff(block.getMeta(), Collections.singletonList(group)));
    }
    CommonCheckpointPayload payload = CommonCheckpointPayload.create(bytes(77), target,
        archiveBlocks, Arrays.asList(
            new StoreMutations("proposal", Collections.singletonList(
                new Mutation(new byte[]{1}, new byte[]{6}))),
            new StoreMutations("account", Collections.singletonList(
                new Mutation(new byte[]{3}, null)))));
    CommonCheckpointPayloadCodec codec = new CommonCheckpointPayloadCodec();
    byte[] encoded = codec.encode(payload);
    assertArrayEquals(encoded, codec.encode(payload));
    CommonCheckpointPayload decoded = codec.decode(encoded);
    assertArrayEquals(payload.getFormatIdentity(), decoded.getFormatIdentity());
    assertArrayEquals(payload.getParentStateRoot(), decoded.getParentStateRoot());
    assertArrayEquals(payload.getStateRoot(), decoded.getStateRoot());
    assertEquals(3, decoded.getBlocks().size());
    assertEquals(2, decoded.getChainbaseStores().size());
    assertEquals(2, decoded.getPathStores().size());
    assertArrayEquals(codec.digest(payload), codec.digest(decoded));
    byte[] corrupt = Arrays.copyOf(encoded, encoded.length);
    corrupt[corrupt.length - 1] ^= 1;
    assertThrows(IllegalArgumentException.class, () -> codec.decode(corrupt));
    assertThrows(IllegalArgumentException.class,
        () -> codec.decode(Arrays.copyOf(encoded, encoded.length - 1)));
    byte[] wrongVersion = Arrays.copyOf(encoded, encoded.length);
    wrongVersion[5]++;
    assertThrows(IllegalArgumentException.class, () -> codec.decode(wrongVersion));
    assertThrows(IllegalArgumentException.class,
        () -> new CommonCheckpointPayloadCodec(encoded.length - 1).decode(encoded));
    java.util.List<BlockReverseDiff> mismatchedArchive = new ArrayList<>(archiveBlocks);
    PathStateFlushTarget.BlockBinding firstBlock = target.getBlocks().get(0);
    BlockSnapshotMeta wrongBlock = BlockSnapshotMeta.forBlock(
        firstBlock.getMeta().getBlockNumber(), bytes(88),
        firstBlock.getMeta().getParentHash(), firstBlock.getMeta().getTimestamp());
    mismatchedArchive.set(0, new BlockReverseDiff(wrongBlock, Collections.emptyList()));
    assertThrows(IllegalArgumentException.class, () -> CommonCheckpointPayload.create(
        bytes(77), target, mismatchedArchive, Collections.emptyList()));

    assertThrows(IllegalArgumentException.class,
        () -> PathStateFlushTarget.coalesce(Collections.emptyList()));
    PathStateSnapshotDelta wrongRoot = mock(PathStateSnapshotDelta.class);
    when(wrongRoot.getMeta()).thenReturn(third.getMeta());
    when(wrongRoot.getParentStateRoot()).thenReturn(bytes(99));
    assertThrows(IllegalArgumentException.class,
        () -> PathStateFlushTarget.coalesce(Arrays.asList(second, wrongRoot)));
  }

  @Test
  public void rewindsOwnedHeadThenBuildsCanonicalSibling() throws Exception {
    for (Engine engine : availableEngines()) {
      Fixture fixture = fixture("rewind-" + engine, engine);
      PathStateSnapshotHead owner = PathStateSnapshotHead.open(
          fixture.manifest, PathStateLayerLimits.defaults());
      PathStateRootMetadata first = owner.advance(transition(101, 11,
          fixture.base.getBlockHash(), Collections.singletonList(
              PathStateMutation.put("proposal", new byte[]{1}, new byte[]{5}))));
      PathStateRootMetadata oldSecond = owner.advance(transition(102, 12,
          first.getBlockHash(), Collections.emptyList()));

      assertArrayEquals(first.encode(), owner.rewindTo(
          first.getBlockNumber(), first.getBlockHash()).encode());
      assertFalse(Files.exists(fixture.manifest.getLayerDirectory(
          oldSecond.getBlockNumber(), oldSecond.getBlockHash())));
      PathStateRootMetadata sibling = owner.advance(transition(102, 22,
          first.getBlockHash(), Collections.singletonList(
              PathStateMutation.put("proposal", new byte[]{1}, new byte[]{9}))));

      assertArrayEquals(sibling.encode(), PathStateSnapshotHead.open(
          fixture.manifest, PathStateLayerLimits.defaults()).getHead().encode());
      assertFalse(owner.isFailed());
    }
  }

  private Fixture fixture(String name, Engine engine) throws Exception {
    PathStateStoreManifest manifest = PathStateStoreManifest.createOrOpen(
        new File(temporaryFolder.getRoot(), name).toPath(), engine);
    try (PathStateNodeStoreSet stores = PathStateNodeStoreSet.openBase(manifest)) {
      PathStateRoot root = stores.createRoot();
      root.apply(Arrays.asList(
          PathStateMutation.put("proposal", new byte[]{1}, new byte[]{2}),
          PathStateMutation.put("account", new byte[]{3}, new byte[]{4})));
      PathStateRootMetadata base = PathStateRootMetadata.base(100, bytes(1), bytes(2), 300,
          P66Phase.P66_ON, manifest.getIdentityDigest(), root.rootHash(), bytes(3));
      new PathStateBasePublication(manifest).publish(stores, base);
      return new Fixture(manifest, base);
    }
  }

  private static PathStateBlockTransition transition(long blockNumber, int hashSeed,
      byte[] parentHash, java.util.List<PathStateMutation> mutations) {
    return new PathStateBlockTransition(blockNumber, bytes(hashSeed), parentHash,
        blockNumber * 3, P66Phase.P66_ON, mutations);
  }

  private static Engine[] availableEngines() {
    return Arch.isArm64() ? new Engine[]{Engine.ROCKSDB}
        : new Engine[]{Engine.LEVELDB, Engine.ROCKSDB};
  }

  private static byte[] bytes(int seed) {
    byte[] value = new byte[32];
    for (int index = 0; index < value.length; index++) {
      value[index] = (byte) (seed + index);
    }
    return value;
  }

  private static final class Fixture {

    private final PathStateStoreManifest manifest;
    private final PathStateRootMetadata base;

    private Fixture(PathStateStoreManifest manifest, PathStateRootMetadata base) {
      this.manifest = manifest;
      this.base = base;
    }
  }
}
