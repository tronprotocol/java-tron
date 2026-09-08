package org.tron.core.db2.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.core.CommonCheckpointMaterializer.Authority;

public class CommonCheckpointMaterializedStoreTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void atomicallyRotatesOneSlotPerAuthorityWithoutHistoryGrowth() throws Exception {
    Path root = temporaryFolder.newFolder("materialized").toPath();
    CommonCheckpointMaterializedStore store = new CommonCheckpointMaterializedStore(root);

    for (int target = 0; target < 100; target++) {
      for (Authority authority : Authority.values()) {
        byte[] encoded = record(target, authority);
        store.replace(authority, encoded);
        store.replace(authority, encoded);
        assertTrue(store.matches(authority, encoded));
      }
    }

    try (Stream<Path> paths = Files.list(root.resolve(
        CommonCheckpointMaterializedStore.DIRECTORY))) {
      assertEquals(Authority.values().length, paths.count());
    }
    for (Authority authority : Authority.values()) {
      assertArrayEquals(record(99, authority), Files.readAllBytes(store.path(authority)));
      assertFalse(store.matches(authority, record(98, authority)));
    }
  }

  @Test
  public void resumesEveryAtomicReplaceBoundaryWithoutGrowingTemporaryFiles() throws Exception {
    for (CommonCheckpointMaterializedStore.Stage failedStage
        : CommonCheckpointMaterializedStore.Stage.values()) {
      Path root = temporaryFolder.newFolder("fault-" + failedStage).toPath();
      CommonCheckpointMaterializedStore original = new CommonCheckpointMaterializedStore(root);
      original.replace(Authority.CHAINBASE, record(1, Authority.CHAINBASE));
      CommonCheckpointMaterializedStore interrupted = new CommonCheckpointMaterializedStore(root,
          (stage, path) -> {
            if (stage == failedStage) {
              throw new IOException("injected " + stage);
            }
          });

      assertThrows(IOException.class,
          () -> interrupted.replace(Authority.CHAINBASE, record(2, Authority.CHAINBASE)));
      CommonCheckpointMaterializedStore recovered = new CommonCheckpointMaterializedStore(root);
      recovered.replace(Authority.CHAINBASE, record(2, Authority.CHAINBASE));
      assertTrue(recovered.matches(Authority.CHAINBASE, record(2, Authority.CHAINBASE)));
      try (Stream<Path> paths = Files.list(root.resolve(
          CommonCheckpointMaterializedStore.DIRECTORY))) {
        assertEquals(1L, paths.count());
      }
    }
  }

  private static byte[] record(int target, Authority authority) {
    return new byte[]{(byte) target, (byte) authority.ordinal()};
  }
}
