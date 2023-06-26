package org.tron.core.net.services;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ReflectUtils;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.net.message.adv.BlockMessage;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.p2p.P2pEventHandler;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Inventory.InventoryType;

public class AdvServiceTest extends BaseTest {

  @Resource
  private AdvService service;
  @Resource
  private PeerConnection peer;

  /**
   * init context.
   */
  @BeforeClass
  public static void init() {
    dbPath = "output-adv-service-test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);
  }

  @Test
  public void test() {
    testAddInv();
    testBroadcast();
    testTrxBroadcast();
  }

  private void testAddInv() {
    boolean flag;
    Item itemTrx = new Item(Sha256Hash.ZERO_HASH, InventoryType.TRX);
    flag = service.addInv(itemTrx);
    Assert.assertTrue(flag);
    flag = service.addInv(itemTrx);
    Assert.assertFalse(flag);

    Item itemBlock = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertTrue(flag);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    service.addInvToCache(itemBlock);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);
  }

  private void testBroadcast() {

    try {
      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(P2pEventHandler.class, "peers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.broadcast(msg);
      Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
      Assert.assertNotNull(service.getMessage(item));
      peer.checkAndPutAdvInvRequest(item, System.currentTimeMillis());
      boolean res = peer.checkAndPutAdvInvRequest(item, System.currentTimeMillis());
      Assert.assertFalse(res);
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }


  private void testTrxBroadcast() {
    Protocol.Transaction trx = Protocol.Transaction.newBuilder().build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg = new TransactionMessage(trx);
    service.broadcast(msg);
    Item item = new Item(msg.getMessageId(), InventoryType.TRX);
    Assert.assertNotNull(service.getMessage(item));
  }

}
