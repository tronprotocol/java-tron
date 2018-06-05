package org.tron.common.message.udp.backup;

import static org.tron.common.message.udp.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;

import org.tron.common.message.udp.Message;
import org.tron.protos.Discover;

public class KeepAliveMessage extends Message {

  private Discover.BackupMessage backupMessage;

  public KeepAliveMessage(byte[] data) throws Exception {
    super(BACKUP_KEEP_ALIVE, data);
    backupMessage = Discover.BackupMessage.parseFrom(data);
  }

  public KeepAliveMessage(boolean flag, int priority) {
    super(BACKUP_KEEP_ALIVE, null);
    backupMessage = Discover.BackupMessage.newBuilder().setFlag(flag).setPriority(priority).build();
    data = backupMessage.toByteArray();
  }

  @Override
  public byte[] getNodeId() {
    return null;
  }
}
