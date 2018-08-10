package org.tron.common.runtime;

import static org.tron.common.runtime.utils.MUtil.transfer;

import org.spongycastle.util.encoders.Hex;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactory;
import org.tron.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.tron.common.storage.Deposit;
import org.tron.common.storage.DepositImpl;
import org.tron.common.storage.DepositQueue;
import org.tron.common.storage.Key;
import org.tron.common.storage.Type;
import org.tron.common.storage.Value;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionTrace;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.OutOfSlotTimeException;

/**
 * Deposit controller : process pre transaction , block, query contract and etc..
 *
 * @author Guo Yonggang
 * @since 27.04.2018
 */
public class DepositController {

  private Manager dbManager;
  private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
  ;
  private static int QUEUE_SIZE = 4;
  private DepositQueue<Deposit> depositQueue = new DepositQueue<>(QUEUE_SIZE);

  private DepositController(Manager dbManager) {
    this.dbManager = dbManager;
  }

  private Deposit getLastDeposit() {
    if (depositQueue.isEmpty()) {
      return null;
    }
    return depositQueue.last();
  }

  /**
   * @API roll back one block's data
   */
  public Deposit rollback() {
    Deposit deposit = depositQueue.removeLast();
    Deposit lastDeposit = getLastDeposit();
    if (lastDeposit != null) {
      lastDeposit.setNextDeposit(null);
    }
    return deposit;
  }

  /**
   * The trx may be invalid due to check with not newest data.
   */
  public int preProcessTransaction(TransactionCapsule trxCap)
      throws ContractValidateException, ContractExeException, OutOfSlotTimeException {
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new Runtime(trxCap.getInstance(), deposit, programInvokeFactory);
    runtime.init();
    runtime.execute();
    runtime.go();
    ProgramResult programResult = runtime.getResult();
    if (programResult.getException() != null) {
      return -1;
    }

    return 0;
  }

  /**
   * @param block
   * @return
   */
  public int processBlock(BlockCapsule block)
      throws ContractValidateException, ContractExeException, OutOfSlotTimeException {
    Deposit lastDeposit = getLastDeposit();
    Deposit currentDeposit;
    if (lastDeposit == null) {
      currentDeposit = DepositImpl.createRoot(dbManager);
    } else {
      currentDeposit = lastDeposit.newDepositNext();
    }

    depositQueue.put(currentDeposit);
    for (TransactionCapsule trxCap : block.getTransactions()) {
      Deposit trxDeposit = currentDeposit.newDepositChild();
      Runtime runtime = new Runtime(new TransactionTrace(trxCap, trxDeposit.getDbManager()),
          block.getInstance(), trxDeposit,
          programInvokeFactory);
      runtime.init();
      runtime.execute();
      runtime.go();

      ProgramResult programResult = runtime.getResult();
      if (programResult.getException() != null) {
        rollback();
        return -1;
      }

      Key key = Key.create(trxCap.getTransactionId().getBytes());
      Value value = Value.create(trxCap.getData(), Type.VALUE_TYPE_CREATE);
      currentDeposit.putTransaction(key, value);
    }

    Key bKey = Key.create(block.getBlockId().getBytes());
    Value bValue = Value.create(block.getData(), Type.VALUE_TYPE_CREATE);
    currentDeposit.putBlock(bKey, bValue);

    // reward witness node
    byte[] foundAddress = Hex.decode("FF00");
    byte[] coinBase = block.getWitnessAddress().toByteArray();
    transfer(currentDeposit, foundAddress, coinBase, 36 * 1000000);

    if (depositQueue.size() > QUEUE_SIZE) {
      Deposit deposit = depositQueue.get();
      deposit.commit();
      depositQueue.peek().setPrevDeposit(null);
    }

    return 0;
  }

  /**
   *
   * @param trxCap
   * @return
   */
  public ProgramResult processConstantTransaction(TransactionCapsule trxCap)
      throws ContractValidateException, ContractExeException, OutOfSlotTimeException {
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new Runtime(trxCap.getInstance(), programInvokeFactory, deposit);
    runtime.init();
    runtime.execute();
    runtime.go();
    ProgramResult programResult = runtime.getResult();
    return programResult;
  }

  public ProgramInvokeFactory getProgramInvokeFactory() {
    return programInvokeFactory;
  }

  /**
   * Single instance
   */
  private static DepositController instance = null;

  public static DepositController create(Manager dbManager) {
    if (instance != null) {
      return instance;
    }
    instance = new DepositController(dbManager);
    return instance;
  }

  public static DepositController getInstance() {
    return instance;
  }
}
