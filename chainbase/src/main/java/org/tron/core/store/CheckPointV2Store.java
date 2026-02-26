package org.tron.core.store;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.core.db.TronDatabase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
public class CheckPointV2Store extends TronDatabase<byte[]> {

  private final WriteOptionsWrapper writeOptions = WriteOptionsWrapper.getInstance()
      .sync(CommonParameter.getInstance().getStorage().isCheckpointSync());

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

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    this.dbSource.updateByBatch(rows, writeOptions);
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    logger.debug("******** Begin to close {}. ********", getName());
    try {
      writeOptions.close();
      dbSource.closeDB();
    } catch (Exception e) {
      logger.warn("Failed to close {}.", getName(), e);
    } finally {
      logger.debug("******** End to close {}. ********", getName());
    }
  }

}
