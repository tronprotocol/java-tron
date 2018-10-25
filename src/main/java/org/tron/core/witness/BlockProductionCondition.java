package org.tron.core.witness;

public enum  BlockProductionCondition {
  PRODUCED,           // Successfully generated block
  UNELECTED,
  NOT_MY_TURN,        // It isn't my turn
  NOT_SYNCED,
  NOT_TIME_YET,       // Not yet arrived
  NO_PRIVATE_KEY,
  LOW_PARTICIPATION,
  LAG,
  CONSECUTIVE,
  TIME_OUT,
  BACKUP_STATUS_IS_NOT_MASTER,
  DUP_WITNESS,
  EXCEPTION_PRODUCING_BLOCK
}
