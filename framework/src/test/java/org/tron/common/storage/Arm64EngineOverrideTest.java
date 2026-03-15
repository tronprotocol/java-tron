package org.tron.common.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mockStatic;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import java.io.IOException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.tron.common.TestEnv;
import org.tron.common.arch.Arch;
import org.tron.core.config.args.Args;
import org.tron.core.config.args.Storage;
import org.tron.core.exception.TronError;

public class Arm64EngineOverrideTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @After
  public void cleanup() {
    Args.clearParam();
  }

  @Test
  public void testGetDbEngineFromConfigReturnsValue() {
    Config leveldb = ConfigFactory.empty()
        .withValue("storage.db.engine", ConfigValueFactory.fromAnyRef("LEVELDB"));
    assertEquals("LEVELDB", Storage.getDbEngineFromConfig(leveldb));

    Config rocksdb = ConfigFactory.empty()
        .withValue("storage.db.engine", ConfigValueFactory.fromAnyRef("ROCKSDB"));
    assertEquals("ROCKSDB", Storage.getDbEngineFromConfig(rocksdb));

    Config empty = ConfigFactory.empty();
    assertEquals("LEVELDB", Storage.getDbEngineFromConfig(empty));
  }

  @Test
  public void testArm64RejectsLevelDbCli() throws IOException {
    try (MockedStatic<Arch> mocked = mockStatic(Arch.class)) {
      mocked.when(Arch::isArm64).thenReturn(true);

      Args.setParam(new String[]{
          "--output-directory", temporaryFolder.newFolder().toString(),
          "--storage-db-engine", "LEVELDB"
      }, TestEnv.TEST_CONF);

      TronError error = assertThrows(TronError.class, Args::validateConfig);
      assertEquals(TronError.ErrCode.PARAMETER_INIT, error.getErrCode());
    }
  }

  @Test
  public void testArm64RejectsLevelDbConfigDefault() throws IOException {
    try (MockedStatic<Arch> mocked = mockStatic(Arch.class)) {
      mocked.when(Arch::isArm64).thenReturn(true);

      // config-test.conf has db.engine=LEVELDB
      Args.setParam(new String[]{
          "--output-directory", temporaryFolder.newFolder().toString()
      }, TestEnv.TEST_CONF);

      TronError error = assertThrows(TronError.class, Args::validateConfig);
      assertEquals(TronError.ErrCode.PARAMETER_INIT, error.getErrCode());
    }
  }

  @Test
  public void testArm64AcceptsRocksDb() throws IOException {
    try (MockedStatic<Arch> mocked = mockStatic(Arch.class)) {
      mocked.when(Arch::isArm64).thenReturn(true);

      Args.setParam(new String[]{
          "--output-directory", temporaryFolder.newFolder().toString(),
          "--storage-db-engine", "ROCKSDB"
      }, TestEnv.DBBACKUP_CONF);

      Args.validateConfig(); // should not throw
      assertEquals("ROCKSDB",
          Args.getInstance().getStorage().getDbEngine());
    }
  }

  @Test
  public void testX86AllowsLevelDb() throws IOException {
    try (MockedStatic<Arch> mocked = mockStatic(Arch.class)) {
      mocked.when(Arch::isArm64).thenReturn(false);

      Args.setParam(new String[]{
          "--output-directory", temporaryFolder.newFolder().toString()
      }, TestEnv.TEST_CONF);

      Args.validateConfig(); // should not throw
      assertEquals("LEVELDB",
          Args.getInstance().getStorage().getDbEngine());
    }
  }

  @Test
  public void testX86AllowsRocksDb() throws IOException {
    try (MockedStatic<Arch> mocked = mockStatic(Arch.class)) {
      mocked.when(Arch::isArm64).thenReturn(false);

      Args.setParam(new String[]{
          "--output-directory", temporaryFolder.newFolder().toString(),
          "--storage-db-engine", "ROCKSDB"
      }, TestEnv.TEST_CONF);

      Args.validateConfig(); // should not throw
      assertEquals("ROCKSDB",
          Args.getInstance().getStorage().getDbEngine());
    }
  }
}
