package org.tron.core.net.message.adv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.overlay.message.Message;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

public class BlockMessageTest {

  private static Block sampleBlock() {
    return Block.newBuilder().setBlockHeader(BlockHeader.newBuilder()
        .setRawData(BlockHeader.raw.newBuilder().setNumber(100).setTimestamp(123456789L))
        .setWitnessSignature(ByteString.copyFrom(new byte[] {0x0c})))
        .build();
  }

  @Test
  public void trailingEndGroupIsRejectedWithEitherFilterSetting() {
    byte[] canonical = sampleBlock().toByteArray();
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        // Also reject an END_GROUP followed by more bytes, not just a final 0x0c byte.
        for (byte[] suffix : new byte[][] {{0x0c}, {0x0c, 0x08, 0x01}}) {
          byte[] malformed = Arrays.copyOf(canonical, canonical.length + suffix.length);
          System.arraycopy(suffix, 0, malformed, canonical.length, suffix.length);
          assertThrows(InvalidProtocolBufferException.class, () -> new BlockMessage(malformed));
        }
      }
    }
  }

  @Test
  public void endGroupByteInsideABytesFieldIsAccepted() throws Exception {
    Block block = sampleBlock();
    byte[] data = block.toByteArray();
    assertEquals(0x0c, data[data.length - 1]);
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages = mockStatic(Message.class, CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        assertEquals(block, new BlockMessage(data).getBlockCapsule().getInstance());
      }
    }
  }

}
