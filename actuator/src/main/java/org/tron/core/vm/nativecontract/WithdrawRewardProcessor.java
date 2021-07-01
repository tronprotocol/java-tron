package org.tron.core.vm.nativecontract;

import com.google.common.math.LongMath;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.nativecontract.param.WithdrawRewardParam;
import org.tron.core.vm.repository.Repository;

import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;

@Slf4j(topic = "Processor")
public class WithdrawRewardProcessor {

  public void validate(WithdrawRewardParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(ContractProcessorConstant.STORE_NOT_EXIST);
    }

    byte[] targetAddress = param.getTargetAddress();
    AccountCapsule accountCapsule = repo.getAccount(targetAddress);
    long reward = VoteRewardUtils.queryReward(targetAddress, repo);

    try {
      LongMath.checkedAdd(LongMath.checkedAdd(
          accountCapsule.getBalance(),
          accountCapsule.getAllowance()),
          reward);
    } catch (ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
  }

  public long execute(WithdrawRewardParam param, Repository repo) {
    byte[] target = param.getTargetAddress();

    VoteRewardUtils.withdrawReward(target, repo);

    AccountCapsule accountCapsule = repo.getAccount(target);
    long oldBalance = accountCapsule.getBalance();
    long allowance = accountCapsule.getAllowance();

    // If no allowance, do nothing and just return zero.
    if (allowance <= 0) {
      return 0;
    }

    accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
        .setBalance(oldBalance + allowance)
        .setAllowance(0L)
        .setLatestWithdrawTime(param.getNowInMs())
        .build());

    repo.putAccountValue(accountCapsule.createDbKey(), accountCapsule);
    return allowance;
  }
}
