package io.bitquery.streaming.services;

import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.common.utils.FileUtil;
import io.bitquery.streaming.messages.ProtobufMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "streaming")
public class EmbeddedFileStorage {

    private ProtobufMessage protobufMessage;
    private TracerConfig config;

    private FileStorage fileStorage;

    public EmbeddedFileStorage(ProtobufMessage protobufMessage, TracerConfig config) {
        this.protobufMessage = protobufMessage;
        this.config = config;

        this.fileStorage = new FileStorage(protobufMessage, config);
    }

    public void store() {
        int bodyLength = protobufMessage.getBody().length;

        // if size is too big, save the message on disk using file storage
        if (bodyLength > config.getEmbeddedFileStorageMessageMaxTotalSize()) {
           logger.info("Message is too big, saving on disk, Size {}, Max: {}", bodyLength, config.getEmbeddedFileStorageMessageMaxTotalSize());
            fileStorage.store();
            return;
        }

        protobufMessage.getMeta().setUri(fileStorage.getBlockPathWithoutStreamingDirectory());
        protobufMessage.getMeta().setServers(null);
        protobufMessage.getMeta().setCompressed(false);
        protobufMessage.getMeta().setSize(bodyLength);
        protobufMessage.getMeta().setEmbeddedBody(protobufMessage.getBody());
    }

    public void close() {
        fileStorage.close();
    }
}
