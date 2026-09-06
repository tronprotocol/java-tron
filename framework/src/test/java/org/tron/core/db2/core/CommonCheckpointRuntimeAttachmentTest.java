package org.tron.core.db2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class CommonCheckpointRuntimeAttachmentTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void everyRuntimeConstructorRequiresMemoryRebaser() {
    assertTrue(Arrays.stream(CommonCheckpointRuntime.class.getConstructors())
        .allMatch(constructor -> Arrays.asList(constructor.getParameterTypes())
            .contains(CommonCheckpointMemoryRebaser.class)));
  }

  @Test
  public void disabledAttachmentDoesNotConstructRuntimeOrCreateDirectory() throws Exception {
    Path root = temporaryFolder.getRoot().toPath().resolve("disabled");
    AtomicBoolean invoked = new AtomicBoolean();
    CommonCheckpointRuntimeAttachment attachment = CommonCheckpointRuntimeAttachment.open(false,
        () -> {
          invoked.set(true);
          Files.createDirectories(root);
          return runtime(root, mock(Chainbase.class));
        });

    assertFalse(invoked.get());
    assertFalse(Files.exists(root));
    assertFalse(attachment.isEnabled());
    assertEquals(CommonCheckpointRuntimeAttachment.State.DISABLED, attachment.getState());
    assertThrows(IllegalStateException.class, () -> attachment.checkpointAndRebase(1));
    assertThrows(IllegalStateException.class, () -> attachment.pinPoint(0));
    attachment.close();
    assertEquals(CommonCheckpointRuntimeAttachment.State.CLOSED, attachment.getState());
    assertFalse(Files.exists(root));
  }

  @Test
  public void enabledAttachmentRecoversBeforeReadyAndOwnsClose() throws Exception {
    Path root = temporaryFolder.getRoot().toPath().resolve("enabled");
    CommonCheckpointRuntime runtime = runtime(root, mock(Chainbase.class));
    CommonCheckpointRuntimeAttachment attachment = CommonCheckpointRuntimeAttachment.open(true,
        () -> runtime);

    assertTrue(attachment.isEnabled());
    assertEquals(CommonCheckpointRuntimeAttachment.State.READY, attachment.getState());
    assertEquals(CommonCheckpointRuntimeOwner.State.READY, runtime.getState());
    assertThrows(IOException.class, () -> attachment.pinPoint(0));
    assertEquals(CommonCheckpointRuntimeAttachment.State.READY, attachment.getState());
    attachment.close();
    attachment.close();
    assertEquals(CommonCheckpointRuntimeAttachment.State.CLOSED, attachment.getState());
    assertEquals(CommonCheckpointRuntimeOwner.State.CLOSED, runtime.getState());
  }

  @Test
  public void startupAndCheckpointFailuresRemainFailClosed() throws Exception {
    Path corruptRoot = temporaryFolder.getRoot().toPath().resolve("corrupt");
    Path wal = corruptRoot.resolve("wal");
    Files.createDirectories(wal);
    Files.write(wal.resolve(CommonCheckpointFile.FILE_NAME), new byte[]{1});
    CommonCheckpointRuntime corrupt = runtime(corruptRoot, mock(Chainbase.class));
    assertThrows(IOException.class,
        () -> CommonCheckpointRuntimeAttachment.open(true, () -> corrupt));
    assertEquals(CommonCheckpointRuntimeOwner.State.CLOSED, corrupt.getState());

    Path publishedRoot = temporaryFolder.getRoot().toPath().resolve("published-failure");
    Path archive = publishedRoot.resolve("archive");
    Files.createDirectories(archive);
    Files.write(archive.resolve("READABLE"), new byte[]{1});
    CommonCheckpointMaterializer startupChainbase = materializer(Authority.CHAINBASE);
    CommonCheckpointMaterializer startupPathState = materializer(Authority.PATH_STATE);
    CommonCheckpointMaterializer startupStateArchive = materializer(Authority.STATE_ARCHIVE);
    CommonCheckpointRuntime published = runtime(publishedRoot, mock(Chainbase.class),
        startupChainbase, startupPathState, startupStateArchive);
    assertThrows(IOException.class,
        () -> CommonCheckpointRuntimeAttachment.open(true, () -> published));
    verify(startupChainbase).close();
    verify(startupPathState).close();
    verify(startupStateArchive).close();
    assertEquals(CommonCheckpointRuntimeOwner.State.CLOSED, published.getState());

    Path failureRoot = temporaryFolder.getRoot().toPath().resolve("checkpoint-failure");
    Chainbase database = mock(Chainbase.class);
    when(database.getHead()).thenThrow(new IllegalStateException("injected capture failure"));
    CommonCheckpointMaterializer chainbase = materializer(Authority.CHAINBASE);
    CommonCheckpointMaterializer pathState = materializer(Authority.PATH_STATE);
    CommonCheckpointMaterializer stateArchive = materializer(Authority.STATE_ARCHIVE);
    CommonCheckpointRuntimeAttachment attachment = CommonCheckpointRuntimeAttachment.open(true,
        () -> runtime(failureRoot, database, chainbase, pathState, stateArchive));
    assertThrows(IllegalStateException.class, () -> attachment.checkpointAndRebase(1));
    assertEquals(CommonCheckpointRuntimeAttachment.State.FAILED, attachment.getState());
    verify(chainbase).close();
    verify(pathState).close();
    verify(stateArchive).close();
    assertThrows(IllegalStateException.class, () -> attachment.pinPoint(0));
    attachment.close();
    assertEquals(CommonCheckpointRuntimeAttachment.State.CLOSED, attachment.getState());
  }

  private static CommonCheckpointRuntime runtime(Path root, Chainbase database) {
    return runtime(root, database, materializer(Authority.CHAINBASE),
        materializer(Authority.PATH_STATE), materializer(Authority.STATE_ARCHIVE));
  }

  private static CommonCheckpointRuntime runtime(Path root, Chainbase database,
      CommonCheckpointMaterializer chainbase, CommonCheckpointMaterializer pathState,
      CommonCheckpointMaterializer stateArchive) {
    CommonCheckpointRedoCoordinator coordinator = new CommonCheckpointRedoCoordinator(
        new CommonCheckpointFile(root.resolve("wal")), chainbase, pathState, stateArchive);
    return new CommonCheckpointRuntime(new CommonCheckpointRuntimeOwner(coordinator),
        Collections.singletonList(database), root.resolve("archive"), hash(1), Engine.LEVELDB,
        (blockNumber, blockHash) -> {
          throw new IOException("latest state is intentionally unavailable");
        }, target -> () -> { });
  }

  private static CommonCheckpointMaterializer materializer(Authority authority) {
    CommonCheckpointMaterializer materializer = mock(CommonCheckpointMaterializer.class);
    when(materializer.authority()).thenReturn(authority);
    return materializer;
  }

  private static byte[] hash(int seed) {
    byte[] value = new byte[32];
    for (int index = 0; index < value.length; index++) {
      value[index] = (byte) (seed + index);
    }
    return value;
  }
}
