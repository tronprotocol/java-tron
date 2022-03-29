package org.tron.core.net.services;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.BlockMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.RelayService;
import org.tron.protos.Protocol;

public class RelayServiceTest {

  protected TronApplicationContext context;
  private RelayService service;
  private ChainBaseManager chainBaseManager;
  private PeerConnection peer;
  private SyncPool syncPool;
  private String dbPath = "output-relay-service-test";

  /**
   * init context.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
            Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    service = context.getBean(RelayService.class);
    chainBaseManager = context.getBean(ChainBaseManager.class);
  }

  /**
   * destroy.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void test() throws Exception {
    testGetSortedScheduleWitness();
    testGetNextWitnesses();
    testBroadcast();
  }

  private void testGetSortedScheduleWitness() throws Exception {
    Method method = service.getClass().getDeclaredMethod("getSortedScheduleWitness");
    method.setAccessible(true);
    List<ByteString> list = new ArrayList<>();
    List<WitnessCapsule> witnesses = chainBaseManager.getWitnessStore().getAllWitnesses();
    witnesses.forEach(witness -> list.add(witness.getAddress()));
    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(list);
    chainBaseManager.getDynamicPropertiesStore().saveNextMaintenanceTime(10);
    List<ByteString> l1 = (List<ByteString>) method.invoke(service);
    Assert.assertEquals(11, l1.size());
    assertEquals(l1.get(0), "A0299F3DB80A24B20A254B89CE639D59132F157F13");
    assertEquals(l1.get(10), "A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");

    byte[] key = Hex.decode("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");
    WitnessCapsule witness = chainBaseManager.getWitnessStore().get(key);
    witness.setVoteCount(1000);
    chainBaseManager.getWitnessStore().put(key, witness);
    chainBaseManager.getDynamicPropertiesStore().saveNextMaintenanceTime(20);
    List<ByteString> l2 = (List<ByteString>) method.invoke(service);
    assertEquals(l2.get(0), "A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");
    assertEquals(l2.get(1), "A0299F3DB80A24B20A254B89CE639D59132F157F13");
  }

  public void testGetNextWitnesses() throws Exception {
    Method method = service.getClass().getDeclaredMethod(
            "getNextWitnesses", ByteString.class, Integer.class);
    method.setAccessible(true);
    List<ByteString> l1 = (List<ByteString>) method.invoke(
            service, getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F"), 3);
    Assert.assertEquals(l1.size(), 3);
    assertEquals(l1.get(0), "A0299F3DB80A24B20A254B89CE639D59132F157F13");
    assertEquals(l1.get(1), "A0807337F180B62A77576377C1D0C9C24DF5C0DD62");
    assertEquals(l1.get(2), "A05430A3F089154E9E182DDD6FE136A62321AF22A7");

    List<ByteString> l2 = (List<ByteString>) method.invoke(
            service, getFromHexString("A0FAB5FBF6AFB681E4E37E9D33BDDB7E923D6132E5"), 3);
    Assert.assertEquals(l2.size(), 3);
    assertEquals(l2.get(0), "A014EEBE4D30A6ACB505C8B00B218BDC4733433C68");
    assertEquals(l2.get(1), "A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");
    assertEquals(l2.get(2), "A0299F3DB80A24B20A254B89CE639D59132F157F13");

    List<ByteString> l3 = (List<ByteString>) method.invoke(
            service, getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F"), 1);
    Assert.assertEquals(l3.size(), 1);
    assertEquals(l3.get(0), "A0299F3DB80A24B20A254B89CE639D59132F157F13");
  }

  private void testBroadcast() {
    try {
      peer = context.getBean(PeerConnection.class);
      peer.setAddress(getFromHexString("A0299F3DB80A24B20A254B89CE639D59132F157F13"));
      peer.setNeedSyncFromPeer(false);
      peer.setNeedSyncFromUs(false);
      syncPool = context.getBean(SyncPool.class);

      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(syncPool, "activePeers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(chainBaseManager.getHeadBlockNum() + 1,
              chainBaseManager.getHeadBlockId(),
              0, getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F"));
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.broadcast(msg);
      Item item = new Item(blockCapsule.getBlockId(), Protocol.Inventory.InventoryType.BLOCK);
      Assert.assertEquals(1, peer.getAdvInvSpread().size());
      Assert.assertNotNull(peer.getAdvInvSpread().getIfPresent(item));
      peer.close();
      syncPool.close();
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }

  private void assertEquals(ByteString byteString, String string) {
    Assert.assertEquals(byteString, ByteString.copyFrom(Hex.decode(string)));
  }

  private ByteString getFromHexString(String s) {
    return ByteString.copyFrom(Hex.decode(s));
  }

}
