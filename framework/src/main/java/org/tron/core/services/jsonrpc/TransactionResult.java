package org.tron.core.services.jsonrpc;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getToAddress;
import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getTransactionAmount;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract;

class TransactionResult {

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

  public String v;
  public String r;
  public String s;

  public TransactionResult(Protocol.Block b, int index, Protocol.Transaction tx, Wallet wallet) {
    BlockCapsule blockCapsule = new BlockCapsule(b);

    byte[] txid = new TransactionCapsule(tx).getTransactionId().getBytes();
    hash = ByteArray.toJsonHex(txid);
    nonce = null; // no value
    blockHash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    blockNumber = ByteArray.toJsonHex(blockCapsule.getNum());
    transactionIndex = ByteArray.toJsonHex(index);

    if (!tx.getRawData().getContractList().isEmpty()) {
      Contract contract = tx.getRawData().getContract(0);
      byte[] fromByte = TransactionCapsule.getOwner(contract);
      byte[] toByte = getToAddress(tx);
      from = ByteArray.toJsonHexAddress(fromByte);
      to = ByteArray.toJsonHexAddress(toByte);
      value = ByteArray.toJsonHex(getTransactionAmount(contract, hash, wallet));
    } else {
      from = null;
      to = null;
      value = null;
    }

    gas = null; // no value
    gasPrice = null; // no value
    input = null; // no value

    ByteString signature = tx.getSignature(0); // r[32] + s[32] + v[1]
    byte[] signData = signature.toByteArray();
    byte vByte = (byte) (signData[64] + 27); // according to Base64toBytes
    byte[] rByte = Arrays.copyOfRange(signData, 0, 32);
    byte[] sByte = Arrays.copyOfRange(signData, 32, 64);
    v = ByteArray.toJsonHex(vByte);
    r = ByteArray.toJsonHex(rByte);
    s = ByteArray.toJsonHex(sByte);
  }

  @Override
  public String toString() {
    return "TransactionResult{"
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