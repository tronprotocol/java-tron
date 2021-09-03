package org.tron.tool.litefullnode.db;

import com.google.common.primitives.Bytes;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.tron.tool.litefullnode.iterator.DBIterator;
import org.tron.tool.litefullnode.iterator.LevelDBIterator;

public class LevelDBImpl extends TronDB {

  private final DB leveldb;
  private final String name;
  private final String sourceDir;
  private TronDB tmp;
  private final ReadOptions readOptions;

  public LevelDBImpl(String sourceDir, DB leveldb, String name) {
    this.leveldb = leveldb;
    this.name = name;
    this.sourceDir = sourceDir;
    this.readOptions = new ReadOptions().fillCache(false);
    Path path = Paths.get(sourceDir, name);
    DB_MAP.put(path.toString(), this);
  }

  @Override
  public byte[] get(byte[] key) {
    if ("tmp".equalsIgnoreCase(name)) {
      return leveldb.get(key);
    }

    if (tmp == null) {
      Path path = Paths.get(sourceDir, "tmp");
      tmp = DB_MAP.get(path.toString());
    }

    byte[] value;
    byte[] valueFromTmp = tmp.get(Bytes.concat(simpleEncode(name), key));
    if (isEmptyBytes(valueFromTmp)) {
      value = leveldb.get(key);
    } else {
      value = valueFromTmp.length == 1
          ? null : Arrays.copyOfRange(valueFromTmp, 1, valueFromTmp.length);
    }
    return value;
  }

  @Override
  public byte[] getDir(byte[] key) {
    return leveldb.get(key, readOptions);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    leveldb.put(key, value);
  }

  @Override
  public void delete(byte[] key) {
    leveldb.delete(key);
  }

  @Override
  public DBIterator iterator() {
    return new LevelDBIterator(leveldb.iterator(new ReadOptions().fillCache(false)));
  }

  @Override
  public void close() throws IOException {
    leveldb.close();
  }
}
