package org.tron.common.backup;

import static org.tron.common.backup.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;

import org.junit.Test;
import org.testng.Assert;
import org.tron.common.backup.message.KeepAliveMessage;
import org.tron.protos.Discover;

public class KeepAliveMessageTest {

  @Test
  public void test() throws Exception {
    KeepAliveMessage m1 = new KeepAliveMessage(true, 10);
    Assert.assertTrue(m1.getFlag());
    Assert.assertEquals(m1.getPriority(), 10);
    Assert.assertEquals(m1.getType(), BACKUP_KEEP_ALIVE);
    Assert.assertEquals(m1.getFrom(), null);
    Assert.assertEquals(m1.getTimestamp(), 0);
    Assert.assertEquals(m1.getData().length + 1, m1.getSendData().length);


    Discover.BackupMessage backupMessage = Discover.BackupMessage.newBuilder()
            .setFlag(true).setPriority(10).build();
    KeepAliveMessage m2 = new KeepAliveMessage(backupMessage.toByteArray());
    Assert.assertTrue(m2.getFlag());
    Assert.assertEquals(m2.getPriority(), 10);
    Assert.assertEquals(m2.getType(), BACKUP_KEEP_ALIVE);

    Assert.assertEquals(m2.getMessageId().getBytes(), m1.getMessageId().getBytes());
  }
}
