package org.tron.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.Quitable;
import org.tron.core.config.args.Args;
import org.tron.core.db.api.IndexHelper;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public abstract class TronDatabase<T> implements Iterable<T>, Quitable {

  protected LevelDbDataSourceImpl dbSource;

  protected IndexHelper indexHelper;

  protected TronDatabase(String dbName) {
    dbSource = new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectory(), dbName);
    dbSource.initDB();
  }

  protected TronDatabase() {
    throw new IllegalStateException("This constructor is not allowed");
  }

  public LevelDbDataSourceImpl getDbSource() {
    return dbSource;
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  public abstract boolean has(byte[] key);

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<T> iterator() {
    throw new UnsupportedOperationException();
  }
}
