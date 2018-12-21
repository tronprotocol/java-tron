package org.tron.common.runtime.vm.event;

public interface ContractEventSource {

  public void addListener(ContractEventListener listener);
  public void deleteListener(ContractEventListener listener);
  public void notifyListener(ContractEvent event, ContractEvent.EventType type);
}
