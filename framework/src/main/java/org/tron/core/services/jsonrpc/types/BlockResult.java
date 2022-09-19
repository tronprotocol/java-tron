package org.tron.core.services.jsonrpc.types;

import static org.tron.core.services.jsonrpc.JsonRpcApiUtil.getEnergyUsageTotal;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
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

  @Getter
  @Setter
  private String number;
  @Getter
  @Setter
  private String hash;
  @Getter
  @Setter
  private String parentHash;
  @Getter
  @Setter
  private String nonce;
  @Getter
  @Setter
  private String sha3Uncles;
  @Getter
  @Setter
  private String logsBloom;
  @Getter
  @Setter
  private String transactionsRoot;
  @Getter
  @Setter
  private String stateRoot;
  @Getter
  @Setter
  private String receiptsRoot;
  @Getter
  @Setter
  private String miner;
  @Getter
  @Setter
  private String difficulty;
  @Getter
  @Setter
  private String totalDifficulty;
  @Getter
  @Setter
  private String extraData;
  @Getter
  @Setter
  private String size;
  @Getter
  @Setter
  private String gasLimit;
  @Getter
  @Setter
  private String gasUsed;
  @Getter
  @Setter
  private String timestamp;
  @Getter
  @Setter
  private Object[] transactions; //TransactionResult or byte32
  @Getter
  @Setter
  private String[] uncles;

  @Getter
  @Setter
  private String baseFeePerGas = "0x0";
  @Getter
  @Setter
  private String mixHash = ByteArray.toJsonHex(new byte[32]);

  public BlockResult(Block block, boolean fullTx, Wallet wallet) {
    BlockCapsule blockCapsule = new BlockCapsule(block);

    number = ByteArray.toJsonHex(blockCapsule.getNum());
    hash = ByteArray.toJsonHex(blockCapsule.getBlockId().getBytes());
    parentHash =
        ByteArray.toJsonHex(block.getBlockHeader().getRawData().getParentHash().toByteArray());
    nonce = ByteArray.toJsonHex(new byte[8]); // no value
    sha3Uncles = ByteArray.toJsonHex(new byte[32]); // no value
    logsBloom = ByteArray.toJsonHex(new byte[256]); // no value
    transactionsRoot = ByteArray
        .toJsonHex(block.getBlockHeader().getRawData().getTxTrieRoot().toByteArray());
    stateRoot = ByteArray
        .toJsonHex(block.getBlockHeader().getRawData().getAccountStateRoot().toByteArray());
    receiptsRoot = ByteArray.toJsonHex(new byte[32]); // no value

    if (blockCapsule.getNum() == 0) {
      miner = ByteArray.toJsonHex(new byte[20]);
    } else {
      miner = ByteArray.toJsonHexAddress(blockCapsule.getWitnessAddress().toByteArray());
    }

    difficulty = "0x0"; // no value
    totalDifficulty = "0x0"; // no value
    extraData = "0x"; // no value
    size = ByteArray.toJsonHex(block.getSerializedSize());
    timestamp = ByteArray.toJsonHex(blockCapsule.getTimeStamp() / 1000);

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