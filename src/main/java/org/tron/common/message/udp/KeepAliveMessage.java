package org.tron.common.message.udp;

import static org.tron.common.message.udp.UdpMessageTypeEnum.BACKUP;

import com.google.protobuf.InvalidProtocolBufferException;
import org.tron.protos.Discover;

public class KeepAliveMessage extends Message {

  private Discover.BackupMessage backupMessage;

  public KeepAliveMessage(byte[] data) throws InvalidProtocolBufferException {
    super(BACKUP, data);
    backupMessage = Discover.BackupMessage.parseFrom(data);
  }

  public KeepAliveMessage(boolean flag, int priority) {
    super(BACKUP, null);
    backupMessage = Discover.BackupMessage.newBuilder().setFlag(flag).setPriority(priority).build();
    data = backupMessage.toByteArray();
  }

  @Override
  public byte[] getNodeId() {
    return null;
  }
}
