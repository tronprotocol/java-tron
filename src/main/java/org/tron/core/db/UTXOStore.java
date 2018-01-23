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

import java.util.Set;

public class UTXOStore {

  public static final Logger logger = LoggerFactory.getLogger("UTXOStore");
  private LevelDbDataSourceImpl uTXODataSource;

  public UTXOStore(String parentName, String childName) {
    uTXODataSource = new LevelDbDataSourceImpl(parentName, childName);
    uTXODataSource.initDB();
  }


  public void reSet() {
    uTXODataSource.resetDB();
  }

  public byte[] find(byte[] key) {
    return uTXODataSource.getData(key);
  }


  public Set<byte[]> getKeys() {
    return uTXODataSource.allKeys();
  }

  /**
   * save  utxo
   *
   * @param utxoKey
   * @param utxoData
   */
  public void saveUTXO(byte[] utxoKey, byte[] utxoData) {
    uTXODataSource.putData(utxoKey, utxoData);
  }

  public void close() {
    uTXODataSource.closeDB();
  }
}
