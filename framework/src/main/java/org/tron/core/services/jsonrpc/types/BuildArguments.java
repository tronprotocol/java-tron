package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressCompatibleToByteArray;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.paramQuantityIsNull;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.paramStringIsNull;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.parseQuantityValue;

import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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
public class BuildArguments {

  public String from;
  public String to;
  public String gas = "0x0";
  public String gasPrice = ""; //not used
  public String value;
  public String data;
  public String nonce = ""; //not used

  public Long tokenId = 0L;
  public Long tokenValue = 0L;
  public String abi = "";
  public Long consumeUserResourcePercent = 0L;
  public Long originEnergyLimit = 0L;
  public String name = "";

  public Integer permissionId = 0;
  public String extraData = "";

  public boolean visible = false;

  public BuildArguments(CallArguments args) {
    from = args.from;
    to = args.to;
    gas = args.gas;
    gasPrice = args.gasPrice;
    value = args.value;
    data = args.data;
  }

  public ContractType getContractType(Wallet wallet) throws JsonRpcInvalidRequestException,
      JsonRpcInvalidParamsException {
    ContractType contractType;

    // to is null
    if (paramStringIsNull(to)) {
      // data is null
      if (paramStringIsNull(data)) {
        throw new JsonRpcInvalidRequestException("invalid json request");
      }

      contractType = ContractType.CreateSmartContract;
    } else {
      // to is not null
      byte[] contractAddressData = addressCompatibleToByteArray(to);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(contractAddressData)).build();
      SmartContract smartContract = wallet.getContract(bytesMessage);

      // check if to is smart contract
      if (smartContract != null) {
        contractType = ContractType.TriggerSmartContract;
      } else {
        // tokenId and tokenValue: trc10, value: TRX
        if (availableTransferAsset()) {
          contractType = ContractType.TransferAssetContract;
        } else {
          if (StringUtils.isNotEmpty(value)) {
            contractType = ContractType.TransferContract;
          } else {
            throw new JsonRpcInvalidRequestException("invalid json request");
          }
        }
      }
    }

    return contractType;
  }

  public long parseValue() throws JsonRpcInvalidParamsException {
    return parseQuantityValue(value);
  }

  public long parseGas() throws JsonRpcInvalidParamsException {
    return parseQuantityValue(gas);
  }

  private boolean availableTransferAsset() {
    return tokenId > 0 && tokenValue > 0 && paramQuantityIsNull(value);
  }

}