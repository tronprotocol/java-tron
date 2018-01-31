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
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import org.tron.protos.Protocal.Block;

public class BlockchainIterator implements Iterator<Block> {

  private Blockchain blockchain;
  private byte[] index;

  /**
   * the iterator of blockchain.
   *
   * @param blockchain the blockchain
   */
  public BlockchainIterator(Blockchain blockchain) {
    this.blockchain = blockchain;

    final byte[] currentHash = blockchain.getCurrentHash();

    index = new byte[currentHash.length];
    index = Arrays.copyOf(currentHash, currentHash.length);
  }

  @Override
  public boolean hasNext() {
    return !(index == null || index.length == 0);
  }

  @Nonnull
  @Override
  public Block next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    byte[] value = blockchain.getBlockDB().getData(index);
    try {
      Block block = Block.parseFrom(value);
      index = block.getBlockHeader().getParentHash().toByteArray();
      return block;
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }
}
