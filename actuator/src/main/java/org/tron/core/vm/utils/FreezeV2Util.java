package org.tron.core.vm.utils;

import java.util.*;
import java.util.stream.Collectors;

import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.DelegatedResourceCapsule;
import org.tron.core.vm.config.VMConfig;
import org.tron.core.vm.repository.Repository;
import org.tron.protos.Protocol;

public class FreezeV2Util {

  private FreezeV2Util() {
  }

  public static long queryExpireFreezeV2Balance(byte[] address, long time, Repository repository) {
    if (!VMConfig.allowTvmFreezeV2()) {
      return 0;
    }

    AccountCapsule accountCapsule = repository.getAccount(address);
    List<Protocol.Account.UnFreezeV2> unfrozenV2List =
        accountCapsule.getInstance().getUnfrozenV2List();

    return getTotalWithdrawUnfreeze(unfrozenV2List, time);
  }

  // v1 included.
  public static long queryTotalFrozenBalance(byte[] address, long type, Repository repository) {
    if (!VMConfig.allowTvmFreezeV2()) {
      return 0;
    }

    AccountCapsule accountCapsule = repository.getAccount(address);

    // BANDWIDTH
    if (type == 0) {
      return accountCapsule.getAllFrozenBalanceForBandwidth();
    }

    // ENERGY
    if (type == 1) {
      return accountCapsule.getAllFrozenBalanceForEnergy();
    }

    // POWER
    if (type == 2) {
      return accountCapsule.getAllTronPower();
    }

    return 0;
  }

  // only freezeV2.
  public static long queryFrozenBalance(byte[] from, byte[] to, long type, Repository repository) {
    if (!VMConfig.allowTvmFreezeV2()) {
      return 0;
    }

    byte[] key = DelegatedResourceCapsule.createDbKeyV2(from, to);
    DelegatedResourceCapsule delegatedResource = repository.getDelegatedResource(key);
    if (delegatedResource == null) {
      return 0;
    }

    // BANDWIDTH
    if (type == 0) {
      return delegatedResource.getFrozenBalanceForBandwidth();
    }

    // ENERGY
    if (type == 1) {
      return delegatedResource.getFrozenBalanceForEnergy();
    }

    return 0;
  }

  public static long[] queryFrozenBalanceUsage(byte[] address, long type, Repository repository) {
    if (!VMConfig.allowTvmFreezeV2()) {
      return new long[]{0L, 0L};
    }

    AccountCapsule accountCapsule = repository.getAccount(address);

    if (type == 0) {
      return repository.getAccountNetUsageBalanceAndRestoreSeconds(accountCapsule);
    } else if (type == 1) {
      return repository.getAccountEnergyUsageBalanceAndRestoreSeconds(accountCapsule);
    }

    return new long[]{0L, 0L};
  }

  private static long getTotalWithdrawUnfreeze(List<Protocol.Account.UnFreezeV2> unfrozenV2List, long time) {
    return getTotalWithdrawList(unfrozenV2List, time).stream()
        .mapToLong(Protocol.Account.UnFreezeV2::getUnfreezeAmount).sum();
  }

  private static List<Protocol.Account.UnFreezeV2> getTotalWithdrawList(List<Protocol.Account.UnFreezeV2> unfrozenV2List, long now) {
    return unfrozenV2List.stream().filter(unfrozenV2 -> (unfrozenV2.getUnfreezeAmount() > 0
        && unfrozenV2.getUnfreezeExpireTime() <= now)).collect(Collectors.toList());
  }

}
