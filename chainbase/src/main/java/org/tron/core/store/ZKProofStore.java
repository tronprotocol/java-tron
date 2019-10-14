package org.tron.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.core.db.TronDatabase;

@Component
public class ZKProofStore extends TronDatabase<Boolean> {

  @Autowired
  public ZKProofStore(ApplicationContext ctx) {
    super("zkProof");
  }

  @Override
  public void put(byte[] key, Boolean item) {
    byte[] b = {(byte) (item.booleanValue() ? 0x01 : 0x00)};
    dbSource.putData(key, b);
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public Boolean get(byte[] key) {
    return dbSource.getData(key)[0] == 0x01;
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }
}
