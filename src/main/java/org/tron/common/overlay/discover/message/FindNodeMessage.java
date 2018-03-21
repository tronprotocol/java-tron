package org.tron.common.overlay.discover.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.common.overlay.discover.Node;
import org.tron.common.utils.ByteArray;
import org.tron.core.net.message.MessageTypes;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Discover.FindNeighbours;

public class FindNodeMessage extends DiscoverMessage {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] rawData) {
    super(MessageTypes.DISCOVER_FIND_PEER.asByte(), rawData);
    unPack();
  }

  public FindNodeMessage(Node from, byte[] targetId, long timestamp) {
    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();

    this.findNeighbours = FindNeighbours.newBuilder()
        .setFrom(fromEndpoint)
        .setTargetId(ByteString.copyFrom(targetId))
        .setTimestamp(timestamp)
        .build();

    this.type = MessageTypes.DISCOVER_FIND_PEER.asByte();
    this.rawData = this.findNeighbours.toByteArray();
  }

  @Override
  public byte[] getRawData() {
    return this.rawData;
  }

  @Override
  public byte[] getNodeId() {
    return this.findNeighbours.getFrom().getNodeId().toByteArray();
  }

  public FindNodeMessage create(Node from, byte[] targetId) {
    return new FindNodeMessage(from, targetId, System.currentTimeMillis());
  }

  private void unPack() {
    try {
      this.findNeighbours = Discover.FindNeighbours.parseFrom(rawData);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
  }

  public byte[] getTargetId(){
    return this.findNeighbours.getTargetId().toByteArray();
  }

  @Override
  public String toString() {

    return String.format("[FindNodeMessage] \n");

  }

  @Override
  public MessageTypes getType() {
    return MessageTypes.fromByte(this.type);
  }

}
