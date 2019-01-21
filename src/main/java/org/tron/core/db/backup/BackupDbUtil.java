package org.tron.core.db.backup;

import static org.tron.core.db.backup.BackupDbUtil.STATE.BAKEDONE;
import static org.tron.core.db.backup.BackupDbUtil.STATE.BAKEDTWO;

import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.storage.leveldb.RocksDbDataSourceImpl;
import org.tron.common.utils.PropUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db.RevokingStoreRocks;

@Slf4j
@Component
public class BackupDbUtil {

  @Getter
  private static String DB_BACKUP_STATE = "DB";
  private static final int DB_BACKUP_INDEX1 = 1;
  private static final int DB_BACKUP_INDEX2 = 2;

  @Getter
  private static final int DB_BACKUP_STATE_DEFAULT = 11;

  public enum STATE {
    BAKINGONE(1), BAKEDONE(11), BAKINGTWO(2), BAKEDTWO(22);
    public int status;

    private STATE(int status) {
      this.status = status;
    }

    public int getStatus() {
      return status;
    }

    public static STATE valueOf(int value) {
      switch (value) {
        case 1:
          return BAKINGONE;
        case 11:
          return BAKEDONE;
        case 2:
          return BAKINGTWO;
        case 22:
          return BAKEDTWO;
        default:
          return BAKEDONE;
      }
    }
  }

  @Getter
  @Autowired
  private RevokingDatabase db;

  private Args args = Args.getInstance();

  private int getBackupState() {
    try {
      return Integer.valueOf(PropUtil
          .readProperty(args.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE)
      );
    } catch (NumberFormatException ignore) {
      return DB_BACKUP_STATE_DEFAULT;  //get default state if prop file is newly created
    }
  }

  private void setBackupState(int status) {
    PropUtil.writeProperty(args.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE,
        String.valueOf(status));
  }

  private void switchBackupState() {
    switch (STATE.valueOf(getBackupState())) {
      case BAKINGONE:
        setBackupState(BAKEDONE.getStatus());
        break;
      case BAKEDONE:
        setBackupState(BAKEDTWO.getStatus());
        break;
      case BAKINGTWO:
        setBackupState(BAKEDTWO.getStatus());
        break;
      case BAKEDTWO:
        setBackupState(BAKEDONE.getStatus());
        break;
      default:
        break;
    }
  }

  public void doBackup(BlockCapsule block) {
    if (block.getNum() % args.getDbBackupConfig().getFrequency() != 0) {
      return;
    }
    long t1 = System.currentTimeMillis();
    try {
      switch (STATE.valueOf(getBackupState())) {
        case BAKINGONE:
          deleteBackup(DB_BACKUP_INDEX1);
          backup(DB_BACKUP_INDEX1);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX2);
          break;
        case BAKEDONE:
          deleteBackup(DB_BACKUP_INDEX2);
          backup(DB_BACKUP_INDEX2);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX1);
          break;
        case BAKINGTWO:
          deleteBackup(DB_BACKUP_INDEX2);
          backup(DB_BACKUP_INDEX2);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX1);
          break;
        case BAKEDTWO:
          deleteBackup(DB_BACKUP_INDEX1);
          backup(DB_BACKUP_INDEX1);
          switchBackupState();
          deleteBackup(DB_BACKUP_INDEX2);
          break;
        default:
          logger.warn("invalid backup state");
      }
    } catch (RocksDBException e) {
      logger.warn("backup db error");
    }
    STATE state = STATE.valueOf(getBackupState());
    if (state == BAKEDONE || state == BAKEDTWO) {

    }
    logger.info("current block number is {}, backup all store use {} ms!", block.getNum(),
        System.currentTimeMillis() - t1);
  }

  private void backup(int i) throws RocksDBException {
    List<RocksDbDataSourceImpl> stores = ((RevokingStoreRocks) db).getDbs();
    for (RocksDbDataSourceImpl store : stores) {
      store.backup(i);
    }
  }

  private void deleteBackup(int i) {
    List<RocksDbDataSourceImpl> stores = ((RevokingStoreRocks) db).getDbs();
    for (RocksDbDataSourceImpl store : stores) {
      store.deleteDbBakPath(i);
    }
  }
}
