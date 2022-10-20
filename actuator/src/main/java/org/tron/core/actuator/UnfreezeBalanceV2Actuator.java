package org.tron.core.actuator;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.DecodeUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.service.MortgageService;
import org.tron.core.store.AccountStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.VotesStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.Common;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.tron.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

@Slf4j(topic = "actuator")
public class UnfreezeBalanceV2Actuator extends AbstractActuator {

  @Getter
  private static final int UNFREEZE_MAX_TIMES = 32;

  public UnfreezeBalanceV2Actuator() {
    super(ContractType.UnfreezeBalanceV2Contract, UnfreezeBalanceV2Contract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    final UnfreezeBalanceV2Contract unfreezeBalanceV2Contract;
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    MortgageService mortgageService = chainBaseManager.getMortgageService();
    try {
      unfreezeBalanceV2Contract = any.unpack(UnfreezeBalanceV2Contract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    byte[] ownerAddress = unfreezeBalanceV2Contract.getOwnerAddress().toByteArray();
    long now = dynamicStore.getLatestBlockHeaderTimestamp();

    mortgageService.withdrawReward(ownerAddress);

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    this.unfreezeExpire(accountCapsule, now);
    long unfreezeBalance = unfreezeBalanceV2Contract.getUnfreezeBalance();

    if (dynamicStore.supportAllowNewResourceModel()
        && accountCapsule.oldTronPowerIsNotInitialized()) {
      accountCapsule.initializeOldTronPower();
    }

    Common.ResourceCode freezeType = unfreezeBalanceV2Contract.getResource();

    this.updateAccountFrozenInfo(freezeType, accountCapsule, unfreezeBalance);

    long expireTime = this.calcUnfreezeExpireTime(now);
    accountCapsule.addUnfrozenV2List(freezeType, unfreezeBalance, expireTime);

    this.updateTotalResourceWeight(unfreezeBalanceV2Contract, unfreezeBalance);
    this.clearVotes(accountCapsule,unfreezeBalanceV2Contract, ownerAddress);

    if (dynamicStore.supportAllowNewResourceModel()
        && !accountCapsule.oldTronPowerIsInvalid()) {
      accountCapsule.invalidateOldTronPower();
    }

    accountStore.put(ownerAddress, accountCapsule);

    ret.setUnfreezeAmount(unfreezeBalance);
    ret.setStatus(fee, code.SUCESS);
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    if (!this.any.is(UnfreezeBalanceV2Contract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [UnfreezeBalanceContract], real type[" + any
              .getClass() + "]");
    }

    if (!dynamicStore.supportUnfreezeDelay()) {
      throw new ContractValidateException("Not support UnfreezeV2 transaction,"
          + " need to be opened by the committee");
    }

    final UnfreezeBalanceV2Contract unfreezeBalanceV2Contract;
    try {
      unfreezeBalanceV2Contract = this.any.unpack(UnfreezeBalanceV2Contract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = unfreezeBalanceV2Contract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] does not exist");
    }

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    switch (unfreezeBalanceV2Contract.getResource()) {
      case BANDWIDTH:
        if (!this.checkExistFreezedBalance(accountCapsule, Common.ResourceCode.BANDWIDTH)) {
          throw new ContractValidateException("no frozenBalance(BANDWIDTH)");
        }
        break;
      case ENERGY:
        if (!this.checkExistFreezedBalance(accountCapsule, Common.ResourceCode.ENERGY)) {
          throw new ContractValidateException("no frozenBalance(Energy)");
        }
        break;
      case TRON_POWER:
        if (dynamicStore.supportAllowNewResourceModel()) {
          if (!this.checkExistFreezedBalance(accountCapsule, Common.ResourceCode.TRON_POWER)) {
            throw new ContractValidateException("no frozenBalance(TronPower)");
          }
        } else {
            throw new ContractValidateException("ResourceCode error.valid ResourceCode[BANDWIDTH、Energy]");
        }
        break;
      default:
        if (dynamicStore.supportAllowNewResourceModel()) {
          throw new ContractValidateException("ResourceCode error.valid ResourceCode[BANDWIDTH、Energy、TRON_POWER]");
        } else {
          throw new ContractValidateException("ResourceCode error.valid ResourceCode[BANDWIDTH、Energy]");
        }
    }

    if (!checkUnfreezeBalance(accountCapsule, unfreezeBalanceV2Contract, unfreezeBalanceV2Contract.getResource())) {
      throw new ContractValidateException(
              "Invalid unfreeze_balance, [" + unfreezeBalanceV2Contract.getUnfreezeBalance() + "] is error"
      );
    }

    int unfreezingCount = accountCapsule.getUnfreezingV2Count(now);
    if (UNFREEZE_MAX_TIMES <= unfreezingCount) {
      throw new ContractValidateException("Invalid unfreeze operation, unfreezing times is over limit");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UnfreezeBalanceV2Contract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

  public boolean checkExistFreezedBalance(AccountCapsule accountCapsule, Common.ResourceCode freezeType) {
    boolean checkOk = false;

    long frozenAmount = 0;
    List<Protocol.Account.FreezeV2> frozenV2List = accountCapsule.getFrozenV2List();
    for (Protocol.Account.FreezeV2 frozenV2 : frozenV2List) {
      if (frozenV2.getType().equals(freezeType)) {
        frozenAmount = frozenV2.getAmount();
        if (frozenAmount > 0) {
          checkOk = true;
          break;
        }
      }
    }

    return checkOk;
  }

  public boolean checkUnfreezeBalance(AccountCapsule accountCapsule,
                                       final UnfreezeBalanceV2Contract unfreezeBalanceV2Contract,
                                      Common.ResourceCode freezeType)  {
    boolean checkOk = false;

    long frozenAmount = 0L;
    List<Protocol.Account.FreezeV2> freezeV2List = accountCapsule.getFrozenV2List();
    for (Protocol.Account.FreezeV2 freezeV2 : freezeV2List) {
      if (freezeV2.getType().equals(freezeType)) {
        frozenAmount = freezeV2.getAmount();
        break;
      }
    }

    if (unfreezeBalanceV2Contract.getUnfreezeBalance() > 0
            && unfreezeBalanceV2Contract.getUnfreezeBalance() <= frozenAmount) {
      checkOk = true;
    }

    return checkOk;
  }

  public long calcUnfreezeExpireTime(long now) {
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    long unfreezeDelayDays = dynamicStore.getUnfreezeDelayDays();

    return now + unfreezeDelayDays * FROZEN_PERIOD;
  }

  public void updateAccountFrozenInfo(Common.ResourceCode freezeType, AccountCapsule accountCapsule, long unfreezeBalance) {
    List<Protocol.Account.FreezeV2> freezeV2List = accountCapsule.getFrozenV2List();
    for (int i = 0; i < freezeV2List.size(); i++) {
      if (freezeV2List.get(i).getType().equals(freezeType)) {
        Protocol.Account.FreezeV2 freezeV2 =  Protocol.Account.FreezeV2.newBuilder()
                .setAmount(freezeV2List.get(i).getAmount() - unfreezeBalance)
                .setType(freezeV2List.get(i).getType())
                .build();
        accountCapsule.updateFrozenV2List(i, freezeV2);
        break;
      }
    }
  }

  public void unfreezeExpire(AccountCapsule accountCapsule, long now) {
    long unfreezeBalance = 0L;

    List<Protocol.Account.UnFreezeV2> unFrozenV2List = Lists.newArrayList();
    unFrozenV2List.addAll(accountCapsule.getUnfrozenV2List());
    Iterator<Protocol.Account.UnFreezeV2> iterator = unFrozenV2List.iterator();

    while (iterator.hasNext()) {
      Protocol.Account.UnFreezeV2 next = iterator.next();
      if (next.getUnfreezeExpireTime() <= now) {
        unfreezeBalance += next.getUnfreezeAmount();
        iterator.remove();
      }
    }

    accountCapsule.setInstance(
            accountCapsule.getInstance().toBuilder()
            .setBalance(accountCapsule.getBalance() + unfreezeBalance)
            .clearUnfrozenV2()
            .addAllUnfrozenV2(unFrozenV2List).build()
    );
  }

  public void updateTotalResourceWeight(final UnfreezeBalanceV2Contract unfreezeBalanceV2Contract,
                                      long unfreezeBalance) {
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    switch (unfreezeBalanceV2Contract.getResource()) {
      case BANDWIDTH:
        dynamicStore.addTotalNetWeight(-unfreezeBalance / TRX_PRECISION);
        break;
      case ENERGY:
        dynamicStore.addTotalEnergyWeight(-unfreezeBalance / TRX_PRECISION);
        break;
      case TRON_POWER:
        dynamicStore.addTotalTronPowerWeight(-unfreezeBalance / TRX_PRECISION);
        break;
      default:
        //this should never happen
        break;
    }
  }

  private void clearVotes(AccountCapsule accountCapsule,
                          final UnfreezeBalanceV2Contract unfreezeBalanceV2Contract,
                          byte[] ownerAddress) {
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    VotesStore votesStore = chainBaseManager.getVotesStore();

    boolean needToClearVote = true;
    if (dynamicStore.supportAllowNewResourceModel()
            && accountCapsule.oldTronPowerIsInvalid()) {
      switch (unfreezeBalanceV2Contract.getResource()) {
        case BANDWIDTH:
        case ENERGY:
          needToClearVote = false;
          break;
        default:
          break;
      }
    }

    if (needToClearVote) {
      VotesCapsule votesCapsule;
      if (!votesStore.has(ownerAddress)) {
        votesCapsule = new VotesCapsule(
                unfreezeBalanceV2Contract.getOwnerAddress(),
                accountCapsule.getVotesList()
        );
      } else {
        votesCapsule = votesStore.get(ownerAddress);
      }
      accountCapsule.clearVotes();
      votesCapsule.clearNewVotes();
      votesStore.put(ownerAddress, votesCapsule);
    }
  }
}