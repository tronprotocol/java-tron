package org.tron.plugins.utils.db;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.FileUtils;


@Slf4j(topic = "tool")
public class DbTool {

  private static final String KEY_ENGINE = "ENGINE";
  private static final String ENGINE_FILE = "engine.properties";
  private static final String FILE_SEPARATOR = File.separator;
  private static final String ROCKSDB = "ROCKSDB";

  private static final Map<String, DBInterface> dbMap = Maps.newConcurrentMap();

  enum DbType {
    LevelDB,
    RocksDB
  }

  /**
   * Get the DB object according to the specified path,
   * create db object when not exists, otherwise get it from the dbMap.
   *
   * @param sourceDir the parent path of db
   * @param dbName db dir name
   *
   * @return db object
   *
   * @throws IOException leveldb error
   * @throws RocksDBException rocksdb error
   */
  public static DBInterface getDB(String sourceDir, String dbName)
          throws IOException, RocksDBException {
    DbType type = getDbType(sourceDir, dbName);
    return getDB(sourceDir, dbName, type);
  }

  /**
   * Get the DB object according to the specified path, keep engine same with source.
   *
   * @param sourceDir read engine
   * @param destDir to be open parent path
   * @param dbName database name
   *
   * @return db object
   *
   * @throws IOException leveldb error
   * @throws RocksDBException rocksdb error
   */
  public static DBInterface getDB(String sourceDir, String destDir, String dbName)
      throws IOException, RocksDBException {
    DbType type = getDbType(sourceDir, dbName);
    return getDB(destDir, dbName, type);
  }

  /**
   * Get the DB object according to the specified path and engine.
   *
   * @param sourceDir to be open parent path
   * @param dbName database name
   * @param type engine
   * @return db object
   * @throws IOException leveldb error
   * @throws RocksDBException rocksdb error
   */
  public static DBInterface getDB(String sourceDir, String dbName, DbType type)
      throws IOException, RocksDBException {
    Path path = Paths.get(sourceDir, dbName);
    if (dbMap.containsKey(path.toString())) {
      return dbMap.get(path.toString());
    }
    DBInterface db;
    switch (type) {
      case LevelDB:
        db = openLevelDb(path);
        dbMap.put(path.toString(), db);
        break;
      case RocksDB:
        db = openRocksDb(path);
        dbMap.put(path.toString(), db);
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
    return db;
  }

  /**
   * Get the DB object according to the specified path,
   *  not managed by dbMap.
   *
   * @param sourceDir the parent path of db
   * @param dbName db dir name
   *
   * @return db object
   *
   * @throws IOException leveldb error
   * @throws RocksDBException rocksdb error
   */
  public static DBInterface getDB(Path sourceDir, String dbName)
      throws IOException, RocksDBException {
    Path path = Paths.get(sourceDir.toString(), dbName);
    DbType type = getDbType(sourceDir.toString(), dbName);
    switch (type) {
      case LevelDB:
        return openLevelDb(path);
      case RocksDB:
        return openRocksDb(path);
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  /**
   * Close db.
   *
   * @param sourceDir db parentPath
   * @param dbName db dirname
   *
   * @throws IOException IOException
   */
  public static void closeDB(String sourceDir, String dbName)
          throws IOException {
    Path path = Paths.get(sourceDir, dbName);
    DBInterface db = dbMap.get(path.toString());
    if (db != null) {
      try {
        dbMap.remove(path.toString());
        db.close();
      } catch (IOException e) {
        logger.error("close db {} error: {}", path, e);
        throw e;
      }
    }
  }

  /**
   * Close all dbs.
   */
  public static void close() {
    Iterator<Map.Entry<String, DBInterface>> iterator = dbMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, DBInterface> next = iterator.next();
      try {
        next.getValue().close();
      } catch (IOException e) {
        logger.error("close db failed, db: {}", next.getKey(), e);
      }
      iterator.remove();
    }
  }

  private static DbType getDbType(String sourceDir, String dbName) {
    String engineFile = String.format("%s%s%s%s%s", sourceDir, FILE_SEPARATOR,
            dbName, FILE_SEPARATOR, ENGINE_FILE);
    if (!new File(engineFile).exists()) {
      return DbType.LevelDB;
    }
    String engine = FileUtils.readProperty(engineFile, KEY_ENGINE);
    if (engine.equalsIgnoreCase(ROCKSDB)) {
      return DbType.RocksDB;
    } else {
      return DbType.LevelDB;
    }
  }

  private static LevelDBImpl openLevelDb(Path db) throws IOException {
    return new LevelDBImpl(DBUtils.newLevelDb(db));
  }

  private static RocksDBImpl openRocksDb(Path db) throws RocksDBException {
    return new RocksDBImpl(DBUtils.newRocksDb(db));
  }


}
