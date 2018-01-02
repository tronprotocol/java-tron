<<<<<<< HEAD:src/test/java/org/tron/dbStore/BlockStoresTestInter.java
package org.tron.dbStore;
=======
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
package org.tron.db;
>>>>>>> a13b6fb432f828b624ae96b8d9412bfcbe635b9c:src/test/java/org/tron/db/BlockStoresTest.java

import org.junit.Test;
import org.tron.utils.ByteArray;

public class BlockStoresTestInter {

    @Test
    public void saveBlock() {
        BlockStores blockStores = new BlockStores();
        blockStores.saveBlock( "0001245".getBytes(),"xxdfrgds".getBytes());
        blockStores.close();
    }

    @Test
    public void findBlockByHash() {
        BlockStores blockStores = new BlockStores();
        byte[] blockByHash = blockStores.findBlockByHash("0001245".getBytes());
        blockStores.close();
        System.out.println(ByteArray.toStr(blockByHash));
    }
}