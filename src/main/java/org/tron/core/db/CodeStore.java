package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.CodeCapsule;

@Slf4j
public class CodeStore extends TronStoreWithRevoking<CodeCapsule> {


  private CodeStore(String dbName) {
    super(dbName);
  }

  @Override
  public CodeCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new CodeCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] code = dbSource.getData(key);
    return null != code;
  }

  public long getTotalCodes() {
    return dbSource.getTotal();
  }

  private static CodeStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  public static CodeStore create(String dbName) {
    if (instance == null) {
      synchronized (CodeStore.class) {
        if (instance == null) {
          instance = new CodeStore(dbName);
        }
      }
    }
    return instance;
  }

  public byte[] findCodeByHash(byte[] hash) {
    return dbSource.getData(hash);
  }
}
