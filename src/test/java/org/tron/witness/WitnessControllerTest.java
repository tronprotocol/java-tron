package org.tron.witness;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.witness.WitnessController;

@Ignore
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

    ByteString a = ByteString.copyFrom("1", Charset.defaultCharset());
    ByteString b = ByteString.copyFrom("2", Charset.defaultCharset());
    WitnessCapsule witnessCapsule1 = new WitnessCapsule(a);
    WitnessCapsule witnessCapsule2 = new WitnessCapsule(b);

    controller.setShuffledWitnessStates(Lists.newArrayList(Arrays.asList(
        witnessCapsule1, witnessCapsule2
    )));

    assertEquals(a, controller.getScheduledWitness(1));

  }


}
