package org.tron.common.logsfilter.trigger;
import lombok.Getter;
import lombok.Setter;

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
}
