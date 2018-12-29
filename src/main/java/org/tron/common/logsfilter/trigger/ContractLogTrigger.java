package org.tron.common.logsfilter.trigger;

import java.util.LinkedList;
import java.util.List;

public class ContractLogTrigger extends ContractTrigger{
    /**
     * topic list produced by the smart contract LOG function
     */
    @Getter
    @Setter
    private List<String> topicList;

    /**
     * data produced by the smart contract LOG function
     */
    @Getter
    @Setter
    private String data;

    public ContractLogTrigger(String txId, String contractAddress, String callerAddress,
                              String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp) {
        super(txId, contractAddress, callerAddress, originAddress, creatorAddress, blockNum, blockTimestamp);
        setTriggerName(Trigger.CONTRACTLOG_TRIGGER_NAME);
    }

    @Override
    public String toString(){
        return new StringBuilder().append("timestamp: ")
            .append(timeStamp)
            .append(", blockNum: ")
            .append(getBlockNum())
            .append(", blockTimestamp: ")
            .append(getTimeStamp())
            .append(", txId: ")
            .append(getTxId())
            .append(", contractAddress: ")
            .append(getContractAddress())
            .append(", callerAddress: ")
            .append(getCallerAddress())
            .append(", creatorAddress: ")
            .append(getCallerAddress())
            .append(", data: ")
            .append(data)
            .append(", contractTopicMap")
            .append(topicList)
            .append(", removed: ")
            .append(isRemoved()).toString();
    }

}
