package org.tron.core.vm.nativecontract.param;

public class TokenIssueParam {

  private byte[] ownerAddress;

  private byte[] name;

  private byte[] abbr;

  private long totalSupply;

  private int precision;

  public byte[] getOwnerAddress() {
    return ownerAddress;
  }

  public void setOwnerAddress(byte[] ownerAddress) {
    this.ownerAddress = ownerAddress;
  }

  public byte[] getName() {
    return name;
  }

  public void setName(byte[] name) {
    this.name = name;
  }

  public byte[] getAbbr() {
    return abbr;
  }

  public void setAbbr(byte[] abbr) {
    this.abbr = abbr;
  }

  public long getTotalSupply() {
    return totalSupply;
  }

  public void setTotalSupply(long totalSupply) {
    this.totalSupply = totalSupply;
  }

  public int getPrecision() {
    return precision;
  }

  public void setPrecision(int precision) {
    this.precision = precision;
  }
}
