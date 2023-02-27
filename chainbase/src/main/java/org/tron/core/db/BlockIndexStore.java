package org.tron.core.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.ItemNotFoundException;

@Component
@Slf4j(topic = "DB")
public class BlockIndexStore extends TronStoreWithRevoking<BytesCapsule> {


  @Autowired
  public BlockIndexStore(@Value("block-index") String dbName) {
    super(dbName);

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
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException(String.format("number: %d is not found!",
          ByteArray.toLong(key)));
    }
    return new BytesCapsule(value);
  }

  public List<BlockId> getLimitNumber(long startNumber, long limit) {
    return pack(revokingDB.getValuesNext(ByteArray.fromLong(startNumber), limit));
  }

  private List<BlockId> pack(Set<byte[]> values) {
    List<BlockId> blocks = new ArrayList<>();
    for (byte[] bytes : values) {
      blocks.add(new BlockId(Sha256Hash.wrap(bytes)));
    }
    blocks.sort(Comparator.comparing(BlockId::getNum));
    return blocks;
  }
}
