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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.datasource.leveldb.LevelDbDataSource;

import static org.tron.core.Constant.BLOCK_DB_NAME;

public class BlockStores  {
    private static final Logger logger = LoggerFactory.getLogger("BlockStores");
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
