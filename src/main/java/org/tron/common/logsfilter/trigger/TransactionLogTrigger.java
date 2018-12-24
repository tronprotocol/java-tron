package org.tron.common.logsfilter.trigger;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;

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
