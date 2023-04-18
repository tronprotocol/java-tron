package org.tron.common.backup;

import static org.tron.common.backup.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;
import static org.tron.common.backup.message.UdpMessageTypeEnum.UNKNOWN;

import org.junit.Test;
import org.testng.Assert;
import org.tron.common.backup.message.UdpMessageTypeEnum;

public class UdpMessageTypeEnumTest {

  @Test
  public void test() {
    UdpMessageTypeEnum type = UdpMessageTypeEnum.fromByte((byte) 5);
    Assert.assertEquals(type.getType(), (byte) 0x05);
    Assert.assertEquals(type, BACKUP_KEEP_ALIVE);


    type = UdpMessageTypeEnum.fromByte((byte) 1);
    Assert.assertEquals(type.getType(), (byte) 0xFF);
    Assert.assertEquals(type, UNKNOWN);
  }
}
