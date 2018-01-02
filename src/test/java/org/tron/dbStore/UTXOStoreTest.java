<<<<<<< HEAD:src/test/java/org/tron/dbStore/UTXOStoreTest.java
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
>>>>>>> a13b6fb432f828b624ae96b8d9412bfcbe635b9c:src/test/java/org/tron/db/UTXOStoreTest.java

import org.junit.Test;
import org.tron.utils.ByteArray;

public class UTXOStoreTest {

    /**
     * save utxo
     */
    @Test
    public void saveUTXO() {
        UTXOStore utxoStore = new UTXOStore();
        utxoStore.saveUTXO("00012546".getBytes(),"300".getBytes());
        utxoStore.close();
    }

    @Test
    public void find() {
        UTXOStore utxoStore = new UTXOStore();
        byte[] bytes = utxoStore.find("00012546".getBytes());
        utxoStore.close();
        System.out.println(ByteArray.toStr(bytes));
    }
}