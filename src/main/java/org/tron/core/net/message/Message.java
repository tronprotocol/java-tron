package org.tron.core.net.message;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.Sha256Hash;

@Slf4j
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

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public abstract byte[] getData();

  public String toString() {
    return "[Message Type: " + getType() + ", Message Hash: " + getMessageId() + "]";
  }

  //public byte getCode() { return type; }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }

  public abstract MessageTypes getType();

}