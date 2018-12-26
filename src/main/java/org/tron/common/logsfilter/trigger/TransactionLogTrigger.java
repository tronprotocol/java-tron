package org.tron.common.logsfilter.trigger;
import lombok.Getter;
import lombok.Setter;

public class TransactionLogTrigger extends Trigger{

    @Override
    public void setTimeStamp(long ts) {
        super.timeStamp = ts;
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
    public String toString(){
        return new StringBuilder().append("timestamp: ")
                .append(timeStamp)
                .append(", transactionId: ")
                .append(transactionId)
                .append(", transactionHash: ")
                .append(transactionHash)
                .append(", blockId: ")
                .append(blockId).toString();
    }
}
