package org.tron.core.witness;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.plaf.synth.SynthTabbedPaneUI;
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
  private static String dbPath = "output_witness_controller_test";

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

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(19000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1);

    assertEquals(4, dbManager.getWitnessController().getAbSlotAtTime(21000));
    assertEquals(1, dbManager.getWitnessController().getSlotAtTime(21000));
    assertEquals(3, dbManager.getWitnessController().getHeadSlot());

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
