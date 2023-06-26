package org.tron.core.net.services;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.relay.RelayService;
import org.tron.protos.Protocol;

public class RelayServiceTest extends BaseTest {

  @Resource
  private RelayService service;
  @Resource
  private PeerConnection peer;
  @Resource
  private P2pEventHandlerImpl p2pEventHandler;

  /**
   * init context.
   */
  @BeforeClass
  public static void init() {
    dbPath = "output-relay-service-test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
            Constant.TEST_CONF);
  }

  @Test
  public void test() throws Exception {
    initWitness();
    testGetNextWitnesses();
    testBroadcast();
  }

  private void initWitness() {
    byte[] key = Hex.decode("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");
    WitnessCapsule witnessCapsule = chainBaseManager.getWitnessStore().get(key);
    witnessCapsule.setVoteCount(1000);
    chainBaseManager.getWitnessStore().put(key, witnessCapsule);
    List<ByteString> list = new ArrayList<>();
    List<WitnessCapsule> witnesses = chainBaseManager.getWitnessStore().getAllWitnesses();
    witnesses.sort(Comparator.comparingLong(w -> -w.getVoteCount()));
    witnesses.forEach(witness -> list.add(witness.getAddress()));
    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(list);
  }

  public void testGetNextWitnesses() throws Exception {
    Method method = service.getClass().getDeclaredMethod(
            "getNextWitnesses", ByteString.class, Integer.class);
    method.setAccessible(true);
    Set<ByteString> s1 = (Set<ByteString>) method.invoke(
            service, getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F"), 3);
    Assert.assertEquals(3, s1.size());
    assertContains(s1, "A0299F3DB80A24B20A254B89CE639D59132F157F13");
    assertContains(s1, "A0807337F180B62A77576377C1D0C9C24DF5C0DD62");
    assertContains(s1, "A05430A3F089154E9E182DDD6FE136A62321AF22A7");

    Set<ByteString> s2 = (Set<ByteString>) method.invoke(
            service, getFromHexString("A0FAB5FBF6AFB681E4E37E9D33BDDB7E923D6132E5"), 3);
    Assert.assertEquals(3, s2.size());
    assertContains(s2, "A014EEBE4D30A6ACB505C8B00B218BDC4733433C68");
    assertContains(s2, "A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");
    assertContains(s2, "A0299F3DB80A24B20A254B89CE639D59132F157F13");

    Set<ByteString> s3 = (Set<ByteString>) method.invoke(
            service, getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F"), 1);
    Assert.assertEquals(1, s3.size());
    assertContains(s3, "A0299F3DB80A24B20A254B89CE639D59132F157F13");
  }

  private void testBroadcast() {
    try {
      peer.setAddress(getFromHexString("A0299F3DB80A24B20A254B89CE639D59132F157F13"));
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(false);

      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(p2pEventHandler, "activePeers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(chainBaseManager.getHeadBlockNum() + 1,
              chainBaseManager.getHeadBlockId(),
              0, getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F"));
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.broadcast(msg);
      Item item = new Item(blockCapsule.getBlockId(), Protocol.Inventory.InventoryType.BLOCK);
      Assert.assertEquals(1, peer.getAdvInvSpread().size());
      Assert.assertNotNull(peer.getAdvInvSpread().getIfPresent(item));
      peer.getChannel().close();
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }

  private void assertContains(Set<ByteString> set, String string) {
    ByteString bytes = getFromHexString(string);
    Assert.assertTrue(set.contains(bytes));
  }

  private ByteString getFromHexString(String s) {
    return ByteString.copyFrom(Hex.decode(s));
  }

}
