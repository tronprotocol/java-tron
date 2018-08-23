package org.tron.core.db;


import static java.lang.Long.max;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j
public class EnergyProcessor extends ResourceProcessor {

  public EnergyProcessor(Manager manager) {
    super(manager);
  }

  @Override
  public void updateUsage(AccountCapsule accountCapsule) {
    long now = dbManager.getWitnessController().getHeadSlot();
    updateUsage(accountCapsule, now);
  }

  private void updateUsage(AccountCapsule accountCapsule, long now) {
    AccountResource accountResource = accountCapsule.getAccountResource();

    long oldEnergyUsage = accountResource.getEnergyUsage();
    long latestConsumeTime = accountResource.getLatestConsumeTimeForEnergy();

    accountCapsule.setEnergyUsage(increase(oldEnergyUsage, 0, latestConsumeTime, now));

  }

  @Override
  public void consume(TransactionCapsule trx, TransactionResultCapsule ret,
      TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException {
    List<Contract> contracts =
        trx.getInstance().getRawData().getContractList();

    for (Contract contract : contracts) {

      //todo
//      if (contract.isPrecompiled()) {
//        continue;
//      }
      //todo
//      long energy = trx.getReceipt().getEnergy();
      long energy = 100L;
      logger.debug("trxId {},energy cost :{}", trx.getTransactionId(), energy);
      byte[] address = TransactionCapsule.getOwner(contract);
      AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
      if (accountCapsule == null) {
        throw new ContractValidateException("account not exists");
      }
      long now = dbManager.getWitnessController().getHeadSlot();

      //todo
//      int creatorRatio = contract.getUserEnergyConsumeRatio();
      int creatorRatio = 50;

      long creatorEnergy = energy * creatorRatio / 100;
      AccountCapsule contractProvider = dbManager.getAccountStore()
          .get(contract.getProvider().toByteArray());

      if (!useEnergy(contractProvider, creatorEnergy, now)) {
        throw new ContractValidateException(
            "creator has not enough energy[" + creatorEnergy + "]");
      }

      long userEnergy = energy * (100 - creatorRatio) / 100;
      //1.The creator and the use of this have sufficient resources
      if (useEnergy(accountCapsule, userEnergy, now)) {
        continue;
      }

//     todo  long feeLimit = getUserFeeLimit();
      long feeLimit = 1000000;//sun
      long fee = calculateFee(userEnergy);
      if (fee > feeLimit) {
        throw new AccountResourceInsufficientException(
            "Account has Insufficient Energy[" + userEnergy + "] and feeLimit[" + feeLimit
                + "] is not enough to trigger this contract");
      }

      //2.The creator of this have sufficient resources
      if (useFee(accountCapsule, fee, ret)) {
        continue;
      }

      throw new AccountResourceInsufficientException(
          "Account has insufficient Energy[" + userEnergy + "] and balance[" + fee
              + "] to trigger this contract");
    }
  }

  private long calculateFee(long userEnergy) {
    return userEnergy * 30;// 30 drop / macroSecond, move to dynamicStore later
  }


  private boolean useFee(AccountCapsule accountCapsule, long fee,
      TransactionResultCapsule ret) {
    if (consumeFee(accountCapsule, fee)) {
      ret.addFee(fee);
      return true;
    } else {
      return false;
    }
  }

  public boolean useEnergy(AccountCapsule accountCapsule, long energy, long now) {

    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(
        accountCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());

    long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

    if (energy > (energyLimit - newEnergyUsage)) {
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newEnergyUsage = increase(newEnergyUsage, energy, latestConsumeTime, now);
    accountCapsule.setEnergyUsage(newEnergyUsage);
    accountCapsule.setLatestOperationTime(latestOperationTime);
    accountCapsule.setLatestConsumeTimeForEnergy(latestConsumeTime);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    return true;
  }


  public long calculateGlobalEnergyLimit(long frozeBalance) {
    if (frozeBalance < 1000_000L) {
      return 0;
    }
    long energyWeight = frozeBalance / 1000_000L;
    long totalEnergyLimit = dbManager.getDynamicPropertiesStore().getTotalEnergyLimit();
    long totalEnergyWeight = dbManager.getDynamicPropertiesStore().getTotalEnergyWeight();
    assert totalEnergyWeight > 0;
    return (long) (energyWeight * ((double) totalEnergyLimit / totalEnergyWeight));
  }

  public long getAccountLeftEnergyFromFreeze(AccountCapsule accountCapsule) {

    long now = dbManager.getWitnessController().getHeadSlot();

    long energyUsage = accountCapsule.getEnergyUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForEnergy();
    long energyLimit = calculateGlobalEnergyLimit(
        accountCapsule.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance());

    long newEnergyUsage = increase(energyUsage, 0, latestConsumeTime, now);

    return max(energyLimit - newEnergyUsage, 0); // us
  }

}


