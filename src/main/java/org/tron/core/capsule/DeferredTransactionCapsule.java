package org.tron.core.capsule;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Protocol.DeferredTransaction;
import org.tron.protos.Protocol.DelegatedResource;

@Slf4j(topic = "capsule")
public class DeferredTransactionCapsule implements ProtoCapsule<DeferredTransaction> {
    @Getter
    private DeferredTransaction deferredTransaction;

    @Override
    public byte[] getData() {
        return this.deferredTransaction.toByteArray();
    }

    public byte[] getKey() {
        long DelayTime = deferredTransaction.getDelayUntil();
        byte[] delayTime =  Longs.toByteArray(DelayTime);
        byte[] trxId = deferredTransaction.getTransactionId().toByteArray();
        byte[] key = new byte[8 + trxId.length];
        System.arraycopy(delayTime, 0, key, 0, 8);
        System.arraycopy(trxId, 0, key, 8, trxId.length);
        return key;
    }

    @Override
    public DeferredTransaction getInstance() {
        return null;
    }

    public DeferredTransactionCapsule(DeferredTransaction deferredTransaction){
        this.deferredTransaction = deferredTransaction;
    }

    public DeferredTransactionCapsule(byte[] data){
        try {
            this.deferredTransaction = DeferredTransaction.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    public ByteString getTransactionId(){
        return deferredTransaction.getTransactionId();
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
