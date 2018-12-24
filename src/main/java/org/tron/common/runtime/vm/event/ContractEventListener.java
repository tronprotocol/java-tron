package org.tron.common.runtime.vm.event;


public interface ContractEventListener {

  /**
   * on Event callback
   * @param event
   */
  void onEvent(ContractEvent event);

}
