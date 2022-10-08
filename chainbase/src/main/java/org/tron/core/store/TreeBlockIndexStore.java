package org.tron.core.store;

import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.exception.ItemNotFoundException;

@Component
public class TreeBlockIndexStore extends TronStoreWithRevoking<BytesCapsule> {


  @Autowired
  public TreeBlockIndexStore(@Value("tree-block-index") String dbName) {
    super(dbName, BytesCapsule.class);

  }

  public void put(long number, byte[] key) {
    put(ByteArray.fromLong(number), new BytesCapsule(key));
  }

  public byte[] get(Long num)
      throws ItemNotFoundException {
    BytesCapsule value = getUnchecked(ByteArray.fromLong(num));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException(String.format("number: %d is not found!", num));
    }

    return value.getData();
  }

  @Override
  public BytesCapsule get(byte[] key)
      throws ItemNotFoundException {
    BytesCapsule value = revokingDB.getUnchecked(key);
    if (Objects.isNull(value) || ArrayUtils.isEmpty(value.getData())) {
      throw new ItemNotFoundException(String.format("number: %d is not found!",
          ByteArray.toLong(key)));
    }
    return value;
  }
}