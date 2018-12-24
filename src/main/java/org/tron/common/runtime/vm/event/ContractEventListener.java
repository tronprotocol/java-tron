package org.tron.common.runtime.vm.event;


public interface ContractEventListener {

  void onEvent(ContractEvent event);

}
