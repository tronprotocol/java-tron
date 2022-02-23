package org.tron.core.vm.nativecontract;

import static org.tron.core.actuator.ActuatorConstant.STORE_NOT_EXIST;
import static org.tron.core.config.Parameter.ChainConstant.FROZEN_PERIOD;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.FastByteComparisons;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.vm.nativecontract.param.FreezeBalanceParam;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

@Slf4j(topic = "VMProcessor")
public class FreezeBalanceProcessor {

  public void validate(FreezeBalanceParam param, Repository repo) throws ContractValidateException {
    if (repo == null) {
      throw new ContractValidateException(STORE_NOT_EXIST);
    }

    // validate arg @frozenBalance
    byte[] ownerAddress = param.getOwnerAddress();
    AccountCapsule ownerCapsule = repo.getAccount(ownerAddress);
    long frozenBalance = param.getFrozenBalance();
    if (frozenBalance <= 0) {
      throw new ContractValidateException("FrozenBalance must be positive");
    } else if (frozenBalance < TRX_PRECISION) {
      throw new ContractValidateException("FrozenBalance must be more than 1TRX");
    } else if (frozenBalance > ownerCapsule.getBalance()) {
      throw new ContractValidateException("FrozenBalance must be less than accountBalance");
    }

    // validate frozen count of owner account
    int frozenCount = ownerCapsule.getFrozenCount();
    if (frozenCount != 0 && frozenCount != 1) {
      throw new ContractValidateException("FrozenCount must be 0 or 1");
    }

    // validate arg @resourceType
    switch (param.getResourceType()) {
      case BANDWIDTH:
      case ENERGY:
        break;
      default:
        throw new ContractValidateException(
            "ResourceCode error,valid ResourceCode[BANDWIDTH、ENERGY]");
    }

    // validate for delegating resource
    byte[] receiverAddress = param.getReceiverAddress();
    if (!FastByteComparisons.isEqual(ownerAddress, receiverAddress)) {
      param.setDelegating(true);

      // check if receiver account exists. if not, then create a new account
      AccountCapsule receiverCapsule = repo.getAccount(receiverAddress);
      if (receiverCapsule == null) {
        receiverCapsule = repo.createNormalAccount(receiverAddress);
      }

      // forbid delegating resource to contract account
      if (receiverCapsule.getType() == Protocol.AccountType.Contract) {
        throw new ContractValidateException(
            "Do not allow delegate resources to contract addresses");
      }
    }
  }

  public void execute(FreezeBalanceParam param,  Repository repo) {
    // calculate expire time
    DynamicPropertiesStore dynamicStore = repo.getDynamicPropertiesStore();
    long nowInMs = dynamicStore.getLatestBlockHeaderTimestamp();
    long expireTime = nowInMs + param.getFrozenDuration() * FROZEN_PERIOD;

    byte[] ownerAddress = param.getOwnerAddress();
    byte[] receiverAddress = param.getReceiverAddress();
    long frozenBalance = param.getFrozenBalance();
    AccountCapsule accountCapsule = repo.getAccount(ownerAddress);
    // acquire or delegate resource
    if (param.isDelegating()) { // delegate resource
      switch (param.getResourceType()) {
        case BANDWIDTH:
          delegateResource(ownerAddress, receiverAddress,
              frozenBalance, expireTime, true, repo);
          accountCapsule.addDelegatedFrozenBalanceForBandwidth(frozenBalance);
          break;
        case ENERGY:
          delegateResource(ownerAddress, receiverAddress,
              frozenBalance, expireTime, false, repo);
          accountCapsule.addDelegatedFrozenBalanceForEnergy(frozenBalance);
          break;
        default:
          logger.debug("Resource Code Error.");
      }
    } else { // acquire resource
      switch (param.getResourceType()) {
        case BANDWIDTH:
          accountCapsule.setFrozenForBandwidth(
              frozenBalance + accountCapsule.getFrozenBalance(),
              expireTime);
          break;
        case ENERGY:
          accountCapsule.setFrozenForEnergy(
              frozenBalance + accountCapsule.getAccountResource()
                  .getFrozenBalanceForEnergy()
                  .getFrozenBalance(),
              expireTime);
          break;
        default:
          logger.debug("Resource Code Error.");
      }
    }

    // adjust total resource
    switch (param.getResourceType()) {
      case BANDWIDTH:
        repo.addTotalNetWeight(frozenBalance / TRX_PRECISION);
        break;
      case ENERGY:
        repo.addTotalEnergyWeight(frozenBalance / TRX_PRECISION);
        break;
      default:
        //this should never happen
        break;
    }

    // deduce balance of owner account
    long newBalance = accountCapsule.getBalance() - frozenBalance;
    accountCapsule.setBalance(newBalance);
    repo.updateAccount(accountCapsule.createDbKey(), accountCapsule);
  }

  private void delegateResource(
      byte[] ownerAddress,
      byte[] receiverAddress,
      long frozenBalance,
      long expireTime,
      boolean isBandwidth,
      Repository repo) {
    byte[] key = DelegatedResourceCapsule.createDbKey(ownerAddress, receiverAddress);

    // insert or update DelegateResource
    DelegatedResourceCapsule delegatedResourceCapsule = repo.getDelegatedResource(key);
    if (delegatedResourceCapsule == null) {
      delegatedResourceCapsule = new DelegatedResourceCapsule(
          ByteString.copyFrom(ownerAddress),
          ByteString.copyFrom(receiverAddress));
    }
    if (isBandwidth) {
      delegatedResourceCapsule.addFrozenBalanceForBandwidth(frozenBalance, expireTime);
    } else {
      delegatedResourceCapsule.addFrozenBalanceForEnergy(frozenBalance, expireTime);
    }
    repo.updateDelegatedResource(key, delegatedResourceCapsule);

    // do delegating resource to receiver account
    AccountCapsule receiverCapsule = repo.getAccount(receiverAddress);
    if (isBandwidth) {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForBandwidth(frozenBalance);
    } else {
      receiverCapsule.addAcquiredDelegatedFrozenBalanceForEnergy(frozenBalance);
    }
    repo.updateAccount(receiverCapsule.createDbKey(), receiverCapsule);
  }
}
