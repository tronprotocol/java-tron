package org.tron.core.admission;

/** Admission rejection of untrusted input; carries no stack trace. */
public class TransactionAdmissionException extends RuntimeException {

  public static final String REJECTION_MESSAGE = "transaction rejected by admission check";

  public enum Reason {
    MALFORMED,
    OVER_BUDGET,
    TOO_DEEP,
    TOO_LARGE
  }

  private final Reason reason;

  TransactionAdmissionException(Reason reason, String detail) {
    super(detail == null ? reason.name() : reason + ": " + detail, null, false, false);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
