package org.tron.core.net.node;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ConcurrentHashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.overlay.client.PeerClient;
import org.tron.common.overlay.server.SyncPool;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.core.config.Parameter.NetConstants;
import org.tron.core.net.peer.PeerConnection;

public class NodeImplTest {

  private static NodeImpl p2pNode;

  @BeforeClass
  public static void init() {
    p2pNode = new NodeImpl();
  }

  @Test
  public void testDisconnectInactive() {
    // generate test data
    ConcurrentHashMap<Sha256Hash, Long> advObjWeRequested1 = new ConcurrentHashMap<>();
    ConcurrentHashMap<Sha256Hash, Long> advObjWeRequested2 = new ConcurrentHashMap<>();
    ConcurrentHashMap<Sha256Hash, Long> advObjWeRequested3 = new ConcurrentHashMap<>();
    ConcurrentHashMap<BlockId, Long> syncBlockRequested1 = new ConcurrentHashMap<>();
    ConcurrentHashMap<BlockId, Long> syncBlockRequested2 = new ConcurrentHashMap<>();
    ConcurrentHashMap<BlockId, Long> syncBlockRequested3 = new ConcurrentHashMap<>();

    advObjWeRequested1.put(new Sha256Hash(1, Sha256Hash.ZERO_HASH),
        System.currentTimeMillis() - NetConstants.ADV_TIME_OUT);
    syncBlockRequested1.put(new BlockId(),
        System.currentTimeMillis());
    advObjWeRequested2.put(new Sha256Hash(1, Sha256Hash.ZERO_HASH),
        System.currentTimeMillis());
    syncBlockRequested2.put(new BlockId(),
        System.currentTimeMillis() - NetConstants.SYNC_TIME_OUT);
    advObjWeRequested3.put(new Sha256Hash(1, Sha256Hash.ZERO_HASH),
        System.currentTimeMillis());
    syncBlockRequested3.put(new BlockId(),
        System.currentTimeMillis());

    PeerConnection peer1 = new PeerConnection();
    PeerConnection peer2 = new PeerConnection();
    PeerConnection peer3 = new PeerConnection();

    peer1.setAdvObjWeRequested(advObjWeRequested1);
    peer1.setSyncBlockRequested(syncBlockRequested1);
    peer2.setAdvObjWeRequested(advObjWeRequested2);
    peer2.setSyncBlockRequested(syncBlockRequested2);
    peer3.setAdvObjWeRequested(advObjWeRequested3);
    peer3.setSyncBlockRequested(syncBlockRequested3);

    // fetch failed
    SyncPool pool = new SyncPool(new PeerClient());
    pool.addActivePeers(peer1);
    p2pNode.setPool(pool);
    try {
      p2pNode.disconnectInactive();
      fail("disconnectInactive failed");
    } catch (RuntimeException e) {
      assertTrue("disconnect successfully, reason is fetch failed", true);
    }

    // sync failed
    pool = new SyncPool(new PeerClient());
    pool.addActivePeers(peer2);
    p2pNode.setPool(pool);
    try {
      p2pNode.disconnectInactive();
      fail("disconnectInactive failed");
    } catch (RuntimeException e) {
      assertTrue("disconnect successfully, reason is sync failed", true);
    }

    // should not disconnect
    pool = new SyncPool(new PeerClient());
    pool.addActivePeers(peer3);
    p2pNode.setPool(pool);
    try {
      p2pNode.disconnectInactive();
      assertTrue("not disconnect", true);
    } catch (RuntimeException e) {
      fail("should not disconnect!");
    }
  }
}
