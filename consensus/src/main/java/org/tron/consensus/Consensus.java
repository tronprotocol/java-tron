package org.tron.consensus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.base.ConsensusInterface;
import org.tron.consensus.base.Param;
import org.tron.consensus.dpos.DposService;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "consensus")
@Component
public class Consensus {

  @Autowired
  private DposService dposService;

  private ConsensusInterface consensusInterface;

  public void start(Param param) {
    consensusInterface = dposService;
    consensusInterface.start(param);
  }

  public void stop() {
    consensusInterface.stop();
  }

  public boolean validBlock(Block block){
    return consensusInterface.validBlock(block);
  }

  public boolean applyBlock(Block block) {
    return consensusInterface.applyBlock(block);
  }

}