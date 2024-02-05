package io.bitquery.streaming;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.StreamingMessageValidateException;
import org.tron.protos.streaming.TronMessage;
import io.bitquery.tron.BlockMessageCreator;
import io.bitquery.streaming.blockchain.BlockMessageDescriptor;
import io.bitquery.tron.BlockMessageValidator;
import io.bitquery.streaming.services.KafkaMessageBroker;
import io.bitquery.streaming.messages.ProtobufMessage;
@Slf4j(topic = "streaming")
public class StreamingProcessor {
    private final KafkaMessageBroker kafkaBroker;
    private final ProtobufMessage protobufMessage;
    private final String topic;
    private final boolean topicEnabled;

    public StreamingProcessor(String config) {

        this.kafkaBroker = new KafkaMessageBroker();
        this.protobufMessage = new ProtobufMessage();

        this.topic = kafkaTopicConf.getString("topic");
        this.topicEnabled = kafkaTopicConf.getBoolean("enable");
    }

    public void process(BlockCapsule newBlock) throws StreamingMessageValidateException {
        Stopwatch timer = Stopwatch.createStarted();

        BlockMessageCreator blockMessageCreator = new BlockMessageCreator(newBlock);
        blockMessageCreator.create();
        TronMessage.BlockMessage blockMessage = blockMessageCreator.getBlockMessage();

        BlockMessageValidator validator = new BlockMessageValidator(blockMessage);
        validator.validate();

        BlockMessageDescriptor blockMsgDescriptor = new BlockMessageDescriptor();
        blockMsgDescriptor.setBlockHash(newBlock.getBlockId().toString());
        blockMsgDescriptor.setBlockNumber(newBlock.getNum());
        blockMsgDescriptor.setParentHash(newBlock.getParentHash().toString());
        blockMsgDescriptor.setParentNumber(newBlock.getParentBlockId().getNum());
        blockMsgDescriptor.setChainId(CommonParameter.getInstance().getStreamingConfig().getChainId());

        protobufMessage.process(blockMsgDescriptor, blockMessage.toByteArray(), topic);
        kafkaBroker.send(topic, protobufMessage);

        logger.info(String.format("Streaming processing took %s, Num: %d", timer.stop(), newBlock.getNum()));
    }

    public void close() {
        protobufMessage.close();
        kafkaBroker.close();

        logger.info("StreamingProcessor closed");
    }

    public boolean enabled() {
        if (!CommonParameter.getInstance().getStreamingConfig().isEnable()) {
            return false;
        }

        return topicEnabled;
    }
}
