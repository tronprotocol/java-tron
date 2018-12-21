package org.tron.common.runtime.vm.event;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ContractLogEvent extends ContractEvent {
  @Getter
  @Setter
  private List<String> topicList;
  @Getter
  @Setter
  private String data;
}
