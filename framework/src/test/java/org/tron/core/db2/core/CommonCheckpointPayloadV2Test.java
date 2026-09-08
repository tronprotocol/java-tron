package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.ArchivePersistenceException;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.archive.OldValue;
import org.tron.core.db2.archive.StateArchiveHotBatchDescriptor;
import org.tron.core.db2.archive.StateArchiveHotStore;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class CommonCheckpointPayloadV2Test {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void roundTripsDigestOnlyArchiveBindingWithoutOldValueBody() throws Exception {
    byte[] format = hash(7);
    byte[] oldValueSentinel = new byte[96];
    for (int index = 0; index < oldValueSentinel.length; index++) {
      oldValueSentinel[index] = (byte) (0xa0 + index % 31);
    }
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    BlockReverseDiff diff = new BlockReverseDiff(meta,
        Collections.singletonList(new DbGroup("code", Collections.singletonList(
            new Entry(new byte[]{1}, OldValue.present(oldValueSentinel))))));
    Path root = temporaryFolder.newFolder("payload-v2").toPath();
    try (StateArchiveHotStore hotStore = StateArchiveHotStore.openOrCreate(root, format,
        Engine.LEVELDB, 0, hash(0), 3, 10, 1024 * 1024)) {
      StateArchiveHotBatchDescriptor descriptor = hotStore.planCheckpoint(
          Collections.singletonList(diff));
      CommonCheckpointPayload payload = CommonCheckpointPayload.createV2(format,
          pathState(meta), descriptor, Collections.emptyList());
      CommonCheckpointPayloadCodec codec = new CommonCheckpointPayloadCodec();
      byte[] encoded = codec.encode(payload);

      assertEquals(CommonCheckpointPayload.COORDINATION_FORMAT_VERSION,
          ByteBuffer.wrap(encoded, Integer.BYTES, Short.BYTES).getShort());
      byte[] oldCoordinationVersion = Arrays.copyOf(encoded, encoded.length);
      ByteBuffer.wrap(oldCoordinationVersion).putShort(Integer.BYTES, (short) 2);
      assertThrows(IllegalArgumentException.class,
          () -> codec.decode(oldCoordinationVersion));
      assertFalse(contains(encoded, oldValueSentinel));
      CommonCheckpointPayload decoded = codec.decode(encoded);
      assertEquals(CommonCheckpointPayload.COORDINATION_FORMAT_VERSION,
          decoded.getVersion());
      assertEquals(descriptor, decoded.getArchiveBinding());
      assertArrayEquals(descriptor.getBlocks().get(0).getArchiveRecordDigest(),
          decoded.getBlocks().get(0).getArchiveRecordDigest());
      assertThrows(IllegalStateException.class,
          () -> decoded.getBlocks().get(0).getArchiveDiff());
      assertArrayEquals(codec.digest(payload), codec.digest(decoded));
      CommonCheckpointFile file = new CommonCheckpointFile(root.resolve("wal"));
      file.publish(payload);
      assertEquals(descriptor, file.loadRequired().getArchiveBinding());
      file.retire();

      BlockReverseDiff changedBody = new BlockReverseDiff(meta,
          Collections.singletonList(new DbGroup("code", Collections.singletonList(
              new Entry(new byte[]{1}, OldValue.absent())))));
      assertThrows(ArchivePersistenceException.class,
          () -> hotStore.prepareCheckpoint(hash(120), descriptor,
              Collections.singletonList(changedBody)));

      CommonCheckpointPayload v1 = CommonCheckpointPayload.create(format,
          pathState(meta), Collections.singletonList(diff), Collections.emptyList());
      byte[] encodedV1 = codec.encode(v1);
      assertEquals(CommonCheckpointPayload.FORMAT_VERSION,
          ByteBuffer.wrap(encodedV1, Integer.BYTES, Short.BYTES).getShort());
      assertTrue(codec.decode(encodedV1).getBlocks().get(0).getArchiveDiff()
          .getGroups().get(0).getEntries().get(0).getOldValue().isPresent());
    }
  }

  @Test
  public void roundTripsExplicitRocksEngineIdentity() throws Exception {
    byte[] format = hash(8);
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    BlockReverseDiff diff = new BlockReverseDiff(meta, Collections.emptyList());
    Path root = temporaryFolder.newFolder("payload-v2-rocks").toPath();
    try (StateArchiveHotStore hotStore = StateArchiveHotStore.openOrCreate(root, format,
        Engine.ROCKSDB, 0, hash(0), 3, 10, 1024 * 1024)) {
      StateArchiveHotBatchDescriptor descriptor = hotStore.planCheckpoint(
          Collections.singletonList(diff));
      CommonCheckpointPayload decoded = new CommonCheckpointPayloadCodec().decode(
          new CommonCheckpointPayloadCodec().encode(CommonCheckpointPayload.createV2(format,
              pathState(meta), descriptor, Collections.emptyList())));
      assertEquals(Engine.ROCKSDB, decoded.getArchiveBinding().getEngine());
    }
  }

  private static PathStateFlushTarget pathState(BlockSnapshotMeta meta) {
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(hash(5));
    when(binding.getStateRoot()).thenReturn(hash(6));
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(71));
    PathStateFlushTarget pathState = mock(PathStateFlushTarget.class);
    when(pathState.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(pathState.getParentStateRoot()).thenReturn(hash(5));
    when(pathState.getStateRoot()).thenReturn(hash(6));
    when(pathState.getStores()).thenReturn(Collections.emptyList());
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return pathState;
  }

  private static boolean contains(byte[] haystack, byte[] needle) {
    for (int offset = 0; offset <= haystack.length - needle.length; offset++) {
      boolean equal = true;
      for (int index = 0; index < needle.length; index++) {
        if (haystack[offset + index] != needle[index]) {
          equal = false;
          break;
        }
      }
      if (equal) {
        return true;
      }
    }
    return false;
  }

  private static byte[] hash(int marker) {
    byte[] hash = new byte[32];
    hash[31] = (byte) marker;
    return hash;
  }
}
