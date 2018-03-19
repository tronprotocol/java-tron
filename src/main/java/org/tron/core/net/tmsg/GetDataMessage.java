package org.tron.core.net.tmsg;

public class GetDataMessage extends Message {

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
  public MessageTypes getType() {
    return null;
  }
}
