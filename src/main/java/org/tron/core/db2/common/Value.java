package org.tron.core.db2.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.tron.core.db.common.WrappedByteArray;

@EqualsAndHashCode(exclude = "operator")
public class Value {
  public enum Operator {
    CREATE((byte) 0),
    MODIFY((byte) 1),
    DELETE((byte) 2);

    @Getter
    private byte value;

    Operator(byte value) {
      this.value = value;
    }
  }

  @Getter
  final private Operator operator;
  final private WrappedByteArray data;

  private Value(Operator operator, WrappedByteArray data) {
    this.operator = operator;
    this.data = data;
  }

  public static Value of(Operator operator, byte[] data) {
    return new Value(operator, WrappedByteArray.of(data));
  }

  public byte[] getBytes() {
    return data.getBytes();
  }
}
