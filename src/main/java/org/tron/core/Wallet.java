/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.application.Application;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.db.BlockStore;
import org.tron.core.db.UtxoStore;
import org.tron.core.net.message.Message;
import org.tron.core.net.message.TransactionMessage;
import org.tron.core.net.node.Node;
import org.tron.protos.Protocal.TXInput;
import org.tron.protos.Protocal.TXOutput;
import org.tron.protos.Protocal.Transaction;


public class Wallet {

  private static final Logger logger = LoggerFactory.getLogger("Wallet");

  private BlockStore db;
  private final ECKey ecKey;
  private UtxoStore utxoStore;
  private Application app;
  private Node p2pnode;
  private UTXOSet utxoSet;

  /**
   * Creates a new Wallet with a random ECKey.
   */
  public Wallet() {
    this.ecKey = new ECKey(Utils.getRandom());
  }


  /**
   * constructor.
   */
  public Wallet(Application app) {
    this.app = app;
    this.p2pnode = app.getP2pNode();
    this.db = app.getBlockStoreS();
    utxoStore = app.getDbManager().getUtxoStore();
    this.ecKey = new ECKey(Utils.getRandom());
  }

  /**
   * Creates a Wallet with an existing ECKey.
   */
  public Wallet(final ECKey ecKey) {
    this.ecKey = ecKey;
    logger.info("wallet address: {}", ByteArray.toHexString(this.ecKey.getAddress()));
  }

  public ECKey getEcKey() {
    return ecKey;
  }

  public byte[] getAddress() {
    return ecKey.getAddress();
  }

  /**
   * Get balance by address.
   */
  public long getBalance(byte[] address) {

    ArrayList<TXOutput> utxos = utxoStore.findUtxo(address);
    long balance = 0;

    for (TXOutput txOutput : utxos) {
      balance += txOutput.getValue();
    }

    logger.info("balance = {}", balance);
    return balance;
  }

  /**
   * Create a transaction.
   */
  public Transaction createTransaction(byte[] address, String to, long amount) {
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    List<TXInput> txInputs = new ArrayList<>();
    List<TXOutput> txOutputs = new ArrayList<>();
    long spendableOutputs = getBalance(address);

    Set<Entry<String, long[]>> entrySet = utxoStore.findSpendableOutputs(address, amount)
        .getUnspentOutputs().entrySet();
    for (Map.Entry<String, long[]> entry : entrySet) {
      String txId = entry.getKey();
      long[] outs = entry.getValue();
      for (long out : outs) {
        TXInput txInput = TXInputUtils
            .newTXInput(ByteArray.fromHexString(txId), out, null, address);
        txInputs.add(txInput);
      }
    }

    txOutputs.add(TXOutputUtils.newTXOutput(amount, to));
    txOutputs
        .add(TXOutputUtils.newTXOutput(spendableOutputs - amount, ByteArray.toHexString(address)));

    if (check(address, to, amount)) {
      for (TXInput txInput : txInputs) {
        transactionBuilder.addVin(txInput);
      }
      for (TXOutput txOutput : txOutputs) {
        transactionBuilder.addVout(txOutput);
      }
      logger.info("Transaction create succeeded！");
    } else {
      logger.error("Transaction create failed！");
    }

    return transactionBuilder.build();
  }

  /**
   * Broadcast a transaction.
   */
  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    if (signaturedTransaction != null) {
      Message message = new TransactionMessage(signaturedTransaction);
      p2pnode.broadcast(message);
      return true;
    }
    return false;
  }


  /**
   * cheack balance of the address.
   */
  public boolean check(byte[] address, String to, long amount) {

    if (to.length() != 40) {
      logger.error("address invalid");
      return false;
    }

    if (amount <= 0) {
      logger.error("amount required a positive number");
      return false;
    }

    if (amount > getBalance(address)) {
      logger.error("don't have enough money");
      return false;
    }

    return true;
  }

}
