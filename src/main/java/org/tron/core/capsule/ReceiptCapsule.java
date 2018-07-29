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
    receipt.toBuilder().setCpuUsage(usage);
  }

  public long getCpuUsage() {
    return receipt.toBuilder().getCpuUsage();
  }

  public void calculateCpuFee() {
    //TODO: calculate
  }

  public void setStorageDelta(long delta) {
    receipt.toBuilder().setStorageDelta(delta);
  }

  public long getStorageDelta() {
    return receipt.toBuilder().getStorageDelta();
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
