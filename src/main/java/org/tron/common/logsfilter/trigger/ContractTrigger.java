package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

public class ContractTrigger extends Trigger{

    /**
     * id of the transaction which produce this event.
     */
    @Getter
    @Setter
    private String txId;

    /**
     * address of the contract triggered by the callerAddress.
     */
    @Getter
    @Setter
    private String contractAddress;

    /**
     * caller of the transaction which produce this event.
     */
    @Getter
    @Setter
    private String callerAddress;

    /**
     * origin address of the contract which produce this event.
     */
    @Getter
    @Setter
    private String originAddress;

    /**
     * caller address of the contract which produce this event.
     */
    @Getter
    @Setter
    private String creatorAddress;

    /**
     * block number of the transaction
     */
    @Getter
    @Setter
    private Long blockNum;

    /**
     * block timestamp of the transaction
     */
    @Getter
    @Setter
    private Long blockTimestamp;

    /**
     * true if the transaction has been revoked
     */
    @Getter
    @Setter
    private boolean removed;


    public ContractTrigger(String txId, String contractAddress, String callerAddress,
                         String originAddress, String creatorAddress, Long blockNum, Long blockTimestamp){
        this.txId = txId;
        this.contractAddress = contractAddress;
        this.callerAddress = callerAddress;
        this.originAddress = originAddress;
        this.creatorAddress = creatorAddress;
        this.blockNum = blockNum;
        this.blockTimestamp = blockTimestamp;
    }
}
