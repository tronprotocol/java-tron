package org.tron.common.zksnark.sapling.walletdb;

public class CKeyMetadata {

  long nCreateTime; // 0 means unknown
  String hdKeypath; //optional HD/zip32 keypath
  uint256 seedFp;

  public CKeyMetadata(long nCreateTime) {
    this.nCreateTime = nCreateTime;
  }
}
