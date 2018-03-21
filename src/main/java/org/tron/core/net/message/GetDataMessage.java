package org.tron.core.net.message;

public class GetDataMessage extends TronMessage {

  public GetDataMessage() {
    super();
  }


  public GetDataMessage(byte[] packed) {
    super(packed);
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  @Override
  public MessageTypes getType() {
    return null;
  }
}
