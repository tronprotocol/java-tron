package org.tron.core.ibc.communicate;

import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol.CrossMessage;

public interface Communicate {

  void sendCrossMessage(CrossMessage crossMessage);

  void receiveCrossMessage(CrossMessage crossMessage);

  boolean validProof(CrossMessage crossMessage);

  boolean checkCommit(Sha256Hash hash);

  boolean broadcastCrossMessage(CrossMessage crossMessage);
}
