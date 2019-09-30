package org.tron.core.actuator;

import com.google.protobuf.GeneratedMessageV3;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class TransactionFactory {

  private static Map<ContractType, Class<? extends Actuator>> actuatorMap = new ConcurrentHashMap<>();
  private static Map<ContractType, Class<? extends GeneratedMessageV3>> contractMap = new ConcurrentHashMap<>();

  public static void register(ContractType type, Class<? extends Actuator> actuatorClass,
      Class<? extends GeneratedMessageV3> clazz) {
    actuatorMap.put(type, actuatorClass);
    contractMap.put(type, clazz);
  }

  public static Class<? extends Actuator> getActuator(ContractType type) {
    return actuatorMap.get(type);
  }

  public static Class<? extends GeneratedMessageV3> getContract(ContractType type) {
    return contractMap.get(type);
  }

  public static Map<ContractType, Class<? extends Actuator>> getActuatorMap() {
    return actuatorMap;
  }

  public static Map<ContractType, Class<? extends GeneratedMessageV3>> getContractMap() {
    return contractMap;
  }
}
