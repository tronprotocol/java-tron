package org.tron.core.db2;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.tron.core.capsule.ProtoCapsule;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProtoCapsuleTest implements ProtoCapsule<Object> {
  private byte[] value;

  @Override
  public byte[] getData() {
    return value;
  }

  @Override
  public Object getInstance() {
    return value;
  }

  @Override
  public String toString() {
    return "ProtoCapsuleTest{"
      + "value=" + Arrays.toString(value)
      + ", string=" + (value == null ? "" : new String(value))
      + '}';
  }
}