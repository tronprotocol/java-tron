package io.bitquery.tron;

import com.google.protobuf.ByteString;
import io.bitquery.streaming.common.utils.ByteArray;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;
import io.bitquery.protos.TronMessage.Chain;
import io.bitquery.protos.TronMessage.Transaction;
import io.bitquery.protos.TronMessage.Witness;
import io.bitquery.protos.TronMessage.BlockHeader;
import io.bitquery.protos.TronMessage.BlockMessage;

@Slf4j(topic = "tracer")
public class BlockMessageBuilder {

    private BlockMessage.Builder messageBuilder;

    public BlockMessageBuilder() {
        this.messageBuilder = BlockMessage.newBuilder();
    }

    public void buildBlockStartMessage(BlockCapsule block, String chainId) {
        setChain(chainId);
        setBlockHeader(block);
        setBlockWitness(block);
    }

    public void buildBlockEndMessage() {
        setTransactionsCount();
    }

   public BlockMessage getMessage() {
       return messageBuilder.build();
   }

    public void addTransaction(Transaction tx) {
        this.messageBuilder.addTransactions(tx);
    }

    private void setTransactionsCount() {
        int count = messageBuilder.getTransactionsCount();
        this.messageBuilder.getHeaderBuilder().setTransactionsCount(count);
    }

    private void setChain(String chainId) {
        Chain chain = Chain.newBuilder()
                .setChainId(ByteString.copyFrom(chainId.getBytes()))
                .build();

        this.messageBuilder.setChain(chain).build();
    }

    private void setBlockHeader(BlockCapsule block) {
        BlockHeader header = BlockHeader.newBuilder()
                .setNumber(block.getNum())
                .setHash(block.getBlockId().getByteString())
                .setTimestamp(block.getTimeStamp())
                .setParentHash(block.getParentBlockId().getByteString())
                .setParentNumber(block.getParentBlockId().getNum())
                .setVersion(block.getInstance().getBlockHeader().getRawData().getVersion())
                .setTxTrieRoot(block.getInstance().getBlockHeader().getRawData().getTxTrieRoot())
                .setAccountStateRoot(block.getInstance().getBlockHeader().getRawData().getAccountStateRoot())
                .build();

        this.messageBuilder.setHeader(header).build();
    }

    private void setBlockWitness(BlockCapsule block) {
        ByteString witnessAddress = ByteString.copyFrom(ByteArray.addressWithout41(block.getWitnessAddress().toByteArray()));

        Witness witness = Witness.newBuilder()
                .setAddress(witnessAddress)
                .setId(block.getInstance().getBlockHeader().getRawData().getWitnessId())
                .setSignature(block.getInstance().getBlockHeader().getWitnessSignature())
                .build();

        this.messageBuilder.setWitness(witness).build();
    }
}
