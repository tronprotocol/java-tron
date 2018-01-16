/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.dbStore;

import org.tron.protos.core.TronBlock;
import org.tron.storage.DataSourceArray;
import org.tron.storage.ObjectDataSource;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public class IndexedBlockStore extends AbstractBlockstore {

    DataSourceArray<List<BlockInfo>> index;
    ObjectDataSource<TronBlock.Block> blocks;

    @Override
    public synchronized TronBlock.Block getBestBlock() {
        Long maxLevel = getMaxNumber();
        if (maxLevel < 0) {
            return null;
        }

        TronBlock.Block bestBlock = getChainBlockByNumber(maxLevel);
        if (bestBlock != null) {
            return bestBlock;
        }

        while (bestBlock == null) {
            --maxLevel;
            bestBlock = getChainBlockByNumber(maxLevel);
        }

        return bestBlock;
    }

    @Override
    public synchronized long getMaxNumber() {

        Long bestIndex = 0L;

        if (index.size() > 0) {
            bestIndex = (long) index.size();
        }


        return bestIndex - 1L;
    }

    @Override
    public TronBlock.Block getChainBlockByNumber(long blockNumber) {

        if (blockNumber > index.size()) {
            return null;
        }

        List<BlockInfo> blockInfos = index.get((int) blockNumber);

        if (blockInfos == null) {
            return null;
        }


        for (BlockInfo blockInfo : blockInfos) {
            if (blockInfo.isMainChain()) {
                byte[] hash = blockInfo.getHash();
                return blocks.getData(hash);
            }
        }


        return null;
    }

    @Override
    public synchronized boolean isBlockExist(byte[] hash) {
        return blocks.getData(hash) != null;
    }

    @Override
    public synchronized TronBlock.Block getBlockByHash(byte[] hash) {
        return blocks.getData(hash);
    }


    public static class BlockInfo implements Serializable {
        byte[] hash;
        BigInteger cummDifficulty;
        boolean mainChain;

        public byte[] getHash() {
            return hash;
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
        }

        public BigInteger getCummDifficulty() {
            return cummDifficulty;
        }

        public void setCummDifficulty(BigInteger cummDifficulty) {
            this.cummDifficulty = cummDifficulty;
        }

        public boolean isMainChain() {
            return mainChain;
        }

        public void setMainChain(boolean mainChain) {
            this.mainChain = mainChain;
        }
    }

}
