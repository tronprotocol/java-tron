package org.tron.core.ibc.sender;

public interface CommunicationService {

  void sendCrossMessage();
  void receiveCrossMessage();
  boolean validProof();
}
