package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import lombok.Getter;
import lombok.ToString;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;

@JsonPropertyOrder(alphabetic = true)
@ToString
public class TransactionResult {

  @Getter
  private final String hash;
  @Getter
  private final String nonce;
  @Getter
  private final String blockHash;
  @Getter
  private final String blockNumber;
  @Getter
  private final String transactionIndex;

  @Getter
  private final String from;
  @Getter
  private final String to;
  @Getter
  private final String gas;
  @Getter
  private final String gasPrice;
  @Getter
  private final String value;
  @Getter
  private final String input;
  @Getter
  private final String type = "0x0";

  @Getter
  private String v;
  @Getter
  private String r;
  @Getter
  private String s;

  private void parseSignature(Transaction tx) {

    if (tx.getSignatureCount() == 0) {
      v = ByteArray.toJsonHex(new byte[1]);
      r = ByteArray.toJsonHex(new byte[32]);
      s = ByteArray.toJsonHex(new byte[32]);
      return;
    }

    ByteString signature = tx.getSignature(0); // r[32] + s[32] + v[1]
    byte[] signData = signature.toByteArray();
    byte[] rByte = Arrays.copyOfRange(signData, 0, 32);
    byte[] sByte = Arrays.copyOfRange(signData, 32, 64);
    byte vByte = signData[64];
    if (vByte < 27) {
      vByte += 27;
    }
    v = ByteArray.toJsonHex(vByte);
    r = ByteArray.toJsonHex(rByte);
    s = ByteArray.toJsonHex(sByte);
  }

  private String parseInput(Transaction tx) {
    String data;
    if (tx.getRawData().getContractCount() == 0) {
      data = "0x";
    } else {
      Contract contract = tx.getRawData().getContract(0);
      if (contract.getType() == ContractType.TriggerSmartContract) {
        try {
          TriggerSmartContract triggerSmartContract = contract.getParameter()
              .unpack(TriggerSmartContract.class);
          data = ByteArray.toJsonHex(triggerSmartContract.getData().toByteArray());
        } catch (Exception e) {
          data = "0x";
        }
      } else {
        data = "0x";
      }
    }

    return data;
  }

  public TransactionResult(BlockCapsule blockCapsule, int index, Protocol.Transaction tx,
      long energyUsageTotal, long energyFee, Wallet wallet) {
    TransactionCapsule capsule = new TransactionCapsule(tx);
    byte[] txId = capsule.getTransactionId().getBytes();
    hash = ByteArray.toJsonHex(txId);
    nonce = ByteArray.toJsonHex(new byte[8]); // no value
    blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
    transactionIndex = ByteArray.toJsonHex(index);

    if (!tx.getRawData().getContractList().isEmpty()) {
      Contract contract = tx.getRawData().getContract(0);
      byte[] fromByte = capsule.getOwnerAddress();
      byte[] toByte = getToAddress(tx);

      if (blockCapsule.getNum() == 0) {
        from = ByteArray.toJsonHex(new byte[20]);
      } else {
        from = ByteArray.toJsonHexAddress(fromByte);
      }

      to = ByteArray.toJsonHexAddress(toByte);
      value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
    } else {
      from = ByteArray.toJsonHex(new byte[20]);
      to = ByteArray.toJsonHex(new byte[20]);
      value = "0x0";
    }

    gas = ByteArray.toJsonHex(energyUsageTotal);
    gasPrice = ByteArray.toJsonHex(energyFee);
    input = parseInput(tx);

    parseSignature(tx);
  }

  public TransactionResult(Transaction tx, Wallet wallet) {
    TransactionCapsule capsule = new TransactionCapsule(tx);
    byte[] txId = capsule.getTransactionId().getBytes();
    hash = ByteArray.toJsonHex(txId);
    nonce = ByteArray.toJsonHex(new byte[8]); // no value
    blockHash = "0x";
    blockNumber = "0x";
    transactionIndex = "0x";

    if (!tx.getRawData().getContractList().isEmpty()) {
      Contract contract = tx.getRawData().getContract(0);
      byte[] fromByte = capsule.getOwnerAddress();
      byte[] toByte = getToAddress(tx);
      from = ByteArray.toJsonHexAddress(fromByte);
      to = ByteArray.toJsonHexAddress(toByte);
      value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
    } else {
      from = ByteArray.toJsonHex(new byte[20]);
      to = ByteArray.toJsonHex(new byte[20]);
      value = "0x0";
    }

    gas = "0x0";
    gasPrice = "0x";
    input = parseInput(tx);

    parseSignature(tx);
  }
}