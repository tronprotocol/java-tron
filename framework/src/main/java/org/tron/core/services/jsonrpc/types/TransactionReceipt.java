package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.convertToTronAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;

@JsonPropertyOrder(alphabetic = true)
public class TransactionReceipt {
  @JsonPropertyOrder(alphabetic = true)
  public static class TransactionLog {

    @Getter
    @Setter
    private String logIndex;
    @Getter
    @Setter
    private String blockHash;
    @Getter
    @Setter
    private String blockNumber;
    @Getter
    @Setter
    private String transactionIndex;
    @Getter
    @Setter
    private String transactionHash;
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String data;
    @Getter
    @Setter
    private String[] topics;
    @Getter
    @Setter
    private boolean removed = false;

    public TransactionLog() {
    }
  }

  @Getter
  @Setter
  private String blockHash;
  @Getter
  @Setter
  private String blockNumber;
  @Getter
  @Setter
  private String transactionIndex;
  @Getter
  @Setter
  private String transactionHash;
  @Getter
  @Setter
  private String from;
  @Getter
  @Setter
  private String to;

  @Getter
  @Setter
  private String cumulativeGasUsed;
  @Getter
  @Setter
  private String effectiveGasPrice;
  @Getter
  @Setter
  private String gasUsed;
  @Getter
  @Setter
  private String contractAddress;
  @Getter
  @Setter
  private TransactionLog[] logs;
  @Getter
  @Setter
  private String logsBloom;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String root;  // 32 bytes of post-transaction stateroot (pre Byzantium)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String status;  //  either 1 (success) or 0 (failure) (post Byzantium)

  @Getter
  @Setter
  private String type = "0x0";

  public TransactionReceipt() {
  }

}