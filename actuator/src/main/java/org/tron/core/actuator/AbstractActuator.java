package org.tron.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import org.tron.common.math.Maths;
import org.tron.common.utils.Commons;
import org.tron.common.utils.ForkController;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.store.AccountStore;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


public abstract class AbstractActuator implements Actuator {

  protected Any any;
  protected ChainBaseManager chainBaseManager;
  protected Contract contract;
  protected TransactionCapsule tx;
  protected ForkController forkController;

  public AbstractActuator(ContractType type, Class<? extends GeneratedMessageV3> clazz) {
    TransactionFactory.register(type, getClass(), clazz);
  }

  public Any getAny() {
    return any;
  }

  public AbstractActuator setAny(Any any) {
    this.any = any;
    return this;
  }

  public ChainBaseManager getChainBaseManager() {
    return chainBaseManager;
  }

  public AbstractActuator setChainBaseManager(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
    return this;
  }

  public Contract getContract() {
    return contract;
  }

  public AbstractActuator setContract(Contract contract) {
    this.contract = contract;
    this.any = contract.getParameter();
    return this;
  }

  public TransactionCapsule getTx() {
    return tx;
  }

  public AbstractActuator setTx(TransactionCapsule tx) {
    this.tx = tx;
    return this;
  }

  public AbstractActuator setForkUtils(ForkController forkController) {
    this.forkController = forkController;
    return this;
  }

  public long addExact(long x, long y) {
    return Maths.addExact(x, y, this.disableJavaLangMath());
  }

  public long addExact(int x, int y) {
    return Maths.addExact(x, y, this.disableJavaLangMath());
  }

  public long floorDiv(long x, long y) {
    return Maths.floorDiv(x, y, this.disableJavaLangMath());
  }

  public long floorDiv(long x, int y) {
    return this.floorDiv(x, (long) y);
  }

  public long multiplyExact(long x, long y) {
    return Maths.multiplyExact(x, y, this.disableJavaLangMath());
  }

  public long multiplyExact(long x, int y) {
    return this.multiplyExact(x, (long) y);
  }

  public int multiplyExact(int x, int y) {
    return Maths.multiplyExact(x, y, this.disableJavaLangMath());
  }

  public long subtractExact(long x, long y) {
    return Maths.subtractExact(x, y, this.disableJavaLangMath());
  }

  public int min(int a, int b) {
    return Maths.min(a, b, this.disableJavaLangMath());
  }

  public long min(long a, long b) {
    return Maths.min(a, b, this.disableJavaLangMath());
  }

  public void adjustBalance(AccountStore accountStore, byte[] accountAddress, long amount)
      throws BalanceInsufficientException {
    AccountCapsule account = accountStore.getUnchecked(accountAddress);
    this.adjustBalance(accountStore, account, amount);
  }

  /**
   * judge balance.
   */
  public void adjustBalance(AccountStore accountStore, AccountCapsule account, long amount)
      throws BalanceInsufficientException {
    Commons.adjustBalance(accountStore, account, amount, this.disableJavaLangMath());
  }

  private boolean disableJavaLangMath() {
    return chainBaseManager.getDynamicPropertiesStore().disableJavaLangMath();
  }
}
