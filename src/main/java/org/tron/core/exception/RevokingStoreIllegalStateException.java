package org.tron.core.exception;

public class RevokingStoreIllegalStateException extends TronRuntimeException {

  /**
   * Constructs an RevokingStoreIllegalStateException with no detail message. A detail message is a
   * String that describes this particular exception.
   */
  public RevokingStoreIllegalStateException() {
    super();
  }

  /**
   * Constructs an RevokingStoreIllegalStateException with the specified detail message.  A detail
   * message is a String that describes this particular exception.
   *
   * @param s the String that contains a detailed message
   */
  public RevokingStoreIllegalStateException(String s) {
    super(s);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * <p>Note that the detail message associated with <code>cause</code> is <i>not</i> automatically
   * incorporated in this exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link
   * Throwable#getMessage()} method).
   * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()}
   * method).  (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
   * unknown.)
   */
  public RevokingStoreIllegalStateException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified cause and a detail message of <tt>(cause==null ?
   * null : cause.toString())</tt> (which typically contains the class and detail message of
   * <tt>cause</tt>). This constructor is useful for exceptions that are little more than wrappers
   * for other throwables (for example, {@link java.security.PrivilegedActionException}).
   *
   * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()}
   * method).  (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
   * unknown.)
   */
  public RevokingStoreIllegalStateException(Throwable cause) {
    super("", cause);
  }

  static final long serialVersionUID = -1848914673093119416L;
}
