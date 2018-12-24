package org.tron.common.logsfilter;

import lombok.extern.slf4j.Slf4j;
import org.tron.common.runtime.vm.event.ContractEvent;
import org.tron.common.runtime.vm.event.ContractEventListener;

@Slf4j
public class ContractTriggerListener implements ContractEventListener {
    @Override
    public synchronized void onEvent(ContractEvent event, ContractEvent.EventType type) {
        logger.info("receive contractEvent");
    }
}
