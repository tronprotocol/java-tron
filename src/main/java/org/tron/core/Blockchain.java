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

import static org.tron.core.Constant.BLOCK_DB_NAME;
import static org.tron.core.Constant.LAST_HASH;

import com.alibaba.fastjson.JSON;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.config.Configer;
import org.tron.core.events.BlockchainListener;
import org.tron.crypto.ECKey;
import org.tron.overlay.Net;
import org.tron.peer.Peer;
import org.tron.protos.core.TronBlock.Block;
import org.tron.protos.core.TronTXInput.TXInput;
import org.tron.protos.core.TronTXOutput.TXOutput;
import org.tron.protos.core.TronTXOutputs.TXOutputs;
import org.tron.protos.core.TronTransaction.Transaction;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.utils.ByteArray;

public class Blockchain {

  public static final String GENESIS_COINBASE_DATA = "0x10";
  public static final Logger logger = LoggerFactory.getLogger("BlockChain");
  public static String parentName = Constant.NORMAL;
  private LevelDbDataSourceImpl blockDB;
  private PendingState pendingState = new PendingStateImpl();

  private byte[] lastHash;
  private byte[] currentHash;

  private List<BlockchainListener> listeners = new ArrayList<>();

  /**
   * create new blockchain
   *
   * @param blockDB block database
   */
  public Blockchain(@Named("block") LevelDbDataSourceImpl blockDB) {
    this.blockDB = blockDB;
    this.lastHash = blockDB.getData(LAST_HASH);

    if (this.lastHash == null) {

      InputStream is = getClass().getClassLoader().getResourceAsStream("genesis.json");
      String json = null;
      try {
        json = new String(ByteStreams.toByteArray(is));
      } catch (IOException e) {
        e.printStackTrace();
      }

      GenesisBlockLoader genesisBlockLoader = JSON.parseObject(json, GenesisBlockLoader.class);

      Iterator iterator = genesisBlockLoader.getTransaction().entrySet().iterator();

      List<Transaction> transactions = new ArrayList<>();

      while (iterator.hasNext()) {
        Map.Entry entry = (Map.Entry) iterator.next();
        String key = (String) entry.getKey();
        Integer value = (Integer) entry.getValue();

        Transaction transaction = TransactionUtils
            .newCoinbaseTransaction(key, GENESIS_COINBASE_DATA, value);
        transactions.add(transaction);
      }

      Block genesisBlock = BlockUtils.newGenesisBlock(transactions);

      this.lastHash = genesisBlock.getBlockHeader().getHash().toByteArray();
      this.currentHash = this.lastHash;

      blockDB.putData(genesisBlock.getBlockHeader().getHash().toByteArray(),
          genesisBlock.toByteArray());
      byte[] lastHash = genesisBlock.getBlockHeader()
          .getHash()
          .toByteArray();
      blockDB.putData(LAST_HASH, lastHash);

      for (BlockchainListener listener : listeners) {
        listener.addGenesisBlock(genesisBlock);
      }

      logger.info("new blockchain");
    } else {
      this.currentHash = this.lastHash;

      logger.info("load blockchain");
    }
  }

  /**
   * Checks if the database file exists
   *
   * @return boolean
   */
  public static boolean dbExists() {
    if (Constant.NORMAL == parentName) {
      parentName = Configer.getConf(Constant.NORMAL_CONF).getString(Constant.DATABASE_DIR);
    } else {
      parentName = Configer.getConf(Constant.TEST_CONF).getString(Constant.DATABASE_DIR);

    }
    File file = new File(Paths.get(parentName, BLOCK_DB_NAME).toString());
    return file.exists();
  }

  /**
   * find transaction by id
   *
   * @param id ByteString id
   * @return {@link Transaction}
   */
  public Transaction findTransaction(ByteString id) {
    Transaction transaction = Transaction.newBuilder().build();

    BlockchainIterator bi = new BlockchainIterator(this);
    while (bi.hasNext()) {
      Block block = (Block) bi.next();

      for (Transaction tx : block.getTransactionsList()) {
        String txID = ByteArray.toHexString(tx.getId().toByteArray());
        String idStr = ByteArray.toHexString(id.toByteArray());
        if (txID.equals(idStr)) {
          transaction = tx.toBuilder().build();
          return transaction;
        }
      }

      if (block.getBlockHeader().getParentHash().isEmpty()) {
        break;
      }
    }

    return transaction;
  }

  public HashMap<String, TXOutputs> findUTXO() {
    HashMap<String, TXOutputs> utxo = new HashMap<>();
    HashMap<String, long[]> spenttxos = new HashMap<>();

    BlockchainIterator bi = new BlockchainIterator(this);
    while (bi.hasNext()) {
      Block block = (Block) bi.next();

      for (Transaction transaction : block.getTransactionsList()) {
        String txid = ByteArray.toHexString(transaction.getId()
            .toByteArray());

        output:
        for (int outIdx = 0; outIdx < transaction.getVoutList().size
            (); outIdx++) {
          TXOutput out = transaction.getVout(outIdx);
          if (!spenttxos.isEmpty() && spenttxos.containsKey(txid)) {
            for (int i = 0; i < spenttxos.get(txid).length; i++) {
              if (spenttxos.get(txid)[i] == outIdx) {
                continue output;
              }
            }
          }

          TXOutputs outs = utxo.get(txid);

          if (outs == null) {
            outs = TXOutputs.newBuilder().build();
          }

          outs = outs.toBuilder().addOutputs(out).build();
          utxo.put(txid, outs);
        }

        if (!TransactionUtils.isCoinbaseTransaction(transaction)) {
          for (TXInput in : transaction.getVinList()) {
            String inTxid = ByteArray.toHexString(in.getTxID()
                .toByteArray());
            long[] vindexs = spenttxos.get(inTxid);

            if (vindexs == null) {
              vindexs = new long[0];
            }

            vindexs = Arrays.copyOf(vindexs, vindexs.length + 1);
            vindexs[vindexs.length - 1] = in.getVout();

            spenttxos.put(inTxid, vindexs);
          }
        }
      }

    }

    return utxo;
  }

