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

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.WitnessService;
import org.tron.protos.Protocal;

public class BlockStore extends TronDatabase {

  public static final Logger logger = LoggerFactory.getLogger("BlockStore");
  //private LevelDbDataSourceImpl blockDbDataSource;
  private LevelDbDataSourceImpl unSpendCache;

  private KhaosDatabase khaosDB;

  private BlockCapsule head;

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
  public Sha256Hash getHeadBlockHash() {
    //TODO:
    return head.getHash();
  }

  public long getHeadBlockNum() {
    return head.getNum();
  }

  public Sha256Hash getBlockHashByNum(long num) {
    //TODO: get it from levelDB
    return Sha256Hash.ZERO_HASH;
  }

  public long getBlockNumByHash(Sha256Hash hash) {
    //TODO: get it form levelDB
    return khaosDB.getBlock(hash).getNum();
  }

  public ArrayList<Sha256Hash> getBlockChainHashesOnFork(Sha256Hash forkBlockHash) {
    ArrayList<Sha256Hash> ret = new ArrayList<>();
    Pair<ArrayList<BlockCapsule>, ArrayList<BlockCapsule>> branch = khaosDB.getBranch(head.getHash(), forkBlockHash);
    branch.getValue().forEach( b -> ret.add(b.getHash()));
    return ret;
  }

  public long getCurrentHeadBlockNum() {
    return (getHeadBlockTime().getMillis() - getGenesisTime().getMillis())
        / WitnessService.LOOP_INTERVAL;
  }

  public DateTime getHeadBlockTime() {
    DateTime time = DateTime.now();
    return time.minus(time.getMillisOfSecond() + 1000); // for test. assume a block generated 1s ago
  }

  public long currentASlot() {
    return getCurrentHeadBlockNum(); // assume no missed slot
  }

  // genesis_time
  public DateTime getGenesisTime() {
    return DateTime.parse("20180101", DateTimeFormat.forPattern("yyyyMMdd"));
  }

  /**
   * judge hash.
   *
   * @param blockHash blockHash
   */
  public boolean containBlock(Sha256Hash blockHash) {
    //TODO: check it from levelDB
    return khaosDB.containBlock(blockHash);
  }

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
    // blockDbDataSource.putData(blockHash, blockData);

  }

  /**
   * find a block by it's hash.
   */
  public byte[] findBlockByHash(byte[] blockHash) {
    return dbSource.getData(blockHash);
  }

  /**
   * deleteData a block.
   */
  public void deleteBlock(byte[] blockHash) {
    dbSource.deleteData(blockHash);
  }


  public void getUnspend(byte[] key) {

  }

  /**
   * resetDB the database.
   */
  public void reset() {
    dbSource.resetDB();
  }

  public void close() {
    dbSource.closeDB();
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
