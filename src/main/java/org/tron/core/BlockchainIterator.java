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
package org.tron.core;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.core.TronBlock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

public class BlockchainIterator implements Iterator {
    private Blockchain blockchain;
    private byte[] index;

    public BlockchainIterator(Blockchain blockchain) {
        this.blockchain = blockchain;
        index = new byte[blockchain.getCurrentHash().length];
        index = Arrays.copyOf(blockchain.getCurrentHash(), blockchain
                .getCurrentHash().length);
    }

    @Override
    public boolean hasNext() {
        return !(index == null || index.length == 0);
    }

    @Override
    public Object next() {
        TronBlock.Block block = null;
        if (hasNext()) {
            byte[] value = blockchain.getBlockDB().getData(index);
            try {
                block = TronBlock.Block.parseFrom(value);
                index = block.getBlockHeader().getParentHash()
                        .toByteArray();
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

        return block;
    }

    @Override
    public void remove() {

    }

    @Override
    public void forEachRemaining(Consumer action) {

    }
}
