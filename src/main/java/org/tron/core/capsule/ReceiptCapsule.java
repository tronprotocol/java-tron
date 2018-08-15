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

  public void setCpuFee(long fee) {
    receipt = receipt.toBuilder().setCpuFee(fee).build();
  }

  public long getCpuUsage() {
    return receipt.getCpuUsage();
  }

  public long getCpuFee() {
    return receipt.getCpuFee();
  }

  public void setNetUsage(long netUsage) {
    this.receipt = this.receipt.toBuilder().setNetUsage(netUsage).build();
  }

  public void setNetFee(long netFee) {
    this.receipt = this.receipt.toBuilder().setNetFee(netFee).build();
  }

  public long getNetUsage() {
    return this.receipt.getNetUsage();
  }

  public long getNetFee() {
    return this.receipt.getNetFee();
  }

  public void calculateCpuFee() {
    //TODO: calculate
  }

  public void setStorageDelta(long delta) {
    this.receipt = this.receipt.toBuilder().setStorageDelta(delta).build();
  }

  public void setStorageFee(long fee) {
    this.receipt = this.receipt.toBuilder().setStorageFee(fee).build();
  }

  public long getStorageDelta() {
    return receipt.getStorageDelta();
  }

  public long getStorageFee() {
    return receipt.getStorageFee();
  }

  /**
   * payCpuBill pay receipt cpu bill by cpu processor.
   */
  public void payCpuBill(
      Manager manager,
      AccountCapsule origin,
      AccountCapsule caller,
      long percent,
      CpuProcessor cpuProcessor,
      long now) {
    if (0 == receipt.getCpuUsage()) {
      return;
    }

    if (caller.getAddress().equals(origin.getAddress())) {
      payCpuBill(manager, caller, receipt.getCpuUsage(), cpuProcessor, now);
    } else {
      long originUsage = receipt.getCpuUsage() * percent / 100;
      originUsage = Math.min(originUsage, cpuProcessor.getAccountLeftCpuInUsFromFreeze(origin));
      long callerUsage = receipt.getCpuUsage() - originUsage;
      payCpuBill(manager, origin, originUsage, cpuProcessor, now);
      this.setOriginCpuUsage(originUsage);
      payCpuBill(manager, caller, callerUsage, cpuProcessor, now);
    }
  }

  private void payCpuBill(
      Manager manager,
      AccountCapsule account,
      long usage,
      CpuProcessor cpuProcessor,
      long now) {
    long accountCpuLeft = cpuProcessor.getAccountLeftCpuInUsFromFreeze(account);
    if (accountCpuLeft >= usage) {
      cpuProcessor.useCpu(account, usage, now);
    } else {
      cpuProcessor.useCpu(account, accountCpuLeft, now);
      long cpuFee = (usage - accountCpuLeft) * Constant.SUN_PER_GAS;
      this.setCpuUsage(getCpuUsage() - (usage - accountCpuLeft));
      this.setCpuFee(cpuFee);
      account.setBalance(account.getBalance() - cpuFee);
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
      long percent,
      StorageMarket storageMarket) {
    if (0 == receipt.getStorageDelta()) {
      return;
    }

    if (caller.getAddress().equals(origin.getAddress())) {
      payStorageBill(manager, caller, receipt.getStorageDelta(), storageMarket);
    } else {
      long originDelta = receipt.getStorageDelta() * percent / 100;
      originDelta = Math.min(originDelta, origin.getStorageLeft());
      long callerDelta = receipt.getStorageDelta() - originDelta;
      payStorageBill(manager, origin, originDelta, storageMarket);
      this.setOriginStorageDelta(originDelta);
      payStorageBill(manager, caller, callerDelta, storageMarket);
    }
  }

  private void payStorageBill(
      Manager manager,
      AccountCapsule account,
      long delta,
      StorageMarket storageMarket) {

    this.setStorageDelta(delta);
    if (account.getStorageLeft() >= delta) {
      account.setStorageUsage(account.getStorageUsage() + delta);
    } else {
      long needStorage = delta - account.getStorageLeft();
      this.setStorageFee(storageMarket.tryBuyStorageBytes(needStorage));
      account = storageMarket.buyStorageBytes(account, needStorage);
      account.setStorageUsage(account.getStorageUsage() + delta);
    }

    manager.getAccountStore().put(account.getAddress().toByteArray(), account);
  }

  public void buyStorage(long storage) {
    //TODO: buy the min storage
  }

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }

  public void setOriginCpuUsage(long delta) {
    this.receipt = this.receipt.toBuilder().setOriginCpuUsage(delta).build();
  }

  public void setOriginStorageDelta(long delta) {
    this.receipt = this.receipt.toBuilder().setOriginStorageDelta(delta).build();
  }
}
