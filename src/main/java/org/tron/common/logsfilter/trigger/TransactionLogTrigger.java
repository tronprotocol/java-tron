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

    @Getter
    @Setter
    private long blockNum = -1;

    @Getter
    @Setter
    private long energyUsage;

    @Getter
    @Setter
    private long energyFee;

    @Getter
    @Setter
    private long originEnergyUsage;

    @Getter
    @Setter
    private long energyUsageTotal;

    @Getter
    @Setter
    private long netUsage;

    @Getter
    @Setter
    private  long netFee;
    @Override
    public String toString(){
        return new StringBuilder().append("timestamp: ")
                .append(timeStamp)
                .append(", transactionId: ")
                .append(transactionId)
                .append(", transactionHash: ")
                .append(transactionHash)
                .append(", blockId: ")
                .append(blockId)
                .append(", energyUsage: ")
                .append(energyUsage)
                .append(", energyFee: ")
                .append(energyFee)
                .append(", originEnergyUsage: ")
                .append(originEnergyUsage)
                .append(", energyUsageTotal: ")
                .append(energyUsageTotal)
                .append(", netUsage: ")
                .append(netUsage)
                .append(", netFee: ")
                .append(netFee).toString();
    }
}
