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
     * true if the transaction has been revoked
     */
    @Getter
    @Setter
    private boolean removed;
}
