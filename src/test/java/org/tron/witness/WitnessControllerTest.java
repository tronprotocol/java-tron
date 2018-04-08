package org.tron.witness;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.Test;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.witness.WitnessController;

public class WitnessControllerTest {

  ByteString blank = ByteString.copyFrom(new byte[1]);

  @Test
  public void test() {

    WitnessController controller = new WitnessController() {

      BlockCapsule genesisBlock = new BlockCapsule(0L, blank, 1522847871000L, blank);
      BlockCapsule headBlock = new BlockCapsule(1L, blank, 1522847890000L, blank);

      @Override
      public BlockCapsule getHead() {
        return headBlock;
      }

      @Override
      public BlockCapsule getGenesisBlock() {
        return genesisBlock;
      }

      @Override
      public boolean lastHeadBlockIsMaintenance() {
        return false;
      }
    };
    assertEquals(1522847891000L, controller.getSlotTime(1L));
    assertEquals(4, controller.getAbSlotAtTime(1522847891000L));
    assertEquals(1, controller.getSlotAtTime(1522847891000L));
    assertEquals(3, controller.getHeadSlot());

    List<WitnessCapsule> list = Lists.newArrayList();
    ByteString a = ByteString.copyFrom("1", Charset.defaultCharset());
    ByteString b = ByteString.copyFrom("2", Charset.defaultCharset());
//    ByteString b = ByteString.copyFrom(new byte[1]);

    WitnessCapsule witnessCapsule1 = new WitnessCapsule(a);
    WitnessCapsule witnessCapsule2 = new WitnessCapsule(b);
    list.add(witnessCapsule1);
    list.add(witnessCapsule2);
    controller.setShuffledWitnessStates(list);

    assertEquals(a, controller.getScheduledWitness(1));

  }


}
