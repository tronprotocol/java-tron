package org.tron.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;

public class TransactionLogTrigger {

    @Getter
    @Setter
    private long timestamp;

    @Getter
    @Setter
    private Sha256Hash transactionId;

    @Getter
    @Setter
    private String transactionHash;

    @Getter
    @Setter
    private BlockId blockId;
}
