package io.bitquery.streaming.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.JsonUtil;
import org.tron.core.config.args.StreamingConfig;
import io.bitquery.streaming.messages.ProtobufMessage;

import java.util.Properties;
@Slf4j(topic = "streaming")
public class KafkaMessageBroker {
    private KafkaProducer producer;

    public KafkaMessageBroker() {
        this.producer = getKafkaProducer();
    }

    public void send(String topicName, ProtobufMessage protobufMessage) {
        String value = JsonUtil.obj2Json(protobufMessage.getMeta()).toString();
        long block = protobufMessage.getMeta().getDescriptor().getBlockNumber();

        ProducerRecord msg = new ProducerRecord<>(topicName, value);

        logger.info(String.format("Sending message, Num: %d, Topic: %s", block, topicName));

        producer.send(msg, new Callback() {
            public void onCompletion(RecordMetadata metadata, Exception e) {
                if (e == null) {
                    logger.info(String.format("Delivered message to topic, Num: %d, Topic: %s, Partition: %d, Offset: %d",
                                    protobufMessage.getMeta().getDescriptor().getBlockNumber(),
                                    metadata.topic(),
                                    metadata.partition(),
                                    metadata.offset())
                            );
                } else {
                    logger.info(String.format("Delivery failed, Num: %d, Error: %s", block, e.getMessage()));
                }
            }
        });
    }

    public void close() {
        producer.close();
    }

    private KafkaProducer<String, String> getKafkaProducer() {
        if (this.producer == null) {
            Properties properties = getProperties();
            this.producer = new KafkaProducer<>(properties);
        }
        return this.producer;
    }

    private Properties getProperties() {
        StreamingConfig streamingConfig = CommonParameter.getInstance().getStreamingConfig();

        Properties config = new Properties();
        config.put("bootstrap.servers", streamingConfig.getKafkaBrokerBootstrapServers());
        config.put("security.protocol", streamingConfig.getKafkaBrokerSecurityProtocol());
        config.put("ssl.truststore.type", streamingConfig.getKafkaBrokerSslTruststoreType());
        config.put("ssl.truststore.location", streamingConfig.getKafkaBrokerSslTruststoreLocation());
        config.put("ssl.keystore.type", streamingConfig.getKafkaBrokerSslKeystoreType());
        config.put("ssl.keystore.location", streamingConfig.getKafkaBrokerSslKeystoreLocation());
        config.put("ssl.key.password", streamingConfig.getKafkaBrokerSslKeyPassword());
        config.put("ssl.endpoint.identification.algorithm", streamingConfig.getKafkaBrokerSslEndpointIdentificationAlgorithm());
        config.put("allow.auto.create.topics", streamingConfig.isKafkaBrokerAllowAutoCreateTopics());

        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return config;
    }
}
