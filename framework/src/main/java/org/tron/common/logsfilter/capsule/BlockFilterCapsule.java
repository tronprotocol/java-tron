package org.tron.common.logsfilter.capsule;

import java.util.Iterator;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.BlockFilterAndResult;

@Slf4j(topic = "API")
@ToString
public class BlockFilterCapsule extends FilterTriggerCapsule {

  @Getter
  @Setter
  private String blockHash;
  @Getter
  @Setter
  private boolean solidified;

  public BlockFilterCapsule(BlockCapsule block, boolean solidified) {
    blockHash = block.getBlockId().toString();
    this.solidified = solidified;
  }

  @Override
  public void processFilterTrigger() {
    logger.info("BlockFilterCapsule processFilterTrigger get blockHash: {}", this);

    Iterator<Entry<String, BlockFilterAndResult>>
        it = TronJsonRpcImpl.getBlockFilter2Result().entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, BlockFilterAndResult> entry = it.next();
      if (entry.getValue().isExpire()) {
        it.remove();
        continue;
      }
      logger.info("add blockHash to filterID:{}", entry.getKey());
      entry.getValue().getResult().add(blockHash);
    }
  }
}
