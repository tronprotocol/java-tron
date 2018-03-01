package org.tron.core.db.actuator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;

public class ActuatorFactory {

  private static final Logger logger = LoggerFactory.getLogger("ActuatorFactory");
  public static final ActuatorFactory INSTANCE = new ActuatorFactory();

  private ActuatorFactory() {
  }

  public static ActuatorFactory getInstance() {
    return INSTANCE;
  }

  public static Actuator createActuator(TransactionCapsule transactionCapsule,
      Manager manager) {
    if (null == transactionCapsule || null == transactionCapsule.getTransaction()) {
      logger.info("transactionCapsule or Transaction is null");
      return null;
    }
    if (null == manager) {
      logger.info("manager is null");
      return null;
    }
    switch (transactionCapsule.getTransaction().getRawData().getType()) {
      case Transfer:
        break;
      case VoteWitess:
        return new TransactionVoteActuator(transactionCapsule, manager);
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
