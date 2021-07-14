package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.addressHashToByteArray;

import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BuildArguments {
  public String from;
  public String to;
  public String gas; //not used
  public String gasPrice; //not used
  public String value;
  public String data;
  public String nonce; //not used

  public Long tokenId = 0L;
  public Long callTokenValue = 0L;
  public String abi = "";
  // public Long callValue = 0L;
  public Long consumeUserResourcePercent = 0L;
  public Long originEnergyLimit = 0L;
  public String name = "";
  public Long feeLimit = 0L;

  public Integer permissionId = 0;
  public String extraData = "";

  public boolean visible = false;

  public ContractType getContractType(Wallet wallet) {
    ContractType contractType;

    // to is null
    if (StringUtils.isEmpty(to) || to.equals("0x")) {
      // data is null
      if (StringUtils.isEmpty(data) || data.equals("0x")) {
        throw new JsonRpcInvalidRequest("invalid json request");
      }

      contractType = ContractType.CreateSmartContract;
    } else {
      // to is not null
      byte[] contractAddressData = addressHashToByteArray(to);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      BytesMessage bytesMessage = build.setValue(ByteString.copyFrom(contractAddressData)).build();
      SmartContract smartContract = wallet.getContract(bytesMessage);

      // check if to is smart contract
      if (smartContract != null) {
        contractType = ContractType.TriggerSmartContract;
      } else {
        if (StringUtils.isEmpty(value)) {
          throw new JsonRpcInvalidRequest("invalid json request");
        }
        contractType = ContractType.TransferContract;
      }
    }

    return contractType;
  }

  public long parseCallValue() {
    long callValue = 0L;

    if (StringUtils.isNotEmpty(value)) {
      try {
        callValue = ByteArray.jsonHexToLong(value);
      } catch (Exception e) {
        throw new JsonRpcInvalidParams("invalid hex number");
      }
    }

    return callValue;
  }

}