package org.tron.core.actuator;

import com.google.protobuf.ByteString;

public interface Actuator {

  boolean execute();

  boolean validator();

  ByteString getOwnerAddress();
}
