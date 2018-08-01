package org.tron.core.capsule;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.db.CpuProcessor;
import org.tron.core.db.StorageMarket;
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
    this.receipt = this.receipt.toBuilder().setStorageDelta(delta).build();
  }

  public long getStorageDelta() {
    return receipt.getStorageDelta();
  }

  /**
   * payCpuBill pay receipt cpu bill by cpu processor.
   *
   * @param account Smart contract caller.
   * @param cpuProcessor CPU processor.
   * @param now Witness slot time.
   */
  public void payCpuBill(AccountCapsule account, CpuProcessor cpuProcessor, long now) {
    if (0 == receipt.getCpuUsage()) {
      return;
    }

    if (cpuProcessor.getAccountLeftCpuInUsFromFreeze(account) >= receipt.getCpuUsage()) {
      cpuProcessor.useCpu(account, receipt.getCpuUsage(), now);
    } else {
      account.setBalance(account.getBalance() - receipt.getCpuUsage() * Constant.DROP_PER_CPU_US);
    }
  }

  /**
   * payStorageBill pay receipt storage bill by storage market.
   *
   * @param account Smart contract caller.
   * @param storageMarket Storage market.
   */
  public void payStorageBill(AccountCapsule account, StorageMarket storageMarket) {
    if (0 == receipt.getStorageDelta()) {
      return;
    }

    if (account.getStorageLeft() >= receipt.getStorageDelta()) {
      account.setStorageUsage(account.getStorageUsage() + receipt.getStorageDelta());
    } else {
      long needStorage = receipt.getStorageDelta() - account.getStorageLeft();
      storageMarket.buyStorage(account, needStorage);
      account.setStorageUsage(account.getStorageUsage() + needStorage);
    }
  }

  public void buyStorage(long storage) {
    //TODO: buy the min storage
  }

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }
}
