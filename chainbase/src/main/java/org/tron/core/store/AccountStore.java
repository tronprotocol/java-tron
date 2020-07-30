package org.tron.core.store;

import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Commons;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockBalanceTraceCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.db.accountstate.AccountStateCallBackUtils;
import org.tron.core.exception.BadItemException;
import org.tron.protos.contract.BalanceContract.TransactionBalanceTrace;
import org.tron.protos.contract.BalanceContract.TransactionBalanceTrace.Operation;

@Slf4j(topic = "DB")
@Component
public class AccountStore extends TronStoreWithRevoking<AccountCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

  @Autowired
  private AccountStateCallBackUtils accountStateCallBackUtils;

  @Autowired
  private BalanceTraceStore balanceTraceStore;

  @Autowired
  private AccountStore(@Value("account") String dbName) {
    super(dbName);
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  @Override
  public void put(byte[] key, AccountCapsule item) {
    AccountCapsule old = super.getUnchecked(key);
    if (old == null) {
      recordBalance(item, item.getBalance());
    } else if (old.getBalance() != item.getBalance()){
      recordBalance(item, item.getBalance() - old.getBalance());
    }

    super.put(key, item);
    accountStateCallBackUtils.accountCallBack(key, item);
  }

  /**
   * Max TRX account.
   */
  public AccountCapsule getSun() {
    return getUnchecked(assertsAddress.get("Sun"));
  }

  /**
   * Min TRX account.
   */
  public AccountCapsule getBlackhole() {
    return getUnchecked(assertsAddress.get("Blackhole"));
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getZion() {
    return getUnchecked(assertsAddress.get("Zion"));
  }

  @Override
  public void close() {
    super.close();
  }

  // do somethings
  // check old balance and new balance, if equals, do nothing, then get balance trace from balancetraceStore
  private void recordBalance(AccountCapsule accountCapsule, long diff) {
    BlockBalanceTraceCapsule blockBalanceTraceCapsule = null;
    try {
      blockBalanceTraceCapsule = balanceTraceStore.getCurrentBlockBalanceTrace();
    } catch (BadItemException e) {
      logger.error(e.getMessage(), e);
    }

    if (blockBalanceTraceCapsule == null) {
      return;
    }

    int index = 0;
    boolean first = false;
    Sha256Hash currentTransactionId = balanceTraceStore.getCurrentTransactionId();
    TransactionBalanceTrace transactionBalanceTrace = null;
    for(; index < blockBalanceTraceCapsule.getInstance().getTransactionBalanceTraceCount(); index++) {
      TransactionBalanceTrace tmp = blockBalanceTraceCapsule.getInstance().getTransactionBalanceTrace(index);
      if (tmp.getTransactionIdentifier().equals(currentTransactionId.getByteString())) {
        transactionBalanceTrace = tmp;
        break;
      }
    }

    if (transactionBalanceTrace == null) {
      transactionBalanceTrace = TransactionBalanceTrace.newBuilder()
          .setTransactionIdentifier(currentTransactionId.getByteString())
          .build();
      first = true;
    }

    ByteString currentOwner = balanceTraceStore.getCurrentOwner();
    ByteString address = accountCapsule.getAddress();
    long operationIdentifier = 0;
    if (!address.equals(currentOwner)) {
      OptionalLong max = transactionBalanceTrace.getOperationList().stream()
          .mapToLong(Operation::getOperationIdentifier)
          .max();
      if (max.isPresent()) {
        operationIdentifier = max.getAsLong() + 1;
      } else {
        operationIdentifier = 1;
      }
    }

    TransactionBalanceTrace.Operation operation = Operation.newBuilder()
        .setAddress(address)
        .setAmount(String.valueOf(diff))
        .setOperationIdentifier(operationIdentifier)
        .build();
    if (operationIdentifier > 0) {
      operation = operation.toBuilder()
          .addRelatedOperation(0)
          .build();
    }
    transactionBalanceTrace = transactionBalanceTrace.toBuilder()
        .addOperation(operation)
        .build();

    if (first) {
      blockBalanceTraceCapsule.addTransactionBalanceTrace(transactionBalanceTrace);
    } else {
      blockBalanceTraceCapsule.setTransactionBalanceTrace(index, transactionBalanceTrace);
    }
    balanceTraceStore.putBlockBalanceTrace(blockBalanceTraceCapsule);
  }

}
