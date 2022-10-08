package org.tron.core.db;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class BlockIndexStore extends TronStoreWithRevoking<BytesCapsule> {


  @Autowired
  public BlockIndexStore(@Value("block-index") String dbName) {
    super(dbName, BytesCapsule.class);

  }

  public void put(BlockId id) {
    put(ByteArray.fromLong(id.getNum()), new BytesCapsule(id.getBytes()));
  }

  public BlockId get(Long num)
      throws ItemNotFoundException {
    BytesCapsule value = getUnchecked(ByteArray.fromLong(num));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException(String.format("number: %d is not found!", num));
    }
    return new BlockId(Sha256Hash.wrap(value.getData()), num);
  }

  @Override
  public BytesCapsule get(byte[] key)
      throws ItemNotFoundException {
    BytesCapsule value = getNonEmpty(key);
    if (Objects.isNull(value)) {
      throw new ItemNotFoundException(String.format("number: %d is not found!",
          ByteArray.toLong(key)));
    }
    return value;
  }
}