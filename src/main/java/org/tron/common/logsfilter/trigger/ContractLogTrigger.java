package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

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
                              String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp){
        super(txId, contractAddress, callerAddress, originAddress, creatorAddress, blockNum, blockTimestamp);
    }
}
