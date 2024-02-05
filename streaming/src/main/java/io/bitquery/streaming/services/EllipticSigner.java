package io.bitquery.streaming.services;

import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.core.config.args.StreamingConfig;

public class EllipticSigner {
    private StreamingConfig streamingConfig;
    private ECKey eckey;

    public EllipticSigner() {
        this.streamingConfig = CommonParameter.getInstance().getStreamingConfig();

        generateECKey();
    }

    public ECKey.ECDSASignature sign(byte[] message){
       return this.eckey.sign(message);
    }

    public String getAddress() {
        return ByteArray.toHexString(this.eckey.getAddress()).substring(2);
    }

    private void generateECKey() {
        String pk = streamingConfig.getEllipticSignerPrivateKeyHex();

        // If private key is not specified, generate random one.
        if (pk.isEmpty()) {
            this.eckey = new ECKey();
            return;
        }

        this.eckey = new ECKey(ByteArray.fromHexString(pk), true);
    }

}
