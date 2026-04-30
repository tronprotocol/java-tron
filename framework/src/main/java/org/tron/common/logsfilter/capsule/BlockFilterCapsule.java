package org.tron.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;

@Slf4j(topic = "API")
@ToString
public class BlockFilterCapsule extends FilterTriggerCapsule {

  @Getter
  @Setter
  private String blockHash;
  @Getter
  @Setter
  private boolean solidified;

  private final TronJsonRpcImpl jsonRpc;

  public BlockFilterCapsule(BlockCapsule block, boolean solidified, TronJsonRpcImpl jsonRpc) {
    this(block.getBlockId().toString(), solidified, jsonRpc);
  }

  public BlockFilterCapsule(String blockHash, boolean solidified, TronJsonRpcImpl jsonRpc) {
    this.blockHash = blockHash;
    this.solidified = solidified;
    this.jsonRpc = jsonRpc;
  }

  @Override
  public void processFilterTrigger() {
    jsonRpc.handleBLockFilter(this);
  }

}

