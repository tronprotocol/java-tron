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

package org.tron.core.db;

import java.util.List;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.Transaction;

public class BlockStore extends Database {

  public static final Logger logger = LoggerFactory.getLogger("BlockStore");
  private LevelDbDataSourceImpl blockDbDataSource;
  private LevelDbDataSourceImpl unSpendCache;
  private Vector<Transaction> pendingTrans;

  private BlockStore(String dbName) {
    super(dbName);
  }

  private static BlockStore instance;

  /**
   * create fun.
   */
  public static BlockStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new BlockStore(dbName);
        }
      }
    }
    return instance;
  }


  /**
   * to do.
   */
  public byte[] getHeadBlockHash() {
    return "".getBytes();
  }

  public boolean hasItem(byte[] hash, String type) {
    if (type == "trx") {
      return hasTranscation(hash);
    } else if (type == "block") {
      return hasBlock(hash);
    }
    return false;
  }

  /**
   *
   * @param blockHash
   * @return
   */
  public boolean hasBlock(byte[] blockHash) {
    return false;
  }

  public boolean hasTranscation(byte[] trxHash) {
    return false;
  }

  public boolean isIncludeBlock(byte[] hash) {
    return false;
  }

  /**
   *
   */

  public void pushTransactions(Protocal.Transaction trx) {
    logger.info("push transaction");
    //pendingTrans.add(trx);
  }


  /**
   * Generate Block return Block.
   */
  public Protocal.Block generateBlock(List<Protocal.Transaction> transactions) {
    return null;
  }

  /**
   * save a block.
   */
  public void saveBlock(byte[] blockHash, byte[] blockData) {
    logger.info("save block");
    blockDbDataSource.putData(blockHash, blockData);

  }

  /**
   * find a block by it's hash.
   */
  public byte[] findBlockByHash(byte[] blockHash) {
    return blockDbDataSource.getData(blockHash);
  }

  public byte[] findTrasactionByHash(byte[] trxHash) {
    return "".getBytes();
  }

  /**
   * deleteData a block.
   */
  public void deleteBlock(byte[] blockHash) {
    blockDbDataSource.deleteData(blockHash);
  }


  public void getUnspend(byte[] key) {

  }

  /***
   * resetDB the database.
   */
  public void reset() {
    blockDbDataSource.resetDB();
  }

  public void close() {
    blockDbDataSource.closeDB();
  }

  @Override
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

  }
}
