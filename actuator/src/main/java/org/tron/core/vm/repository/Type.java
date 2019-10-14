package org.tron.core.vm.repository;

public class Type {

  /**
   * Default Mode : VALUE_TYPE_NORMAL
   */
  public static final int VALUE_TYPE_NORMAL = 0;
  public static final int VALUE_TYPE_DIRTY = 1 << 0;
  public static final int VALUE_TYPE_CREATE = 1 << 1;
  public static final int VALUE_TYPE_UNKNOWN = 0xFFFFFFFC;

  protected int type = VALUE_TYPE_NORMAL;

  /**
   * @param type
   */
  public Type(int type) {
    this.type |= type;
  }

  /**
   * default constructor
   */
  public Type() {
  }

  /**
   * @param T
   */
  private Type(Type T) {
    this.type = T.getType();
  }

  /**
   * @return
   */
  public Type clone() {
    return new Type(this);
  }

  /**
   * @return
   */
  public boolean isDirty() {
    return (this.type & VALUE_TYPE_DIRTY) == VALUE_TYPE_DIRTY;
  }

  /**
   * @return
   */
  public boolean isNormal() {
    return this.type == VALUE_TYPE_NORMAL;
  }

  /**
   * @return
   */
  public boolean isCreate() {
    return (this.type & VALUE_TYPE_CREATE) == VALUE_TYPE_CREATE;
  }

  /**
   * @return
   */
  public boolean shouldCommit() {
    return this.type != VALUE_TYPE_NORMAL;
  }

  /**
   * @return
   */
  public int getType() {
    return type;
  }

  /**
   * @param type
   */
  public void setType(int type) {
    if (isValidType(type)) {
      this.type = type;
    }
  }

  /**
   * @param type
   * @return
   */
  public boolean isValidType(int type) {
    return (type & VALUE_TYPE_UNKNOWN) == VALUE_TYPE_NORMAL;
  }

  /**
   * @param type
   */
  public void addType(int type) {
    if (isValidType(type)) {
      this.type |= type;
    }
  }

  /**
   * @param T
   */
  public void addType(Type T) {
    addType(T.getType());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }

    Type T = (Type) obj;
    return this.type == T.getType();
  }

  @Override
  public int hashCode() {
    return new Integer(type).hashCode();
  }

  @Override
  public String toString() {
    return "Type{" +
        "type=" + type +
        '}';
  }
}
