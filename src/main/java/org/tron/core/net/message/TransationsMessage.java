package org.tron.core.net.message;

import com.google.protobuf.InvalidProtocolBufferException;

public class TransationsMessage extends Message{

    private org.tron.protos.core.Tron.Transactions trxs;

    public TransationsMessage() {
        super();
    }

    public TransationsMessage(org.tron.protos.core.Tron.Transactions trx) {
        this.trxs = trx;
        unpacked = true;
    }

    public TransationsMessage(byte[] packed) {
        super(packed);
    }

    @Override
    public byte[] getData() {
        if(data == null) pack();
        return data;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public MessageTypes getType() {
        return MessageTypes.TRXS;
    }

    private void pack() {
        this.data = this.trxs.toByteArray();
    }

    private synchronized void unPack() {
        if(unpacked) return;

        try {
            this.trxs = org.tron.protos.core.Tron.Transactions.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage());
        }

        unpacked = true;
    }
}
