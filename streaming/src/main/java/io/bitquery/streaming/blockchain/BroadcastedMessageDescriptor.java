package io.bitquery.streaming.blockchain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.bitquery.streaming.messages.Descriptor;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class BroadcastedMessageDescriptor implements Descriptor {
    private String blockHash;
    private String blockNumber;
    private String parentHash;
    private String parentNumber;
    private String chainId;

    private String timeStart;
    private String timeEnd;
    private List<String> TransactionHashes;
}
