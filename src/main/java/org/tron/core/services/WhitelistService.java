package org.tron.core.services;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.db.Manager;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.exception.WhitelistException;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

// TODO
@Component
@Slf4j
public class WhitelistService {
  private static Map<WrappedByteArray, WrappedByteArray> whitelist = new HashMap<>();

  public WhitelistService() {
    WhitelistTestCase.testMap.forEach((k, v) -> {
      WrappedByteArray key = WrappedByteArray.of(ByteArray.fromHexString(k));
      WrappedByteArray value = WrappedByteArray.of(ByteArray.fromHexString(v));
      whitelist.put(key, value);
    });
  }

  // TODO
  @PostConstruct
  public void loadFromConfig() {
//    Args.getInstance().getBlacklist().forEach((k, v) -> {
//      WrappedByteArray key = WrappedByteArray.of(ByteArray.fromHexString(k));
//      WrappedByteArray value = WrappedByteArray.of(ByteArray.fromHexString(v));
//      whitelist.put(key, value);
//    });
  }

  public static void check(TransactionCapsule transactionCapsule) throws WhitelistException {
    if (!ForkController.pass(ForkBlockVersionEnum.VERSION_3_5)) {
      return;
    }

    if (MapUtils.isEmpty(whitelist)) {
      return;
    }

    Contract contract = transactionCapsule.getInstance().getRawData().getContractList().get(0);
    Contract.ContractType contractType = contract.getType();
    if (contractType == ContractType.UnfreezeBalanceContract) {
      return;
    }

    byte[] fromAddress = TransactionCapsule.getOwner(contract);
    byte[] toAddress = TransactionCapsule.getToAddress(contract);
    WrappedByteArray from = WrappedByteArray.of(fromAddress);
    WrappedByteArray to = WrappedByteArray.of(toAddress);
    WrappedByteArray value = whitelist.get(from);
    if (Objects.nonNull(value) && (contractType != ContractType.TransferContract || !value.equals(to))) {
      throw new WhitelistException();
    }
  }

  @Component
  public static class ForkController {

    private static final byte VERSION_UPGRADE = (byte) 1;

    @Getter
    private static Manager manager;

    @Autowired
    public void setManager(Manager manager) {
      ForkController.manager = manager;
    }

    public static boolean pass(ForkBlockVersionEnum forkBlockVersionEnum) {
      return pass(forkBlockVersionEnum.getValue());
    }

    private static synchronized boolean pass(int version) {
      byte[] stats = manager.getDynamicPropertiesStore().statsByVersion(version);
      return check(stats);
    }

    private static boolean check(byte[] stats) {
      if (stats == null || stats.length == 0) {
        return false;
      }

      int count = 0;
      for (int i = 0; i < stats.length; i++) {
        if (VERSION_UPGRADE == stats[i]) {
          ++count;
        }
      }

      if (count >= 24) {
        return true;
      }

      return false;
    }
  }

}
