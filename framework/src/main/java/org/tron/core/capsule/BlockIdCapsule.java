package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.tron.common.utils.Sha256Sm3Hash;

public class BlockIdCapsule extends Sha256Sm3Hash implements ProtoCapsule {

  private long num;

  public BlockIdCapsule() {
    super(Sha256Sm3Hash.ZERO_HASH.getBytes());
    num = 0;
  }

  /**
   * Use {@link #wrap(byte[])} instead.
   */
  public BlockIdCapsule(Sha256Sm3Hash hash, long num) {
    super(hash.getBytes());
    this.num = num;
  }

  public BlockIdCapsule(byte[] hash, long num) {
    super(hash);
    this.num = num;
  }

  public BlockIdCapsule(ByteString hash, long num) {
    super(hash.toByteArray());
    this.num = num;
  }

  @Override
  public byte[] getData() {
    return new byte[0];
  }

  @Override
  public Object getInstance() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || (getClass() != o.getClass() && !(o instanceof Sha256Sm3Hash))) {
      return false;
    }
    return Arrays.equals(getBytes(), ((Sha256Sm3Hash) o).getBytes());
  }

  public String getString() {
    return "Num: " + num + ", ID:" + super.toString();
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public int compareTo(Sha256Sm3Hash other) {
    if (other.getClass().equals(BlockIdCapsule.class)) {
      long otherNum = ((BlockIdCapsule) other).getNum();
      return Long.compare(num, otherNum);
    }
    return super.compareTo(other);
  }

  public long getNum() {
    return num;
  }
}
