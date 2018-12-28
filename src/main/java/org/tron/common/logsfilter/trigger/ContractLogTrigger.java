package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;

import java.util.LinkedList;
import java.util.List;

public class ContractLogTrigger extends ContractTrigger{
    /**
     * topic list produced by the smart contract LOG function
     */
    @Getter
    @Setter
    private List<byte[]> topicList;

    /**
     * data produced by the smart contract LOG function
     */
    @Getter
    @Setter
    private byte[] data;

    public ContractLogTrigger(String txId, String contractAddress, String callerAddress,
                              String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp) {
        super(txId, contractAddress, callerAddress, originAddress, creatorAddress, blockNum, blockTimestamp);
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
            .append(Hex.toHexString(data))
            .append(", contractTopicMap")
            .append(getHexTopics())
            .append(", removed: ")
            .append(isRemoved()).toString();
    }

    private List<String> getHexTopics() {
        List<String> list = new LinkedList<>();
        if (topicList != null && !topicList.isEmpty()){
            for (byte[] bytes: topicList) {
                list.add(Hex.toHexString(bytes));
            }
        }
        return list;
    }
}
