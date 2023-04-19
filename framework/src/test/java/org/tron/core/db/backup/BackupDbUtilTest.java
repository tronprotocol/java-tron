package org.tron.core.db.backup;

import java.io.File;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDB;
import org.tron.common.BaseTest;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PropUtil;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.ManagerForTest;

@Slf4j
public class BackupDbUtilTest extends BaseTest {

  static {
    RocksDB.loadLibrary();
  }

  @Resource
  public ConsensusService consensusService;
  @Resource
  public DposSlot dposSlot;
  public ManagerForTest mngForTest;

  String propPath;
  String bak1Path;
  String bak2Path;
  int frequency;

  static {
    dbPath = "output-BackupDbUtilTest";
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", "database",
            "--storage-index-directory", "index"
        },
        "config-test-dbbackup.conf"
    );
  }

  @Before
  public void before() {
    consensusService.start();
    mngForTest = new ManagerForTest(dbManager, dposSlot);
    //prepare prop.properties
    propPath = dbPath + File.separator + "test_prop.properties";
    bak1Path = dbPath + File.separator + "bak1/database";
    bak2Path = dbPath + File.separator + "bak2/database";
    frequency = 50;
    CommonParameter parameter = Args.getInstance();
    parameter.getDbBackupConfig()
        .initArgs(true, propPath, bak1Path, bak2Path, frequency);
    FileUtil.createFileIfNotExists(propPath);
  }

  @Test
  public void testDoBackup() {
    PropUtil.writeProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE(), "11");
    mngForTest.pushNTestBlock(50);

    Assert.assertEquals(50, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    Assert.assertEquals("22", PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE()));

    mngForTest.pushNTestBlock(50);
    Assert.assertEquals(100, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    Assert.assertEquals("11", PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE()));

    mngForTest.pushNTestBlock(50);
    Assert.assertEquals(150, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    Assert.assertEquals("22", PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE()));

    PropUtil.writeProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE(), "1");
    mngForTest.pushNTestBlock(50);
    Assert.assertEquals(200, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    Assert.assertEquals("11", PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE()));

    PropUtil.writeProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE(), "2");
    mngForTest.pushNTestBlock(50);
    Assert.assertEquals(250, dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    Assert.assertEquals("22", PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE()));
  }
}
