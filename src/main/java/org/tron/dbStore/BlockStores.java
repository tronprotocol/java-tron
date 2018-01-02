package org.tron.dbStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.storage.leveldb.LevelDbDataSourceImpl;

import static org.tron.core.Constant.BLOCK_DB_NAME;

public class BlockStores  {
    public static final Logger logger = LoggerFactory.getLogger("BlockStores");
    private LevelDbDataSourceImpl blockDbDataSource;


    public BlockStores( ) {

        blockDbDataSource=new LevelDbDataSourceImpl(BLOCK_DB_NAME);
        blockDbDataSource.initDB();
    }

    /**
     * save a block
     * @param blockHash
     * @param blockData
     */
    public void saveBlock(byte[] blockHash, byte[] blockData){
        blockDbDataSource.putData(blockHash,blockData);

    }

    /**
     * find a block by it's hash
     * @param blockHash
     * @return
     */
    public  byte[] findBlockByHash(byte[] blockHash){
        return blockDbDataSource.getData(blockHash);
    }

    /**
     * deleteData a block
     * @param blockHash
     */
    public void deleteBlock(byte[] blockHash){
        blockDbDataSource.deleteData(blockHash);
    }


    /***
     * resetDB the database
     */
    public void reset(){
        blockDbDataSource.resetDB();
    }

    public void close(){
        blockDbDataSource.closeDB();
    }

}
