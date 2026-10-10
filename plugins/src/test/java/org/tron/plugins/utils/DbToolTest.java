package org.tron.plugins.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tron.plugins.utils.db.DbTool;
import org.tron.plugins.utils.db.DbTool.DbType;

public class DbToolTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testMissingMetadataDefaultsToLegacyEngineWithoutWriting() throws Exception {
    File directory = temporaryFolder.newFolder("database");
    File database = new File(directory, "legacy");
    Assert.assertTrue(database.mkdir());

    Assert.assertEquals(DbType.LevelDB, DbTool.getDbType(directory.toString(), "legacy"));
    Assert.assertEquals(0, database.list().length);
    Assert.assertEquals(DbType.LevelDB, DbTool.getDbType(directory.toString(), "missing"));
    Assert.assertFalse(new File(directory, "missing").exists());
  }

  @Test
  public void testRecognizesBothEnginesIgnoringCase() throws Exception {
    File directory = temporaryFolder.newFolder("database");
    Path database = Files.createDirectory(directory.toPath().resolve("store"));
    Path metadata = database.resolve(DBUtils.FILE_ENGINE);
    for (DbType type : DbType.values()) {
      byte[] content = ("ENGINE=" + type.name()).getBytes(StandardCharsets.UTF_8);
      Files.write(metadata, content);

      Assert.assertEquals(type, DbTool.getDbType(directory.toString(), "store"));
      Assert.assertArrayEquals(content, Files.readAllBytes(metadata));
    }
  }

  @Test
  public void testEmptyMissingAndUnknownEngineValuesDefaultToLegacyEngine() throws Exception {
    File directory = temporaryFolder.newFolder("database");
    Path database = Files.createDirectory(directory.toPath().resolve("store"));
    Path metadata = database.resolve(DBUtils.FILE_ENGINE);
    for (String content : new String[] {"", "OTHER=ROCKSDB", "ENGINE=", "ENGINE=UNKNOWN"}) {
      byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
      Files.write(metadata, bytes);

      Assert.assertEquals(DbType.LevelDB, DbTool.getDbType(directory.toString(), "store"));
      Assert.assertArrayEquals(bytes, Files.readAllBytes(metadata));
    }
  }

  @Test
  public void testUnreadableMetadataDefaultsToLegacyEngine() throws Exception {
    File directory = temporaryFolder.newFolder("database");
    Path database = Files.createDirectory(directory.toPath().resolve("store"));
    Files.createDirectory(database.resolve(DBUtils.FILE_ENGINE));

    Assert.assertEquals(DbType.LevelDB, DbTool.getDbType(directory.toString(), "store"));
    Assert.assertFalse(Files.exists(database.resolve("CURRENT")));
  }
}
