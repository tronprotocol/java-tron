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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.protos.core.TronTransaction;

import java.util.Vector;

public class BlockStore {
  public static final Logger logger = LoggerFactory.getLogger("BlockStore");
  private LevelDbDataSourceImpl blockDbDataSource;
  private LevelDbDataSourceImpl unSpendCache;
  private Vector<TronTransaction.Transaction> pendingTrans;

  public BlockStore(String parentName, String childName) {
    blockDbDataSource = new LevelDbDataSourceImpl(parentName, childName);
    blockDbDataSource.initDB();
  }

  public void initBlockDbSource(String parentName, String childName) {
    blockDbDataSource = new LevelDbDataSourceImpl(parentName, childName);
    blockDbDataSource.initDB();
  }

  public void initUnspendDbSource(String parentName, String childName) {
    unSpendCache = new LevelDbDataSourceImpl(parentName, childName);
    unSpendCache.initDB();
  }

  /**
   *
   */

  public void pushTransactions(TronTransaction.Transaction trx)  {
    pendingTrans.add(trx);
  }

  /**
   * Generate Block
   */
  public void generateBlock() {

  }

  /**
   * save a block
   *
   * @param blockHash
   * @param blockData
   */
  public void saveBlock(byte[] blockHash, byte[] blockData) {
    blockDbDataSource.putData(blockHash, blockData);

  }

  /**
   * find a block by it's hash
   *
   * @param blockHash
   * @return
   */
  public byte[] findBlockByHash(byte[] blockHash) {
    return blockDbDataSource.getData(blockHash);
  }

  /**
   * deleteData a block
   *
   * @param blockHash
   */
  public void deleteBlock(byte[] blockHash) {
    blockDbDataSource.deleteData(blockHash);
  }


  public void getUnspend(byte[] key) {

  }

  /***
   * resetDB the database
   */


  public void reset() {
    blockDbDataSource.resetDB();
  }

  public void close() {
    blockDbDataSource.closeDB();
  }

}
