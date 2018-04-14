package org.tron.core.witness;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.config.args.Args;

public class WitnessControllerTest {
  private static Manager dbManager = new Manager();
  private static String dbPath = "output_manager_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath, "-w"},
        Constant.TEST_CONF);

    dbManager.init();
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    dbManager.destory();
  }

  ByteString blank = ByteString.copyFrom(new byte[1]);

  @Test
  public void testSlot() {

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

  }

  @Test
  public void testWitnessSchedule() {

    // no witness produce block
    assertEquals(0,dbManager.getHeadBlockNum());

    // the second witness in sorted active witnesses
    assertEquals("a0807337f180b62a77576377c1d0c9c24df5c0dd62",
        ByteArray.toHexString((dbManager.getWitnessController().getScheduledWitness(1).toByteArray())));

    // test maintenance
    ByteString a = ByteString.copyFrom(ByteArray.fromHexString("a0ec6525979a351a54fa09fea64beb4cce33ffbb7a"));
    ByteString b = ByteString.copyFrom(ByteArray.fromHexString("a0fab5fbf6afb681e4e37e9d33bddb7e923d6132e5"));
    //system.out.print("a address:" + ByteArray.toHexString(a.toByteArray()) + "\n");
    //System.out.print("b address:" + ByteArray.toHexString(b.toByteArray()));
    List<ByteString> w = new ArrayList<>();
    w.add(a);
    w.add(b);

    // update active witness
    dbManager.getWitnessScheduleStore().saveActiveWitnesses(w);
    // now 2 active witnesses
    assertEquals(2,dbManager.getWitnessScheduleStore().getActiveWitnesses().size());

    // update shuffled witness
    dbManager.getWitnessScheduleStore().saveCurrentShuffledWitnesses(w);

    assertEquals(b,dbManager.getWitnessController().getScheduledWitness(1));
    assertEquals(a,dbManager.getWitnessController().getScheduledWitness(2));
    assertEquals(b,dbManager.getWitnessController().getScheduledWitness(3));
    assertEquals(a,dbManager.getWitnessController().getScheduledWitness(4));
  }


}
