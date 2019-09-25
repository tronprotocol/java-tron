package org.tron.common.runtime2.tvm.interpretor;

public class Costs {

  public static final int MEMORY = 3;
  public static final int COPY_ENERGY = 3;


  public static final int EXP_ENERGY = 10;
  public static int EXP_BYTE_ENERGY = 10;

  public static final int SHA3 = 30;
  public static final int SHA3_WORD = 6;


  public static final int EXT_CODE_HASH = 400;

  public static final int SLOAD = 50;
  public static final int CLEAR_SSTORE = 5000;
  public static final int SET_SSTORE = 20000;
  public static final int RESET_SSTORE = 5000;
  public static final int REFUND_SSTORE = 15000;
  public static final int LOG_DATA_ENERGY = 8;
  public static final int LOG_ENERGY = 375;
  public static final int LOG_TOPIC_ENERGY = 375;

  public static final int CALL = 40;
  public static final int NEW_ACCT_CALL = 25000;  //new account call
  public static final int VT_CALL = 9000;  //value transfer call
  public static final int STIPEND_CALL = 2300;


}
