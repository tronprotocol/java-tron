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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import javafx.util.Pair;

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
        Args.getInstance().getOutputDirectory(), dbName + "_NUM_HASH");
    numHashCache.initDB();
    khaosDb = new KhaosDatabase(dbName + "_KDB");
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
  public Sha256Hash getHeadBlockId() {
    if (head == null) {
      return Sha256Hash.ZERO_HASH;
    }
    return head.getBlockId();
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
   * Get the block id from the number.
   */
  public Sha256Hash getBlockIdByNum(long num) {
    byte[] hash = numHashCache.getData(ByteArray.fromLong(num));
    if (hash != null) {
      return Sha256Hash.wrap(hash);
    }
    return Sha256Hash.ZERO_HASH;
  }

  /**
   * Get number of block by the block id.
   */
  public long getBlockNumById(Sha256Hash hash) {
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
        khaosDb.getBranch(head.getBlockId(), forkBlockHash);
    branch.getValue().forEach(b -> ret.add(b.getBlockId()));
    return ret;
  }

  public DateTime getHeadBlockTime() {
    if (head == null) {
      return getGenesisTime();
    } else {
      return new DateTime(head.getTimeStamp());
    }
  }

  public long currentASlot() {
    return getHeadBlockNum(); // assume no missed slot
  }

  // genesis_time
  public DateTime getGenesisTime() {
    return DateTime.parse("20180101", DateTimeFormat.forPattern("yyyyMMdd"));
  }

  /**
   * judge id.
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

  /**
   * judge has blocks.
   */
  public boolean hasBlocks() {
    return dbSource.allKeys().size() > 0 || khaosDb.hasData();
  }

  /**
   * push transaction into db.
   */
  public boolean pushTransactions(TransactionCapsule trx) {
    logger.info("push transaction");
    if (!trx.validateSignature()) {
      return false;
    }
    dbSource.putData(trx.getTransactionId().getBytes(), trx.getData());
    return true;
  }

  /**
   * save a block.
   */
  public void pushBlock(BlockCapsule block) {
    khaosDb.push(block);

    //todo: check block's validity
    if (!block.generatedByMyself) {
      if (!block.validateSignature()) {
        logger.info("The siganature is not validated.");
        return;
      }

      if (!block.calcMerklerRoot().equals(block.getMerklerRoot())) {
        logger.info("The merkler root doesn't match, Calc result is " + block.calcMerklerRoot()
            + " , the headers is " + block.getMerklerRoot());
        return;
      }

      block.getTransactions().forEach(trx -> {
        if (!pushTransactions(trx)) {
          return;
        }
      });

      //todo: In some case it need to switch the branch
    }

    dbSource.putData(block.getBlockId().getBytes(), block.getData());
    logger.info("save block, Its ID is " + block.getBlockId() + ", Its num is " + block.getNum());
    numHashCache.putData(ByteArray.fromLong(block.getNum()), block.getBlockId().getBytes());
    head = khaosDb.getHead();
    // blockDbDataSource.putData(blockHash, blockData);
  }

  /**
   * find a block packed data by id.
   */
  public byte[] findBlockByHash(Sha256Hash hash) {
    if (khaosDb.containBlock(hash)) {
      return khaosDb.getBlock(hash).getData();
    }
    return dbSource.getData(hash.getBytes());
  }

  /**
   * Get a BlockCapsule by id.
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

  @Override
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
