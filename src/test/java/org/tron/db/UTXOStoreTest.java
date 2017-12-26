package org.tron.db;

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