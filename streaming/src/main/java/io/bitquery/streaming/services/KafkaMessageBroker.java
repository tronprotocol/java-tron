package io.bitquery.streaming.services;

import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.messages.ProtobufMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.tron.common.utils.JsonUtil;

import java.util.Properties;
@Slf4j(topic = "streaming")
public class KafkaMessageBroker {
    private final TracerConfig config;
    private KafkaProducer producer;

    public KafkaMessageBroker(TracerConfig config) {
        this.config = config;
        this.producer = getKafkaProducer();
    }

    public void send(String topicName, ProtobufMessage protobufMessage) {
        String value = JsonUtil.obj2Json(protobufMessage.getMeta()).toString();
        long block = protobufMessage.getMeta().getDescriptor().getBlockNumber();

        ProducerRecord msg = new ProducerRecord<>(topicName, value);

        logger.info("Sending message, Num: {}, Topic: {}", block, topicName);

        producer.send(msg, new Callback() {
            public void onCompletion(RecordMetadata metadata, Exception e) {
                if (e == null) {
                    logger.info(
                        "Delivered message to topic, Num: {}, Topic: {}, Partition: {}, Offset: {}",
                        protobufMessage.getMeta().getDescriptor().getBlockNumber(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                    );
                } else {
                    logger.info("Delivery failed, Num: {}, Error: {}", block, e.getMessage());
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
        Properties properties = new Properties();

        properties.put("bootstrap.servers", config.getKafkaBrokerBootstrapServers());
        properties.put("security.protocol", config.getKafkaBrokerSecurityProtocol());
        properties.put("ssl.truststore.type", config.getKafkaBrokerSslTruststoreType());
        properties.put("ssl.truststore.location", config.getKafkaBrokerSslTruststoreLocation());
        properties.put("ssl.keystore.type", config.getKafkaBrokerSslKeystoreType());
        properties.put("ssl.keystore.location", config.getKafkaBrokerSslKeystoreLocation());
        properties.put("ssl.key.password", config.getKafkaBrokerSslKeyPassword());
        properties.put("ssl.endpoint.identification.algorithm", config.getKafkaBrokerSslEndpointIdentificationAlgorithm());
        properties.put("allow.auto.create.topics", config.isKafkaBrokerAllowAutoCreateTopics());

        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return properties;
    }
}
