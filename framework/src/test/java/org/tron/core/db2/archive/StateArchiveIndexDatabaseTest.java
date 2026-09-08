package org.tron.core.db2.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.core.config.args.StorageConfig.NativeDbConfig;
import org.tron.core.db2.stateroot.PathStateStoreManifest.Engine;

public class StateArchiveIndexDatabaseTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void supportsConfiguredEngineAndRejectsEngineDrift() throws Exception {
    for (Engine engine : Engine.values()) {
      Path root = temporaryFolder.newFolder(engine.name().toLowerCase()).toPath();
      Path database = root.resolve("keys");
      StateArchiveIndexEngineManifest.openOrCreate(root, engine);
      assertEquals(12, Files.size(root.resolve(StateArchiveIndexEngineManifest.FILE)));
      try (StateArchiveIndexDatabase.Writer writer =
          StateArchiveIndexDatabase.openWriter(database, engine)) {
        writer.write(Arrays.asList(
            StateArchiveIndexDatabase.put(new byte[]{1}, new byte[]{11}),
            StateArchiveIndexDatabase.put(new byte[]{3}, new byte[]{33})));
      }
      try (StateArchiveIndexDatabase.Reader reader =
          StateArchiveIndexDatabase.openReader(database, engine)) {
        assertArrayEquals(new byte[]{11}, reader.get(new byte[]{1}));
        StateArchiveIndexDatabase.KeyValue found = reader.seek(new byte[]{2});
        assertNotNull(found);
        assertArrayEquals(new byte[]{3}, found.getKey());
        assertArrayEquals(new byte[]{33}, found.getValue());
      }
      Engine other = engine == Engine.LEVELDB ? Engine.ROCKSDB : Engine.LEVELDB;
      assertThrows(IOException.class, () -> StateArchiveIndexEngineManifest.require(root, other));
    }
  }

  @Test
  public void rocksReaderAndWriterShareConfiguredDatabaseHandle() throws Exception {
    Path database = temporaryFolder.newFolder("rocks-shared-handle").toPath().resolve("keys");
    try (StateArchiveIndexDatabase.Writer writer =
        StateArchiveIndexDatabase.openWriter(database, Engine.ROCKSDB)) {
      writer.write(Arrays.asList(StateArchiveIndexDatabase.put(new byte[]{1}, new byte[]{2})));
      try (StateArchiveIndexDatabase.Reader reader =
          StateArchiveIndexDatabase.openReader(database, Engine.ROCKSDB)) {
        assertArrayEquals(new byte[]{2}, reader.get(new byte[]{1}));
      }
    }

    String nativeOptions = new String(Files.readAllBytes(latestOptionsFile(database)),
        StandardCharsets.US_ASCII);
    assertTrue(nativeOptions.contains("write_buffer_size=67108864"));
    assertTrue(nativeOptions.contains("block_size=4096"));
    assertTrue(nativeOptions.contains("filter_policy=rocksdb.BuiltinBloomFilter"));
  }

  @Test
  public void hotRocksRejectsLegacySingleColumnFamilyLayout() throws Exception {
    Path database = temporaryFolder.newFolder("rocks-flat-hot-reject").toPath().resolve("keys");
    try (StateArchiveIndexDatabase.Writer writer =
        StateArchiveIndexDatabase.openWriter(database, Engine.ROCKSDB)) {
      writer.write(Arrays.asList(StateArchiveIndexDatabase.put(new byte[]{1}, new byte[]{2})));
    }

    assertThrows(IOException.class, () -> StateArchiveIndexDatabase.openHotWriter(
        database, Engine.ROCKSDB, NativeDbConfig.large()));
  }

  @Test
  public void rejectsExistingDatabaseWithoutEngineIdentity() throws Exception {
    Path root = temporaryFolder.newFolder("missing-manifest").toPath();
    Files.createDirectory(root.resolve("keys"));
    assertThrows(IOException.class,
        () -> StateArchiveIndexEngineManifest.openOrCreate(root, Engine.LEVELDB));
  }

  @Test
  public void acceptsPriorShaIdentityAndRejectsCrcCorruption() throws Exception {
    Path legacy = temporaryFolder.newFolder("legacy-sha").toPath();
    byte[] body = ByteBuffer.allocate(8).putInt(0x53414945).putShort((short) 1)
        .putShort((short) 2).array();
    Files.write(legacy.resolve(StateArchiveIndexEngineManifest.FILE),
        ByteBuffer.allocate(40).put(body).put(Hashing.sha256().hashBytes(body).asBytes()).array());
    assertEquals(Engine.ROCKSDB, StateArchiveIndexEngineManifest.load(legacy));

    Path current = temporaryFolder.newFolder("current-crc").toPath();
    StateArchiveIndexEngineManifest.openOrCreate(current, Engine.LEVELDB);
    byte[] corrupt = Files.readAllBytes(current.resolve(StateArchiveIndexEngineManifest.FILE));
    corrupt[7] = 2;
    Files.write(current.resolve(StateArchiveIndexEngineManifest.FILE), corrupt);
    assertThrows(IOException.class, () -> StateArchiveIndexEngineManifest.load(current));
  }

  private static Path latestOptionsFile(Path directory) throws IOException {
    try (Stream<Path> files = Files.list(directory)) {
      return files.filter(path -> path.getFileName().toString().startsWith("OPTIONS-"))
          .max(java.util.Comparator.comparing(path -> path.getFileName().toString()))
          .orElseThrow(() -> new IOException("RocksDB OPTIONS file is missing"));
    }
  }
}
