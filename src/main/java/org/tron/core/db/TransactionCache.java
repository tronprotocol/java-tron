package org.tron.core.db;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.KhaosDatabase.KhaosBlock;
import org.tron.core.db2.common.TxCacheDB;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.StoreException;

@Slf4j
public class TransactionCache extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  public TransactionCache(@Value("trans-cache") String dbName) {
    super(dbName, TxCacheDB.class);
  }
}
