package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff;
import org.tron.core.db2.archive.BlockSnapshotMeta;
import org.tron.core.db2.stateroot.PathStateFlushTarget;

public class CommonCheckpointFileTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void publishesLoadsRetriesAndRetiresOneImmutablePayload() throws Exception {
    Path directory = temporaryFolder.getRoot().toPath().resolve("common-checkpoint");
    CommonCheckpointFile file = new CommonCheckpointFile(directory);
    CommonCheckpointPayload payload = payload(7);

    assertNull(file.loadIfPresent());
    file.publish(payload);
    assertTrue(Files.isRegularFile(file.getCheckpointPath()));
    assertFalse(Files.exists(file.getTemporaryPath()));
    assertArrayEquals(payload.getStateRoot(), file.loadRequired().getStateRoot());
    byte[] firstBytes = Files.readAllBytes(file.getCheckpointPath());

    file.publish(payload);
    assertArrayEquals(firstBytes, Files.readAllBytes(file.getCheckpointPath()));
    assertThrows(IOException.class, () -> file.publish(payload(8)));
    file.retire();
    assertNull(file.loadIfPresent());
    file.retire();
  }

  @Test
  public void retriesEveryPublishCrashBoundaryWithoutAcceptingTemporaryAuthority()
      throws Exception {
    for (CommonCheckpointFile.Stage stage : new CommonCheckpointFile.Stage[]{
        CommonCheckpointFile.Stage.AFTER_TEMPORARY_FORCE,
        CommonCheckpointFile.Stage.AFTER_ATOMIC_PUBLISH,
        CommonCheckpointFile.Stage.AFTER_DIRECTORY_FORCE}) {
      Path directory = temporaryFolder.getRoot().toPath().resolve("publish-" + stage);
      CommonCheckpointPayload payload = payload(7);
      CommonCheckpointFile interrupted = new CommonCheckpointFile(directory,
          CommonCheckpointPayloadCodec.DEFAULT_MAX_ENCODED_LENGTH, failAt(stage));
      assertThrows(IOException.class, () -> interrupted.publish(payload));

      CommonCheckpointFile recovered = new CommonCheckpointFile(directory);
      if (stage == CommonCheckpointFile.Stage.AFTER_TEMPORARY_FORCE) {
        assertNull(recovered.loadIfPresent());
        assertTrue(Files.isRegularFile(recovered.getTemporaryPath()));
      } else {
        assertNotNull(recovered.loadRequired());
      }
      recovered.publish(payload);
      assertNotNull(recovered.loadRequired());
      assertFalse(Files.exists(recovered.getTemporaryPath()));
    }
  }

  @Test
  public void rejectsCorruptOrOversizedFilesAndRecoversInterruptedRetire() throws Exception {
    Path directory = temporaryFolder.getRoot().toPath().resolve("retire");
    CommonCheckpointPayload payload = payload(7);
    CommonCheckpointFile file = new CommonCheckpointFile(directory);
    file.publish(payload);
    byte[] corrupt = Files.readAllBytes(file.getCheckpointPath());
    corrupt[corrupt.length - 1] ^= 1;
    Files.write(file.getCheckpointPath(), corrupt);
    assertThrows(IOException.class, file::loadRequired);

    file.retire();
    file.publish(payload);
    CommonCheckpointFile interrupted = new CommonCheckpointFile(directory,
        CommonCheckpointPayloadCodec.DEFAULT_MAX_ENCODED_LENGTH,
        failAt(CommonCheckpointFile.Stage.AFTER_RETIRE_DELETE));
    assertThrows(IOException.class, interrupted::retire);
    assertNull(new CommonCheckpointFile(directory).loadIfPresent());
    new CommonCheckpointFile(directory).retire();

    CommonCheckpointFile bounded = new CommonCheckpointFile(directory,
        CommonCheckpointPayloadCodec.HEADER_LENGTH + 1, (stage, path) -> { });
    assertThrows(IllegalArgumentException.class, () -> bounded.publish(payload));
  }

  private static CommonCheckpointFile.FaultHook failAt(CommonCheckpointFile.Stage expected) {
    return (actual, path) -> {
      if (actual == expected) {
        throw new IOException("injected " + expected);
      }
    };
  }

  private static CommonCheckpointPayload payload(int seed) {
    BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(1, hash(1), hash(0), 3_000L);
    PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
    when(binding.getMeta()).thenReturn(meta);
    when(binding.getParentStateRoot()).thenReturn(hash(5));
    when(binding.getStateRoot()).thenReturn(hash(6));
    when(binding.getTransitionPayloadDigest()).thenReturn(hash(3));
    PathStateFlushTarget target = mock(PathStateFlushTarget.class);
    when(target.getBlocks()).thenReturn(Collections.singletonList(binding));
    when(target.getParentStateRoot()).thenReturn(hash(5));
    when(target.getStateRoot()).thenReturn(hash(6));
    when(target.getStores()).thenReturn(Collections.emptyList());
    when(target.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    BlockReverseDiff archive = new BlockReverseDiff(meta, Collections.emptyList());
    return CommonCheckpointPayload.create(hash(seed), target,
        Collections.singletonList(archive), Collections.singletonList(
            new CommonCheckpointPayload.StoreMutations("code", Collections.singletonList(
                new CommonCheckpointPayload.Mutation(new byte[]{1}, new byte[]{2})))));
  }

  private static byte[] hash(int seed) {
    byte[] hash = new byte[32];
    for (int index = 0; index < hash.length; index++) {
      hash[index] = (byte) (seed + index);
    }
    return hash;
  }
}
