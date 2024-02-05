package io.bitquery.streaming.messages;

import com.google.common.primitives.Bytes;
import java.time.Instant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import io.bitquery.streaming.blockchain.BlockMessageDescriptor;
import io.bitquery.streaming.services.EllipticSigner;
import io.bitquery.streaming.services.FileStorage;

@Slf4j(topic = "streaming")
public class ProtobufMessage {
    @Getter
    public MessageMetaInfo meta = new MessageMetaInfo();

    @Getter
    private byte[] body;

    @Getter
    private String topic;

    private StreamingConfig streamingConfig;

    private EllipticSigner signer;
    private FileStorage fileStorage;

    public ProtobufMessage() {
        this.streamingConfig = CommonParameter.getInstance().getStreamingConfig();
        this.signer = new EllipticSigner();
        this.fileStorage = new FileStorage(this);
    }

    public void process(BlockMessageDescriptor descriptor, byte[] body, String topic) {
        getMeta().setDescriptor(descriptor);
        getMeta().setAuthenticator(new MessageAuthenticator());
        getMeta().setSize(body.length);
        getMeta().setServers(this.streamingConfig.getFileStorageUrls());

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
        ECKey.ECDSASignature signature = this.signer.sign(message);

        getMeta().getAuthenticator().setSigner(this.signer.getAddress());
        getMeta().getAuthenticator().setSignature(ByteArray.toHexString(signature.toByteArray()));
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
