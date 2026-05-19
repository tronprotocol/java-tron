package org.tron.core.services.jsonrpc.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.tron.core.services.jsonrpc.TronJsonRpc.LogFilterElement;

@JsonPropertyOrder(alphabetic = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SimulateCallResult {

  @Getter
  @Setter
  private String returnData;

  @Getter
  @Setter
  private String gasUsed;

  @Getter
  @Setter
  private String status;

  @Getter
  @Setter
  private List<LogFilterElement> logs;

  @Getter
  @Setter
  private String transactionHash;

  @Getter
  @Setter
  private String transactionIndex;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Getter
  @Setter
  private String contractAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Getter
  @Setter
  private String errorCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Getter
  @Setter
  private String errorMessage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Getter
  @Setter
  private String errorData;
}
