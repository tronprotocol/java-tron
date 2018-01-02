package org.tron.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.storage.leveldb.LevelDbDataSourceInterImpl;

import java.util.Set;

import static org.tron.core.Constant.TRANSACTION_DB_NAME;

public class UTXOStore   {
    public static final Logger logger = LoggerFactory.getLogger("UTXOStore");
    private LevelDbDataSourceInterImpl uTXODataSource;

    public UTXOStore( ) {
        uTXODataSource=new LevelDbDataSourceInterImpl(TRANSACTION_DB_NAME);
        uTXODataSource.initDB();
    }

    public void reset(){
        uTXODataSource.resetDB();
    }

    public byte[] find(byte[] key){
        return uTXODataSource.getData(key);
    }


    public Set<byte[]> getKeys(){
        return uTXODataSource.allKeys();
    }
    /**
     * save  utxo
     * @param utxoKey
     * @param utxoData
     */
    public void saveUTXO(byte[] utxoKey, byte[]utxoData){
        uTXODataSource.putData(utxoKey,utxoData);
    }

    public void close(){
        uTXODataSource.closeDB();
    }
}
