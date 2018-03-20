package org.tron.common.overlay.discover.message;

import java.security.SignatureException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.FastByteComparisons;
import org.tron.common.utils.Sha256Hash;

public abstract class Message {

  protected static final Logger logger = LoggerFactory.getLogger("Net");

  protected byte[] data;
  protected byte type;
  protected boolean unpacked;

  public Message() {

  }

  public Message(byte[] data) {
    this.data = data;
  }

  public Sha256Hash getMessageId() {
    return Sha256Hash.of(getData());
  }

  public byte[] getData() {
    return this.data;
  }

  public byte[] getSendData() {
    return ArrayUtils.add(this.data, 0, this.type);
  }

  public byte getType() {
    return type;
  }

  @Override
  public String toString() {
    return "[Message Type: " + getType() + ", Message Hash: " + getMessageId() + "]";
  }

  @Override
  public int hashCode() {
    return getMessageId().hashCode();
  }
}