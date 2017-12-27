package org.tron.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.datasource.leveldb.LevelDbDataSource;

import static org.tron.core.Constant.BLOCK_DB_NAME;

public class BlockStores  {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlockStores");
    private LevelDbDataSource blockDbDataSource;


    public BlockStores( ) {

        blockDbDataSource=new LevelDbDataSource(BLOCK_DB_NAME);
        blockDbDataSource.init();
    }

    /**
     * save a block
     * @param blockHash
     * @param blockData
     */
    public void saveBlock(byte[] blockHash, byte[] blockData){
        blockDbDataSource.put(blockHash,blockData);

    }

    /**
     * find a block by it's hash
     * @param blockHash
     * @return
     */
    public  byte[] findBlockByHash(byte[] blockHash){
        return blockDbDataSource.get(blockHash);
    }

    /**
     * delete a block
     * @param blockHash
     */
    public void deleteBlock(byte[] blockHash){
        blockDbDataSource.delete(blockHash);
    }


    /***
     * reset the database
     */
    public void reset(){
        blockDbDataSource.reset();
    }

    public void close(){
        blockDbDataSource.close();
    }

}
