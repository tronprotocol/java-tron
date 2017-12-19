
package org.tron.core;

import org.tron.protos.core.TronTransaction.Transaction;

import java.util.List;


public interface PendingState {

    List<Transaction> addPendingTransactions(List<Transaction> transactions);

    void addPendingTransaction(Transaction tx);

    List<Transaction> getPendingTransactions();

}
