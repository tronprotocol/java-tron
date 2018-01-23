package org.tron.core.net.message;

public class MessageFactory {

    public Message create(byte code, byte[] packed) {
        MessageTypes receivedTypes = MessageTypes.fromByte(code);
        switch (receivedTypes) {
            case TRX:
                return new TransationMessage(packed);
            case TRXS:
                return new TransationsMessage(packed);
            case BLOCK:
                return new BlockMessage(packed);
            case BLOCKS:
                return new BlocksMessage(packed);
            case BLOCKHEADERS:
                return new BlockHeadersMessage(packed);
            case GETITEMS:
                return new GetInvertoryItemsMessage(packed);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}
