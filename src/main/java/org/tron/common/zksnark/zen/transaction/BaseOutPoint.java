package org.tron.common.zksnark.zen.transaction;

import org.tron.common.utils.ByteArray;

public class BaseOutPoint {

  public byte[] hash; // 256
  public Integer n; //

  void SetNull() {
    hash = new byte[256];
    //    n = (uint32_t) - 1;
  }

  boolean IsNull() {

    return ByteArray.isEmpty(hash);
  }

  public class OutPoint extends BaseOutPoint {

  }
}
