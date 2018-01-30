package org.tron.core.net.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Message {

  protected static final Logger logger = LoggerFactory.getLogger("net");

  protected boolean unpacked;
  protected byte[] data;
  protected byte type;

  public Message() {
  }

  public Message(byte[] packed) {
    this.data = packed;
    unpacked = false;
  }

  public abstract byte[] getData();

  public abstract String toString();

  //public byte getCode() { return type; }

  public abstract MessageTypes getType();

}
