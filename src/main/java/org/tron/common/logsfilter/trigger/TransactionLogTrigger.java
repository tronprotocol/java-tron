package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;

public class TransactionLogTrigger extends Trigger{
    public void setTimestamp(long ts) {
        timeStamp = ts;
    }
    @Getter
    @Setter
    private String transactionId;

    @Getter
    @Setter
    private String transactionHash;

    @Getter
    @Setter
    private String blockId;

    @Override
    public void processTrigger(){
        EventPluginLoader.getInstance().postTransactionTrigger(this);
    }
}
