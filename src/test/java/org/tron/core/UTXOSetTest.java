package org.tron.core;

import org.junit.Test;
import org.mockito.Mockito;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;

public class UTXOSetTest {
    @Test
    public void testReindex() {
        Blockchain mockBlockchain = Mockito.mock(Blockchain.class);
        LevelDbDataSourceImpl mockTransactionDb = Mockito.mock(LevelDbDataSourceImpl.class);
        UTXOSet utxoSet = new UTXOSet(mockTransactionDb);
        utxoSet.setBlockchain(mockBlockchain);

        utxoSet.reindex();
    }
}
