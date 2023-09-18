package org.tron.core.store;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.db.TronDatabase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

import java.util.Spliterator;
import java.util.function.Consumer;

@Slf4j(topic = "DB")
public class CheckPointV2Store extends TronDatabase<byte[]> {

  @Autowired
  public CheckPointV2Store(String dbPath) {
    super(dbPath);
  }

  @Override
  public void put(byte[] key, byte[] item) {
  }

  @Override
  public void delete(byte[] key) {
    getDbSource().deleteData(key);
  }

  @Override
  public byte[] get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  @Override
  public void forEach(Consumer action) {

  }

  @Override
  public Spliterator spliterator() {
    return null;
  }

  @Override
  protected void init() {
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    logger.debug("******** Begin to close {}. ********", getName());
    try {
      dbSource.closeDB();
    } catch (Exception e) {
      logger.warn("Failed to close {}.", getName(), e);
    } finally {
      logger.debug("******** End to close {}. ********", getName());
    }
  }

}
