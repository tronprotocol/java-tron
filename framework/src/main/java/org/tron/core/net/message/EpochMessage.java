package org.tron.core.net.message;

import org.tron.protos.Protocol;

public class EpochMessage extends TronMessage {

  private Protocol.EpochMessage epochMessage;

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
