package org.tron.core.capsule;

import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.ResourceReceipt;

public class ReceiptCapsule {

  private ResourceReceipt receipt;

  private Sha256Hash receiptAddress;

  public ReceiptCapsule(ResourceReceipt data, Sha256Hash receiptAddress) {
    receipt = data;
    this.receiptAddress = receiptAddress;
  }

  public ReceiptCapsule(Sha256Hash receiptAddress) {
    receipt = ResourceReceipt.newBuilder().build();
    this.receiptAddress = receiptAddress;
  }

  public void setCpuUsage(long usage) {
    receipt = receipt.toBuilder().setCpuUsage(usage).build();
  }

  public long getCpuUsage() {
    return receipt.getCpuUsage();
  }

  public void setCpuFee(long fee) {
    receipt = receipt.toBuilder().setCpuFee(fee).build();
  }

  public long getCpuFee() {
    return receipt.getCpuFee();
  }

  public void calculateCpuFee() {
    //TODO: calculate
  }

  public void setStorageDelta(long delta) {
    receipt = receipt.toBuilder().setStorageDelta(delta).build();
  }

  public long getStorageDelta() {
    return receipt.getStorageDelta();
  }

  public void setStorageFee(long fee) {
    receipt = receipt.toBuilder().setStorageFee(fee).build();
  }

  public long getStorageFee() {
    return receipt.getStorageFee();
  }

  public void setNetUsage(long usage) {
    receipt = receipt.toBuilder().setNetUsage(usage).build();
  }

  public long getNetUsage() {
    return receipt.getNetUsage();
  }

  public void setNetFee(long fee) {
    receipt = receipt.toBuilder().setNetFee(fee).build();
  }

  public long getNetFee() {
    return receipt.getNetFee();
  }

  public void payCpuBill() {
    //TODO: pay cpu bill
    if (0 == receipt.getCpuUsage()) {
      return;
    }
  }

  public void payStorageBill() {
    //TODO: pay storage bill
    if (0 == receipt.getSerializedSize()) {
      return;
    }
  }

  public void buyStorage(long storage) {
    //TODO: buy the min storage
  }
}
