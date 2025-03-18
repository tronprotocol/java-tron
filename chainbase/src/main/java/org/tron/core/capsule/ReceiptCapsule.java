package org.tron.core.capsule;

import static org.tron.common.math.Maths.min;
import static org.tron.common.math.Maths.multiplyExact;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Commons;
import org.tron.common.utils.ForkController;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.StringUtil;
import org.tron.core.Constant;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.db.EnergyProcessor;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

public class ReceiptCapsule {

  private ResourceReceipt receipt;

  @Getter
  @Setter
  private long multiSignFee;

  @Getter
  @Setter
  private long memoFee;

  /**
   * Available energy of contract deployer before executing transaction
   */
  @Setter
  private long originEnergyLeft;

  /**
   * Available energy of caller before executing transaction
   */
  @Setter
  private long callerEnergyLeft;

  /**
   * Energy usage of caller before merging frozen energy
   */
  @Getter
  @Setter
  private long callerEnergyUsage;

  /**
   * Energy usage of caller after merging frozen energy
   */
  @Getter
  @Setter
  private long callerEnergyMergedUsage;

  /**
   * Energy usage of origin after merging frozen energy
   */
  @Getter
  @Setter
  private long originEnergyMergedUsage;

  /**
   * Window size of caller before merging frozen energy
   */
  @Getter
  @Setter
  private long callerEnergyWindowSize;

  @Getter
  @Setter
  private long callerEnergyWindowSizeV2;

  /**
   * Window size of caller after merging frozen energy
   */
  @Getter
  @Setter
  private long callerEnergyMergedWindowSize;

  /**
   * Window size of origin before merging frozen energy
   */
  @Getter
  @Setter
  private long originEnergyWindowSize;

  @Getter
  @Setter
  private long originEnergyWindowSizeV2;

  /**
   * Window size of origin after merging frozen energy
   */
  @Getter
  @Setter
  private long originEnergyMergedWindowSize;

  private Sha256Hash receiptAddress;

  public ReceiptCapsule(ResourceReceipt data, Sha256Hash receiptAddress) {
    this.receipt = data;
    this.receiptAddress = receiptAddress;
  }

  public ReceiptCapsule(Sha256Hash receiptAddress) {
    this.receipt = ResourceReceipt.newBuilder().build();
    this.receiptAddress = receiptAddress;
  }

  public static ResourceReceipt copyReceipt(ReceiptCapsule origin) {
    return origin.getReceipt().toBuilder().build();
  }

  public static boolean checkForEnergyLimit(DynamicPropertiesStore ds) {
    long blockNum = ds.getLatestBlockHeaderNumber();
    return blockNum >= CommonParameter.getInstance()
        .getBlockNumForEnergyLimit();
  }

  public ResourceReceipt getReceipt() {
    return this.receipt;
  }

  public void setReceipt(ResourceReceipt receipt) {
    this.receipt = receipt;
  }

  public Sha256Hash getReceiptAddress() {
    return this.receiptAddress;
  }

  public void addNetFee(long netFee) {
    this.receipt = this.receipt.toBuilder().setNetFee(getNetFee() + netFee).build();
  }

  public long getEnergyUsage() {
    return this.receipt.getEnergyUsage();
  }

  public void setEnergyUsage(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setEnergyUsage(energyUsage).build();
  }

  public long getEnergyFee() {
    return this.receipt.getEnergyFee();
  }

  public void setEnergyFee(long energyFee) {
    this.receipt = this.receipt.toBuilder().setEnergyFee(energyFee).build();
  }

  public long getOriginEnergyUsage() {
    return this.receipt.getOriginEnergyUsage();
  }

  public void setOriginEnergyUsage(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setOriginEnergyUsage(energyUsage).build();
  }

  public long getEnergyUsageTotal() {
    return this.receipt.getEnergyUsageTotal();
  }

  public void setEnergyUsageTotal(long energyUsage) {
    this.receipt = this.receipt.toBuilder().setEnergyUsageTotal(energyUsage).build();
  }

  public long getEnergyPenaltyTotal() {
    return this.receipt.getEnergyPenaltyTotal();
  }

  public void setEnergyPenaltyTotal(long penalty) {
    this.receipt = this.receipt.toBuilder().setEnergyPenaltyTotal(penalty).build();
  }

  public long getNetUsage() {
    return this.receipt.getNetUsage();
  }

  public void setNetUsage(long netUsage) {
    this.receipt = this.receipt.toBuilder().setNetUsage(netUsage).build();
  }

  public long getNetFee() {
    return this.receipt.getNetFee();
  }

  public void setNetFee(long netFee) {
    this.receipt = this.receipt.toBuilder().setNetFee(netFee).build();
  }

