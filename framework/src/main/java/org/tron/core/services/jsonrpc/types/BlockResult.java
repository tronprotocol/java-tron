package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getEnergyUsageTotal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;

@JsonPropertyOrder(alphabetic = true)
public class BlockResult {

  public String number;
  public String hash;
  public String parentHash;
  public String nonce;
  public String sha3Uncles;
  public String logsBloom;
  public String transactionsRoot;
  public String stateRoot;
  public String receiptsRoot;
  public String miner;
  public String difficulty;
  public String totalDifficulty;
  public String extraData;
  public String size;
  public String gasLimit;
  public String gasUsed;
  public String timestamp;
  public Object[] transactions; //TransactionResult or byte32
  public String[] uncles;

  public String baseFeePerGas = null;
  public String mixHash = null;

  public BlockResult(Block block, boolean fullTx, Wallet wallet) {
    BlockCapsule blockCapsule = new BlockCapsule(block);

    number = ByteArray.toJsonHex(blockCapsule.getNum());
    hash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    parentHash =
        ByteArray.toJsonHex(block.getBlockHeader().getRawData().getParentHash().toByteArray());
    nonce = null; // no value
    sha3Uncles = null; // no value
    logsBloom = ByteArray.toJsonHex(new byte[256]); // no value
    transactionsRoot = ByteArray
        .toJsonHex(block.getBlockHeader().getRawData().getTxTrieRoot().toByteArray());
    stateRoot = ByteArray
        .toJsonHex(block.getBlockHeader().getRawData().getAccountStateRoot().toByteArray());
    receiptsRoot = null; // no value
    miner = ByteArray.toJsonHexAddress(blockCapsule.getWitnessAddress().toByteArray());
    difficulty = null; // no value
    totalDifficulty = null; // no value
    extraData = null; // no value
    size = ByteArray.toJsonHex(block.getSerializedSize());
    timestamp = ByteArray.toJsonHex(blockCapsule.getTimeStamp());

    long gasUsedInBlock = 0;
    long gasLimitInBlock = 0;

    List<Object> txes = new ArrayList<>();
    List<Transaction> transactionsList = block.getTransactionsList();
    List<TransactionInfo> transactionInfoList =
        wallet.getTransactionInfoByBlockNum(blockCapsule.getNum()).getTransactionInfoList();
    if (fullTx) {
      long energyFee = wallet.getEnergyFee(blockCapsule.getTimeStamp());

      for (int i = 0; i < transactionsList.size(); i++) {
        Transaction transaction = transactionsList.get(i);
        gasLimitInBlock += transaction.getRawData().getFeeLimit();

        long energyUsageTotal = getEnergyUsageTotal(transactionInfoList, i, blockCapsule.getNum());
        gasUsedInBlock += energyUsageTotal;

        txes.add(new TransactionResult(blockCapsule, i, transaction,
            energyUsageTotal, energyFee, wallet));
      }
    } else {
      for (int i = 0; i < transactionsList.size(); i++) {
        gasLimitInBlock += transactionsList.get(i).getRawData().getFeeLimit();
        gasUsedInBlock += getEnergyUsageTotal(transactionInfoList, i, blockCapsule.getNum());

        byte[] txHash = Sha256Hash
            .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
                transactionsList.get(i).getRawData().toByteArray());
        txes.add(ByteArray.toJsonHex(txHash));
      }
    }
    transactions = txes.toArray();

    gasLimit = ByteArray.toJsonHex(gasLimitInBlock);
    gasUsed = ByteArray.toJsonHex(gasUsedInBlock);
    uncles = new String[0];
  }
}