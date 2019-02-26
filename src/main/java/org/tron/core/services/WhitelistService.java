package org.tron.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ForkController;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ForkBlockVersionEnum;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.exception.WhitelistException;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

// TODO
@Component
@Slf4j
public class WhitelistService {
  private final static String TEST_FROM = "";
  private final static String TEST_TO = "";
  private static Map<WrappedByteArray, WrappedByteArray> whitelist = new HashMap<>();

  public WhitelistService() {
    // test
//    whitelist.put(WrappedByteArray.of(ByteArray.fromHexString(TEST_FROM)),
//        WrappedByteArray.of(ByteArray.fromHexString(TEST_TO)));
  }

  // TODO
  @PostConstruct
  public void loadFromConfig() {
    Args.getInstance().getBlacklist().forEach((k, v) -> {
      WrappedByteArray key = WrappedByteArray.of(ByteArray.fromHexString(k));
      WrappedByteArray value = WrappedByteArray.of(ByteArray.fromHexString(v));
      whitelist.put(key, value);
    });
  }

  public static void check(TransactionCapsule transactionCapsule) throws WhitelistException {
    if (!ForkController.instance().pass(ForkBlockVersionEnum.VERSION_3_5)) {
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
      throw new WhitelistException("Not the specified address. "
          + "from:" + Wallet.encode58Check(fromAddress)
          + ", to:" + Wallet.encode58Check(toAddress));
    }
  }
}
