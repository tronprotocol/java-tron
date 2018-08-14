package org.tron.core.db;


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
//      long cpuTime = trx.getReceipt().getCpuTime();
      long cpuTime = 100L;
      logger.debug("trxId {},cpu cost :{}", trx.getTransactionId(), cpuTime);
      byte[] address = TransactionCapsule.getOwner(contract);
      AccountCapsule accountCapsule = dbManager.getAccountStore().get(address);
      if (accountCapsule == null) {
        throw new ContractValidateException("account not exists");
      }
      long now = dbManager.getWitnessController().getHeadSlot();

      //todo
//      int creatorRatio = contract.getUserCpuConsumeRatio();
      int creatorRatio = 50;

      long creatorCpuTime = cpuTime * creatorRatio / 100;
      AccountCapsule contractProvider = dbManager.getAccountStore()
          .get(contract.getProvider().toByteArray());

      if (!useCpu(contractProvider, creatorCpuTime, now)) {
        throw new ContractValidateException("creator has not enough cpu[" + creatorCpuTime + "]");
      }

      long userCpuTime = cpuTime * (100 - creatorRatio) / 100;
      //1.The creator and the use of this have sufficient resources
      if (useCpu(accountCapsule, userCpuTime, now)) {
        continue;
      }

//     todo  long feeLimit = getUserFeeLimit();
      long feeLimit = 1000000;//sun
      long fee = calculateFee(userCpuTime);
      if (fee > feeLimit) {
        throw new AccountResourceInsufficientException(
            "Account has Insufficient Cpu[" + userCpuTime + "] and feeLimit[" + feeLimit
                + "] is not enough to trigger this contract");
      }

      //2.The creator of this have sufficient resources
      if (useFee(accountCapsule, fee, ret)) {
        continue;
      }

      throw new AccountResourceInsufficientException(
          "Account has insufficient Cpu[" + userCpuTime + "] and balance[" + fee
              + "] to trigger this contract");
    }
  }

  private long calculateFee(long userCpuTime) {
    return userCpuTime * 30;// 30 drop / macroSecond, move to dynamicStore later
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

  public boolean useCpu(AccountCapsule accountCapsule, long cpuTime, long now) {

    long cpuUsage = accountCapsule.getCpuUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForCpu();
    long cpuLimit = calculateGlobalCpuLimit(
        accountCapsule.getAccountResource().getFrozenBalanceForCpu().getFrozenBalance());

    long newCpuUsage = increase(cpuUsage, 0, latestConsumeTime, now);

    if (cpuTime > (cpuLimit - newCpuUsage)) {
      return false;
    }

    latestConsumeTime = now;
    long latestOperationTime = dbManager.getHeadBlockTimeStamp();
    newCpuUsage = increase(newCpuUsage, cpuTime, latestConsumeTime, now);
    accountCapsule.setCpuUsage(newCpuUsage);
    accountCapsule.setLatestOperationTime(latestOperationTime);
    accountCapsule.setLatestConsumeTimeForCpu(latestConsumeTime);

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

  // todo: will change the name from us to gas
  public long getAccountLeftCpuInUsFromFreeze(AccountCapsule accountCapsule) {

    long now = dbManager.getWitnessController().getHeadSlot();

    long cpuUsage = accountCapsule.getCpuUsage();
    long latestConsumeTime = accountCapsule.getAccountResource().getLatestConsumeTimeForCpu();
    long cpuLimit = calculateGlobalCpuLimit(
        accountCapsule.getAccountResource().getFrozenBalanceForCpu().getFrozenBalance());

    long newCpuUsage = increase(cpuUsage, 0, latestConsumeTime, now);

    return cpuLimit - newCpuUsage; // us
  }

}


