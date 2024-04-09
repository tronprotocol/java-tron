package io.bitquery.streaming.services;

import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.messages.MessageMetaInfo;
import io.bitquery.streaming.messages.ProtobufMessage;
import io.bitquery.streaming.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.ArrayList;
import java.util.List;
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
        MessageMetaInfo protobufMetaInfo = protobufMessage.getMeta();

        List<Header> headers = createHeadersIfNeeded(protobufMetaInfo);
        String messageJson = JsonUtil.obj2Json(protobufMetaInfo);
        String blockNum = protobufMetaInfo.getDescriptor().getBlockNumber();

//        ProducerRecord msg = new ProducerRecord<>(topicName, value);
        ProducerRecord msg = new ProducerRecord(topicName, null, (Object) null, messageJson, headers);

        logger.info("Sending message, Num: {}, Topic: {}", blockNum, topicName);

        producer.send(msg, (metadata, e) -> {
            if (e == null) {
                logSuccessMessage(metadata, blockNum);
            } else {
                logFailureMessage(e, blockNum);
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

    private List<Header> createHeadersIfNeeded(MessageMetaInfo protobufMetaInfo) {
        List<Header> headers = new ArrayList<>();
        if (protobufMetaInfo.getEmbeddedBody() != null && protobufMetaInfo.getServers() == null) {
            headers.add(new RecordHeader(protobufMetaInfo.getUri(), protobufMetaInfo.getEmbeddedBody()));
        }
        return headers;
    }

    private void logSuccessMessage(RecordMetadata metadata, String blockNum) {
        logger.info("Delivered message to topic, Num: {}, Topic: {}, Partition: {}, Offset: {}",
                blockNum, metadata.topic(), metadata.partition(), metadata.offset());
    }

    private void logFailureMessage(Exception e, String blockNum) {
        logger.error("Delivery failed, Num: {}, Error: {}", blockNum, e.getMessage());
    }
}
