package org.tron.core.store;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.utils.Pair;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.db.TronDatabase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.service.RootHashService;

@Slf4j(topic = "DB")
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
public class CheckPointV2Store extends TronDatabase<byte[]> {

  @Autowired
  private StateRootStore stateRootStore;

  @Autowired
  public CheckPointV2Store(@Value("checkpoint") String dbPath) {
    this(dbPath, String.valueOf(System.currentTimeMillis()));
  }

  public CheckPointV2Store(String dbPath, String name) {
    super(dbPath + "/" + name);
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
    logger.debug("******** Begin to close {}. ********", getDbName());
    try {
      dbSource.closeDB();
    } catch (Exception e) {
      logger.warn("Failed to close {}.", getDbName(), e);
    } finally {
      logger.debug("******** End to close {}. ********", getDbName());
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows, WriteOptionsWrapper writeOptions) {
    Pair<Optional<Long>, Sha256Hash> ret = RootHashService.getRootHash(rows);
    super.updateByBatch(rows, writeOptions);
    ret.getKey().ifPresent(height -> stateRootStore.put(height, ret.getValue()));
  }

}
