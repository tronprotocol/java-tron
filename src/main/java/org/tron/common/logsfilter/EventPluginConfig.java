package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EventPluginConfig {

    public static final int BLOCK_TRIGGER = 0;
    public static final int TRANSACTION_TRIGGER = 1;
    public static final int CONTRACTLOG_TRIGGER = 2;
    public static final int CONTRACTEVENT_TRIGGER = 3;

    public static final String BLOCK_TRIGGER_NAME = "block";
    public static final String TRANSACTION_TRIGGER_NAME = "transaction";
    public static final String CONTRACTEVENT_TRIGGER_NAME = "contractevent";
    public static final String CONTRACTLOG_TRIGGER_NAME = "contractlog";

    @Getter
    @Setter
    private String pluginPath;

    @Getter
    @Setter
    private String serverAddress;


    @Getter
    @Setter
    private List<TriggerConfig> triggerConfigList;


    public EventPluginConfig(){
        pluginPath = "";
        serverAddress = "";
        triggerConfigList = new ArrayList<TriggerConfig>();
    }
}


