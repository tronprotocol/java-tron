package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;

@Slf4j
public class BlockFilterCapsule extends FilterTriggerCapsule {

  @Getter
  @Setter
  private String blockHash;

  public BlockFilterCapsule(BlockCapsule block) {
    blockHash = block.getBlockId().toString();
  }

  @Override
  public void processFilterTrigger() {
    // todo process block filter: handle(blockHash)
    logger.info("BlockFilterCapsule processFilterTrigger get blockHash: {}", blockHash);
  }
}
