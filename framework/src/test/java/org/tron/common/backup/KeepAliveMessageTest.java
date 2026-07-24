package org.tron.common.backup;

import static org.tron.common.backup.message.UdpMessageTypeEnum.BACKUP_KEEP_ALIVE;

import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.backup.message.KeepAliveMessage;
import org.tron.protos.Discover;

public class KeepAliveMessageTest {

  // A field number not used by BackupMessage, so it always parses as an unknown field.
  private static final int UNKNOWN_FIELD_NUMBER = 1000;

  @Test
  public void discardsUnknownFields() throws Exception {
    Discover.BackupMessage backupMessage = Discover.BackupMessage.newBuilder()
        .setFlag(true).setPriority(10).build();
    UnknownFieldSet unknown = UnknownFieldSet.newBuilder()
        .addField(UNKNOWN_FIELD_NUMBER, UnknownFieldSet.Field.newBuilder()
            .addLengthDelimited(ByteString.copyFrom(new byte[8192]))
            .build())
        .build();
    byte[] padded = backupMessage.toBuilder().setUnknownFields(unknown).build().toByteArray();
    Assert.assertTrue(padded.length > backupMessage.toByteArray().length + 8000);

    // Parsing uses DiscardUnknownFieldsParser, so the padded message still parses and its known
    // fields are intact while the unknown padding is discarded from the parsed proto.
    KeepAliveMessage m = new KeepAliveMessage(padded);
    Assert.assertTrue(m.getFlag());
    Assert.assertEquals(10, m.getPriority());
  }

  @Test
  public void test() throws Exception {
    KeepAliveMessage m1 = new KeepAliveMessage(true, 10);
    Assert.assertTrue(m1.getFlag());
    Assert.assertEquals(m1.getPriority(), 10);
    Assert.assertEquals(m1.getType(), BACKUP_KEEP_ALIVE);
    Assert.assertNull(m1.getFrom());
    Assert.assertEquals(m1.getTimestamp(), 0);
    Assert.assertEquals(m1.getData().length + 1, m1.getSendData().length);


    Discover.BackupMessage backupMessage = Discover.BackupMessage.newBuilder()
            .setFlag(true).setPriority(10).build();
    KeepAliveMessage m2 = new KeepAliveMessage(backupMessage.toByteArray());
    Assert.assertTrue(m2.getFlag());
    Assert.assertEquals(m2.getPriority(), 10);
    Assert.assertEquals(m2.getType(), BACKUP_KEEP_ALIVE);

    Assert.assertArrayEquals(m2.getMessageId().getBytes(), m1.getMessageId().getBytes());
  }
}
