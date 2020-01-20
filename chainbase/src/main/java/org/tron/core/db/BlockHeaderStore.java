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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.capsule.BlockHeaderCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j(topic = "DB")
@Component
public class BlockHeaderStore extends TronStoreWithRevoking<BlockHeaderCapsule> {

  private static final String SPLIT = "_";

  @Autowired
  private BlockHeaderStore(@Value("block_header") String dbName) {
    super(dbName);
  }

  public List<BlockHeaderCapsule> getLimitNumber(long startNumber, long limit) {
    BlockId startBlockId = new BlockId(Sha256Hash.ZERO_HASH, startNumber);
    return revokingDB.getValuesNext(startBlockId.getBytes(), limit).stream()
        .map(bytes -> {
          try {
            return new BlockHeaderCapsule(bytes);
          } catch (BadItemException ignored) {
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(BlockHeaderCapsule::getNum))
        .collect(Collectors.toList());
  }

  public List<BlockHeaderCapsule> getBlockHeaderByLatestNum(long getNum) {

    return revokingDB.getlatestValues(getNum).stream()
        .map(bytes -> {
          try {
            return new BlockHeaderCapsule(bytes);
          } catch (BadItemException ignored) {
          }
          return null;
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(BlockHeaderCapsule::getNum))
        .collect(Collectors.toList());
  }

  private byte[] buildKey(String chainId, BlockId blockId) {
    return Bytes.concat((chainId + SPLIT).getBytes(), blockId.getBytes());
  }

  public void put(String chainId, BlockHeaderCapsule headerCapsule) {
    put(buildKey(chainId, headerCapsule.getBlockId()), headerCapsule);
  }

  public BlockHeaderCapsule get(String chainId, BlockId blockId)
      throws ItemNotFoundException {
    BlockHeaderCapsule value = getUnchecked(buildKey(chainId, blockId));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException("block hash: " + blockId.getString() + " is not found!");
    }
    return value;
  }

  public BlockHeaderCapsule getUnchecked(String chainId, BlockId blockId) {
    BlockHeaderCapsule value = getUnchecked(buildKey(chainId, blockId));
    return value;
  }


}
