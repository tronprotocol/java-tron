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

package org.tron.core.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public class BlockStore extends TronStoreWithRevoking<BlockCapsule> {


  private BlockCapsule head;

  private BlockStore(String dbName) {
    super(dbName);
  }

  private static BlockStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   */
  public static BlockStore create(String dbName) {
    if (instance == null) {
      synchronized (BlockStore.class) {
        if (instance == null) {
          instance = new BlockStore(dbName);
        }
      }
    }
    return instance;
  }


  @Override
  public BlockCapsule get(byte[] key) throws ItemNotFoundException, BadItemException {
    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException();
    }
    return new BlockCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] block = dbSource.getData(key);
    logger.info("address is {}, block is {}", key, block);
    return null != block;
  }

}
