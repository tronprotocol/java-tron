package org.tron.core.witness;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;

public class WitnessControllerTest extends BaseTest {

  @Resource
  private DposSlot dposSlot;


  static {
    dbPath = "output_witness_controller_test";
    Args.setParam(new String[]{"-d", dbPath}, Constant.TEST_CONF);
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
