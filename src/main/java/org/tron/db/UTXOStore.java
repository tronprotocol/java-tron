package org.tron.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.datasource.leveldb.LevelDbDataSource;

import java.util.Set;

import static org.tron.core.Constant.TRANSACTION_DB_NAME;

public class UTXOStore   {
    private static final Logger LOGGER = LoggerFactory.getLogger("UTXOStore");
    private LevelDbDataSource uTXODataSource;

    public UTXOStore( ) {
        uTXODataSource=new LevelDbDataSource(TRANSACTION_DB_NAME);
        uTXODataSource.init();
    }

    public void reset(){
        uTXODataSource.reset();
    }

    public byte[] find(byte[] key){
        return uTXODataSource.get(key);
    }


    public Set<byte[]> getKeys(){
        return uTXODataSource.keys();
    }
    /**
     * save  utxo
     * @param utxoKey
     * @param utxoData
     */
    public void saveUTXO(byte[] utxoKey, byte[]utxoData){
        uTXODataSource.put(utxoKey,utxoData);
    }

    public void close(){
        uTXODataSource.close();
    }
}
