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

package org.tron.dbStore;

import static org.tron.core.Constant.BLOCK_DB_NAME;

import org.junit.Ignore;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.dbStore.BlockStores;

@Ignore
public class BlockStoresTest {

  @Test
  public void saveBlock() {
    BlockStores blockStores = new BlockStores(Constant.TEST, BLOCK_DB_NAME);
    blockStores.saveBlock("0001245".getBytes(), "xxdfrgds".getBytes());
    blockStores.close();
  }

  @Test
  public void findBlockByHash() {
    BlockStores blockStores = new BlockStores(Constant.TEST, BLOCK_DB_NAME);
    byte[] blockByHash = blockStores.findBlockByHash("0001245".getBytes());
    blockStores.close();
    System.out.println(ByteArray.toStr(blockByHash));
  }
}