package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.DeferredTransaction;

public class DeferredTransactionCapsule implements ProtoCapsule<DeferredTransaction> {
    private DeferredTransaction deferredTransaction;

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public DeferredTransaction getInstance() {
        return null;
    }

    public DeferredTransactionCapsule(DeferredTransaction deferredTransaction){
        this.deferredTransaction = deferredTransaction;
    }

    public DeferredTransactionCapsule(byte[] data) throws BadItemException {
        try {
            this.deferredTransaction = DeferredTransaction.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new BadItemException("Transaction proto data parse exception");
        }
    }

    public long getSenderId(){
        return deferredTransaction.getSenderId();
    }

    public long getPublishTime(){
        return deferredTransaction.getPublishTime();
    }

    public long getDelayUntil(){
        return deferredTransaction.getDelayUntil();
    }

    public long getExpiration(){
        return deferredTransaction.getExpiration();
    }


    public ByteString getSenderAddress(){
        return deferredTransaction.getSenderAddress();
    }

    public ByteString getReceiverAddress(){
        return deferredTransaction.getReceiverAddress();
    }

}
