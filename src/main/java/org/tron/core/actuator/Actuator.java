package org.tron.core.actuator;

import com.google.protobuf.ByteString;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;

public interface Actuator {

  boolean execute() throws ContractExeException;

  boolean validate() throws ContractValidateException;

  ByteString getOwnerAddress();
}
