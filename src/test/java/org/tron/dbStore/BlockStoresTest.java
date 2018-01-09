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

import org.junit.Ignore;
import org.junit.Test;
import org.tron.config.Configer;
import org.tron.core.Constant;
import org.tron.utils.ByteArray;

@Ignore
public class BlockStoresTest {

    @Test
    public void saveBlock() {
        Configer.TRON_CONF = Constant.TEST_CONF;
        BlockStores blockStores = new BlockStores();
        blockStores.saveBlock( "0001245".getBytes(),"xxdfrgds".getBytes());
        blockStores.close();
    }

    @Test
    public void findBlockByHash() {
        Configer.TRON_CONF = Constant.TEST_CONF;
        BlockStores blockStores = new BlockStores();
        byte[] blockByHash = blockStores.findBlockByHash("0001245".getBytes());
        blockStores.close();
        System.out.println(ByteArray.toStr(blockByHash));
    }
}