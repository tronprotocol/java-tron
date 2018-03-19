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

import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;

public class BlockStore extends TronDatabase<BlockCapsule> {

  public static final Logger logger = LoggerFactory.getLogger("BlockStore");

  private BlockCapsule head;

  private BlockStore(String dbName) {
    super(dbName);
  }

  private static BlockStore instance;

  /**
   * create fun.
   */
  public static BlockStore create(String dbName) {
    if (instance == null) {
      synchronized (AccountStore.class) {
        if (instance == null) {
          instance = new BlockStore(dbName);
        }
      }
    }
    return instance;
  }


  /**
   * to do.
   */
  public Sha256Hash getHeadBlockId() {
    return head == null ? Sha256Hash.ZERO_HASH : head.getBlockId();
  }

  /**
   * Get the head block's number.
   */
  @Deprecated
  public long getHeadBlockNum() {
    return head == null ? 0 : head.getNum();
  }

  @Deprecated
  public DateTime getHeadBlockTime() {
    return head == null ? getGenesisTime() : new DateTime(head.getTimeStamp());
  }

  @Deprecated
  public long currentASlot() {
    return getHeadBlockNum(); // assume no missed slot
  }

  // genesis_time
  public DateTime getGenesisTime() {
    return DateTime.parse("20180101", DateTimeFormat.forPattern("yyyyMMdd"));
  }

  @Override
  public void put(byte[] key, BlockCapsule item) {
    logger.info("address is {},account is {}", key, item);

    byte[] value = dbSource.getData(key);
    if (ArrayUtils.isNotEmpty(value)) {
      onModify(key, value);
    }

    logger.info("address is {} ", ByteArray.toHexString(key));
    dbSource.putData(key, item.getData());

    if (ArrayUtils.isEmpty(value)) {
      onCreate(key);
    }
  }

  @Override
  public void delete(byte[] key) {
    // This should be called just before an object is removed.
    onDelete(key);
    dbSource.deleteData(key);
  }

  @Override
  public BlockCapsule get(byte[] key) {
    byte[] value = dbSource.getData(key);
    return ArrayUtils.isEmpty(value) ? null : new BlockCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] block = dbSource.getData(key);
    logger.info("address is {}, block is {}", key, block);
    return null != block;
  }

}
