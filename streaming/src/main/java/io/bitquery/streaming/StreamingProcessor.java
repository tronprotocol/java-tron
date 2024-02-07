package io.bitquery.streaming;

import com.google.common.base.Stopwatch;
import io.bitquery.streaming.messages.Descriptor;
import lombok.extern.slf4j.Slf4j;
import io.bitquery.streaming.services.KafkaMessageBroker;
import io.bitquery.streaming.messages.ProtobufMessage;

@Slf4j(topic = "streaming")
public class StreamingProcessor {

    private TracerConfig config;
    private final String topic;
    private final boolean topicEnabled;

    private final KafkaMessageBroker kafkaBroker;

    private final ProtobufMessage protobufMessage;

    public StreamingProcessor(TracerConfig config) {
        this.config = config;
        this.topic = config.getKafkaTopicBlocks().getString("topic");
        this.topicEnabled = config.getKafkaTopicBlocks().getBoolean("enable");

        this.protobufMessage = new ProtobufMessage(config);
        this.kafkaBroker = new KafkaMessageBroker(config);
    }

     public void process(Descriptor descriptor, byte[] message) {
        Stopwatch timer = Stopwatch.createStarted();

        protobufMessage.process(descriptor, message, topic);
        kafkaBroker.send(topic, protobufMessage);

        logger.info("Streaming processing took {}, Num: {}", timer.stop(), descriptor.getBlockNumber());
     }

    public void close() {
        protobufMessage.close();
        kafkaBroker.close();

        logger.info("StreamingProcessor closed");
    }

//    public boolean enabled() {
//        if (!CommonParameter.getInstance().getStreamingConfig().isEnable()) {
//            return false;
//        }
//
//        return topicEnabled;
//    }
}
