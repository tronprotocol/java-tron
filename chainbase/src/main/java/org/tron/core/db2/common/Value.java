package org.tron.core.db2.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.ProtoCapsule;

@EqualsAndHashCode(exclude = "operator")
public final class Value<T extends ProtoCapsule> {

  @Getter
  private final  Operator operator;
  private final  T data;

  public Value(Operator operator, T data) {
    this.operator = operator;
    this.data = data;
  }


  public byte[] encode() {
    byte[] d;
    if (data == null || ArrayUtils.isEmpty(d = data.getData())) {
      return new byte[]{operator.getValue()};
    }

    byte[] r = new byte[1 + d.length];
    r[0] = operator.getValue();
    System.arraycopy(d, 0, r, 1, d.length);
    return r;
  }

  public T getData() {
    if (this.data == null) {
      return null;
    }
    return (T) this.data.newInstance();
  }

  public byte[] getBytes() {
    if (data == null) {
      return null;
    }
    return this.data.getData();
  }

  public enum Operator {
    CREATE((byte) 0),
    MODIFY((byte) 1),
    DELETE((byte) 2),
    PUT((byte) 3);

    @Getter
    private byte value;

    Operator(byte value) {
      this.value = value;
    }

    static Operator valueOf(byte b) {
      switch (b) {
        case 0:
          return Operator.CREATE;
        case 1:
          return Operator.MODIFY;
        case 2:
          return Operator.DELETE;
        case 3:
          return Operator.PUT;
        default:
          return null;
      }
    }
  }
}
