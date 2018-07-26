package org.tron.core.db;


import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol.Account.AccountResource;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j
public class CpuProcessor extends ResourceProcessor {

  public CpuProcessor(Manager manager) {
    super(manager);
  }

  @Override
  public void updateUsage(AccountCapsule accountCapsule) {
    long now = dbManager.getWitnessController().getHeadSlot();
    updateUsage(accountCapsule, now);
  }

  private void updateUsage(AccountCapsule accountCapsule, long now) {
    AccountResource accountResource = accountCapsule.getAccountResource();

    long oldCpuUsage = accountResource.getCpuUsage();
    long latestConsumeTime = accountResource.getLatestConsumeTimeForCpu();

    accountCapsule.setCpuUsage(increase(oldCpuUsage, 0, latestConsumeTime, now));

  }

  @Override
  public void consume(TransactionCapsule trx, TransactionResultCapsule ret)
      throws ContractValidateException, AccountResourceInsufficientException {
    List<Contract> contracts =
        trx.getInstance().getRawData().getContractList();

    for (Contract contract : contracts) {

      if (contract.isPrecompiled()) {
        continue;
      }
      long cpuTime = trx.getReceipt().getCpuTime();
      logger.debug("trxId {},cpu cost :{}", trx.getTransactionId(), cpuTime);
      byte[] address = TransactionCapsule.getOwner(contract);
      AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
      if (accountCapsule == null) {
        throw new ContractValidateException("account not exists");
      }
      long now = dbManager.getWitnessController().getHeadSlot();

      int creatorRatio = contract.getUserCpuComsumeRatio();

      if (!useContractCreatorCpu(contract, cpuTime * creatorRatio / 100, now)) {
        throw new ContractValidateException("creator has not enough cpu");
      }

      long userCpuTime = cpuTime * (100 - creatorRatio) / 100;
      //1.The creator and the use of this have sufficient resources
      if (useAccountCpu(accountCapsule, userCpuTime, now)) {
        continue;
      }

      long feeLimit = getUserFeeLimit();
      long fee = calculateFee(userCpuTime);
      if (fee > feeLimit) {
        throw new AccountResourceInsufficientException(
            "Account has Insufficient Cpu and feeLimit is not engouht to trigger this contract");
      }

      //2.The creator of this have sufficient resources
      if (useFee(accountCapsule, fee, ret)) {
        continue;
      }

      throw new AccountResourceInsufficientException(
          "Account Insufficient Cpu and balance to trigger this contract");
    }
  }

  private long calculateFee(long userCpuTime) {
    return userCpuTime * 30;// 30 sun / macrisecond
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

  private boolean useContractCreatorCpu(Contract contract, long cpuTime, long now) {

    AccountCapsule accountCapsule = dbManager
        .getAccountStore().get(contract.getResourceRelatedAccount());

    long cpuUsage = accountCapsule.getCpuUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForCpu();
    long cpuLimit = calculateGlobalCpuLimit(accountCapsule.getCpuFrozenBalance());
//    long totalCpuLimitInConfig = contract.getContractResource().getCpuTotalLimit();//total contract

    long newCpuUsage = increase(cpuUsage, 0, latestConsumeTime, now);

    if (cpuTime > (cpuLimit - newCpuUsage)) {
      logger.debug("ContractCreator's cpu is running out. ");
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newCpuUsage = increase(newCpuUsage, cpuTime, latestConsumeTime, now);
    accountCapsule.setNetUsage(newCpuUsage);
    accountCapsule.setLatestOperationTime(latestOperationTime);
    accountCapsule.setLatestConsumeTime(latestConsumeTime);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    return true;
  }

  private boolean useAccountCpu(AccountCapsule accountCapsule, long cpuTime, long now) {

    long cpuUsage = accountCapsule.getCpuUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForCpu();
    long cpuLimit = calculateGlobalCpuLimit(
        accountCapsule.getAccountResource().getFrozenBalanceForCpu().getFrozenBalance());

    long newCpuUsage = increase(cpuUsage, 0, latestConsumeTime, now);

    if (cpuTime > (cpuLimit - newCpuUsage)) {
      logger.debug("User's cpu is running out. now use fee");
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newCpuUsage = increase(newCpuUsage, cpuTime, latestConsumeTime, now);
    accountCapsule.setNetUsage(newCpuUsage);
    accountCapsule.setLatestOperationTime(latestOperationTime);
    accountCapsule.setLatestConsumeTime(latestConsumeTime);

    dbManager.getAccountStore().put(accountCapsule.createDbKey(), accountCapsule);
    return true;
  }


  public long calculateGlobalCpuLimit(long frozeBalance) {
    if (frozeBalance < 1000_000L) {
      return 0;
    }
    long cpuWeight = frozeBalance / 1000_000L;
    long totalCpuLimit = dbManager.getDynamicPropertiesStore().getTotalCpuLimit();
    long totalCpuWeight = dbManager.getDynamicPropertiesStore().getTotalCpuWeight();
    assert totalCpuWeight > 0;
    return (long) (cpuWeight * ((double) totalCpuLimit / totalCpuWeight));
  }
}


