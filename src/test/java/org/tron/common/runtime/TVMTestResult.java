package org.tron.common.runtime;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime2.TxRunner;
import org.tron.core.capsule.ReceiptCapsule;

@Slf4j
public class TVMTestResult {

  private TxRunner runtime;
  private ReceiptCapsule receipt;
  private byte[] contractAddress;

  public byte[] getContractAddress() {
    return contractAddress;
  }

  public TVMTestResult setContractAddress(byte[] contractAddress) {
    this.contractAddress = contractAddress;
    return this;
  }

  public TxRunner getRuntime() {
    return runtime;
  }

  public TVMTestResult setRuntime(TxRunner runtime) {
    this.runtime = runtime;
    return this;
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public TVMTestResult setReceipt(ReceiptCapsule receipt) {
    this.receipt = receipt;
    return this;
  }

  public TVMTestResult(TxRunner runtime, ReceiptCapsule receipt, byte[] contractAddress) {
    this.runtime = runtime;
    this.receipt = receipt;
    this.contractAddress = contractAddress;
  }

}
