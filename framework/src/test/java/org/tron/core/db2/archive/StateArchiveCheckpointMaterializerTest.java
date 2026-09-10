package org.tron.core.db2.archive;

import static org.fusesource.leveldbjni.JniDBFactory.factory;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Status;
import org.tron.core.db2.core.CommonCheckpointPayload;
import org.tron.core.db2.core.CommonCheckpointTarget;
import org.tron.core.db2.stateroot.PathStateFlushTarget;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveCheckpointMaterializerTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void preservesEveryBlockBoundaryBeforePublishingReadableAcrossReopen()
      throws Exception {
    Path root = temporaryFolder.newFolder("normal").toPath();
    byte[] format = hash(90);
    CommonCheckpointPayload payload = payload(format, 1, 3, hash(0), hash(10), hash(13));
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    StateArchiveCheckpointMaterializer materializer =
        new StateArchiveCheckpointMaterializer(root, format, null, Engine.LEVELDB);

    assertEquals(Status.NEEDS_MATERIALIZATION, materializer.inspect(target));
    materializer.materialize(payload, target);
    assertEquals(Status.MATERIALIZED, materializer.inspect(target));
    assertFalse(Files.exists(root.resolve(StateArchiveCheckpointMaterializer.READABLE_FILE)));
    assertEquals(3, blockFileCount(root));
    assertThrows(IOException.class,
        () -> StateArchiveCheckpointReadAdapter.open(root, target, Engine.LEVELDB));
    for (int index = 0; index < payload.getBlocks().size(); index++) {
      BlockReverseDiff actual = materializer.loadBlock(target, index);
      assertEquals(payload.getBlocks().get(index).getMeta(), actual.getMeta());
      assertEquals("code", actual.getGroups().get(0).getDbName());
    }

    materializer.materialize(payload, target);
    assertEquals(3, blockFileCount(root));
    materializer.publish(target);
    assertEquals(Status.PUBLISHED, materializer.inspect(target));
    try (StateArchiveCheckpointReadAdapter reader =
        StateArchiveCheckpointReadAdapter.open(root, target, Engine.LEVELDB);
        StateArchiveCheckpointReadAdapter concurrent =
            StateArchiveCheckpointReadAdapter.open(root, target, Engine.LEVELDB)) {
      assertEquals(0, reader.getIndexedFrom());
      assertEquals(3, reader.getIndexedThrough());
      assertArrayEquals(new byte[]{0}, reader.findOldValueAfter("code", new byte[]{1}, 0)
          .get().getValue());
      assertFalse(reader.findOldValueAfter("code", new byte[]{2}, 2).isPresent());
      assertArrayEquals(new byte[]{0}, concurrent.findOldValueAfter("code", new byte[]{1}, 0)
          .get().getValue());
    }
    assertTrue(Files.isRegularFile(root.resolve(StateArchiveCheckpointServingIndex.DIRECTORY)
        .resolve(StateArchiveIndexEngineManifest.FILE)));
    StateArchiveCheckpointMaterializer wrongEngine = new StateArchiveCheckpointMaterializer(
        root, format, null, Engine.ROCKSDB);
    assertThrows(IOException.class, () -> wrongEngine.inspect(target));

    StateArchiveCheckpointMaterializer reopened =
        new StateArchiveCheckpointMaterializer(root, format, null, Engine.LEVELDB);
    assertEquals(Status.PUBLISHED, reopened.inspect(target));
    reopened.publish(target);

    CommonCheckpointPayload child = payload(format, 4, 2, hash(3), hash(13), hash(15));
    CommonCheckpointTarget childTarget = CommonCheckpointTarget.from(child);
    reopened.materialize(child, childTarget);
    reopened.publish(childTarget);
    assertEquals(Status.PUBLISHED, reopened.inspect(childTarget));
    try (StateArchiveCheckpointReadAdapter reader =
        StateArchiveCheckpointReadAdapter.open(root, childTarget, Engine.LEVELDB)) {
      assertEquals(0, reader.getIndexedFrom());
      assertEquals(5, reader.getIndexedThrough());
      assertArrayEquals(hash(5), reader.getHeadHash());
      assertArrayEquals(new byte[]{1}, reader.findOldValueAfter("code", new byte[]{2}, 0)
          .get().getValue());
      assertArrayEquals(new byte[]{3}, reader.findOldValueAfter("code", new byte[]{4}, 3)
          .get().getValue());
    }
    CommonCheckpointTarget restored =
        StateArchiveCheckpointMaterializer.loadPublishedTarget(root, format, Engine.LEVELDB);
    assertEquals(childTarget, restored);
    try (StateArchiveCheckpointReadAdapter reader =
        StateArchiveCheckpointReadAdapter.open(root, format, Engine.LEVELDB)) {
      assertEquals(5, reader.getIndexedThrough());
      assertArrayEquals(new byte[]{1}, reader.findOldValueAfter("code", new byte[]{2}, 0)
          .get().getValue());
    }
  }

  @Test
  public void reusesServingWriterAcrossTargetsAndClosesItWithMaterializer() throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder("writer-lifecycle-" + engine).toPath();
      byte[] format = hash(89);
      StateArchiveCheckpointMaterializer materializer =
          new StateArchiveCheckpointMaterializer(root, format, null, engine);

      CommonCheckpointPayload first = payload(format, 1, 2, hash(0), hash(10), hash(12));
      CommonCheckpointTarget firstTarget = CommonCheckpointTarget.from(first);
      materializer.beginCheckpoint(firstTarget);
      materializer.materialize(first, firstTarget);
      materializer.publish(firstTarget);
      materializer.endCheckpoint(firstTarget);
      assertEquals(1, StateArchiveCheckpointServingIndex.openReferenceCount(root, engine));

      CommonCheckpointPayload second = payload(format, 3, 2, hash(2), hash(12), hash(14));
      CommonCheckpointTarget secondTarget = CommonCheckpointTarget.from(second);
      materializer.beginCheckpoint(secondTarget);
      materializer.materialize(second, secondTarget);
      materializer.publish(secondTarget);
      materializer.endCheckpoint(secondTarget);
      assertEquals(1, StateArchiveCheckpointServingIndex.openReferenceCount(root, engine));

      materializer.close();
      materializer.close();
      assertEquals(0, StateArchiveCheckpointServingIndex.openReferenceCount(root, engine));
      assertThrows(IOException.class, () -> materializer.inspect(secondTarget));
    }
  }

  @Test
  public void releasesRetainedWriterAfterCheckpointFailure() throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder("writer-failure-" + engine).toPath();
      byte[] format = hash(88);
      CommonCheckpointPayload payload = payload(format, 1, 2, hash(0), hash(10), hash(12));
      CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
      StateArchiveCheckpointMaterializer materializer = new StateArchiveCheckpointMaterializer(
          root, format, engine,
          failAt(StateArchiveCheckpointMaterializer.Stage.AFTER_SERVING_INDEX_BATCH));

      materializer.beginCheckpoint(target);
      assertThrows(IOException.class, () -> materializer.materialize(payload, target));
      assertEquals(1, StateArchiveCheckpointServingIndex.openReferenceCount(root, engine));
      materializer.endCheckpoint(target);
      materializer.close();
      assertEquals(0, StateArchiveCheckpointServingIndex.openReferenceCount(root, engine));
    }
  }

  @Test
  public void resumesEveryDurabilityBoundaryUsingOnlyCheckpointRedo() throws Exception {
    for (StateArchiveCheckpointMaterializer.Stage stage
        : StateArchiveCheckpointMaterializer.Stage.values()) {
      Path root = temporaryFolder.newFolder("fault-" + stage).toPath();
      byte[] format = hash(91);
      CommonCheckpointPayload payload = payload(format, 8, 3, hash(7), hash(20), hash(23));
      CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
      StateArchiveCheckpointMaterializer failed = new StateArchiveCheckpointMaterializer(root,
          format, failAt(stage));
      if (stage == StateArchiveCheckpointMaterializer.Stage.AFTER_READABLE) {
        failed.materialize(payload, target);
        assertThrows(IOException.class, () -> failed.publish(target));
      } else {
        assertThrows(IOException.class, () -> failed.materialize(payload, target));
      }

      StateArchiveCheckpointMaterializer recovered =
          new StateArchiveCheckpointMaterializer(root, format);
      if (recovered.inspect(target) == Status.NEEDS_MATERIALIZATION) {
        recovered.materialize(payload, target);
      }
      recovered.publish(target);
      assertEquals(Status.PUBLISHED, recovered.inspect(target));
      assertEquals(3, blockFileCount(root));
    }
  }

  @Test
  public void rejectsForeignFormatCorruptImmutableBlockAndNonParentReadable()
      throws Exception {
    Path root = temporaryFolder.newFolder("reject").toPath();
    byte[] format = hash(92);
    CommonCheckpointPayload payload = payload(format, 1, 2, hash(0), hash(30), hash(32));
    CommonCheckpointTarget target = CommonCheckpointTarget.from(payload);
    StateArchiveCheckpointMaterializer materializer =
        new StateArchiveCheckpointMaterializer(root, format, null, Engine.LEVELDB);

    CommonCheckpointPayload foreign = payload(hash(99), 1, 1, hash(0), hash(30), hash(31));
    assertThrows(IOException.class, () -> materializer.materialize(foreign,
        CommonCheckpointTarget.from(foreign)));

    StateArchiveCheckpointMaterializer interrupted = new StateArchiveCheckpointMaterializer(root,
        format, Engine.LEVELDB,
        failAt(StateArchiveCheckpointMaterializer.Stage.AFTER_BLOCK_FILE));
    assertThrows(IOException.class, () -> interrupted.materialize(payload, target));
    Path block = firstBlockFile(root);
    byte[] corrupt = Files.readAllBytes(block);
    corrupt[corrupt.length - 1] ^= 1;
    Files.write(block, corrupt);
    assertThrows(IOException.class, () -> materializer.materialize(payload, target));

    Path cleanRoot = temporaryFolder.newFolder("non-parent").toPath();
    StateArchiveCheckpointMaterializer clean =
        new StateArchiveCheckpointMaterializer(cleanRoot, format, null, Engine.LEVELDB);
    clean.materialize(payload, target);
    clean.publish(target);
    CommonCheckpointPayload nonChild = payload(format, 5, 1, hash(9), hash(40), hash(41));
    assertThrows(IOException.class,
        () -> clean.inspect(CommonCheckpointTarget.from(nonChild)));
    assertTrue(Files.isRegularFile(cleanRoot.resolve(
        StateArchiveCheckpointMaterializer.READABLE_FILE)));
    Options options = new Options().createIfMissing(false);
    try (DB database = factory.open(cleanRoot.resolve(
        StateArchiveCheckpointServingIndex.DIRECTORY).resolve("keys").toFile(), options)) {
      database.put(new byte[]{0}, new byte[]{1});
    }
    assertThrows(IOException.class, () -> clean.inspect(target));
  }

  private static StateArchiveCheckpointMaterializer.FaultHook failAt(
      StateArchiveCheckpointMaterializer.Stage failedStage) {
    return (stage, blockIndex) -> {
      if (stage == failedStage) {
        throw new IOException("injected " + stage + " at " + blockIndex);
      }
    };
  }

  private static CommonCheckpointPayload payload(byte[] format, long firstBlock, int count,
      byte[] parentHash, byte[] parentRoot, byte[] stateRoot) {
    List<PathStateFlushTarget.BlockBinding> bindings = new ArrayList<>();
    List<BlockReverseDiff> archives = new ArrayList<>();
    byte[] priorHash = parentHash;
    byte[] priorRoot = parentRoot;
    for (int index = 0; index < count; index++) {
      long number = firstBlock + index;
      byte[] blockHash = hash((int) number);
      byte[] nextRoot = index == count - 1 ? stateRoot : hash(30 + (int) number);
      BlockSnapshotMeta meta = BlockSnapshotMeta.forBlock(number, blockHash, priorHash,
          number * 3_000L);
      PathStateFlushTarget.BlockBinding binding = mock(PathStateFlushTarget.BlockBinding.class);
      when(binding.getMeta()).thenReturn(meta);
      when(binding.getParentStateRoot()).thenReturn(priorRoot);
      when(binding.getStateRoot()).thenReturn(nextRoot);
      when(binding.getTransitionPayloadDigest()).thenReturn(hash(70 + (int) number));
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
    when(pathState.getParentStateRoot()).thenReturn(parentRoot);
    when(pathState.getStateRoot()).thenReturn(stateRoot);
    when(pathState.getStores()).thenReturn(Collections.emptyList());
    when(pathState.getSuperNodeMutations()).thenReturn(Collections.emptyList());
    return CommonCheckpointPayload.create(format, pathState, archives,
        Collections.emptyList());
  }

  private static int blockFileCount(Path root) throws IOException {
    Path targets = root.resolve(StateArchiveCheckpointMaterializer.TARGET_DIRECTORY);
    try (DirectoryStream<Path> targetPaths = Files.newDirectoryStream(targets)) {
      for (Path target : targetPaths) {
        Path blocks = target.resolve(StateArchiveCheckpointMaterializer.BLOCK_DIRECTORY);
        if (Files.isDirectory(blocks)) {
          int count = 0;
          try (DirectoryStream<Path> paths = Files.newDirectoryStream(blocks, "*.diff")) {
            for (Path ignored : paths) {
              count++;
            }
          }
          return count;
        }
      }
    }
    return 0;
  }

  private static Path firstBlockFile(Path root) throws IOException {
    Path targets = root.resolve(StateArchiveCheckpointMaterializer.TARGET_DIRECTORY);
    try (DirectoryStream<Path> targetPaths = Files.newDirectoryStream(targets)) {
      for (Path target : targetPaths) {
        Path blocks = target.resolve(StateArchiveCheckpointMaterializer.BLOCK_DIRECTORY);
        if (Files.isDirectory(blocks)) {
          try (DirectoryStream<Path> paths = Files.newDirectoryStream(blocks, "*.diff")) {
            for (Path path : paths) {
              return path;
            }
          }
        }
      }
    }
    throw new IOException("checkpoint block not found");
  }

  private static byte[] hash(int seed) {
    byte[] hash = new byte[32];
    for (int index = 0; index < hash.length; index++) {
      hash[index] = (byte) (seed + index);
    }
    return hash;
  }
}
