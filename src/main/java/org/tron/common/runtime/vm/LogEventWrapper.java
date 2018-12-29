package org.tron.common.runtime.vm;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.trigger.ContractTrigger;
import org.tron.protos.Protocol;

import java.util.List;

public class LogEventWrapper extends ContractTrigger {

  @Getter
  @Setter
  private List<byte[]> topicList;

  @Getter
  @Setter
  private byte[] data;

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

  public LogEventWrapper(){
    super();
  }

}
