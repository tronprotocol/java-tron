package org.tron.core.db.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.error.TronDBException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.tron.common.utils.PropUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db2.common.RocksDB;
import org.tron.core.db2.common.TxCacheDB;
import org.tron.core.db2.core.ITronChainBase;
import org.tron.core.store.DynamicPropertiesStore;

@Slf4j(topic = "DB")
@Component
public class BackupDbUtil {

  @Getter
  private static final String DB_BACKUP_STATE = "DB";
  private static final int DB_BACKUP_INDEX1 = 1;
  private static final int DB_BACKUP_INDEX2 = 2;

  @Getter
  private static final int DB_BACKUP_STATE_DEFAULT = 11;
  @Autowired
  private List<ITronChainBase<?>> stores;
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;
  private List<RocksDB> rocksDBsToBackup;
  private CommonParameter parameter = Args.getInstance();


  @PostConstruct
  private void init() {
    rocksDBsToBackup = stores.stream().map(this::getRocksDB)
        .filter(Objects::nonNull)
        .filter(db -> !"tmp".equalsIgnoreCase(db.getDbName()))
        .filter(db -> !db.getDbName().startsWith("checkpoint"))
        .collect(Collectors.toList());
  }

  private RocksDB getRocksDB(ITronChainBase<?> store) {
    if (store instanceof TronStoreWithRevoking && ((TronStoreWithRevoking<?>) store).getDb()
        .getClass() == RocksDB.class) {
      return (RocksDB) ((TronStoreWithRevoking<?>) store).getDb();
    }  else if (store instanceof TronStoreWithRevoking
        && ((TronStoreWithRevoking<?>) store).getDb()
        .getClass() == TxCacheDB.class
        && ((TxCacheDB) ((TronStoreWithRevoking<?>) store).getDb()).getPersistentStore().getClass()
        == RocksDB.class) {
      return (RocksDB) ((TxCacheDB) ((TronStoreWithRevoking<?>) store).getDb())
          .getPersistentStore();
    } else if (store instanceof TronDatabase && ((TronDatabase<?>) store).getDbSource().getClass()
        == RocksDbDataSourceImpl.class) {
      return new RocksDB((RocksDbDataSourceImpl) ((TronDatabase<?>) store).getDbSource());
    } else {
      return null;
    }
  }

  private int getBackupState() {
    try {
      return Integer.valueOf(PropUtil
          .readProperty(parameter.getDbBackupConfig().getPropPath(), BackupDbUtil.DB_BACKUP_STATE)
      );
    } catch (NumberFormatException ignore) {
      return DB_BACKUP_STATE_DEFAULT;  //get default state if prop file is newly created
    }
  }

  private void setBackupState(int status, long blockNum) {
    Map<String, String> params = new HashMap<>();
    params.put("header", String.valueOf(blockNum));
    params.put(BackupDbUtil.DB_BACKUP_STATE, String.valueOf(status));
    PropUtil.writeProperties(parameter.getDbBackupConfig().getPropPath(), params);
  }

  private void switchBackupState(long blockNum) {
    switch (State.valueOf(getBackupState())) {
      case BAKINGONE:
      case BAKEDTWO:
        setBackupState(State.BAKEDONE.getStatus(), blockNum);
        break;
      case BAKEDONE:
      case BAKINGTWO:
        setBackupState(State.BAKEDTWO.getStatus(), blockNum);
        break;
      default:
        break;
    }
  }

  public void doBackup(BlockCapsule block) {
    long t1 = System.currentTimeMillis();
    long header = dynamicPropertiesStore.getLatestBlockHeaderNumberFromDB();
    try {
      switch (State.valueOf(getBackupState())) {
        case BAKINGONE:
        case BAKEDTWO:
          deleteBackup(DB_BACKUP_INDEX1);
          backup(DB_BACKUP_INDEX1);
          switchBackupState(header);
          deleteBackup(DB_BACKUP_INDEX2);
          break;
        case BAKEDONE:
        case BAKINGTWO:
          deleteBackup(DB_BACKUP_INDEX2);
          backup(DB_BACKUP_INDEX2);
          switchBackupState(header);
          deleteBackup(DB_BACKUP_INDEX1);
          break;
        default:
          logger.warn("invalid backup state {}.", getBackupState());
      }
      long timeUsed = System.currentTimeMillis() - t1;
      logger
          .info("Current block number is {},root header is {}, backup all store use {} ms!",
              block.getNum(), header, timeUsed);
      if (timeUsed >= 3000) {
        logger.warn("Backing up db uses too much time. {} ms.", timeUsed);
      }
    } catch (RocksDBException | SecurityException | IOException e) {
      throw new TronDBException("Backup rocksdb on " + header + " failed.", e);
    }
  }

  private void backup(int i) throws RocksDBException {
    String path = "";
    if (i == DB_BACKUP_INDEX1) {
      path = parameter.getDbBackupConfig().getBak1path();
    } else if (i == DB_BACKUP_INDEX2) {
      path = parameter.getDbBackupConfig().getBak2path();
    } else {
      throw new RuntimeException(String.format("error backup with undefined index %d", i));
    }
    String finalPath = path;
    rocksDBsToBackup.parallelStream().forEach(db -> db.backup(finalPath));
    List<String> backedDBs = Arrays.stream(Objects.requireNonNull(
            Paths.get(finalPath).toFile().listFiles(File::isDirectory)))
        .map(File::getName).collect(Collectors.toList());
    List<String> dbNames = rocksDBsToBackup.stream().map(RocksDB::getDbName)
        .collect(Collectors.toList());
    dbNames.removeAll(backedDBs);
    if (!dbNames.isEmpty()) {
      throw new RocksDBException("Some db not backed up: " + dbNames);
    }
  }

  private void deleteBackup(int i) throws RocksDBException, IOException {
    String path = "";
    if (i == DB_BACKUP_INDEX1) {
      path = parameter.getDbBackupConfig().getBak1path();
    } else if (i == DB_BACKUP_INDEX2) {
      path = parameter.getDbBackupConfig().getBak2path();
    } else {
      throw new RuntimeException(String.format("error deleteBackup with undefined index %d", i));
    }
    String finalPath = path;
    rocksDBsToBackup.parallelStream().forEach(db -> RocksDB.destroy(db.getDbName(), finalPath));
    FileUtils.cleanDirectory(new File(finalPath)); // clean bak dir by File or other.
    List<String> backedDBs = Arrays.stream(Objects.requireNonNull(
            Paths.get(finalPath).toFile().listFiles(File::isDirectory)))
        .map(File::getName).collect(Collectors.toList());
    if (!backedDBs.isEmpty()) {
      throw new RocksDBException("Some db not delete: " + backedDBs);
    }
  }

  public enum State {
    BAKINGONE(1), BAKEDONE(11), BAKINGTWO(2), BAKEDTWO(22);
    private int status;

    State(int status) {
      this.status = status;
    }

    public static State valueOf(int value) {
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

    public int getStatus() {
      return status;
    }
  }
}
