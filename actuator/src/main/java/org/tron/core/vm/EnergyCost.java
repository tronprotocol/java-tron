package org.tron.core.vm;


public class EnergyCost {

  private static EnergyCost instance = null;
  private final int BALANCE = 20;
  private final int SHA3 = 30;
  private final int SHA3_WORD = 6;
  private final int SLOAD = 50;
  private final int STOP = 0;
  private final int SUICIDE = 0;
  private final int CLEAR_SSTORE = 5000;
  private final int SET_SSTORE = 20000;
  private final int RESET_SSTORE = 5000;
  private final int REFUND_SSTORE = 15000;
  private final int CREATE = 32000;
  private final int CALL = 40;
  private final int STIPEND_CALL = 2300;
  private final int VT_CALL = 9000;  // value transfer call
  private final int NEW_ACCT_CALL = 25000;  // new account call
  private final int MEMORY = 3;
  private final int CREATE_DATA = 200;
  private final int LOG_ENERGY = 375;
  private final int LOG_DATA_ENERGY = 8;
  private final int LOG_TOPIC_ENERGY = 375;
  private final int COPY_ENERGY = 3;
  private final int EXP_ENERGY = 10;
  private final int EXP_BYTE_ENERGY = 10;
  private final int EXT_CODE_SIZE = 20;
  private final int EXT_CODE_COPY = 20;
  private final int EXT_CODE_HASH = 400;
  // TODO Why zero?
  private final int NEW_ACCT_SUICIDE = 0;
  private final int FREEZE = 20000;
  private final int UNFREEZE = 20000;
  private final int FREEZE_EXPIRE_TIME = 50;
  private final int VOTE_WITNESS = 30000;
  private final int WITHDRAW_REWARD = 20000;

  public static EnergyCost getInstance() {
    if (instance == null) {
      instance = new EnergyCost();
    }

    return instance;
  }

  public int getBalance() {
    return BALANCE;
  }

  public int getSha3() {
    return SHA3;
  }

  public int getSha3Word() {
    return SHA3_WORD;
  }

  public int getSLoad() {
    return SLOAD;
  }

  public int getStop() {
    return STOP;
  }

  public int getSuicide() {
    return SUICIDE;
  }

  public int getClearSStore() {
    return CLEAR_SSTORE;
  }

  public int getSetSStore() {
    return SET_SSTORE;
  }

  public int getResetSStore() {
    return RESET_SSTORE;
  }

  public int getRefundSStore() {
    return REFUND_SSTORE;
  }

  public int getCreate() {
    return CREATE;
  }

  public int getCall() {
    return CALL;
  }

  public int getStipendCall() {
    return STIPEND_CALL;
  }

  public int getVtCall() {
    return VT_CALL;
  }

  public int getNewAcctCall() {
    return NEW_ACCT_CALL;
  }

  public int getNewAcctSuicide() {
    return NEW_ACCT_SUICIDE;
  }

  public int getMemory() {
    return MEMORY;
  }

  public int getCREATE_DATA() {
    return CREATE_DATA;
  }

  public int getLogEnergy() {
    return LOG_ENERGY;
  }

  public int getLogDataEnergy() {
    return LOG_DATA_ENERGY;
  }

  public int getLogTopicEnergy() {
    return LOG_TOPIC_ENERGY;
  }

  public int getCopyEnergy() {
    return COPY_ENERGY;
  }

  public int getExpEnergy() {
    return EXP_ENERGY;
  }

  public int getExpByteEnergy() {
    return EXP_BYTE_ENERGY;
  }

  public int getExtCodeSize() {
    return EXT_CODE_SIZE;
  }

  public int getExtCodeCopy() {
    return EXT_CODE_COPY;
  }

  public int getExtCodeHash() {
    return EXT_CODE_HASH;
  }

  public int getFreeze() {
    return FREEZE;
  }

  public int getUnfreeze() {
    return UNFREEZE;
  }

  public int getFreezeExpireTime() {
    return FREEZE_EXPIRE_TIME;
  }

  public int getVoteWitness() {
    return VOTE_WITNESS;
  }

  public int getWithdrawReward() {
    return WITHDRAW_REWARD;
  }
}
