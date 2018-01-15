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

package org.tron.dbStore;
<<<<<<< HEAD:src/main/java/org/tron/dbStore/BlockStorage.java
=======

import org.tron.protos.core.TronBlock;
>>>>>>> a84aa0f4221b66bba458a8c1fd581686fae1075b:src/main/java/org/tron/dbStore/BlockStoreInput.java

import org.tron.protos.core.TronBlock.Block;

<<<<<<< HEAD:src/main/java/org/tron/dbStore/BlockStorage.java
public interface BlockStorage {

    Block getBestBlock();
=======
  TronBlock.Block getBestBlock();

  long getMaxNumber();
>>>>>>> a84aa0f4221b66bba458a8c1fd581686fae1075b:src/main/java/org/tron/dbStore/BlockStoreInput.java

  TronBlock.Block getChainBlockByNumber(long blockNumber);

<<<<<<< HEAD:src/main/java/org/tron/dbStore/BlockStorage.java
    Block getChainBlockByNumber(long blockNumber);

    boolean isBlockExist(byte[] hash);

    Block getBlockByHash(byte[] hash);
=======
  boolean isBlockExist(byte[] hash);

  TronBlock.Block getBlockByHash(byte[] hash);
>>>>>>> a84aa0f4221b66bba458a8c1fd581686fae1075b:src/main/java/org/tron/dbStore/BlockStoreInput.java

}
