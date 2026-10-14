package org.tron.p2p.exception;

public class P2pException extends Exception {

  private static final long serialVersionUID = 1390312274369330710L;
  private final TypeEnum type;

  public P2pException(TypeEnum type, String errMsg) {
    super(errMsg);
    this.type = type;
  }

  public P2pException(TypeEnum type, Throwable throwable) {
    super(throwable);
    this.type = type;
  }

  public P2pException(TypeEnum type, String errMsg, Throwable throwable) {
    super(errMsg, throwable);
    this.type = type;
  }

  public TypeEnum getType() {
    return type;
  }

  public enum TypeEnum {
    NO_SUCH_MESSAGE(1, "no such message"),
    PARSE_MESSAGE_FAILED(2, "parse message failed"),
    MESSAGE_WITH_WRONG_LENGTH(3, "message with wrong length"),
    BAD_MESSAGE(4, "bad message"),
    BAD_PROTOCOL(5, "bad protocol"),
    TYPE_ALREADY_REGISTERED(6, "type already registered"),
    EMPTY_MESSAGE(7, "empty message"),
    BIG_MESSAGE(8, "big message");

    private final Integer value;
    private final String desc;

    TypeEnum(Integer value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    public Integer getValue() {
      return value;
    }

    public String getDesc() {
      return desc;
    }

    @Override
    public String toString() {
      return value + ", " + desc;
    }
  }

}
