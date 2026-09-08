package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.ArchiveReadSnapshot.PinnedLatestState;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.core.CommonCheckpointFile;
import org.tron.core.db2.core.CommonCheckpointMaterializer;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointRedoCoordinator;
import org.tron.core.db2.core.CommonCheckpointRuntimeOwner;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateFlushTarget;

public class StateArchiveCheckpointReadSnapshotTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void combinesHistoryAndPinnedLatestUnderOneRuntimeLease() throws Exception {
    Path root = temporaryFolder.newFolder("snapshot").toPath();
    byte[] format = hash(90);
    CommonCheckpointPayload payload = payload(format, 1, 3);
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    StateArchiveCheckpointMaterializer materializer =
        new StateArchiveCheckpointMaterializer(root, format);
    materializer.materialize(payload, target);
    materializer.publish(target);
    CommonCheckpointRuntimeOwner owner = readyOwner(root.resolve("runtime"));
    FakeLatest latest = new FakeLatest(3, hash(3), OldValue.present(new byte[]{99}));

    try (StateArchiveCheckpointReadSnapshot snapshot =
        StateArchiveCheckpointReadSnapshot.pin(2, owner, root, format,
            (blockNumber, blockHash) -> {
              assertEquals(3, blockNumber);
              assertArrayEquals(hash(3), blockHash);
              return latest;
            })) {
      assertEquals(2, snapshot.getTargetBlock());
      assertEquals(3, snapshot.getPinnedBlock());
      assertArrayEquals(hash(3), snapshot.getPinnedHash());
      assertArrayEquals(new byte[]{2}, snapshot.get("code", new byte[]{3}).getValue());
      assertArrayEquals(new byte[]{99}, snapshot.get("code", new byte[]{1}).getValue());
      snapshot.requirePinnedIdentity();
      assertFalse(latest.closed);
    }
    assertTrue(latest.closed);
    owner.close();
    assertEquals(CommonCheckpointRuntimeOwner.State.CLOSED, owner.getState());
  }

  @Test
  public void rejectsLatestHeadMismatchAndClosesFailedPin() throws Exception {
    Path root = temporaryFolder.newFolder("mismatch").toPath();
    byte[] format = hash(91);
    CommonCheckpointPayload payload = payload(format, 1, 2);
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    StateArchiveCheckpointMaterializer materializer =
        new StateArchiveCheckpointMaterializer(root, format);
    materializer.materialize(payload, target);
    materializer.publish(target);
    CommonCheckpointRuntimeOwner owner = readyOwner(root.resolve("runtime"));
    FakeLatest latest = new FakeLatest(1, hash(1), OldValue.absent());

    assertThrows(IllegalArgumentException.class,
        () -> StateArchiveCheckpointReadSnapshot.pin(1, owner, root, format,
            (blockNumber, blockHash) -> latest));
    assertTrue(latest.closed);
    owner.close();
    assertEquals(CommonCheckpointRuntimeOwner.State.CLOSED, owner.getState());
  }

  private static CommonCheckpointRuntimeOwner readyOwner(Path directory) throws IOException {
    CommonCheckpointMaterializer chainbase = materializer(Authority.CHAINBASE);
    CommonCheckpointMaterializer pathState = materializer(Authority.PATH_STATE);
    CommonCheckpointMaterializer archive = materializer(Authority.STATE_ARCHIVE);
    CommonCheckpointRuntimeOwner owner = new CommonCheckpointRuntimeOwner(
        new CommonCheckpointRedoCoordinator(new CommonCheckpointFile(directory), chainbase,
            pathState, archive));
    owner.recoverBeforeServing();
    return owner;
  }

  private static CommonCheckpointMaterializer materializer(Authority authority) {
    CommonCheckpointMaterializer materializer = mock(CommonCheckpointMaterializer.class);
    when(materializer.authority()).thenReturn(authority);
    return materializer;
  }

  private static CommonCheckpointPayload payload(byte[] format, long firstBlock, int count) {
    List<PathStateFlushTarget.BlockBinding> bindings = new ArrayList<>();
    List<BlockReverseDiff> archives = new ArrayList<>();
    byte[] priorHash = hash((int) firstBlock - 1);
    byte[] priorRoot = hash(30);
    for (int index = 0; index < count; index++) {
      long number = firstBlock + index;
      byte[] blockHash = hash((int) number);
      byte[] nextRoot = hash(31 + index);
      BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(number, blockHash, priorHash,
          number * 3_000L);
      PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
      when(binding.getMeta()).thenReturn(meta);
      when(binding.getParentStateRoot()).thenReturn(priorRoot);
      when(binding.getStateRoot()).thenReturn(nextRoot);
      when(binding.getTransitionPayloadDigest()).thenReturn(hash(70 + index));
      bindings.add(binding);
      DbGroup group = new DbGroup("code", Collections.singletonList(
          new Entry(new byte[]{(byte) number},
              OldValue.present(new byte[]{(byte) (number - 1)}))));
      archives.add(new BlockReverseDiff(meta, Collections.singletonList(group)));
      priorHash = blockHash;
      priorRoot = nextRoot;
    }
    PathStateFlushTarget pathState = mock(PathStateFlushTarget.class);
    when(pathState.getBlocks()).thenReturn(bindings);
    when(pathState.getParentStateRoot()).thenReturn(hash(30));
    when(pathState.getStateRoot()).thenReturn(priorRoot);
    when(pathState.getStores()).thenReturn(Collections.emptyList());
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return CommonCheckpointPayload.create(format, pathState, archives,
        Collections.emptyList());
  }

  private static byte[] hash(int seed) {
    byte[] value = new byte[32];
    for (int index = 0; index < value.length; index++) {
      value[index] = (byte) (seed + index);
    }
    return value;
  }

  private static final class FakeLatest implements PinnedLatestState {

    private final long blockNumber;
    private final byte[] blockHash;
    private final OldValue value;
    private boolean closed;

    private FakeLatest(long blockNumber, byte[] blockHash, OldValue value) {
      this.blockNumber = blockNumber;
      this.blockHash = blockHash;
      this.value = value;
    }

    @Override
    public long getBlockNumber() {
      return blockNumber;
    }

    @Override
    public byte[] getBlockHash() {
      return blockHash;
    }

    @Override
    public OldValue get(String dbName, byte[] physicalRawKey) {
      return value;
    }

    @Override
    public List<HistoricalRangeOverlay.Entry> range(String dbName, byte[] lowerInclusive,
        byte[] upperExclusive, int maxEntries) {
      throw new UnsupportedOperationException("point-only checkpoint snapshot");
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
