package org.tron.core.db.actuator;

import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;

public class ActuatorFactory {

  public static final ActuatorFactory INSTANCE = new ActuatorFactory();

  private ActuatorFactory() {
  }

  public static ActuatorFactory getInstance() {
    return INSTANCE;
  }

  public static Actuator createActuator(TransactionCapsule transactionCapsule,
      Manager manager) {
    if (null == transactionCapsule || null == transactionCapsule.getTransaction()) {
      return null;
    }
    switch (transactionCapsule.getTransaction().getType()) {
      case Transfer:
        break;
      case VoteWitess:
        return new TransactionVoteActuator(transactionCapsule, manager.getWitnessStore());
      case CreateAccount:
        break;
      case DeployContract:
        break;
      default:
        break;
    }
    return null;
  }
}
