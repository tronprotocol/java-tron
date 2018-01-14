package org.tron.core;

import org.junit.Test;
import org.mockito.Mockito;
import org.tron.protos.core.TronTXOutput;
import org.tron.protos.core.TronTXOutputs;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.utils.ByteArray;

import java.util.HashMap;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class UTXOSetTest {
    @Test
    public void testReindex() {
        String key = "15f3988aa8d56eab3bfca45144bad77fc60acce50437a0a9d794a03a83c15c5e";
        TronTXOutput.TXOutput testOutput = TronTXOutput.TXOutput.newBuilder().build();
        TronTXOutputs.TXOutputs testOutputs = TronTXOutputs.TXOutputs.newBuilder()
                .addOutputs(testOutput)
                .build();

        HashMap<String, TronTXOutputs.TXOutputs> testUTXO = new HashMap<>();
        testUTXO.put(key, testOutputs);

        Blockchain mockBlockchain = Mockito.mock(Blockchain.class);
        when(mockBlockchain.findUTXO()).thenReturn(testUTXO);

        LevelDbDataSourceImpl mockTransactionDb = Mockito.mock(LevelDbDataSourceImpl.class);

        UTXOSet utxoSet = new UTXOSet(mockTransactionDb);
        utxoSet.setBlockchain(mockBlockchain);

        utxoSet.reindex();
        Mockito.verify(mockTransactionDb, times(1)).resetDB();
        Mockito.verify(mockTransactionDb, times(1))
                .putData(eq(ByteArray.fromHexString(key)), eq(testOutputs.toByteArray()));
    }
}
