package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.core.TronTransaction;

public class TransationMessage extends Message{

    private TronTransaction.Transaction trx;

    public TransationMessage(byte[] packed) {
        super(packed);
    }

    public TransationMessage(TronTransaction.Transaction trx) {
        this.trx = trx;
        unpacked = true;
    }

    public TransationMessage(String msg) {

    }

    @Override
    public MessageTypes getType() {
        return MessageTypes.TRX;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public byte[] getData() {
        if(data == null) pack();
        return data;
    }

    private synchronized void unPack() {
        if(unpacked) return;

        try {
            this.trx = TronTransaction.Transaction.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }

        unpacked = true;
    }

    private void pack() {
        this.data = this.trx.toByteArray();
    }

}
