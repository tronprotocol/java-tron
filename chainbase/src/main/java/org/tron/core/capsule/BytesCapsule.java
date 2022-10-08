package org.tron.core.capsule;

public class BytesCapsule implements ProtoCapsule<byte[]> {

  private final byte[] bytes;

  public BytesCapsule(byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public byte[] getData() {
    return bytes;
  }

  @Override
  public byte[] getInstance() {
    return bytes;
  }

  @Override
  public BytesCapsule newInstance() {
    return new BytesCapsule(this.bytes);
  }
}
