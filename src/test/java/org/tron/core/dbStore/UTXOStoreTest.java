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

package org.tron.core.dbstore;

import static org.tron.core.Constant.BLOCK_DB_NAME;

import org.junit.Ignore;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;

@Ignore
public class UTXOStoreTest {

  /**
   * save utxo
   */
  @Test
  public void saveUTXO() {
    UTXOStore utxoStore = new UTXOStore(Constant.TEST, BLOCK_DB_NAME);
    utxoStore.saveUTXO("00012546".getBytes(), "300".getBytes());
    utxoStore.close();
  }

  @Test
  public void find() {
    UTXOStore utxoStore = new UTXOStore(Constant.TEST, BLOCK_DB_NAME);
    byte[] bytes = utxoStore.find("00012546".getBytes());
    utxoStore.close();
    System.out.println(ByteArray.toStr(bytes));
  }
}