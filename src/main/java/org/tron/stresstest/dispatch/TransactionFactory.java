package org.tron.stresstest.dispatch;

import java.io.IOException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.stresstest.dispatch.strategy.Dispatcher;
import org.tron.stresstest.dispatch.strategy.Level1Strategy;
import org.tron.stresstest.dispatch.strategy.Level2Strategy;
import org.tron.protos.Protocol;

@Component
public class TransactionFactory {

  public static ApplicationContext context;

  private static Dispatcher dispatcher;

  public static void init(ApplicationContext ctx) {
    context = ctx;
    dispatcher = context.getBean(Dispatcher.class);
  }

  public static Protocol.Transaction newTransaction() {
    Level1Strategy level1Strategy = dispatcher.dispatch();
    if (level1Strategy == null) {
      return null;
    }

    Level2Strategy level2Strategy = level1Strategy.dispatch();
    if (level2Strategy == null) {
      return null;
    }

    return level2Strategy.dispatch();
  }

  public static void main(String[] args) throws IOException {
//    Protocol.Transaction createAssetTransaction = newTransaction(NiceCreateAssetTransactionCreator.class);
//
//    Protocol.Transaction freezeBlanceTransaction = newTransaction(NiceFreezeBalanceTransactionCreator.class);
//
//    Protocol.Transaction transferAssetTransaction = newTransaction(NiceTransferAssetTransactionCreator.class);
  }
}
