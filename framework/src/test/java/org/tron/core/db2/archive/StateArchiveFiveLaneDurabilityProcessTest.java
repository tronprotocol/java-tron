package org.tron.core.db2.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.db2.archive.BlockReverseDiff.DbGroup;
import org.tron.core.db2.archive.BlockReverseDiff.Entry;
import org.tron.core.db2.archive.StateArchiveFiveLaneBlockCodecV3.EncodedBundle;
import org.tron.core.db2.archive.StateArchiveFiveLaneRecoveryIntentV3.RecoveryPoint;
import org.tron.core.db2.archive.StateArchiveFiveLaneSegmentWriterV3.ArchiveDurabilityProof;

/** Process boundary for marker force: the child never closes the writer normally. */
public class StateArchiveFiveLaneDurabilityProcessTest {

  private static final int HALT_CODE = 92;
  private static final byte[] BASELINE = hash(20);
  private static final byte[] COMMON_TARGET = hash(90);

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void jvmHaltAfterFiveLaneForceLeavesRestartVerifiableProof() throws Exception {
    Path root = temporaryFolder.newFolder("five-lane-durability-process").toPath();
    Process child = new ProcessBuilder(javaExecutable(), "-cp", runtimeClasspath(),
        StateArchiveFiveLaneDurabilityProcessTest.class.getName(), "halt-after-sync",
        root.toString()).redirectErrorStream(true)
        .redirectOutput(root.resolve("halt-after-sync.log").toFile()).start();
    assertTrue("child process timed out", child.waitFor(30, TimeUnit.SECONDS));
    assertEquals(HALT_CODE, child.exitValue());

    try (StateArchiveFiveLaneSegmentWriterV3 reopened =
        new StateArchiveFiveLaneSegmentWriterV3(root, BASELINE,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000)) {
      ArchiveDurabilityProof proof = reopened.getLastDurabilityProof();
      assertNotNull(proof);
      assertEquals(1, proof.getCheckpointSequence());
      assertEquals(2, proof.getTarget().getBlockNumber());
      assertEquals(5, proof.getFileTails().size());
      reopened.verifyDurabilityProof(proof);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !"halt-after-sync".equals(args[0])) {
      throw new IllegalArgumentException("unknown five-lane durability process mode");
    }
    Path root = Paths.get(args[1]);
    StateArchiveFiveLaneBlockCodecV3 codec = new StateArchiveFiveLaneBlockCodecV3();
    EncodedBundle first = codec.encode(diff(1), BASELINE,
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    EncodedBundle second = codec.encode(diff(2), first.getResultHistoryDigest(),
        StateArchiveFileFormatV3.COMPRESSION_NONE);
    StateArchiveFiveLaneSegmentWriterV3 writer =
        new StateArchiveFiveLaneSegmentWriterV3(root, BASELINE,
            StateArchiveFileFormatV3.COMPRESSION_NONE, 10_000);
    writer.append(first);
    writer.append(second);
    writer.sync(1, point(second), COMMON_TARGET);
    Runtime.getRuntime().halt(HALT_CODE);
  }

  private static BlockReverseDiff diff(int blockNumber) {
    byte[] value = new byte[10];
    java.util.Arrays.fill(value, (byte) blockNumber);
    DbGroup group = new DbGroup(StateArchiveFileFormatV3.dbName(1),
        Collections.singletonList(new Entry(new byte[]{1}, OldValue.present(value))));
    return new BlockReverseDiff(new BlockSnapshotMeta(blockNumber, blockNumber,
        hash(blockNumber), hash(blockNumber - 1), blockNumber * 3_000L),
        Collections.singletonList(group));
  }

  private static RecoveryPoint point(EncodedBundle bundle) {
    BlockSnapshotMeta meta = bundle.getDiff().getMeta();
    return new RecoveryPoint(meta.getEpoch(), meta.getBlockNumber(), meta.getTimestamp(),
        meta.getBlockHash(), meta.getParentHash(), bundle.getResultHistoryDigest());
  }

  private static String javaExecutable() {
    return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
  }

  private static String runtimeClasspath() {
    Set<String> entries = new LinkedHashSet<>();
    String configured = System.getProperty("java.class.path", "");
    Collections.addAll(entries, configured.split(java.util.regex.Pattern.quote(
        File.pathSeparator)));
    for (ClassLoader loader = StateArchiveFiveLaneDurabilityProcessTest.class.getClassLoader();
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

  private static byte[] hash(int suffix) {
    byte[] result = new byte[32];
    result[31] = (byte) suffix;
    return result;
  }
}
