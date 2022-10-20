package org.tron.core.vm.repository;

import java.util.Arrays;
import java.util.Objects;

import lombok.Getter;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.vm.config.VMConfig;

public class Value<T> {

  @Getter
  private Type type;

  @Getter
  private T value;

  private Value(T value, int type) {
    if (value != null) {
      this.value = value;
      this.type = new Type(type);
    } else {
      if (VMConfig.allowMultiSign()) {
        this.type = new Type(Type.UNKNOWN);
      }
    }
  }

  public static <T> Value<T> create(ProtoCapsule<T> capsule, int type) {
    return new Value<>(capsule.getInstance(), type);
  }

  public static <T> Value<T> create(ProtoCapsule<T> capsule) {
    return create(capsule, Type.NORMAL);
  }

  public static Value<byte[]> create(byte[] value, int type) {
    return (value == null || value.length ==0) ? new Value<>(null, type) :
        new Value<>(Arrays.copyOf(value, value.length), type);
  }

  public static Value<byte[]> create(byte[] value) {
    return create(value, Type.NORMAL);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }

    Value<T> other = (Value) obj;
    return Objects.equals(this.getValue(), other.getValue());
  }

  @Override
  public int hashCode() {
    return new Integer(type.hashCode() + Objects.hashCode(value)).hashCode();
  }
}
