package org.tron.core.spv;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.db.BlockHeaderIndexStore;
import org.tron.core.db.BlockHeaderStore;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.HeaderDynamicPropertiesStore;

@Slf4j
@Component
public class HeaderManager {

  @Autowired
  private HeaderDynamicPropertiesStore headerDynamicPropertiesStore;
  @Autowired
  private BlockHeaderIndexStore blockHeaderIndexStore;
  @Autowired
  private BlockHeaderStore blockHeaderStore;

  public BlockId getSolidBlockId(String chainId) {
    try {
      long num = headerDynamicPropertiesStore.getLatestSolidifiedBlockNum(chainId);
      return getBlockIdByNum(chainId, num);
    } catch (Exception e) {
      return null;
    }
  }

  public BlockId getHead(String chainId) throws ItemNotFoundException {
    long num = headerDynamicPropertiesStore.getLatestBlockHeaderNumber(chainId);
    return getBlockIdByNum(chainId, num);
  }

  public BlockId getBlockIdByNum(String chainId, long num) throws ItemNotFoundException {
    return this.blockHeaderIndexStore.get(chainId, num);
  }

}
