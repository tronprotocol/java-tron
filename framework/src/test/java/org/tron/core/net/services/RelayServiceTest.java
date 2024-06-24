package org.tron.core.net.services;

import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.tron.common.BaseTest;
import org.tron.common.utils.ReflectUtils;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.net.P2pEventHandlerImpl;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.handshake.HelloMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerManager;
import org.tron.core.net.service.relay.RelayService;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.discover.Node;
import org.tron.p2p.utils.NetUtil;
import org.tron.protos.Protocol;

@Slf4j(topic = "net")
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
    Args.setParam(new String[]{"--output-directory", dbPath(), "--debug"},
            Constant.TEST_CONF);
  }

  @Test
  public void test() throws Exception {
    initWitness();
    testGetNextWitnesses();
    testBroadcast();
    testCheckHelloMessage();
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

  private void testCheckHelloMessage() {
    ByteString address = getFromHexString("A04711BF7AFBDF44557DEFBDF4C4E7AA6138C6331F");
    InetSocketAddress a1 = new InetSocketAddress("127.0.0.1", 10001);
    Node node = new Node(NetUtil.getNodeId(), a1.getAddress().getHostAddress(),
        null, a1.getPort());
    HelloMessage helloMessage = new HelloMessage(node, System.currentTimeMillis(),
        ChainBaseManager.getChainBaseManager());
    helloMessage.setHelloMessage(helloMessage.getHelloMessage().toBuilder()
        .setAddress(address).build());
    Channel c1 = mock(Channel.class);
    Mockito.when(c1.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c1.getInetAddress()).thenReturn(a1.getAddress());
    Channel c2 = mock(Channel.class);
    Mockito.when(c2.getInetSocketAddress()).thenReturn(a1);
    Mockito.when(c2.getInetAddress()).thenReturn(a1.getAddress());
    Args.getInstance().fastForward = true;
    ApplicationContext ctx = (ApplicationContext) ReflectUtils.getFieldObject(p2pEventHandler,
        "ctx");
    PeerConnection peer1 = PeerManager.add(ctx, c1);
    assert peer1 != null;
    peer1.setAddress(address);
    PeerConnection peer2 = PeerManager.add(ctx, c2);
    assert peer2 != null;
    peer2.setAddress(address);
    try {
      Field field = service.getClass().getDeclaredField("witnessScheduleStore");
      field.setAccessible(true);
      field.set(service, chainBaseManager.getWitnessScheduleStore());
      boolean res = service.checkHelloMessage(helloMessage, c1);
      Assert.assertFalse(res);
    } catch (Exception e) {
      logger.info("{}", e.getMessage());
    }
  }
}