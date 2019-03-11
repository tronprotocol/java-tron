package org.tron.common.zksnark.sapling.transaction;

public class BaseOutPoint {

  uint256 hash;
  uint32_t n;

  void SetNull() {
    hash.SetNull();
    n = (uint32_t) - 1;
  }

  bool IsNull() const

  {
    return (hash.IsNull() && n == (uint32_t) - 1);
  }

  public class SaplingOutPoint extends BaseOutPoint {

  }
}
