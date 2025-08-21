package org.tron.common.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.tron.core.db.common.DbSourceInter.ENGINE_FILE;
import static org.tron.core.db.common.DbSourceInter.ENGINE_KEY;
import static org.tron.core.db.common.DbSourceInter.LEVELDB;
import static org.tron.core.db.common.DbSourceInter.ROCKSDB;
import static org.tron.core.db.common.DbSourceInter.checkOrInitEngine;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.exception.TronError;


public class CheckOrInitEngineTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String ACCOUNT = "account";

  @After
  public void  clearMocks() {
    Mockito.clearAllCaches();
  }

  @Test
  public void testLevelDbDetectedWhenExpectingRocksDb() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder(ACCOUNT).toString();
      File currentFile = new File(dir, "CURRENT");
      assertTrue(currentFile.createNewFile());
      TronError.ErrCode errCode = TronError.ErrCode.ROCKSDB_INIT;
      TronError exception = assertThrows(TronError.class, () ->
          checkOrInitEngine(ROCKSDB, dir, errCode));
      assertEquals("Cannot open LEVELDB database with ROCKSDB engine.",
          exception.getMessage());
      assertEquals(errCode, exception.getErrCode());
    }
  }

  @Test
  public void testCannotCreateDir() {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class)) {
      String dir = "/invalid/path/that/cannot/be/created";

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(false);
      TronError.ErrCode errCode = TronError.ErrCode.LEVELDB_INIT;
      TronError exception = assertThrows(TronError.class, () ->
          checkOrInitEngine(LEVELDB, dir, errCode));
      assertEquals("Cannot create dir: " + dir + ".", exception.getMessage());
      assertEquals(errCode, exception.getErrCode());
    }
  }

  @Test
  public void testCannotCreateEngineFile() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class)) {
      String dir = temporaryFolder.newFolder().toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();
      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(false);
      TronError.ErrCode errCode = TronError.ErrCode.ROCKSDB_INIT;
      TronError exception = assertThrows(TronError.class, () ->
          checkOrInitEngine(ROCKSDB, dir, errCode));

      assertEquals("Cannot create file: " + engineFile + ".", exception.getMessage());
      assertEquals(errCode, exception.getErrCode());
    }
  }

  @Test
  public void testCannotWritePropertyFile() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder().toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);

      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY)).thenReturn(null);
      strings.when(() -> Strings.isNullOrEmpty(null)).thenReturn(true);

      propUtil.when(() -> PropUtil.writeProperty(engineFile, ENGINE_KEY, ROCKSDB))
          .thenReturn(false);

      TronError.ErrCode errCode = TronError.ErrCode.LEVELDB_INIT;

      TronError exception = assertThrows(TronError.class, () ->
          checkOrInitEngine(ROCKSDB, dir, errCode));

      assertEquals("Cannot write file: " + engineFile + ".", exception.getMessage());
      assertEquals(errCode, exception.getErrCode());
    }

  }

  @Test
  public void testEngineMismatch() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder(ACCOUNT).toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);

      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY)).thenReturn(LEVELDB);
      strings.when(() -> Strings.isNullOrEmpty(LEVELDB)).thenReturn(false);

      TronError.ErrCode errCode = TronError.ErrCode.ROCKSDB_INIT;

      TronError exception = assertThrows(TronError.class, () ->
          checkOrInitEngine(ROCKSDB, dir, errCode));

      assertEquals("Cannot open LEVELDB database with ROCKSDB engine.",
          exception.getMessage());
      assertEquals(errCode, exception.getErrCode());
    }
  }

  @Test
  public void testSuccessfulFirstTimeInit() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder(ACCOUNT).toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);

      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY))
          .thenReturn(null)
          .thenReturn(LEVELDB);
      strings.when(() -> Strings.isNullOrEmpty(null)).thenReturn(true);

      propUtil.when(() -> PropUtil.writeProperty(engineFile, ENGINE_KEY, LEVELDB))
          .thenReturn(true);

      TronError.ErrCode errCode = TronError.ErrCode.LEVELDB_INIT;
      checkOrInitEngine(LEVELDB, dir, errCode);
    }
  }

  @Test
  public void testSuccessfulExistingEngine() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder(ACCOUNT).toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);
      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY)).thenReturn(ROCKSDB);
      strings.when(() -> Strings.isNullOrEmpty(ROCKSDB)).thenReturn(false);

      TronError.ErrCode errCode = TronError.ErrCode.ROCKSDB_INIT;
      checkOrInitEngine(ROCKSDB, dir, errCode);
    }
  }

  @Test
  /**
   * 000003.log   CURRENT  LOCK MANIFEST-000002
   */
  public void testCurrentFileExistsWithNoEngineFile() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder(ACCOUNT).toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();
      File currentFile = new File(dir, "CURRENT");
      assertTrue(currentFile.createNewFile());

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);
      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY)).thenReturn(LEVELDB);
      strings.when(() -> Strings.isNullOrEmpty(LEVELDB)).thenReturn(false);

      TronError.ErrCode errCode = TronError.ErrCode.LEVELDB_INIT;

      checkOrInitEngine(LEVELDB, dir, errCode);
    }
  }

  @Test
  /**
   * 000003.log   CURRENT  LOCK MANIFEST-000002  engine.properties(RocksDB)
   */
  public void testCurrentFileExistsEngineFileExists() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder(ACCOUNT).toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();

      File currentFile = new File(dir, "CURRENT");
      File engineFileObj = new File(engineFile);
      assertTrue(currentFile.createNewFile());
      assertTrue(engineFileObj.createNewFile());


      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);

      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY)).thenReturn(ROCKSDB);
      strings.when(() -> Strings.isNullOrEmpty(ROCKSDB)).thenReturn(false);

      TronError.ErrCode errCode = TronError.ErrCode.ROCKSDB_INIT;
      checkOrInitEngine(ROCKSDB, dir, errCode);
    }
  }

  @Test
  public void testEmptyStringEngine() throws IOException {
    try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
         MockedStatic<PropUtil> propUtil = mockStatic(PropUtil.class);
         MockedStatic<Strings> strings = mockStatic(Strings.class)) {

      String dir = temporaryFolder.newFolder("account").toString();
      String engineFile = Paths.get(dir, ENGINE_FILE).toString();

      fileUtil.when(() -> FileUtil.createDirIfNotExists(dir)).thenReturn(true);
      fileUtil.when(() -> FileUtil.createFileIfNotExists(engineFile)).thenReturn(true);

      propUtil.when(() -> PropUtil.readProperty(engineFile, ENGINE_KEY))
          .thenReturn("").thenReturn(ROCKSDB);
      strings.when(() -> Strings.isNullOrEmpty("")).thenReturn(true);

      propUtil.when(() -> PropUtil.writeProperty(engineFile, ENGINE_KEY, ROCKSDB))
          .thenReturn(true);
      TronError.ErrCode errCode = TronError.ErrCode.ROCKSDB_INIT;
      checkOrInitEngine(ROCKSDB, dir, errCode);
    }
  }
}
