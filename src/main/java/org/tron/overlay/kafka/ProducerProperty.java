package org.tron.overlay.kafka;

import org.tron.config.Configer;

import static org.tron.overlay.kafka.Kafka.EMPTY_STRING;
import static org.tron.overlay.kafka.Kafka.KAFKA_HOST;
import static org.tron.overlay.kafka.Kafka.KAFKA_PORT;

public class ProducerProperty {
    private final static String DEFAULT_BOOTSTRAP_SERVERS = "127.0.0.1:9092";
    private final static String DEFAULT_ACKS = "all";
    private final static String DEFAULT_RETRIES = "0";
    private final static String DEFAULT_BATCH_SIZE = "16384";
    private final static String DEFAULT_LINGER_MS = "1";
    private final static String DEFAULT_BUFFER_MEMORY = "33554432";
    private final static String DEFAULT_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    private final static String DEFAULT_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    private String bootstrapServers;
    private String acks;
    private String retries;
    private String batchSize;
    private String lingerMS;
    private String bufferMemory;
    private String keySerializer;
    private String valueSerializer;

    public ProducerProperty() {
    }

    public ProducerProperty(String bootstrapServers, String acks, String retries, String batchSize, String lingerMS,
                            String
                                    bufferMemory, String keySerializer, String valueSerializer) {
        this.bootstrapServers = bootstrapServers;
        this.acks = acks;
        this.retries = retries;
        this.batchSize = batchSize;
        this.lingerMS = lingerMS;
        this.bufferMemory = bufferMemory;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    public static ProducerProperty getDefault() {
        String bootstrapServers = Configer.getConf().getString(KAFKA_HOST) + Configer.getConf().getString(KAFKA_PORT);

        if (EMPTY_STRING.equals(bootstrapServers)) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
        }

        return new ProducerProperty(
                bootstrapServers,
                DEFAULT_ACKS,
                DEFAULT_RETRIES,
                DEFAULT_BATCH_SIZE,
                DEFAULT_LINGER_MS,
                DEFAULT_BUFFER_MEMORY,
                DEFAULT_KEY_SERIALIZER,
                DEFAULT_VALUE_SERIALIZER
        );
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getAcks() {
        return acks;
    }

    public void setAcks(String acks) {
        this.acks = acks;
    }

    public String getRetries() {
        return retries;
    }

    public void setRetries(String retries) {
        this.retries = retries;
    }

    public String getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public String getLingerMS() {
        return lingerMS;
    }

    public void setLingerMS(String lingerMS) {
        this.lingerMS = lingerMS;
    }

    public String getBufferMemory() {
        return bufferMemory;
    }

    public void setBufferMemory(String bufferMemory) {
        this.bufferMemory = bufferMemory;
    }

    public String getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(String keySerializer) {
        this.keySerializer = keySerializer;
    }

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
}
