package io.bitquery.streaming.messages;

import com.google.common.primitives.Bytes;
import io.bitquery.streaming.TracerConfig;
import io.bitquery.streaming.common.crypto.Hash;
import io.bitquery.streaming.services.EllipticSigner;
import io.bitquery.streaming.services.FileStorage;
import io.bitquery.streaming.common.utils.ByteArray;
import io.bitquery.streaming.common.utils.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j(topic = "streaming")
public class ProtobufMessage {
    @Getter
    public MessageMetaInfo meta = new MessageMetaInfo();

    @Getter
    private byte[] body;

    @Getter
    private String topic;

    private final TracerConfig config;

    private final EllipticSigner signer;
    private final FileStorage fileStorage;

    public ProtobufMessage(TracerConfig config) {
        this.config = config;

        this.signer = new EllipticSigner(config);
        this.fileStorage = new FileStorage(this, config);
    }

    public void process(Descriptor descriptor, byte[] body, String topic) {
        getMeta().setDescriptor(descriptor);
        getMeta().setAuthenticator(new MessageAuthenticator());
        getMeta().setSize(body.length);
        getMeta().setServers(config.getFileStorageUrls());

        this.body = body;
        this.topic = topic;

        sign();
        fileStorage.store();
    }

    public void close() {
        fileStorage.close();
    }

    public void sign() {
        prepareAuthenticator();

        byte[] message = ByteArray.fromHexString(getMeta().getAuthenticator().getId());
        byte[] signature = this.signer.sign(message);

        getMeta().getAuthenticator().setSigner(this.signer.getAddress());
        getMeta().getAuthenticator().setSignature(ByteArray.toHexString(signature));
    }

    public byte[] getBodyHash() {
        return Hash.sha3(body);
    }

    private void prepareAuthenticator() {
        logger.info("Preparing authenticator for block protobuf message");

        byte[] bodyHash = getBodyHash();

        Instant insTime = Instant.now();
        String time = String.format("%d%d", insTime.getEpochSecond(), insTime.getNano());
        byte[] timeBytes = ByteArray.fromLong(Long.parseLong(time));

        byte[] descriptor = ByteArray.fromObject(JsonUtil.obj2Json(getMeta().getDescriptor()));

        byte[] idHash = Bytes.concat(bodyHash, timeBytes, descriptor);
        idHash = Hash.sha3(idHash);

        getMeta().getAuthenticator().setBodyHash(ByteArray.toHexString(bodyHash));
        getMeta().getAuthenticator().setTime(time);
        getMeta().getAuthenticator().setId(ByteArray.toHexString(idHash));
    }
}
