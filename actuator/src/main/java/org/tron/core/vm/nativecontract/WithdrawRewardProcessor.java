package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;

import com.google.common.math.LongMath;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.vm.nativecontract.param.WithdrawRewardParam;
import org.tron.core.vm.repository.Repository;

@Slf4j(topic = "Processor")
public class WithdrawRewardProcessor {

  public void validate(WithdrawRewardParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    byte[] ownerAddress = param.getOwnerAddress();
    String readableOwnerAddress = StringUtil.encode58Check(ownerAddress);

    boolean isGP = CommonParameter.getInstance()
        .getGenesisBlock().getWitnesses().stream().anyMatch(witness ->
            Arrays.equals(ownerAddress, witness.getAddress()));
    if (isGP) {
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress
              + "] is a guard representative and is not allowed to withdraw Balance");
    }

    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);
    long reward = VoteRewardUtils.queryReward(ownerAddress, repo);

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
    byte[] ownerAddress = param.getOwnerAddress();

    VoteRewardUtils.withdrawReward(ownerAddress, repo);

    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);
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
