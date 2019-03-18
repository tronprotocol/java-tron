package org.tron.common.zksnark.sapling.transaction;

import org.tron.common.utils.ByteArray;

public class BaseOutPoint {

  byte[] hash; // 256
  Integer n; //

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
