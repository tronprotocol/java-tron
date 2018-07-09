package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.CodeCapsule;

@Slf4j
@Component
public class CodeStore extends TronStoreWithRevoking<CodeCapsule> {

  @Autowired
  private CodeStore(@Value("code") String dbName) {
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

  public byte[] findCodeByHash(byte[] hash) {
    return dbSource.getData(hash);
  }
}
