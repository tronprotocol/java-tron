package io.bitquery.tron;

import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.streaming.TronMessage.Transaction;
import org.tron.protos.streaming.TronMessage.Witness;
import org.tron.protos.streaming.TronMessage.BlockHeader;
import org.tron.protos.streaming.TronMessage.BlockMessage;

@Slf4j(topic = "tracer")
public class BlockMessageBuilder {

    private BlockMessage.Builder messageBuilder;

    public BlockMessageBuilder() {
        this.messageBuilder = BlockMessage.newBuilder();
    }

    public void buildBlockStartMessage(BlockCapsule block) {
        setBlockHeader(block);
        setBlockWitness(block);
    }

   public BlockMessage getMessage() {
       return messageBuilder.build();
   }

    private void setBlockHeader(BlockCapsule block) {
        BlockHeader header = BlockHeader.newBuilder()
                .setNumber(block.getNum())
                .setHash(block.getBlockId().getByteString())
                .setTimestamp(block.getTimeStamp())
                .setParentHash(block.getParentBlockId().getByteString())
                .setVersion(block.getInstance().getBlockHeader().getRawData().getVersion())
                .setTxTrieRoot(block.getInstance().getBlockHeader().getRawData().getTxTrieRoot())
                .setAccountStateRoot(block.getInstance().getBlockHeader().getRawData().getAccountStateRoot())
                .build();

        this.messageBuilder.setHeader(header).build();
    }

    private void setBlockWitness(BlockCapsule block) {
        Witness witness = Witness.newBuilder()
                .setAddress(block.getWitnessAddress())
                .setId(block.getInstance().getBlockHeader().getRawData().getWitnessId())
                .setSignature(block.getInstance().getBlockHeader().getWitnessSignature())
                .build();

        this.messageBuilder.setWitness(witness).build();
    }

    public void addTransaction(Transaction tx) {
        this.messageBuilder.addTransactions(tx);
    }
}
