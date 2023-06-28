package org.tron.core.exception;

public class BadBlockException extends TronException {

  private TypeEnum type = TypeEnum.DEFAULT;

  public BadBlockException() {
    super();
  }

  public BadBlockException(String message) {
    super(message);
  }

  public BadBlockException(TypeEnum type, String message) {
    super(message);
    this.type = type;
  }

  public TypeEnum getType() {
    return type;
  }

  public enum TypeEnum {
    CALC_MERKLE_ROOT_FAILED(1),
    DEFAULT(100);

    private Integer value;

    TypeEnum(Integer value) {
      this.value = value;
    }

    public Integer getValue() {
      return value;
    }
  }
}
