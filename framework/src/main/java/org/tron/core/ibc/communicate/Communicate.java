package org.tron.core.ibc.communicate;

import org.tron.common.utils.Sha256Hash;
import org.tron.core.net.peer.PeerConnection;
import org.tron.protos.Protocol.CrossMessage;

public interface Communicate {

  void sendCrossMessage(CrossMessage crossMessage, boolean save);

  void receiveCrossMessage(PeerConnection peer, CrossMessage crossMessage);

  boolean validProof(CrossMessage crossMessage);

  boolean checkCommit(Sha256Hash hash);

  boolean broadcastCrossMessage(CrossMessage crossMessage);
}
