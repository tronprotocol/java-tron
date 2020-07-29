package org.tron.core.capsule;

public class BytesCapsule implements ProtoCapsule {

  byte[] bytes;

  public BytesCapsule(byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public byte[] getData() {
    return bytes;
  }

  @Override
  public Object getInstance() {
    return null;
  }
}
