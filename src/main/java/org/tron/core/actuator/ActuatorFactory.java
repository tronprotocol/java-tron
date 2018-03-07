package org.tron.core.actuator;

import com.google.common.collect.Lists;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocal;
import org.tron.protos.Protocal.Transaction.Contract;
import org.tron.protos.Protocal.Transaction.TranscationType;

public class ActuatorFactory {

  private static final Logger logger = LoggerFactory.getLogger("ActuatorFactory");
  public static final ActuatorFactory INSTANCE = new ActuatorFactory();

  private ActuatorFactory() {
  }

  public static ActuatorFactory getInstance() {
    return INSTANCE;
  }

  public static List<Actuator> createActuator(TransactionCapsule transactionCapsule,
      Manager manager) {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getTransaction()) {
      logger.info("transactionCapsule or Transaction is null");
      return actuatorList;
    }
    if (null == manager) {
      logger.info("manager is null");
      return actuatorList;
    }

    Protocal.Transaction.raw rawData = transactionCapsule.getTransaction().getRawData();
    if (TranscationType.ContractType.equals(rawData.getType())) {
      rawData.getContractList().forEach(contract -> actuatorList.add(getActuatorByContract(contract, manager)));
    }
    return actuatorList;
  }

  private static Actuator getActuatorByContract(Contract contract, Manager manager) {
    switch (contract.getType()) {
      case AccountCreateContract:
        new CreateAccountActuator(contract.getParameter(), manager);
        break;
      case TransferContract:
        break;
      case TransferAssertContract:
        break;
      case VoteAssetContract:
        break;
      case VoteWitnessContract:
        new VoteWitnessActuator(contract.getParameter(), manager);
        break;
      case WitnessCreateContract:
        new WitnessCreateActuator(contract.getParameter(), manager);
        break;
      case AssetIssueContract:
        break;
      case DeployContract:
        break;
      default:

    }
    return null;
  }

}
