package org.tron.core.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class BlockHeaderIndexStore extends TronDatabase<BytesCapsule> {

  private static final String SPLIT = "_";

  @Autowired
  public BlockHeaderIndexStore(@Value("block-header-index") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    dbSource.putData(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public BytesCapsule getUnchecked(byte[] key) {
    return get(key);
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  public void put(String chainId, BlockId id) {
    put(buildKey(chainId, id.getNum()), new BytesCapsule(id.getBytes()));
  }

  public BlockId get(String chainId, Long num)
      throws ItemNotFoundException {
    BytesCapsule value = getUnchecked(buildKey(chainId, num));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException("number: " + num + " is not found!");
    }
    return new BlockId(Sha256Hash.wrap(value.getData()), num);
  }

  public BlockId getUnchecked(String chainId, Long num) {
    BytesCapsule value = getUnchecked(buildKey(chainId, num));
    if (value == null || value.getData() == null) {
      return null;
    }
    return new BlockId(Sha256Hash.wrap(value.getData()), num);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] data = dbSource.getData(key);
    if (ByteUtil.isNullOrZeroArray(data)) {
      return null;
    }
    return new BytesCapsule(data);
  }

  private byte[] buildKey(String chainId, Long num) {
    return (chainId + SPLIT + num).getBytes();
  }
}