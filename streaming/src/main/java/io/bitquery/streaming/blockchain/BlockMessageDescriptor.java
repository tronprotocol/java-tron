package io.bitquery.streaming.blockchain;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class BlockMessageDescriptor {
  private String blockHash;
  private long blockNumber;
  private String parentHash;
  private long parentNumber;
  private String chainId;
}
