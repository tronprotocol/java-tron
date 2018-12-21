package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class ContractEmitEvent {
  @Getter
  @Setter
  private String eventSignature;
  @Getter
  @Setter
  private Map<String, String> topicMap;
  @Getter
  @Setter
  private String data;
}
