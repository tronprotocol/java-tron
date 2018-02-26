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
import javafx.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocal;

public class BlockStore extends TronDatabase {

  public static final Logger logger = LoggerFactory.getLogger("BlockStore");
  //private LevelDbDataSourceImpl blockDbDataSource;
  //private LevelDbDataSourceImpl unSpendCache;

  private LevelDbDataSourceImpl numHashCache;

  private KhaosDatabase khaosDb;

  private BlockCapsule head;

  private BlockStore(String dbName) {
    super(dbName);
    numHashCache = new LevelDbDataSourceImpl(
        Constant.OUTPUT_DIR, dbName + "_NUM_HASH");
    numHashCache.initDB();
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
    if (head == null) {
      return Sha256Hash.ZERO_HASH;
    }
    return head.getHash();
  }

  /**
   * Get the head block's number.
   */
  public long getHeadBlockNum() {
    if (head == null) {
      return 0;
    }
    return head.getNum();
  }

  /**
   * Get the block hash from the number.
   */
  public Sha256Hash getBlockHashByNum(long num) {
    byte[] hash = numHashCache.getData(ByteArray.fromLong(num));
    if (hash != null) {
      return Sha256Hash.wrap(hash);
    }
    return Sha256Hash.ZERO_HASH;
  }

  /**
   * Get number of block by the block hash.
   */
  public long getBlockNumByHash(Sha256Hash hash) {
    if (khaosDb.containBlock(hash)) {
      return khaosDb.getBlock(hash).getNum();
    }

    //TODO: optimize here
    byte[] blockByte = dbSource.getData(hash.getBytes());

    if (blockByte != null) {
      return new BlockCapsule(blockByte).getNum();
    }

    return 0;
  }

  /**
   * Get the fork branch.
   */
  public ArrayList<Sha256Hash> getBlockChainHashesOnFork(Sha256Hash forkBlockHash) {
    ArrayList<Sha256Hash> ret = new ArrayList<>();
    Pair<ArrayList<BlockCapsule>, ArrayList<BlockCapsule>> branch =
        khaosDb.getBranch(head.getHash(), forkBlockHash);
    branch.getValue().forEach(b -> ret.add(b.getHash()));
    return ret;
  }

  public DateTime getHeadBlockTime() {
    DateTime time = DateTime.now();
    return time.minus(time.getMillisOfSecond() + 1000); // for test. assume a block generated 1s ago
  }

  public long currentASlot() {
    return getHeadBlockNum(); // assume no missed slot
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
    if (khaosDb.containBlock(blockHash)) {
      return true;
    }

    if (dbSource.getData(blockHash.getBytes()) != null) {
      return true;
    }
    return false;
  }

  public void pushTransactions(Protocal.Transaction trx) {
    logger.info("push transaction");
    //pendingTrans.add(trx);
  }

  /**
   * save a block.
   */
  public void saveBlock(Sha256Hash hash, BlockCapsule block) {
    logger.info("save block");

    khaosDb.push(block);

    //todo: check block's validity
    //todo: In some case it need to switch the branch
    if (block.validate()) {
      dbSource.putData(block.getHash().getBytes(), block.getData());
      numHashCache.putData(ByteArray.fromLong(block.getNum()), block.getHash().getBytes());
    }

    head = khaosDb.getHead();
    // blockDbDataSource.putData(blockHash, blockData);
  }

  /**
   * find a block packed data by hash.
   */
  public byte[] findBlockByHash(Sha256Hash hash) {
    if (khaosDb.containBlock(hash)) {
      return khaosDb.getBlock(hash).getData();
    }
    return dbSource.getData(hash.getBytes());
  }

  /**
   * Get a BlockCapsule by hash.
   */
  public BlockCapsule getBlockByHash(Sha256Hash hash) {
    if (khaosDb.containBlock(hash)) {
      return khaosDb.getBlock(hash);
    }
    return new BlockCapsule(dbSource.getData(hash.getBytes()));
  }

  /**
   * Delete a block.
   */
  public void deleteBlock(Sha256Hash blockHash) {
    BlockCapsule block = getBlockByHash(blockHash);
    khaosDb.removeBlk(blockHash);
    dbSource.deleteData(blockHash.getBytes());
    numHashCache.deleteData(ByteArray.fromLong(block.getNum()));
    head = khaosDb.getHead();
  }

  public void getUnspend(byte[] key) {
  }

  /**
   * resetDb the database.
   */
  public void reset() {
    dbSource.resetDb();
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
