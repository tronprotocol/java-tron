package org.tron.common.overlay.message;

import com.google.protobuf.CodedOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.message.MessageTypes;


public abstract class Message {

  protected static final Logger logger = LoggerFactory.getLogger("Net");

  protected boolean unpacked;
  protected byte[] data;
  protected byte type;

  public Message() {
  }

  public Message(byte[] packed) {
    this.data = packed;
    unpacked = false;
  }

  public Message(byte type, byte[] packed) {
    this.type = type;
    this.data = packed;
    unpacked = false;
  }


  public ByteBuf getSendData(){
    try{
      ByteBuf msg = Unpooled.wrappedBuffer(ArrayUtils.add(this.getData(), 0 ,type));
      int headerLen = CodedOutputStream.computeRawVarint32Size(data.length);
      ByteBuf out = Unpooled.buffer(data.length + headerLen);
      CodedOutputStream headerOut =
              CodedOutputStream.newInstance(new ByteBufOutputStream(out), headerLen);
      headerOut.writeRawVarint32(data.length);
      headerOut.flush();
      out.writeBytes(msg, msg.readerIndex(), data.length);
      return  out;
    }catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public abstract byte[] getData();

  public String toString() {
    return "[Message Type: " + getType() + ", Message Hash: " + getMessageId() + "]";
  }

  public abstract Class<?> getAnswerMessage();

  //public byte getCode() { return type; }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }

  public abstract MessageTypes getType();

}