  /**
   * add a block into database
   *
   * @param block
   */
  public void addBlock(Block block) {
    byte[] blockInDB = blockDB.getData(block.getBlockHeader().getHash().toByteArray());

    if (blockInDB == null || blockInDB.length == 0) {
      return;
    }

    blockDB.putData(block.getBlockHeader().getHash().toByteArray(), block.toByteArray());

    byte[] lastHash = blockDB.getData(ByteArray.fromString("lashHash"));
    byte[] lastBlockData = blockDB.getData(lastHash);
    try {
      Block lastBlock = Block.parseFrom(lastBlockData);
      if (block.getBlockHeader().getNumber() > lastBlock.getBlockHeader().getNumber()) {
        blockDB.putData(ByteArray.fromString("lashHash"),
            block.getBlockHeader().getHash().toByteArray());
        this.lastHash = block.getBlockHeader().getHash().toByteArray();
        this.currentHash = this.lastHash;
      }
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public Transaction signTransaction(Transaction transaction, ECKey myKey) {
    HashMap<String, Transaction> prevTXs = new HashMap<>();

    for (TXInput txInput : transaction.getVinList()) {
      ByteString txID = txInput.getTxID();
      Transaction prevTX = this.findTransaction(txID).toBuilder().build();
      String key = ByteArray.toHexString(txID.toByteArray());
      prevTXs.put(key, prevTX);
    }

    transaction = TransactionUtils.sign(transaction, myKey, prevTXs);
    return transaction;
  }

  /**
   * {@see org.tron.overlay.kafka.KafkaTest#testKafka()}
   *
   * @param transactions transactions
   */
  public void addBlock(List<Transaction> transactions, Net net) {
    // getData lastHash
    byte[] lastHash = blockDB.getData(LAST_HASH);
    ByteString parentHash = ByteString.copyFrom(lastHash);
    // getData number
    long number = BlockUtils.getIncreaseNumber(this);
    // getData difficulty
    ByteString difficulty = ByteString.copyFromUtf8(Constant.DIFFICULTY);
    Block block = BlockUtils.newBlock(transactions, parentHash, difficulty,
        number);

    for (BlockchainListener listener : listeners) {
      listener.addBlockNet(block, net);
    }
  }

  public void addBlock(List<Transaction> transactions) {
    // get lastHash
    byte[] lastHash = blockDB.getData(LAST_HASH);
    ByteString parentHash = ByteString.copyFrom(lastHash);
    // get number
    long number = BlockUtils.getIncreaseNumber(this);
    // get difficulty
    ByteString difficulty = ByteString.copyFromUtf8(Constant.DIFFICULTY);
    Block block = BlockUtils.newBlock(transactions, parentHash, difficulty,
        number);

    for (BlockchainListener listener : listeners) {
      listener.addBlock(block);
    }
  }

  /**
   * receive a block and save it into database,update caching at the same time.
   *
   * @param block   block
   * @param utxoSet utxoSet
   */
  public void receiveBlock(Block block, UTXOSet utxoSet, Peer peer) {

    byte[] lastHashKey = LAST_HASH;
    byte[] lastHash = blockDB.getData(lastHashKey);

    if (!ByteArray.toHexString(block.getBlockHeader().getParentHash().toByteArray())
        .equals(ByteArray.toHexString
            (lastHash))) {
      return;
    }

    // save the block into the database
    byte[] blockHashKey = block.getBlockHeader().getHash().toByteArray();
    byte[] blockVal = block.toByteArray();
    blockDB.putData(blockHashKey, blockVal);

    byte[] ch = block.getBlockHeader().getHash().toByteArray();

    // update lastHash
    peer.getBlockchain().getBlockDB().putData(lastHashKey, ch);

    this.lastHash = ch;
    currentHash = ch;
    System.out.println(BlockUtils.toPrintString(block));
    // update UTXO cache
    utxoSet.reindex();
  }

  public void addListener(BlockchainListener listener) {
    this.listeners.add(listener);
  }

  public LevelDbDataSourceImpl getBlockDB() {
    return blockDB;
  }

  public void setBlockDB(LevelDbDataSourceImpl blockDB) {
    this.blockDB = blockDB;
  }

  public PendingState getPendingState() {
    return pendingState;
  }

  public void setPendingState(PendingState pendingState) {
    this.pendingState = pendingState;
  }

  public byte[] getLastHash() {
    return lastHash;
  }

  public void setLastHash(byte[] lastHash) {
    this.lastHash = lastHash;
  }

  public byte[] getCurrentHash() {
    return currentHash;
  }

  public void setCurrentHash(byte[] currentHash) {
    this.currentHash = currentHash;
  }


}
