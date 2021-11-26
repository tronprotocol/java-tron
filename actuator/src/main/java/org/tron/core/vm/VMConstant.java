package org.tron.core.vm;

public class VMConstant {

  public static final int CONTRACT_NAME_LENGTH = 32;
  public static final int MIN_TOKEN_ID = 1_000_000;

  // Numbers
  public static final int ONE_HUNDRED = 100;
  public static final int ONE_THOUSAND = 1000;
  public static final long SUN_PER_ENERGY = 100;

  // Messages
  public static final String VALIDATE_FAILURE = "validateForSmartContract failure:%s";
  public static final String INVALID_TOKEN_ID_MSG = "not valid token id";
  public static final String REFUND_ENERGY_FROM_MESSAGE_CALL = "refund energy from message call";
  public static final String CALL_PRE_COMPILED = "call pre-compiled";

  private VMConstant() {
  }
}
