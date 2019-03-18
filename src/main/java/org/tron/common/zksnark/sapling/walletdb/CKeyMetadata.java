package org.tron.common.zksnark.sapling.walletdb;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CKeyMetadata {

  public long nCreateTime; // 0 means unknown
  public String hdKeypath; //optional HD/zip32 keypath
  public byte[] seedFp;//256

  public CKeyMetadata(long nCreateTime) {
    this.nCreateTime = nCreateTime;
  }
}
