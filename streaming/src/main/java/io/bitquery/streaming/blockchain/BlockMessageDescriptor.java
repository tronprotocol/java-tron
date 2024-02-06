package io.bitquery.streaming.blockchain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class BlockMessageDescriptor {
  private String blockHash;
  private long blockNumber;
  private String parentHash;
  private long parentNumber;
  private String chainId;
}
