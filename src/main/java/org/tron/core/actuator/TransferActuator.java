package org.tron.core.actuator;

import com.google.protobuf.Any;
import org.tron.core.db.Manager;

public class TransferActuator extends AbstractActuator {


  TransferActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
  }

  @Override
  public boolean execute() {
    //TODO
    return true;
  }

  @Override
  public boolean Validator() {
    //TODO
    return false;
  }
}
