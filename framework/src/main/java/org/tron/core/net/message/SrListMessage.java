package org.tron.core.net.message;

import org.tron.protos.Protocol;

public class SrListMessage extends TronMessage {

  private Protocol.SrList srList;
  private Protocol.DataSign dataSign;

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