  /**
   * payEnergyBill pay receipt energy bill by energy processor.
   */
  public void payEnergyBill(DynamicPropertiesStore dynamicPropertiesStore,
      AccountStore accountStore, ForkController forkController, AccountCapsule origin,
      AccountCapsule caller,
      long percent, long originEnergyLimit, EnergyProcessor energyProcessor, long now)
      throws BalanceInsufficientException {

    // Reset origin energy usage here! Because after stake 2.0, this field are reused for
    // recording pre-merge frozen energy for origin account. If total energy usage is zero, this
    // field will be a dirty record.
    this.setOriginEnergyUsage(0);

    if (receipt.getEnergyUsageTotal() <= 0) {
      return;
    }

    if (Objects.isNull(origin) && dynamicPropertiesStore.getAllowTvmConstantinople() == 1) {
      payEnergyBill(dynamicPropertiesStore, accountStore, forkController, caller,
          receipt.getEnergyUsageTotal(), receipt.getResult(), energyProcessor, now);
      return;
    }
    boolean useStrict2 = dynamicPropertiesStore.disableJavaLangMath();

    if ((!Objects.isNull(origin)) && caller.getAddress().equals(origin.getAddress())) {
      payEnergyBill(dynamicPropertiesStore, accountStore, forkController, caller,
          receipt.getEnergyUsageTotal(), receipt.getResult(), energyProcessor, now);
    } else {
      long originUsage = multiplyExact(receipt.getEnergyUsageTotal(), percent, useStrict2) / 100;
      originUsage = getOriginUsage(dynamicPropertiesStore, origin, originEnergyLimit,
          energyProcessor,
          originUsage);

      long callerUsage = receipt.getEnergyUsageTotal() - originUsage;
      energyProcessor.useEnergy(origin, originUsage, now);
      this.setOriginEnergyUsage(originUsage);
      payEnergyBill(dynamicPropertiesStore, accountStore, forkController,
          caller, callerUsage, receipt.getResult(), energyProcessor, now);
    }
  }

  private long getOriginUsage(DynamicPropertiesStore dynamicPropertiesStore, AccountCapsule origin,
      long originEnergyLimit,
      EnergyProcessor energyProcessor, long originUsage) {
    boolean useStrict2 = dynamicPropertiesStore.disableJavaLangMath();
    if (dynamicPropertiesStore.getAllowTvmFreeze() == 1
        || dynamicPropertiesStore.supportUnfreezeDelay()) {
      return min(originUsage, min(originEnergyLeft, originEnergyLimit, useStrict2), useStrict2);
    }

    if (checkForEnergyLimit(dynamicPropertiesStore)) {
      return min(originUsage,
          min(energyProcessor.getAccountLeftEnergyFromFreeze(origin), originEnergyLimit,
              useStrict2), useStrict2);
    }
    return min(originUsage, energyProcessor.getAccountLeftEnergyFromFreeze(origin), useStrict2);
  }

  private void payEnergyBill(
      DynamicPropertiesStore dynamicPropertiesStore, AccountStore accountStore,
      ForkController forkController,
      AccountCapsule account,
      long usage,
      contractResult contractResult,
      EnergyProcessor energyProcessor,
      long now) throws BalanceInsufficientException {
    long accountEnergyLeft;
    if (dynamicPropertiesStore.getAllowTvmFreeze() == 1
        || dynamicPropertiesStore.supportUnfreezeDelay()) {
      accountEnergyLeft = callerEnergyLeft;
    } else {
      accountEnergyLeft = energyProcessor.getAccountLeftEnergyFromFreeze(account);
    }
    if (accountEnergyLeft >= usage) {
      energyProcessor.useEnergy(account, usage, now);
      this.setEnergyUsage(usage);
    } else {
      energyProcessor.useEnergy(account, accountEnergyLeft, now);

      if (forkController.pass(ForkBlockVersionEnum.VERSION_3_6_5) &&
          dynamicPropertiesStore.getAllowAdaptiveEnergy() == 1) {
        long blockEnergyUsage =
            dynamicPropertiesStore.getBlockEnergyUsage() + (usage - accountEnergyLeft);
        dynamicPropertiesStore.saveBlockEnergyUsage(blockEnergyUsage);
      }

      long sunPerEnergy = Constant.SUN_PER_ENERGY;
      long dynamicEnergyFee = dynamicPropertiesStore.getEnergyFee();
      if (dynamicEnergyFee > 0) {
        sunPerEnergy = dynamicEnergyFee;
      }
      long energyFee =
          (usage - accountEnergyLeft) * sunPerEnergy;
      this.setEnergyUsage(accountEnergyLeft);
      this.setEnergyFee(energyFee);
      long balance = account.getBalance();
      if (balance < energyFee) {
        throw new BalanceInsufficientException(
            StringUtil.createReadableString(account.createDbKey()) + " insufficient balance");
      }
      account.setBalance(balance - energyFee);

      if (dynamicPropertiesStore.supportTransactionFeePool() &&
          !contractResult.equals(contractResult.OUT_OF_TIME)) {
        dynamicPropertiesStore.addTransactionFeePool(energyFee);
      } else if (dynamicPropertiesStore.supportBlackHoleOptimization()) {
        dynamicPropertiesStore.burnTrx(energyFee);
      } else {
        //send to blackHole
        Commons.adjustBalance(accountStore, accountStore.getBlackhole(),
            energyFee, dynamicPropertiesStore.disableJavaLangMath());
      }

    }

    accountStore.put(account.getAddress().toByteArray(), account);
  }

  public contractResult getResult() {
    return this.receipt.getResult();
  }

  public void setResult(contractResult success) {
    this.receipt = receipt.toBuilder().setResult(success).build();
  }
}
