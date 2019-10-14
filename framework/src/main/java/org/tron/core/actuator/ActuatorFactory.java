package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract;

@Slf4j(topic = "actuator")
public class ActuatorFactory {

  public static final ActuatorFactory INSTANCE = new ActuatorFactory();

  private ActuatorFactory() {
  }

  public static ActuatorFactory getInstance() {
    return INSTANCE;
  }

  /**
   * create actuator.
   */
  public static List<Actuator> createActuator(TransactionCapsule transactionCapsule,
      Manager manager) {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getInstance()) {
      logger.info("transactionCapsule or Transaction is null");
      return actuatorList;
    }

    Preconditions.checkNotNull(manager, "manager is null");
    Protocol.Transaction.raw rawData = transactionCapsule.getInstance().getRawData();
    rawData.getContractList()
        .forEach(contract -> {
          try {
            actuatorList
                .add(getActuatorByContract(contract, manager, transactionCapsule));
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InstantiationException e) {
            e.printStackTrace();
          }
        });
    return actuatorList;
  }

  private static Actuator getActuatorByContract(Contract contract, Manager manager,
      TransactionCapsule tx) throws IllegalAccessException, InstantiationException {
    Class<? extends Actuator> clazz = TransactionFactory.getActuator(contract.getType());
    AbstractActuator abstractActuator = (AbstractActuator) clazz.newInstance();
    abstractActuator.setChainBaseManager(manager.getChainBaseManager()).setContract(contract)
        .setForkUtils(manager.getForkController()).setTx(tx);
    return abstractActuator;
  }

}
