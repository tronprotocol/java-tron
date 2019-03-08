package org.tron.core.db.backup;


import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDB;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.storage.leveldb.RocksDbDataSourceImpl;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.ManagerForTest;
import org.tron.core.db2.core.RevokingDBWithCachingNewValue;
import org.tron.core.db2.core.SnapshotManager;

@Slf4j
public class BackupDbUtilTest {

  static {
    RocksDB.loadLibrary();
  }

  public TronApplicationContext context;
  public BackupDbUtil dbBackupUtil;
  public Manager dbManager;
  public ManagerForTest mng_test;
  public String dbPath = "output-BackupDbUtilTest";

  String prop_path;
  String bak1_path;
  String bak2_path;
  int frequency;

  @Before
  public void before() {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", "database",
            "--storage-index-directory", "index"
        },
        "config-test-dbbackup.conf"
    );

    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);
    dbBackupUtil = context.getBean(BackupDbUtil.class);
    mng_test = new ManagerForTest(dbManager);

    //prepare prop.properties
    prop_path = dbPath + File.separator + "test_prop.properties";
    bak1_path = dbPath + File.separator + "bak1/database";
    bak2_path = dbPath + File.separator + "bak2/database";
    frequency = 50;
    Args cfgArgs = Args.getInstance();
    cfgArgs.getDbBackupConfig()
        .initArgs(true, prop_path, bak1_path, bak2_path, frequency);
    FileUtil.createFileIfNotExists(prop_path);
  }

  @After
  public void after() {
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testDoBackup() {
    PropUtil.writeProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE(),
        String.valueOf("11"));
    mng_test.pushNTestBlock(50);
    List<RevokingDBWithCachingNewValue> alist = ((SnapshotManager)dbBackupUtil.getDb()).getDbs();

    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 50);
    Assert.assertTrue("22".equals(
        PropUtil.readProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE())));

    mng_test.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 100);
    Assert.assertTrue("11".equals(
        PropUtil.readProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE())));

    mng_test.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 150);
    Assert.assertTrue("22".equals(
        PropUtil.readProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE())));

    PropUtil.writeProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE(),
        String.valueOf("1"));
    mng_test.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 200);
    Assert.assertTrue("11".equals(
        PropUtil.readProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE())));

    PropUtil.writeProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE(),
        String.valueOf("2"));
    mng_test.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 250);
    Assert.assertTrue("22".equals(
        PropUtil.readProperty(prop_path, BackupDbUtil.getDB_BACKUP_STATE())));
  }
}