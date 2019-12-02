package org.tron.core.ibc.spv.message;

import com.google.protobuf.ByteString;
import org.tron.core.net.message.MessageTypes;
import org.tron.core.net.message.TronMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Items;

public class NotDataDownloadMessage extends TronMessage {

  private Protocol.Items notFound;

  /**
   * means can not find this block or trx.
   */
  public NotDataDownloadMessage(ByteString uuid) {
    Protocol.Items.Builder itemsBuilder = Protocol.Items.newBuilder();
    itemsBuilder.setType(Protocol.Items.ItemType.ERR);
    notFound = itemsBuilder.build();
    this.type = MessageTypes.NOT_DATA_DOWNLOAD.asByte();
    this.data = notFound.toByteArray();
  }

  public Items getItems() {
    return notFound;
  }

  @Override
  public String toString() {
    return "data not found";
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
