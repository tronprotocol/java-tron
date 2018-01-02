package org.tron.db;

import org.junit.Test;
import org.tron.utils.ByteArray;

public class BlockStoresTest {

    @Test
    public void saveBlock() {
        BlockStores blockStores = new BlockStores();
        blockStores.saveBlock( "0001245".getBytes(),"xxdfrgds".getBytes());
        blockStores.close();
    }

    @Test
    public void findBlockByHash() {
        BlockStores blockStores = new BlockStores();
        byte[] blockByHash = blockStores.findBlockByHash("0001245".getBytes());
        blockStores.close();
        System.out.println(ByteArray.toStr(blockByHash));
    }
}