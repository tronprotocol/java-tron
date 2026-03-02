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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.error.TronDBException;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.exception.BadItemException;

@Slf4j(topic = "DB")
@Component
public class BlockStore extends TronStoreWithRevoking<BlockCapsule> {

  @Autowired
  private BlockStore(@Value("block") String dbName) {
    super(dbName);
  }

  public List<BlockCapsule> getLimitNumber(long startNumber, long limit) {
    BlockId startBlockId = new BlockId(Sha256Hash.ZERO_HASH, startNumber);
    return pack(revokingDB.getValuesNext(startBlockId.getBytes(), limit));
  }

  public List<BlockCapsule> getBlockByLatestNum(long getNum) {
    return pack(revokingDB.getlatestValues(getNum));
  }

  private List<BlockCapsule> pack(Set<byte[]> values) {
    List<BlockCapsule> blocks = new ArrayList<>();
    for (byte[] bytes : values) {
      try {
        blocks.add(new BlockCapsule(bytes));
      } catch (BadItemException e) {
        logger.error("Find bad item: {}", e.getMessage());
        // throw new TronDBException(e);
      }
    }
    blocks.sort(Comparator.comparing(BlockCapsule::getNum));
    return blocks;
  }
}
