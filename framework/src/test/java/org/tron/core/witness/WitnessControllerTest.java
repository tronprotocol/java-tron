package org.tron.core.witness;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

public class WitnessControllerTest {

  private static Manager dbManager = new Manager();
  private static DposSlot dposSlot;
  private static ChainBaseManager chainBaseManager;

  private static TronApplicationContext context;
  private static String dbPath = "output_witness_controller_test";

  static {
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
  }

  ByteString blank = ByteString.copyFrom(new byte[1]);

  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);

    dposSlot = context.getBean(DposSlot.class);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testSlot() {

    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(19000);
    chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1);

  }

  //  @Test
  public void testWitnessSchedule() {

    // no witness produce block
    assertEquals(0, chainBaseManager.getHeadBlockNum());

    // test witnesses in genesis block
    assertEquals(
        "a0904fe896536f4bebc64c95326b5054a2c3d27df6", // first(current witness)
        ByteArray.toHexString(
            (dposSlot.getScheduledWitness(0).toByteArray())));
    assertEquals(
        "a0904fe896536f4bebc64c95326b5054a2c3d27df6",
        ByteArray.toHexString(
            (dposSlot.getScheduledWitness(5).toByteArray())));
    assertEquals(
        "a0807337f180b62a77576377c1d0c9c24df5c0dd62", // second(next witness)
        ByteArray.toHexString(
            (dposSlot.getScheduledWitness(6).toByteArray())));
    assertEquals(
        "a0807337f180b62a77576377c1d0c9c24df5c0dd62",
        ByteArray.toHexString(
            (dposSlot.getScheduledWitness(11).toByteArray())));
    assertEquals(
        "a05430a3f089154e9e182ddd6fe136a62321af22a7", // third
        ByteArray.toHexString(
            (dposSlot.getScheduledWitness(12).toByteArray())));

    // test maintenance
    ByteString a =
        ByteString.copyFrom(ByteArray.fromHexString("a0ec6525979a351a54fa09fea64beb4cce33ffbb7a"));
    ByteString b =
        ByteString.copyFrom(ByteArray.fromHexString("a0fab5fbf6afb681e4e37e9d33bddb7e923d6132e5"));
    // system.out.print("a address:" + ByteArray.toHexString(a.toByteArray()) + "\n");
    // System.out.print("b address:" + ByteArray.toHexString(b.toByteArray()));
    List<ByteString> w = new ArrayList<>();
    w.add(a);
    w.add(b);

    // update active witness
    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(w);
    // now 2 active witnesses
    assertEquals(2, chainBaseManager.getWitnessScheduleStore().getActiveWitnesses().size());

    // update shuffled witness
    chainBaseManager.getWitnessScheduleStore().saveCurrentShuffledWitnesses(w);

    assertEquals(a, dposSlot.getScheduledWitness(1));
    assertEquals(b, dposSlot.getScheduledWitness(2));
    assertEquals(a, dposSlot.getScheduledWitness(3));
    assertEquals(b, dposSlot.getScheduledWitness(4));
  }
}
