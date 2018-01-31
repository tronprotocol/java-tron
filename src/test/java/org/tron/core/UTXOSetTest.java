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
package org.tron.core;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocal.TXOutput;
import org.tron.protos.Protocal.TXOutputs;

public class UTXOSetTest {

  @Test
  public void testReindex() {
    String key = "15f3988aa8d56eab3bfca45144bad77fc60acce50437a0a9d794a03a83c15c5e";
    TXOutput testOutput = TXOutput.newBuilder().build();
    TXOutputs testOutputs = TXOutputs.newBuilder()
        .addOutputs(testOutput)
        .build();

    HashMap<String, TXOutputs> testUTXO = new HashMap<>();
    testUTXO.put(key, testOutputs);

    Blockchain mockBlockchain = Mockito.mock(Blockchain.class);
    when(mockBlockchain.findUtxo()).thenReturn(testUTXO);

    LevelDbDataSourceImpl mockTransactionDb = Mockito.mock(LevelDbDataSourceImpl.class);

    UTXOSet utxoSet = new UTXOSet(mockTransactionDb, mockBlockchain);

    utxoSet.reindex();
    Mockito.verify(mockTransactionDb, times(1)).resetDB();
    Mockito.verify(mockTransactionDb, times(1))
        .putData(eq(ByteArray.fromHexString(key)), eq(testOutputs.toByteArray()));
  }
}
