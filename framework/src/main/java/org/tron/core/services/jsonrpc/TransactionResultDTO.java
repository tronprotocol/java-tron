package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;

import lombok.Value;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;

@Value
public class TransactionResultDTO {

  public String hash;
  public String nonce;
  public String blockHash;
  public String blockNumber;
  public String transactionIndex;

  public String from;
  public String to;
  public String gas;
  public String gasPrice;
  public String value;
  public String input;

  public TransactionResultDTO(Block b, int index, Transaction tx, Wallet wallet) {
    BlockCapsule blockCapsule = new BlockCapsule(b);

    byte[] txid = new TransactionCapsule(tx).getTransactionId().getBytes();
    hash = ByteArray.toJsonHex(txid);
    nonce = ""; // no value
    blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
    transactionIndex = ByteArray.toJsonHex(index);

    if (!tx.getRawData().getContractList().isEmpty()) {
      Contract contract = tx.getRawData().getContract(0);
      from = ByteArray.toJsonHex(TransactionCapsule.getOwner(contract));
      to = ByteArray.toJsonHex(getToAddress(tx));
      value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
    } else {
      from = "";
      to = "";
      value = "";
    }

    gas = "";
    gasPrice = "";

    input = "";
  }

  @Override
  public String toString() {
    return "TransactionResultDTO{"
        + "hash='" + hash + '\''
        + ", nonce='" + nonce + '\''
        + ", blockHash='" + blockHash + '\''
        + ", blockNumber='" + blockNumber + '\''
        + ", transactionIndex='" + transactionIndex + '\''
        + ", from='" + from + '\''
        + ", to='" + to + '\''
        + ", gas='" + gas + '\''
        + ", gasPrice='" + gasPrice + '\''
        + ", value='" + value + '\''
        + ", input='" + input + '\''
        + '}';
  }
}