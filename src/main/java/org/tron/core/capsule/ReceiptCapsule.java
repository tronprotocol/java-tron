package org.tron.core.capsule;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.db.CpuProcessor;
import org.tron.core.db.Manager;
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
   */
  public void payCpuBill(
      Manager manager,
      AccountCapsule origin,
      AccountCapsule caller,
      int percent,
      CpuProcessor cpuProcessor,
      long now) {
    if (0 == receipt.getCpuUsage()) {
      return;
    }

    long originUsage = receipt.getCpuUsage() * percent / 100;
    originUsage = Math.min(originUsage, cpuProcessor.getAccountLeftCpuInUsFromFreeze(caller));
    long callerUsage = receipt.getCpuUsage() - originUsage;

    payCpuBill(manager, origin, originUsage, cpuProcessor, now);
    payCpuBill(manager, caller, callerUsage, cpuProcessor, now);
  }

  private void payCpuBill(
      Manager manager,
      AccountCapsule account,
      long usage,
      CpuProcessor cpuProcessor,
      long now) {

    if (cpuProcessor.getAccountLeftCpuInUsFromFreeze(account) >= usage) {
      cpuProcessor.useCpu(account, usage, now);
    } else {
      account.setBalance(account.getBalance() - usage * Constant.DROP_PER_CPU_US);
    }

    manager.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  /**
   * payStorageBill pay receipt storage bill by storage market.
   */
  public void payStorageBill(
      Manager manager,
      AccountCapsule origin,
      AccountCapsule caller,
      int percent,
      StorageMarket storageMarket) {
    if (0 == receipt.getStorageDelta()) {
      return;
    }

    long originDelta = receipt.getStorageDelta() * percent / 100;
    originDelta = Math.min(originDelta, origin.getStorageLeft());
    long callerDelta = receipt.getStorageDelta() - originDelta;

    payStorageBill(manager, origin, originDelta, storageMarket);
    payStorageBill(manager, caller, callerDelta, storageMarket);
  }

  private void payStorageBill(
      Manager manager,
      AccountCapsule account,
      long delta,
      StorageMarket storageMarket) {

    if (account.getStorageLeft() >= delta) {
      account.setStorageUsage(account.getStorageUsage() + delta);
    } else {
      long needStorage = delta - account.getStorageLeft();
      storageMarket.buyStorageBytes(account, needStorage);
      account.setStorageUsage(account.getStorageUsage() + needStorage);
    }

    manager.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  public void buyStorage(long storage) {
    //TODO: buy the min storage
  }

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }
}
