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
  private static final String ROCKSDB = "ROCKSDB";
  private static final String LEVELDB = "LEVELDB";

  private static final Map<String, DBInterface> dbMap = Maps.newConcurrentMap();

  public enum DbType {
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
        db = openLevelDb(path, dbName);
        dbMap.put(path.toString(), db);
        break;
      case RocksDB:
        db = openRocksDb(path, dbName);
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
        return openLevelDb(path, dbName);
      case RocksDB:
        return openRocksDb(path, dbName);
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
    String engineFile = Paths.get(sourceDir, dbName, ENGINE_FILE).toString();
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

  public static LevelDBImpl openLevelDb(Path db, String name) throws IOException {
    LevelDBImpl leveldb = new LevelDBImpl(DBUtils.newLevelDb(db), name);
    tryInitEngineFile(db, LEVELDB);
    return leveldb;
  }

  public static RocksDBImpl openRocksDb(Path db, String name) throws RocksDBException {
    RocksDBImpl rocksdb = new RocksDBImpl(DBUtils.newRocksDb(db), name);
    tryInitEngineFile(db, ROCKSDB);
    return rocksdb;
  }

  private static void tryInitEngineFile(Path db, String engine) {
    String engineFile = Paths.get(db.toString(), ENGINE_FILE).toString();
    if (FileUtils.createFileIfNotExists(engineFile)) {
      FileUtils.writeProperty(engineFile, KEY_ENGINE, engine);
    }
  }
}
