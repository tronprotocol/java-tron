package io.bitquery.streaming;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

;import java.io.File;
import java.util.List;

public class TracerConfig {
    private Config config;

    /**
     * Keys (names) of config
     */
    private static final String ENABLE_KEY = "enable";
    private static final String FILE_STORAGE_ROOT_KEY = "file_storage.root";

    /**
     * Default values
     */
    private static final boolean DEFAULT_ENABLE = false;
    private static final String DEFAULT_FILE_STORAGE_ROOT = "streaming-directory";

    /**
     * General information
     */
    @Getter
    private boolean enable;

    @Getter
    private String chainId;

    /**
     * File Storage config
     */
    @Getter
    private String fileStorageRoot;

    @Getter
    private int fileStorageTtlSecs;

    @Getter
    private int fileStoragePoolPeriodSec;

    @Getter
    private List<String> fileStorageUrls;

    /**
     * Elliptic Signer config
     */
    @Getter
    private String ellipticSignerPrivateKeyHex;

    /**
     * Kafka Topics config
     */
    @Getter
    private Config kafkaTopicBlocks;

    /**
     * Kafka Broker config
     */
    @Getter
    private String kafkaBrokerBootstrapServers;

    @Getter
    private String kafkaBrokerSecurityProtocol;

    @Getter
    private String kafkaBrokerSslTruststoreType;

    @Getter
    private String kafkaBrokerSslTruststoreLocation;

    @Getter
    private String kafkaBrokerSslKeystoreType;

    @Getter
    private String kafkaBrokerSslKeystoreLocation;

    @Getter
    private String kafkaBrokerSslKeyPassword;

    @Getter
    private String kafkaBrokerSslEndpointIdentificationAlgorithm;

    @Getter
    private boolean kafkaBrokerAllowAutoCreateTopics;

    /**
     * Path Generator config
     */
    @Getter
    private int pathGeneratorBucketSize;

    @Getter
    private int pathGeneratorBlockNumberPadding;

    @Getter
    private String pathGeneratorSpacer;

    @Getter
    private String pathGeneratorSuffix;

    public static boolean getEnableFromConfig(final Config config) {
        return config.hasPath(ENABLE_KEY)
                ? config.getBoolean(ENABLE_KEY)
                : DEFAULT_ENABLE;
    }

    public static String getFileStorageFromConfig(final Config config) {
        return config.hasPath(FILE_STORAGE_ROOT_KEY)
                ? config.getString(FILE_STORAGE_ROOT_KEY)
                : DEFAULT_FILE_STORAGE_ROOT;
    }

    public TracerConfig(String configPath) {
        config = ConfigFactory.parseFile(new File(configPath));

        enable = getEnableFromConfig(config);
        chainId = config.getString("chain_id");

        fileStorageRoot = getFileStorageFromConfig(config);
        fileStorageTtlSecs = config.getInt("file_storage.ttl_secs");
        fileStoragePoolPeriodSec = config.getInt("file_storage.pool_period_sec");
        fileStorageUrls = config.getStringList("file_storage.urls");

        ellipticSignerPrivateKeyHex = config.getString("elliptic_signer.private_key_hex");

        kafkaTopicBlocks = config.getConfig("kafka_topics.blocks");
        kafkaBrokerBootstrapServers = config.getString("kafka_broker.bootstrap_servers");
        kafkaBrokerSecurityProtocol = config.getString("kafka_broker.security_protocol");
        kafkaBrokerSslTruststoreType = config.getString("kafka_broker.ssl_truststore_type");
        kafkaBrokerSslTruststoreLocation = config.getString("kafka_broker.ssl_truststore_location");
        kafkaBrokerSslKeystoreType = config.getString("kafka_broker.ssl_keystore_type");
        kafkaBrokerSslKeystoreLocation = config.getString("kafka_broker.ssl_keystore_location");
        kafkaBrokerSslKeyPassword = config.getString("kafka_broker.ssl_key_password");
        kafkaBrokerSslEndpointIdentificationAlgorithm = config.getString("kafka_broker.ssl_endpoint_identification_algorithm");
        kafkaBrokerAllowAutoCreateTopics = config.getBoolean("kafka_broker.allow_auto_create_topics");

        pathGeneratorBucketSize = config.getInt("path_generator.bucket_size");
        pathGeneratorBlockNumberPadding = config.getInt("path_generator.block_number_padding");
        pathGeneratorSpacer = config.getString("path_generator.spacer");
        pathGeneratorSuffix = config.getString("path_generator.suffix");
    }
}
