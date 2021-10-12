package org.tron.common.logsfilter.capsule;

import static org.tron.core.services.jsonrpc.TronJsonRpcImpl.handleBLockFilter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;

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

  public BlockFilterCapsule(String blockHash, boolean solidified) {
    this.blockHash = blockHash;
    this.solidified = solidified;
  }

  @Override
  public void processFilterTrigger() {
    handleBLockFilter(this);
  }

}

