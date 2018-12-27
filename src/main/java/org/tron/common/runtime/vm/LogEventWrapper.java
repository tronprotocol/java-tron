package org.tron.common.runtime.vm;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.trigger.ContractLogTrigger;
import org.tron.protos.Protocol;

import java.util.Map;

public class LogEventWrapper extends ContractLogTrigger{
  /**
   * decode from sha3($EventSignature) with the ABI of this contract.
   */
  @Getter
  @Setter
  private String eventSignature;

  /**
   * ABI Entry of this event.
   */
  @Getter
  @Setter
  private Protocol.SmartContract.ABI.Entry abiEntry;

  public LogEventWrapper(String txId, String contractAddress, String callerAddress,
                         String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp){
    super(txId, contractAddress, callerAddress, originAddress, creatorAddress, blockNum, blockTimestamp);
  }

}
