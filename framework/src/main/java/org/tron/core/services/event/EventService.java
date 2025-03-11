package org.tron.core.services.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.core.db.Manager;

@Slf4j(topic = "event")
@Component
public class EventService {
  @Autowired
  private RealtimeEventService realtimeEventService;

  @Autowired
  private BlockEventLoad blockEventLoad;

  @Autowired
  private HistoryEventService historyEventService;

  @Autowired
  private SolidEventService solidEventService;

  @Autowired
  private Manager manager;

  private EventPluginLoader instance = EventPluginLoader.getInstance();

  public void init()  {
    logger.info("Start to load eventPlugin. {}, {}, {} "
        + "block: {}, {} trx: {}, {}, {} event: {}, {} log: {}, {}, {}, {} solid: {}",
        manager.isEventPluginLoaded(),

        instance.getVersion(),
        instance.getStartSyncBlockNum(),

        instance.isBlockLogTriggerEnable(),
        instance.isBlockLogTriggerSolidified(),

        instance.isTransactionLogTriggerEnable(),
        instance.isTransactionLogTriggerSolidified(),
        instance.isTransactionLogTriggerEthCompatible(),

        instance.isContractEventTriggerEnable(),
        instance.isSolidityEventTriggerEnable(),

        instance.isContractLogTriggerEnable(),
        instance.isContractLogTriggerRedundancy(),
        instance.isSolidityLogTriggerEnable(),
        instance.isSolidityLogTriggerRedundancy(),

        instance.isSolidityTriggerEnable());

    if (!manager.isEventPluginLoaded() || instance.getVersion() != 1) {
      return;
    }

    historyEventService.init();
  }

  public void close() {
    if (!manager.isEventPluginLoaded() || instance.getVersion() != 1) {
      return;
    }
    historyEventService.close();
    blockEventLoad.close();
    realtimeEventService.close();
    solidEventService.close();
  }
}
