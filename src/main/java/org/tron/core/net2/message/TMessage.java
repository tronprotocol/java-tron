package org.tron.core.net2.message;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TMessage {

    private byte version = 1;
    private byte msgType;
    private byte[] data;
    private SocketChannel channel;

    public TMessage (byte msgType, SocketChannel channel){
        this.msgType = msgType;
        this.channel = channel;
    }

    public TMessage (byte version, byte msgType, byte[] data, SocketChannel channel){
        this.version = version;
        this.msgType = msgType;
        this.data = data;
        this.channel = channel;
    }

    public static ByteBuffer encode(TMessage msg){
        int length = msg.getData() == null? 0 : msg.getData().length;
        ByteBuffer buffer = ByteBuffer.allocate(length + 2);
        buffer.put(msg.getVersion()).put(msg.getMsgType());
        if (length != 0){
            buffer.put(msg.getData());
        }
        return ByteBuffer.wrap(buffer.array());
    }

    public static TMessage decode(StringBuilder msgBuild, SocketChannel channel){
        String str =  msgBuild.toString();
        byte version = str.getBytes()[0];
        byte mstType = str.getBytes()[1];
        byte[] data = str.substring(2,str.length()).getBytes();
        return new TMessage(version, mstType, data, channel);
    }

    @Override
    public String toString(){
        int length = data == null? 0 : data.length;
        return new StringBuilder().append("v=").append(version).append(" t=")
                .append(msgType).append(" dataLength=").append(length).toString();
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getMsgType() {
        return msgType;
    }

    public void setMsgType(byte msgType) {
        this.msgType = msgType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }
}
