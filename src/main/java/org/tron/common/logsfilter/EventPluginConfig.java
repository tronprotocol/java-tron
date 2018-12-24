package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EventPluginConfig {
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


