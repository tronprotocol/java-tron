package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.encode58Check;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;

import com.google.protobuf.ByteString;
import java.util.Arrays;
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
  public String fromBase58;
  public String to;
  public String toBase58;
  public String gas;
  public String gasPrice;
  public String value;
  public String input;

  public String v;
  public String r;
  public String s;

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
      byte[] fromByte = TransactionCapsule.getOwner(contract);
      byte[] toByte = getToAddress(tx);
      from = ByteArray.toJsonHex(fromByte);
      to = ByteArray.toJsonHex(toByte);
      fromBase58 = encode58Check(fromByte);
      toBase58 = encode58Check(toByte);
      value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
    } else {
      from = "";
      to = "";
      fromBase58 = "";
      toBase58 = "";
      value = "";
    }

    gas = "";
    gasPrice = "";
    input = "";

    ByteString signature = tx.getSignature(0); // r[32] + s[32] + 符号位v[1]
    byte[] signData = signature.toByteArray();
    byte vByte = (byte) (signData[64] + 27); //参考函数 Base64toBytes
    byte[] rByte = Arrays.copyOfRange(signData, 0, 32);
    byte[] sByte = Arrays.copyOfRange(signData, 32, 64);
    v = ByteArray.toJsonHex(vByte);
    r = ByteArray.toJsonHex(rByte);
    s = ByteArray.toJsonHex(sByte);
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