package org.tron.common.error;

public class TronDBException extends RuntimeException {
    public TronDBException() {
  }

    public TronDBException(String s) {
    super(s);
  }

    public TronDBException(String s, Throwable throwable) {
    super(s, throwable);
  }

    public TronDBException(Throwable throwable) {
    super(throwable);
  }
}
