package org.tron.core.pbft;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.ByteArray;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.PBFTMessage;

public class PbftTest {

  @Test
  public void pbftChecksEndTagWithEitherFilterSetting() throws Exception {
    PBFTMessage message = PBFTMessage.newBuilder()
        .setRawData(PBFTMessage.Raw.newBuilder().setViewN(1))
        .setSignature(ByteString.copyFrom(new byte[] {0x0c})).build();
    byte[] canonical = message.toByteArray();
    Assert.assertEquals(0x0c, canonical[canonical.length - 1]);
    for (boolean filter : new boolean[] {false, true}) {
      try (MockedStatic<Message> messages =
          Mockito.mockStatic(Message.class, Mockito.CALLS_REAL_METHODS)) {
        messages.when(Message::isFilter).thenReturn(filter);
        Assert.assertEquals(message, new PbftMessage(canonical).getPbftMessage());
        for (byte[] suffix : new byte[][] {{0x0c}, {0x14}, {0x0c, 0x08, 0x01}}) {
          byte[] malformed = Arrays.copyOf(canonical, canonical.length + suffix.length);
          System.arraycopy(suffix, 0, malformed, canonical.length, suffix.length);
          Assert.assertThrows(InvalidProtocolBufferException.class,
              () -> new PbftMessage(malformed));
        }
      }
    }
  }

  @Test
  public void testPbftSrMessage() {
    byte[] pk = ByteArray.fromHexString("41f08012b4881c320eb40b80f1228731898824e09d");
    BlockCapsule blockCapsule = new BlockCapsule(Block.getDefaultInstance());
    List<ByteString> srList = new ArrayList<>();
    Param param = Param.getInstance();
    List<Miner> minerList = new ArrayList<>();
    Param.Miner miner = param.new Miner(pk, null, null);
    minerList.add(miner);
    param.setMiners(minerList);
    srList.add(
        ByteString.copyFrom(ByteArray.fromHexString("41f08012b4881c320eb40b80f1228731898824e09d")));
    srList.add(
        ByteString.copyFrom(ByteArray.fromHexString("41df309fef25b311e7895562bd9e11aab2a58816d2")));
    PbftMessage pbftSrMessage = PbftMessage
        .prePrepareSRLMsg(blockCapsule, srList, 1, miner);
    PbftMessage.fullNodePrePrepareSRLMsg(blockCapsule, srList, 1);
    org.junit.Assert.assertNotNull(pbftSrMessage);
  }

}
