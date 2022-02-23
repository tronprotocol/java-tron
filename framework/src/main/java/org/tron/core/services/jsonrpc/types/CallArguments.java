package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressCompatibleToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.paramStringIsNull;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseQuantityValue;

import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.core.Wallet;
import org.tron.core.exception.JsonRpcInvalidParamsException;
import org.tron.core.exception.JsonRpcInvalidRequestException;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CallArguments {

  @Getter
  @Setter
  private String from = "0x0000000000000000000000000000000000000000";
  @Getter
  @Setter
  private String to;
  @Getter
  @Setter
  private String gas = ""; //not used
  @Getter
  @Setter
  private String gasPrice = ""; //not used
  @Getter
  @Setter
  private String value = "";
  @Getter
  @Setter
  private String data;
  @Getter
  @Setter
  private String nonce; // not used

  /**
   * just support TransferContract, CreateSmartContract and TriggerSmartContract
   * */
  public ContractType getContractType(Wallet wallet) throws JsonRpcInvalidRequestException,
      JsonRpcInvalidParamsException {
    ContractType contractType;

    // from or to is null
    if (paramStringIsNull(from)) {
      throw new JsonRpcInvalidRequestException("invalid json request");
    } else if (paramStringIsNull(to)) {
      // data is null
      if (paramStringIsNull(data)) {
        throw new JsonRpcInvalidRequestException("invalid json request");
      }

      contractType = ContractType.CreateSmartContract;
    } else {
      byte[] contractAddressData = addressCompatibleToByteArray(to);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      BytesMessage bytesMessage =
          build.setValue(ByteString.copyFrom(contractAddressData)).build();
      SmartContract smartContract = wallet.getContract(bytesMessage);

      // check if to is smart contract
      if (smartContract != null) {
        contractType = ContractType.TriggerSmartContract;
      } else {
        if (StringUtils.isNotEmpty(value)) {
          contractType = ContractType.TransferContract;
        } else {
          throw new JsonRpcInvalidRequestException("invalid json request: invalid value");
        }
      }
    }
    return contractType;
  }

  public long parseValue() throws JsonRpcInvalidParamsException {
    return parseQuantityValue(value);
  }
}