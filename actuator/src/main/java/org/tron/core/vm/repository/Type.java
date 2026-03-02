package org.tron.core.vm.repository;

public class Type implements Cloneable {

  // Default Mode : NORMAL
  public static final int NORMAL = 0;
  public static final int DIRTY = 1;
  public static final int CREATE = 1 << 1;
  public static final int UNKNOWN = 0xFFFFFFFC;

  protected int type = NORMAL;

  public Type() {}

  public Type(int type) {
    this.type |= type;
  }

  private Type(Type t) {
    this.type = t.type;
  }

  public Type clone() {
    return new Type(this);
  }

  public boolean isDirty() {
    return (this.type & DIRTY) == DIRTY;
  }

  public boolean isNormal() {
    return this.type == NORMAL;
  }

  public boolean isCreate() {
    return (this.type & CREATE) == CREATE;
  }

  public boolean shouldCommit() {
    return this.type != NORMAL;
  }

  public Type setType(int type) {
    if (isValidType(type)) {
      this.type = type;
    }
    return this;
  }

  public boolean isValidType(int type) {
    return (type & UNKNOWN) == NORMAL;
  }

  public int addType(int type) {
    if (isValidType(type)) {
      this.type |= type;
    }
    return this.type;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }

    Type other = (Type) obj;
    return this.type == other.type;
  }

  @Override
  public int hashCode() {
    return new Integer(type).hashCode();
  }

  @Override
  public String toString() {
    return "Type{" + "type=" + type + '}';
  }
}
