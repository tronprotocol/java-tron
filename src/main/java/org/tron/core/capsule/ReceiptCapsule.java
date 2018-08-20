package org.tron.core.capsule;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.db.EnergyProcessor;
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

  public void setEnergyUsage(long usage) {
    receipt = receipt.toBuilder().setEnergyUsage(usage).build();
  }

  public void setEnergyFee(long fee) {
    receipt = receipt.toBuilder().setEnergyFee(fee).build();
  }

  public long getEnergyUsage() {
    return receipt.getEnergyUsage();
  }

  public long getEnergyFee() {
    return receipt.getEnergyFee();
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

  public void calculateEnergyFee() {
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
   * payEnergyBill pay receipt energy bill by energy processor.
   */
  public void payEnergyBill(
      Manager manager,
      AccountCapsule origin,
      AccountCapsule caller,
      long percent,
      EnergyProcessor energyProcessor,
      long now) {
    if (0 == receipt.getEnergyUsage()) {
      return;
    }

    if (caller.getAddress().equals(origin.getAddress())) {
      payEnergyBill(manager, caller, receipt.getEnergyUsage(), energyProcessor, now);
    } else {
      long originUsage = receipt.getEnergyUsage() * percent / 100;
      originUsage = Math
          .min(originUsage, energyProcessor.getAccountLeftEnergyFromFreeze(origin));
      long callerUsage = receipt.getEnergyUsage() - originUsage;
      payEnergyBill(manager, origin, originUsage, energyProcessor, now);
      this.setOriginEnergyUsage(originUsage);
      payEnergyBill(manager, caller, callerUsage, energyProcessor, now);
    }
  }

  private void payEnergyBill(
      Manager manager,
      AccountCapsule account,
      long usage,
      EnergyProcessor energyProcessor,
      long now) {
    long accountEnergyLeft = energyProcessor.getAccountLeftEnergyFromFreeze(account);
    if (accountEnergyLeft >= usage) {
      energyProcessor.useEnergy(account, usage, now);
    } else {
      energyProcessor.useEnergy(account, accountEnergyLeft, now);
      long energyFee = (usage - accountEnergyLeft) * Constant.SUN_PER_GAS;
      this.setEnergyUsage(getEnergyUsage() - (usage - accountEnergyLeft));
      this.setEnergyFee(energyFee);
      account.setBalance(account.getBalance() - energyFee);
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

  public void setOriginEnergyUsage(long delta) {
    this.receipt = this.receipt.toBuilder().setOriginEnergyUsage(delta).build();
  }

  public void setOriginStorageDelta(long delta) {
    this.receipt = this.receipt.toBuilder().setOriginStorageDelta(delta).build();
  }
}
