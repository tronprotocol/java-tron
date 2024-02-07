package io.bitquery.streaming.blockchain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.bitquery.streaming.messages.Descriptor;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class BlockMessageDescriptor implements Descriptor {
  private String blockHash;
  private long blockNumber;
  private String parentHash;
  private long parentNumber;
  private String chainId;
}
