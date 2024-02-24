package io.bitquery.streaming;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

import java.io.File;
import java.util.List;

public class TracerConfig {
    private Config configFile;

    private Config servicesConfig;

    /**
     * Keys (names) of config
     */
    private static final String ENABLE_KEY = "enable";
    private static final String FILE_STORAGE_ROOT_KEY = "services_config.file_storage.root";

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
     * Embedded File Storage config
     */
    @Getter
    private int embeddedFileStorageMessageMaxTotalSize;

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

    @Getter
    private Config kafkaTopicBroadcasted;

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
        configFile = ConfigFactory.parseFile(new File(configPath));
        servicesConfig = configFile.getConfig("services_config");

        enable = getEnableFromConfig(configFile);
        chainId = configFile.getString("chain_id");

        kafkaTopicBlocks = configFile.getConfig("kafka_topics.blocks");
        kafkaTopicBroadcasted = configFile.getConfig("kafka_topics.broadcasted");

        setServicesConfig();
    }

    private void setServicesConfig() {
        // Embedded File Storage service
        embeddedFileStorageMessageMaxTotalSize = servicesConfig.getInt("embedded_file_storage.message_max_total_size");

        // File Storage service
        fileStorageRoot = getFileStorageFromConfig(configFile);
        fileStorageTtlSecs = servicesConfig.getInt("file_storage.ttl_secs");
        fileStoragePoolPeriodSec = servicesConfig.getInt("file_storage.pool_period_sec");
        fileStorageUrls = servicesConfig.getStringList("file_storage.urls");

        // Elliptic Signer service
        ellipticSignerPrivateKeyHex = servicesConfig.getString("elliptic_signer.private_key_hex");

        // Kafka Broker service
        kafkaBrokerBootstrapServers = servicesConfig.getString("kafka_broker.bootstrap_servers");
        kafkaBrokerSecurityProtocol = servicesConfig.getString("kafka_broker.security_protocol");
        kafkaBrokerSslTruststoreType = servicesConfig.getString("kafka_broker.ssl_truststore_type");
        kafkaBrokerSslTruststoreLocation = servicesConfig.getString("kafka_broker.ssl_truststore_location");
        kafkaBrokerSslKeystoreType = servicesConfig.getString("kafka_broker.ssl_keystore_type");
        kafkaBrokerSslKeystoreLocation = servicesConfig.getString("kafka_broker.ssl_keystore_location");
        kafkaBrokerSslKeyPassword = servicesConfig.getString("kafka_broker.ssl_key_password");
        kafkaBrokerSslEndpointIdentificationAlgorithm = servicesConfig.getString("kafka_broker.ssl_endpoint_identification_algorithm");
        kafkaBrokerAllowAutoCreateTopics = servicesConfig.getBoolean("kafka_broker.allow_auto_create_topics");

        // Path Generator service
        pathGeneratorBucketSize = servicesConfig.getInt("path_generator.bucket_size");
        pathGeneratorBlockNumberPadding = servicesConfig.getInt("path_generator.block_number_padding");
        pathGeneratorSpacer = servicesConfig.getString("path_generator.spacer");
        pathGeneratorSuffix = servicesConfig.getString("path_generator.suffix");
    }
}
