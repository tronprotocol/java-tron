package org.tron.core.capsule;

import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.ResourceReceipt;

public class ReceiptCapsule {

  private ResourceReceipt receipt;

  private Sha256Hash receiptAddress;

  public ReceiptCapsule(ResourceReceipt data, Sha256Hash receiptAddress) {
    this.receipt = data;
    this.receiptAddress = receiptAddress;
  }

  public ReceiptCapsule(Sha256Hash receiptAddress) {
    this.receipt = ResourceReceipt.newBuilder().build();
    this.receiptAddress = receiptAddress;
  }

  public void setReceipt(ResourceReceipt receipt) {
    this.receipt = receipt;
  }

  public ResourceReceipt getReceipt() {
    return this.receipt;
  }

  public Sha256Hash getReceiptAddress() {
    return this.receiptAddress;
  }

  public void setCpuUsage(long usage) {
    receipt = receipt.toBuilder().setCpuUsage(usage).build();
  }

  public long getCpuUsage() {
    return receipt.getCpuUsage();
  }

  public void setNetUsage(long netUsage) {
    this.receipt = this.receipt.toBuilder().setNetUsage(netUsage).build();
  }

  public long getNetUsage() {
    return this.receipt.getNetUsage();
  }

  public void calculateCpuFee() {
    //TODO: calculate
  }

  public void setStorageDelta(long delta) {
    receipt = receipt.toBuilder().setStorageDelta(delta).build();
    this.receipt = this.receipt.toBuilder().setStorageDelta(delta).build();
  }

  public long getStorageDelta() {
    return receipt.getStorageDelta();
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

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }
}
