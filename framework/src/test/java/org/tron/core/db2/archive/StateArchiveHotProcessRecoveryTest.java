package org.tron.core.db2.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.core.CommonCheckpointFile;
import org.tron.core.db2.core.CommonCheckpointHotRecovery;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

/** Verifies recovery after an actual JVM halt, not only an injected in-process exception. */
public class StateArchiveHotProcessRecoveryTest {

  private static final int HALT_CODE = 91;
  private static final byte[] FORMAT = hash(70);
  private static final byte[] BASE_HASH = hash(0);
  private static final byte[] TARGET = hash(80);

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void jvmHaltAfterHotPrepareBeforeCommonWalTruncatesOrphanOnRestart()
      throws Exception {
    for (Engine engine : Engine.values()) {
      assertHaltRecovery(engine);
    }
  }

  private void assertHaltRecovery(Engine engine) throws Exception {
    Path root = temporaryFolder.newFolder("hot-process-recovery-" + engine.name()).toPath();
    Process child = new ProcessBuilder(javaExecutable(), "-cp", runtimeClasspath(),
        StateArchiveHotProcessRecoveryTest.class.getName(), "halt-after-prepare",
        root.toString(), engine.name()).redirectErrorStream(true)
        .redirectOutput(root.resolve("halt-after-prepare.log").toFile()).start();
    assertTrue("child process timed out", child.waitFor(30, TimeUnit.SECONDS));
    assertEquals(HALT_CODE, child.exitValue());

    Path hotRoot = root.resolve("hot");
    assertPreparedBatch(hotRoot, engine);
    try (StateArchiveHotStore store = open(hotRoot, engine)) {
      assertEquals(StateArchiveHotStore.HotCheckpointStatus.MATERIALIZED,
          store.inspectCheckpoint(TARGET));
      assertEquals(0L, store.getCommittedHead());
      assertEquals(1L, store.getMaterializedHead());
      CommonCheckpointHotRecovery recovery = new CommonCheckpointHotRecovery(
          new CommonCheckpointFile(root.resolve("common")),
          () -> new CommonCheckpointHotRecovery.PersistentDynamicHead(0, BASE_HASH),
          ignored -> meta(0, 0), store::reconcilePreparedTail);
      CommonCheckpointHotRecovery.Result result = recovery.reconcileBeforeCommonRedo();
      assertEquals(CommonCheckpointHotRecovery.Source.PERSISTED_DYNAMIC, result.getSource());
      assertEquals(1L, result.getRemovedBlocks());
      assertEquals(0L, store.getMaterializedHead());
      assertEquals(StateArchiveHotStore.HotCheckpointStatus.NEEDS_MATERIALIZATION,
          store.inspectCheckpoint(TARGET));
      assertThrows(ArchivePersistenceException.class, () -> store.loadBlock(1));
    }

    try (StateArchiveHotStore reopened = open(hotRoot, engine)) {
      assertEquals(0L, reopened.getCommittedHead());
      assertEquals(0L, reopened.getMaterializedHead());
      assertEquals(StateArchiveHotStore.HotCheckpointStatus.NEEDS_MATERIALIZATION,
          reopened.inspectCheckpoint(TARGET));
    }
  }

  /** Child-process entry point used to leave only the native-sync Hot PREPARED authority. */
  public static void main(String[] args) throws Exception {
    if (args.length != 3 || !"halt-after-prepare".equals(args[0])) {
      throw new IllegalArgumentException("unknown process recovery mode");
    }
    StateArchiveHotStore store = open(Paths.get(args[1]).resolve("hot"),
        Engine.valueOf(args[2]));
    store.prepareCheckpoint(TARGET, Collections.singletonList(diff(1, 0)));
    Runtime.getRuntime().halt(HALT_CODE);
  }

  private static StateArchiveHotStore open(Path root, Engine engine) throws Exception {
    return StateArchiveHotStore.openOrCreate(root, FORMAT, engine, 0, BASE_HASH,
        3, 10, 1024 * 1024);
  }

  private static void assertPreparedBatch(Path hotRoot, Engine engine) throws Exception {
    if (engine != Engine.ROCKSDB) {
      return;
    }
    Path database = hotRoot.resolve(StateArchiveHotStore.GENERATIONS)
        .resolve("00000000000000000000").resolve(StateArchiveHotStore.DATABASE);
    try (StateArchiveIndexDatabase.Reader reader = StateArchiveIndexDatabase.openHotReader(
        database, engine, org.tron.core.config.args.StorageConfig.NativeDbConfig.large())) {
      assertEquals(29, StateArchiveIndexDatabase.hotColumnFamilies().size());
      org.junit.Assert.assertArrayEquals(TARGET, reader.get(
          "meta/prepared-target".getBytes(StandardCharsets.US_ASCII)));
      org.junit.Assert.assertNotNull(reader.get(
          StateArchiveIndexDatabase.HOT_BLOCKS_COLUMN_FAMILY,
          ByteBuffer.allocate(1 + Long.BYTES).put((byte) 0x42).putLong(1).array()));
      for (String store : Arrays.asList("account", "code", "storage-row")) {
        org.junit.Assert.assertNotNull(reader.get(
            StateArchiveIndexDatabase.hotStoreColumnFamily(store),
            hotIndexKey(store, new byte[]{(byte) store.length()}, 1)));
      }
    }
  }

  private static BlockReverseDiff diff(long block, int parent) {
    return new BlockReverseDiff(meta(block, parent), Arrays.asList(
        group("account"), group("code"), group("storage-row")));
  }

  private static DbGroup group(String store) {
    return new DbGroup(store, Collections.singletonList(
        new Entry(new byte[]{(byte) store.length()}, OldValue.absent())));
  }

  private static byte[] hotIndexKey(String store, byte[] key, long block) {
    byte[] storeBytes = store.getBytes(StandardCharsets.UTF_8);
    return ByteBuffer.allocate(1 + Short.BYTES + storeBytes.length + Integer.BYTES + key.length
        + Long.BYTES).put((byte) 0x4b).putShort((short) storeBytes.length).put(storeBytes)
        .putInt(key.length).put(key).putLong(block).array();
  }

  private static BlockSnapshotMeta meta(long block, int parent) {
    return BlockSnapshotMeta.forBlock(block, hash((int) block), hash(parent), block * 3_000L);
  }

  private static String javaExecutable() {
    return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
  }

  private static String runtimeClasspath() {
    Set<String> entries = new LinkedHashSet<>();
    String configured = System.getProperty("java.class.path", "");
    Collections.addAll(entries, configured.split(java.util.regex.Pattern.quote(
        File.pathSeparator)));
    for (ClassLoader loader = StateArchiveHotProcessRecoveryTest.class.getClassLoader();
        loader != null; loader = loader.getParent()) {
      if (loader instanceof URLClassLoader) {
        for (URL url : ((URLClassLoader) loader).getURLs()) {
          if ("file".equals(url.getProtocol())) {
            try {
              entries.add(Paths.get(url.toURI()).toString());
            } catch (java.net.URISyntaxException invalid) {
              throw new IllegalStateException("invalid test runtime classpath", invalid);
            }
          }
        }
      }
    }
    return String.join(File.pathSeparator, entries);
  }

  private static byte[] hash(int marker) {
    byte[] hash = new byte[32];
    hash[31] = (byte) marker;
    return hash;
  }
}
