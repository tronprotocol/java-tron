package org.tron.core.store;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.db.TronDatabase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

import java.util.Spliterator;
import java.util.function.Consumer;

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

}
