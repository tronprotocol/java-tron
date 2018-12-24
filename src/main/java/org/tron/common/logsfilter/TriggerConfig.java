package org.tron.common.logsfilter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TriggerConfig {
    @Getter
    @Setter
    private String triggerName;

    @Getter
    @Setter
    private boolean enabled;

    @Getter
    @Setter
    private String topic;

    public TriggerConfig(){
        triggerName = "";
        enabled = false;
        topic = "";
    }
}