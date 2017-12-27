/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.core;

import org.apache.commons.collections4.map.LRUMap;
import org.tron.overlay.Net;
import org.tron.protos.core.TronTransaction.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PendingStateImpl implements PendingState {

    private final Map<byte[], Object> receivedTxs = new LRUMap<>(100000);
    private final Object dummyObject = new Object();
    private List<Transaction> pendingTransactions = new ArrayList<>();

    private boolean addNewTxIfNotExist(Transaction tx) {
        return receivedTxs.put(TransactionUtils.getHash(tx), dummyObject) == null;
    }

    @Override
    public synchronized List<Transaction> addPendingTransactions(List<Transaction> transactions) {
        int unknownTx = 0;
        List<Transaction> newPending = new ArrayList<>();
        for (Transaction tx : transactions) {
            if (addNewTxIfNotExist(tx)) {
                unknownTx++;

                //TODO  Executes pending tx on the latest best block
                newPending.add(tx);

            }
        }
        return newPending;
    }

    @Override
    public void addPendingTransaction(Transaction tx) {
        addPendingTransactions(Collections.singletonList(tx));
    }

    public synchronized void addPendingTransaction(Blockchain blockchain, Transaction tx, Net net) {
        pendingTransactions.add(tx);
        if (pendingTransactions.size() == 1) {
            blockchain.addBlock(pendingTransactions, net);
            pendingTransactions.clear();
            System.out.println();
        }
    }

    @Override
    public synchronized List<Transaction> getPendingTransactions() {

        ArrayList<Transaction> txs = new ArrayList<>();

        // TODO  create pendingTransactions
        for (Transaction tx : pendingTransactions) {
            txs.add(tx);
        }


        return txs;
    }
}
